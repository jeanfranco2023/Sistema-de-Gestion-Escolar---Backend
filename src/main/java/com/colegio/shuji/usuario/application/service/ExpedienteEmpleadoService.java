package com.colegio.shuji.usuario.application.service;

import com.colegio.shuji.shared.application.port.out.AuditContextPort;
import com.colegio.shuji.shared.domain.exception.ResourceNotFoundException;
import com.colegio.shuji.usuario.application.dto.out.ExpedienteEmpleadoDocumentoResponseDto;
import com.colegio.shuji.usuario.application.port.in.ExpedienteEmpleadoUseCase;
import com.colegio.shuji.usuario.application.port.out.ExpedienteEmpleadoRepositoryPort;
import com.colegio.shuji.usuario.application.port.out.ExpedienteEmpleadoStoragePort;
import com.colegio.shuji.usuario.application.port.out.UserRepositoryPort;
import com.colegio.shuji.usuario.application.port.out.ValidadorDocumentoEmpleadoPort;
import com.colegio.shuji.usuario.domain.enums.TipoDocumentoEmpleado;
import com.colegio.shuji.usuario.domain.exception.ReglaNegocioException;
import com.colegio.shuji.usuario.domain.model.ExpedienteEmpleadoDocumento;
import com.colegio.shuji.usuario.domain.model.ResultadoValidacionDocumentoEmpleado;
import com.colegio.shuji.usuario.domain.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@Transactional
public class ExpedienteEmpleadoService implements ExpedienteEmpleadoUseCase {

  private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

  private final ExpedienteEmpleadoRepositoryPort expedienteRepo;
  private final ExpedienteEmpleadoStoragePort expedienteStorage;
  private final ValidadorDocumentoEmpleadoPort validadorDocumento;
  private final UserRepositoryPort userRepositoryPort;
  private final AuditContextPort auditoria;
  private final ObjectMapper json;

  public ExpedienteEmpleadoService(
      ExpedienteEmpleadoRepositoryPort expedienteRepo,
      ExpedienteEmpleadoStoragePort expedienteStorage,
      ValidadorDocumentoEmpleadoPort validadorDocumento,
      UserRepositoryPort userRepositoryPort,
      AuditContextPort auditoria,
      ObjectMapper json) {
    this.expedienteRepo = expedienteRepo;
    this.expedienteStorage = expedienteStorage;
    this.validadorDocumento = validadorDocumento;
    this.userRepositoryPort = userRepositoryPort;
    this.auditoria = auditoria;
    this.json = json;
  }

  @Override
  public ExpedienteEmpleadoDocumentoResponseDto subirYValidarDocumento(
      Long usuarioId, TipoDocumentoEmpleado tipoDocumento, String nombreArchivo, String tipoContenido, byte[] bytes) {
    log.info("Carga de documento de expediente {} para usuario ID: {}", tipoDocumento, usuarioId);

    if (bytes == null || bytes.length == 0) {
      throw new ReglaNegocioException("Debe proporcionar un archivo PDF no vacío", "ARCHIVO_VACIO");
    }

    if (tipoDocumento == null) {
      throw new ReglaNegocioException("El tipo de documento es obligatorio", "TIPO_DOCUMENTO_REQUERIDO");
    }

    String originalFilename = nombreArchivo;
    if (originalFilename == null || originalFilename.isBlank()) originalFilename = "documento.pdf";
    originalFilename = originalFilename.replace((char) 92, '/');
    originalFilename = originalFilename.substring(originalFilename.lastIndexOf('/') + 1);
    if (originalFilename.length() > 200) {
      throw new ReglaNegocioException("El nombre del archivo no puede exceder los 200 caracteres", "NOMBRE_ARCHIVO_EXCEDIDO");
    }
    String contentType = tipoContenido != null ? tipoContenido : "application/pdf";

    if (!"application/pdf".equalsIgnoreCase(contentType)
        || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
      throw new ReglaNegocioException("Solo se admiten documentos en formato PDF", "FORMATO_INVALIDO");
    }

    if (bytes.length > MAX_FILE_SIZE_BYTES) {
      throw new ReglaNegocioException("El tamaño del archivo no puede exceder los 10 MB", "TAMANO_EXCEDIDO");
    }

    Usuario usuario =
        userRepositoryPort
            .findById(usuarioId)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

    auditoria.syncCurrentUserFromSecurityContext();

    ResultadoValidacionDocumentoEmpleado dictamen = validadorDocumento.validar(tipoDocumento, bytes, usuario);
    if (dictamen == null || dictamen.estado() == null) {
      throw new ReglaNegocioException("No se pudo completar la validación documental", "VALIDACION_NO_DISPONIBLE");
    }
    String metadatosJson = construirMetadatos(bytes.length, dictamen, tipoDocumento);
    String rutaObjeto = "expedientes/" + usuarioId + "/" + tipoDocumento.name().toLowerCase(Locale.ROOT)
        + "/" + UUID.randomUUID() + ".pdf";
    var ahora = OffsetDateTime.now(ZoneOffset.UTC);

    var documento = new ExpedienteEmpleadoDocumento(
        null,
        usuarioId,
        tipoDocumento,
        originalFilename,
        rutaObjeto,
        "application/pdf",
        (long) bytes.length,
        dictamen.estado(),
        dictamen.observacion(),
        metadatosJson,
        ahora,
        ahora);

    expedienteStorage.guardar(rutaObjeto, bytes);
    ExpedienteEmpleadoDocumento guardado;
    try {
      guardado = expedienteRepo.guardar(documento);
    } catch (RuntimeException ex) {
      eliminarObjeto(rutaObjeto);
      throw ex;
    }
    registrarLimpiezaSiRollback(rutaObjeto);
    log.info(
        "Documento {} registrado para usuario ID: {} con estado de validación: {}",
        tipoDocumento,
        usuarioId,
        dictamen.estado());

    return toDto(guardado);
  }

