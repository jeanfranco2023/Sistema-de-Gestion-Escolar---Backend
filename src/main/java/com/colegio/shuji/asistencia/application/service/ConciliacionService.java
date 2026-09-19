package com.colegio.shuji.asistencia.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.asistencia.application.dto.out.ConciliacionAsistenciaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.DiscrepanciaAlertaResponseDto;
import com.colegio.shuji.asistencia.application.mapper.AsistenciaMapper;
import com.colegio.shuji.asistencia.application.port.in.EjecutarConciliacionDiariaUseCase;
import com.colegio.shuji.asistencia.application.port.out.AsistenciaAulaRepositoryPort;
import com.colegio.shuji.asistencia.application.port.out.ConciliacionRepositoryPort;
import com.colegio.shuji.asistencia.application.port.out.MarcaPorteriaRepositoryPort;
import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import com.colegio.shuji.asistencia.domain.enums.EstadoMarca;
import com.colegio.shuji.asistencia.domain.enums.TipoDiscrepancia;
import com.colegio.shuji.asistencia.domain.model.ConciliacionAsistencia;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ConciliacionService implements EjecutarConciliacionDiariaUseCase {
  private final AsistenciaMapper mapper;
  private final ConciliacionRepositoryPort conciliaciones;
  private final MarcaPorteriaRepositoryPort marcas;
  private final AsistenciaAulaRepositoryPort asistencias;
  private final MatriculaRepositoryPort matriculas;
  private final AnioLectivoRepositoryPort anios;

  public List<ConciliacionAsistenciaResponseDto> conciliar(
      Short anioId, LocalDate fecha, boolean turnoCerrado) {
    var anio = requerido(anios.bloquearPorId(anioId));
    exigir(
        !fecha.isBefore(anio.getFechaInicio()) && !fecha.isAfter(anio.getFechaFin()),
        "Fecha fuera del año lectivo");
    exigir(
        !fecha.isAfter(LocalDate.now(ZoneId.of("America/Lima"))), "No se concilian fechas futuras");
    var result = new ArrayList<ConciliacionAsistenciaResponseDto>();
    var zona = ZoneId.of("America/Lima");
    for (var m : matriculas.buscarPorAnioLectivoId(anioId)) {
      if (m.getEstadoMatricula() != EstadoMatricula.MATRICULADO) continue;
      var delDia =
          marcas.buscarPorEstudianteId(m.getEstudianteId()).stream()
              .filter(
                  v ->
                      v.getFechaHora().atZoneSameInstant(zona).toLocalDate().equals(fecha)
                          && v.getEstadoProcesamiento() != EstadoMarca.DUPLICADO
                          && v.getEstadoProcesamiento() != EstadoMarca.DNI_NO_IDENTIFICADO)
              .toList();
      boolean porteria = !delDia.isEmpty();
      var lista =
          asistencias.buscarPorMatriculaId(m.getId()).stream()
              .filter(a -> a.getFechaSesion().equals(fecha))
              .findFirst();
      boolean presente =
          lista
              .map(
                  a ->
                      a.getEstado() == EstadoAsistenciaAula.PRESENTE
                          || a.getEstado() == EstadoAsistenciaAula.TARDANZA)
              .orElse(false);
      var c =
          conciliaciones.buscarPorEstudianteId(m.getEstudianteId()).stream()
              .filter(v -> v.getFecha().equals(fecha))
              .findFirst()
              .orElseGet(
                  () ->
                      ConciliacionAsistencia.builder()
                          .fecha(fecha)
                          .estudianteId(m.getEstudianteId())
                          .build());
      if (c.getId() != null
          && c.getTipoDiscrepancia() != TipoDiscrepancia.PENDIENTE_CIERRE_TURNO
          && !turnoCerrado) {
        result.add(mapper.toResponse(c));
        continue;
      }
      c.conciliar(porteria, presente, turnoCerrado);
      result.add(mapper.toResponse(conciliaciones.guardar(c)));
      if (turnoCerrado)
        for (var marca : delDia) {
          marca.conciliar();
          marcas.guardar(marca);
        }
    }
    return result;
  }

  @Transactional(readOnly = true)
  public List<DiscrepanciaAlertaResponseDto> alertas(LocalDate fecha) {
    return conciliaciones.buscarPorFecha(fecha).stream()
        .filter(
            c ->
                c.getTipoDiscrepancia() == TipoDiscrepancia.DISCREPANCIA_FUGA
                    || c.getTipoDiscrepancia() == TipoDiscrepancia.DISCREPANCIA_OMISION_PORTERIA)
        .map(
            c ->
                new DiscrepanciaAlertaResponseDto(
                    c.getEstudianteId(), c.getFecha(), c.getTipoDiscrepancia()))
        .toList();
  }
}
