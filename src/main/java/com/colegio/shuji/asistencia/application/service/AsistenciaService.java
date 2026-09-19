package com.colegio.shuji.asistencia.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.asistencia.application.dto.in.ImportarLoteBiometricoRequestDto;
import com.colegio.shuji.asistencia.application.dto.in.JustificarInasistenciaRequestDto;
import com.colegio.shuji.asistencia.application.dto.in.RegistrarAsistenciaAulaRequestDto;
import com.colegio.shuji.asistencia.application.dto.out.AsistenciaAulaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.LoteBiometricoResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.ReporteAsistenciaDiariaResponseDto;
import com.colegio.shuji.asistencia.application.mapper.AsistenciaMapper;
import com.colegio.shuji.asistencia.application.port.in.ProcesarBiometricoUseCase;
import com.colegio.shuji.asistencia.application.port.in.RegistrarListaAulaUseCase;
import com.colegio.shuji.asistencia.application.port.out.AsistenciaAulaRepositoryPort;
import com.colegio.shuji.asistencia.application.port.out.BiometricoRepositoryPort;
import com.colegio.shuji.asistencia.application.port.out.MarcaPorteriaRepositoryPort;
import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import com.colegio.shuji.asistencia.domain.enums.EstadoMarca;
import com.colegio.shuji.asistencia.domain.model.LoteBiometrico;
import com.colegio.shuji.asistencia.domain.model.MarcaPorteria;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AsistenciaService implements ProcesarBiometricoUseCase, RegistrarListaAulaUseCase {
  private final AsistenciaMapper mapper;
  private final BiometricoRepositoryPort lotes;
  private final MarcaPorteriaRepositoryPort marcas;
  private final EstudianteRepositoryPort estudiantes;
  private final MatriculaRepositoryPort matriculas;
  private final AsistenciaAulaRepositoryPort asistencias;
  private final ActorActualPort actor;

  public LoteBiometricoResponseDto importar(ImportarLoteBiometricoRequestDto r) {
    var lote =
        LoteBiometrico.builder()
            .nombreArchivo(r.nombreArchivo())
            .totalFilas(r.marcas().size())
            .marcasValidas(0)
            .marcasErroneas(0)
            .importadoPorUsuarioId(actor.usuarioId())
            .build();
    lote = lotes.guardar(lote);
    int validas = 0, errores = 0;
    var vistas = new HashSet<String>();
    var estudiantesPorDni =
        estudiantes
            .buscarPorNumerosDocumento(
                r.marcas().stream().map(f -> f.dniLeido()).distinct().toList())
            .stream()
            .filter(e -> e.getTipoDocumento() == TipoDocumento.DNI)
            .collect(Collectors.toMap(e -> e.getNumeroDocumento(), Function.identity(), (a, b) -> a));
    var estudiantesBloqueados =
        estudiantes.bloquearPorIds(
            estudiantesPorDni.values().stream().map(e -> e.getId()).distinct().toList());
    var estudiantesPorId =
        estudiantesBloqueados.stream()
            .collect(Collectors.toMap(e -> e.getId(), Function.identity()));
    estudiantesPorDni.replaceAll((dni, e) -> estudiantesPorId.getOrDefault(e.getId(), e));

    var inicio = r.marcas().stream().map(f -> f.fechaHora()).min(java.util.Comparator.naturalOrder());
    var fin = r.marcas().stream().map(f -> f.fechaHora()).max(java.util.Comparator.naturalOrder());
    var existentes = new HashSet<String>();
    if (inicio.isPresent() && fin.isPresent()) {
      marcas.buscarPorRangoFecha(inicio.get(), fin.get().plusNanos(1)).stream()
          .filter(m -> m.getEstudianteId() != null)
          .map(
              m ->
                  m.getEstudianteId()
                      + "/"
                      + m.getFechaHora().toInstant()
                      + "/"
                      + m.getDispositivoCodigo())
          .forEach(existentes::add);
    }
    for (var fila : r.marcas()) {
      var marca =
          MarcaPorteria.builder()
              .loteId(lote.getId())
              .dniLeido(fila.dniLeido())
              .fechaHora(fila.fechaHora())
              .dispositivoCodigo(fila.dispositivoCodigo())
              .build();
      var est = java.util.Optional.ofNullable(estudiantesPorDni.get(fila.dniLeido()));
      String clave =
          fila.dniLeido() + "/" + fila.fechaHora().toInstant() + "/" + fila.dispositivoCodigo();
      boolean duplicada = !vistas.add(clave);
      if (est.isPresent()) {
        marca.identificar(est.get().getId());
        String claveExistente =
            est.get().getId()
                + "/"
                + fila.fechaHora().toInstant()
                + "/"
                + fila.dispositivoCodigo();
        duplicada = duplicada || existentes.contains(claveExistente);
      }
      if (duplicada) marca.marcarDuplicado();
      else if (est.isEmpty()) marca.marcarDniNoIdentificado();
      if (marca.getEstadoProcesamiento() == EstadoMarca.PENDIENTE) validas++;
      else errores++;
      marcas.guardar(marca);
    }
    lote.registrarResultado(validas, errores);
    return mapper.toResponse(lotes.guardar(lote));
  }

  public AsistenciaAulaResponseDto registrar(RegistrarAsistenciaAulaRequestDto r) {
    var m = requerido(matriculas.bloquearPorId(r.matriculaId()));
    exigir(m.getEstadoMatricula() == EstadoMatricula.MATRICULADO, "Matrícula no activa");
    if (asistencias.buscarPorMatriculaId(r.matriculaId()).stream()
        .anyMatch(a -> a.getFechaSesion().equals(r.fechaSesion())))
      throw new com.colegio.shuji.asistencia.domain.exception.AsistenciaYaRegistradaException(
          "Ya existe asistencia para esa fecha");
    exigir(
        r.estado() != EstadoAsistenciaAula.FALTA_JUSTIFICADA,
        "Registre la falta y luego su justificación");
    var a = mapper.toDomain(r);
    a.registrarPor(actor.usuarioId());
    return mapper.toResponse(asistencias.guardar(a));
  }

  public AsistenciaAulaResponseDto justificar(JustificarInasistenciaRequestDto r) {
    var a = requerido(asistencias.bloquearPorId(r.asistenciaId()));
    a.justificar(r.motivo(), r.documentoSustentoUrl());
    return mapper.toResponse(asistencias.guardar(a));
  }

  @Transactional(readOnly = true)
  public ReporteAsistenciaDiariaResponseDto reporte(LocalDate fecha) {
    return new ReporteAsistenciaDiariaResponseDto(
        fecha, asistencias.buscarPorFechaSesion(fecha).stream().map(mapper::toResponse).toList());
  }
}