  @Override
  @Transactional(readOnly = true)
  public List<ExpedienteEmpleadoDocumentoResponseDto> listarDocumentos(Long usuarioId) {
    log.debug("Listando documentos del expediente para usuario ID: {}", usuarioId);
    return expedienteRepo.listarPorUsuarioId(usuarioId).stream()
        .map(this::toDto)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public String urlDescargaDocumento(Long usuarioId, Long documentoId) {
    ExpedienteEmpleadoDocumento documento = expedienteRepo.buscarPorIdYUsuario(documentoId, usuarioId)
        .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado en el expediente del empleado"));
    return expedienteStorage.urlFirmada(documento.rutaObjeto());
  }

  private void registrarLimpiezaSiRollback(String rutaObjeto) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
    try {
      TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
        @Override
        public void afterCompletion(int status) {
          if (status != TransactionSynchronization.STATUS_COMMITTED) eliminarObjeto(rutaObjeto);
        }
      });
    } catch (RuntimeException ex) {
      eliminarObjeto(rutaObjeto);
      throw ex;
    }
  }

  private void eliminarObjeto(String rutaObjeto) {
    try {
      expedienteStorage.eliminar(rutaObjeto);
    } catch (RuntimeException cleanupError) {
      log.error("No se pudo limpiar un archivo de expediente luego de fallar la transacción.", cleanupError);
    }
  }
  private String construirMetadatos(
      int tamanoBytes, ResultadoValidacionDocumentoEmpleado dictamen, TipoDocumentoEmpleado tipo) {
    try {
      Map<String, Object> meta = Map.of(
          "paginas", dictamen.paginas(),
          "tamanoBytes", tamanoBytes,
          "tipoDocumento", tipo.name(),
          "identidadCoincide", dictamen.identidadCoincide(),
          "motorValidacion", dictamen.motorValidacion(),
          "fechaValidacion", OffsetDateTime.now(ZoneOffset.UTC).toString()
      );
      return json.writeValueAsString(meta);
    } catch (Exception e) {
      return "{}";
    }
  }

  private ExpedienteEmpleadoDocumentoResponseDto toDto(ExpedienteEmpleadoDocumento entity) {
    return new ExpedienteEmpleadoDocumentoResponseDto(
        entity.id(),
        entity.usuarioId(),
        entity.tipoDocumento(),
        entity.nombreArchivo(),
        entity.rutaObjeto(),
        entity.tipoContenido(),
        entity.tamanoBytes(),
        entity.estadoIa(),
        entity.observacionesIa(),
        entity.metadatosIa(),
        entity.fechaCreacion(),
        entity.fechaValidacionIa()
    );
  }
}
