package com.colegio.shuji.asistencia.application.service;

import com.colegio.shuji.asistencia.application.dto.in.ImportarLoteBiometricoRequestDto;
import com.colegio.shuji.asistencia.application.dto.out.ImportacionBiometricaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.LoteBiometricoResponseDto;
import com.colegio.shuji.asistencia.domain.enums.EstadoImportacionBiometrica;
import com.colegio.shuji.asistencia.infrastructure.adapter.RedisColaBiometrica;
import com.colegio.shuji.asistencia.infrastructure.entity.ImportacionBiometricaEntity;
import com.colegio.shuji.asistencia.infrastructure.repository.ImportacionBiometricaPendienteView;
import com.colegio.shuji.asistencia.infrastructure.repository.ImportacionBiometricaStatusView;
import com.colegio.shuji.asistencia.infrastructure.repository.JpaImportacionBiometricaRepository;
import com.colegio.shuji.config.security.UserPrincipal;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportacionBiometricaService {
  private final JpaImportacionBiometricaRepository repository;
  private final ObjectMapper objectMapper;
  private final RedisColaBiometrica cola;
  private final com.colegio.shuji.asistencia.application.port.in.ProcesarBiometricoUseCase procesador;
  private final ActorActualPort actor;

  @Value("${app.biometrico.queue-enabled:false}")
  private boolean colaHabilitada;

  @Value("${app.biometrico.max-pending:20}")
  private int maxPendientes;

  @Value("${app.biometrico.stale-minutes:60}")
  private long minutosEstancada;

  @Transactional
  public ImportacionBiometricaResponseDto encolar(ImportarLoteBiometricoRequestDto request) {
    if (!colaHabilitada) return importarEnLinea(request);
    var principal = principalAutenticado();
    String hash = huellaPorUsuario(request, principal.getId());
    var existente = repository.findByHuellaContenido(hash);
    if (existente.isPresent()) {
      var job = existente.get();
      if (job.getEstado() != EstadoImportacionBiometrica.ERROR) return respuesta(job);
      job.setEstado(EstadoImportacionBiometrica.EN_COLA);
      job.setPayload(serializar(new BiometricoImportacionPayload(
          request, principal.getId(), principal.getUsername(), principal.getEmail(), rolesActuales())));
      job.setMarcasValidas(0);
      job.setMarcasErroneas(0);
      job.setMensajeError(null);
      job.setIniciadoEn(null);
      job.setFinalizadoEn(null);
      repository.saveAndFlush(job);
      encolarDespuesDelCommit(job.getId(), job.getUsuarioId());
      return respuesta(job);
    }

    var job = new ImportacionBiometricaEntity();
    job.setId(UUID.randomUUID());
    job.setUsuarioId(principal.getId());
    job.setHuellaContenido(hash);
    job.setNombreArchivo(request.nombreArchivo());
    job.setPayload(serializar(new BiometricoImportacionPayload(
        request, principal.getId(), principal.getUsername(), principal.getEmail(), rolesActuales())));
    job.setEstado(EstadoImportacionBiometrica.EN_COLA);
    job.setTotalFilas(request.marcas().size());
    job.setMarcasValidas(0);
    job.setMarcasErroneas(0);
    job = repository.saveAndFlush(job);
    encolarDespuesDelCommit(job.getId(), job.getUsuarioId());
    return respuesta(job);
  }

  @Transactional(readOnly = true)
  public ImportacionBiometricaResponseDto estado(UUID id) {
    ImportacionBiometricaStatusView estado = repository.buscarEstado(id, actor.usuarioId())
        .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
            org.springframework.http.HttpStatus.NOT_FOUND, "Importación no encontrada"));
    return respuesta(estado);
  }

  @Transactional(readOnly = true)
  public List<ImportacionBiometricaPendienteView> pendientes() {
    return repository.listarPorEstado(
        EstadoImportacionBiometrica.EN_COLA, PageRequest.of(0, maxPendientes));
  }

  @Transactional
  public boolean reclamar(UUID id) {
    return repository.reclamar(
        id,
        EstadoImportacionBiometrica.EN_COLA,
        EstadoImportacionBiometrica.PROCESANDO,
        OffsetDateTime.now(ZoneOffset.UTC)) == 1;
  }

  @Transactional
  public void devolverACola(UUID id) {
    repository.devolverACola(
        id, EstadoImportacionBiometrica.PROCESANDO, EstadoImportacionBiometrica.EN_COLA);
  }

  @Transactional
  public Optional<BiometricoImportacionPayload> payload(UUID id) {
    return repository.findById(id).map(job -> {
      try {
        return objectMapper.readValue(job.getPayload(), BiometricoImportacionPayload.class);
      } catch (Exception ex) {
        throw new IllegalStateException("No se pudo recuperar el trabajo biométrico", ex);
      }
    });
  }

  @Transactional
  public void completar(UUID id, LoteBiometricoResponseDto lote) {
    repository.findById(id).ifPresent(job -> {
      job.setEstado(EstadoImportacionBiometrica.COMPLETADO);
      job.setLoteId(lote.id());
      job.setMarcasValidas(lote.marcasValidas());
      job.setMarcasErroneas(lote.marcasErroneas());
      job.setPayload(null);
      job.setMensajeError(null);
      job.setFinalizadoEn(OffsetDateTime.now(ZoneOffset.UTC));
      repository.save(job);
    });
  }

  @Transactional
  public void fallar(UUID id) {
    repository.findById(id).ifPresent(job -> {
      job.setEstado(EstadoImportacionBiometrica.ERROR);
      job.setPayload(null);
      job.setMensajeError("No se pudo procesar el archivo. Vuelve a cargarlo o contacta al administrador.");
      job.setFinalizadoEn(OffsetDateTime.now(ZoneOffset.UTC));
      repository.save(job);
    });
    log.warn("Falló el procesamiento de la importación biométrica {}", id);
  }

  @Transactional
  public int recuperarEstancadas() {
    return repository.recuperarEstancadas(
        EstadoImportacionBiometrica.PROCESANDO,
        EstadoImportacionBiometrica.EN_COLA,
        OffsetDateTime.now(ZoneOffset.UTC).minusMinutes(minutosEstancada));
  }

  private ImportacionBiometricaResponseDto importarEnLinea(ImportarLoteBiometricoRequestDto request) {
    LoteBiometricoResponseDto lote = procesador.importar(request);
    return new ImportacionBiometricaResponseDto(
        UUID.randomUUID(), lote.nombreArchivo(), EstadoImportacionBiometrica.COMPLETADO,
        lote.totalFilas(), lote.marcasValidas(), lote.marcasErroneas(), lote.id(), null,
        lote.fechaCreacion(), lote.fechaCreacion(), lote.fechaCreacion());
  }

  private void encolarDespuesDelCommit(UUID jobId, Long usuarioId) {
    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCommit() {
        try {
          cola.encolar(usuarioId, jobId);
        } catch (RuntimeException ex) {
          log.warn("La importación {} quedó pendiente de despacho a la cola", jobId);
        }
      }
    });
  }

  private UserPrincipal principalAutenticado() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
      throw new org.springframework.security.access.AccessDeniedException("Se requiere autenticación");
    }
    return principal;
  }

  private List<String> rolesActuales() {
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) return List.of();
    return authentication.getAuthorities().stream().map(a -> a.getAuthority()).sorted().toList();
  }

  private byte[] serializar(BiometricoImportacionPayload payload) {
    try {
      return objectMapper.writeValueAsBytes(payload);
    } catch (JsonProcessingException ex) {
      throw new IllegalStateException("No se pudo guardar la importación en cola", ex);
    }
  }

  private String huellaPorUsuario(ImportarLoteBiometricoRequestDto request, Long usuarioId) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      digest.update((usuarioId + "|").getBytes(StandardCharsets.UTF_8));
      digest.update(HuellaImportacionBiometrica.calcular(request).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 no disponible", ex);
    }
  }

  private ImportacionBiometricaResponseDto respuesta(ImportacionBiometricaEntity job) {
    return new ImportacionBiometricaResponseDto(
        job.getId(), job.getNombreArchivo(), job.getEstado(), job.getTotalFilas(),
        job.getMarcasValidas(), job.getMarcasErroneas(), job.getLoteId(), job.getMensajeError(),
        job.getCreadoEn(), job.getIniciadoEn(), job.getFinalizadoEn());
  }

  private ImportacionBiometricaResponseDto respuesta(ImportacionBiometricaStatusView job) {
    return new ImportacionBiometricaResponseDto(
        job.getId(), job.getNombreArchivo(), job.getEstado(), job.getTotalFilas(),
        job.getMarcasValidas(), job.getMarcasErroneas(), job.getLoteId(), job.getMensajeError(),
        job.getCreadoEn(), job.getIniciadoEn(), job.getFinalizadoEn());
  }
}
