package com.colegio.shuji.shared.infrastructure.controller;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.asistencia.application.port.out.AsistenciaAulaRepositoryPort;
import com.colegio.shuji.evaluacion.application.port.out.CalificacionRepositoryPort;
import com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.tesoreria.application.port.out.PagoRepositoryPort;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {
  private final AnioLectivoRepositoryPort anios;
  private final MatriculaRepositoryPort matriculas;
  private final AsistenciaAulaRepositoryPort asistencias;
  private final CalificacionRepositoryPort calificaciones;
  private final PagoRepositoryPort pagos;

  /** Cada metrica es null cuando no pudo calcularse; el cliente debe mostrarla como "sin dato". */
  public record Resumen(int anio, Long totalEstudiantes, Integer asistenciaHoy,
                        BigDecimal recaudacionMes, Long alumnosEnRiesgo) {}

  @GetMapping("/resumen")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  public Resumen resumen() {
    var hoy = LocalDate.now(ZoneId.of("America/Lima"));
    int anioActual = hoy.getYear();
    Long totalEstudiantes = null;
    Integer porcentajeAsistencia = null;
    BigDecimal recaudacion = null;
    Long alumnosRiesgo = null;

    try {
      var anioOpt = anios.listar().stream().filter(a -> a.getAnio() == hoy.getYear()).findFirst();
      if (anioOpt.isPresent()) {
        var anio = anioOpt.get();
        anioActual = (int) anio.getAnio();
        try {
          totalEstudiantes = matriculas.buscarPorAnioLectivoId(anio.getId()).stream()
              .filter(m -> m.getEstadoMatricula() == EstadoMatricula.MATRICULADO).count();
        } catch (Exception e) {
          log.warn("Error calculando matriculas activas: {}", e.getMessage());
        }
        try {
          alumnosRiesgo = calificaciones.buscarPorAnioLectivoId(anio.getId()).stream()
              .filter(c -> c.getCalificacionCualitativa() == CalificacionCualitativa.C)
              .map(c -> c.getMatriculaId()).distinct().count();
        } catch (Exception e) {
          log.warn("Error calculando alumnos en riesgo: {}", e.getMessage());
        }
      }
    } catch (Exception e) {
      log.warn("Error consultando anio lectivo: {}", e.getMessage());
    }

    try {
      var asistencia = asistencias.buscarPorFechaSesion(hoy);
      if (asistencia != null) {
        porcentajeAsistencia = asistencia.isEmpty() ? 0
            : (int) Math.round(100.0 * asistencia.stream()
                .filter(a -> a.esPresente()).count() / asistencia.size());
      }
    } catch (Exception e) {
      log.warn("Error calculando asistencia: {}", e.getMessage());
    }

    try {
      var listaPagos = pagos.listar();
      if (listaPagos != null) {
        recaudacion = listaPagos.stream()
            .filter(p -> p != null && p.estaAprobado() && p.getFechaPago() != null
                && p.getMontoPagado() != null
                && p.getFechaPago().atZoneSameInstant(ZoneId.of("America/Lima")).getYear() == hoy.getYear()
                && p.getFechaPago().atZoneSameInstant(ZoneId.of("America/Lima")).getMonthValue() == hoy.getMonthValue())
            .map(p -> p.getMontoPagado())
            .reduce(BigDecimal.ZERO, BigDecimal::add);
      }
    } catch (Exception e) {
      log.warn("Error calculando recaudacion del mes: {}", e.getMessage());
    }

    return new Resumen(anioActual, totalEstudiantes, porcentajeAsistencia, recaudacion, alumnosRiesgo);
  }
}
