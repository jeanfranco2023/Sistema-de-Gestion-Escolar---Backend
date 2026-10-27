package com.colegio.shuji.matricula.domain;
import static org.junit.jupiter.api.Assertions.*;
import com.colegio.shuji.matricula.domain.enums.EstadoSuplencia;
import com.colegio.shuji.matricula.domain.model.Suplencia;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
class SuplenciaTest {
  private final LocalDate hoy=LocalDate.of(2026,10,26);
  private Suplencia pendiente(){return Suplencia.builder().estado(EstadoSuplencia.PENDIENTE_APROBACION).fechaInicio(hoy).fechaFin(hoy.plusDays(2)).build();}
  @Test void aprobacionConservaAutorYNoPuedeRepetirse(){var s=pendiente();s.aprobar(5L,hoy);assertEquals(EstadoSuplencia.ACTIVO,s.getEstado());assertEquals(5L,s.getAutorizadoPorUsuarioId());assertNotNull(s.getFechaResolucion());assertThrows(BusinessException.class,()->s.aprobar(6L,hoy));}
  @Test void revocarYRechazarExigenMotivo(){var s=pendiente();assertThrows(BusinessException.class,()->s.rechazar(5L," "));s.aprobar(5L,hoy);assertThrows(BusinessException.class,()->s.revocar(5L,""));s.revocar(6L,"Fin de autorización");assertEquals("Fin de autorización",s.getMotivoResolucion());assertEquals(EstadoSuplencia.REVOCADO,s.getEstado());}
  @Test void elVencimientoEsDerivadoSinEliminarHistorial(){var s=pendiente();s.aprobar(5L,hoy);assertEquals("ACTIVO",s.estadoVisible(hoy.plusDays(2)));assertEquals("VENCIDO",s.estadoVisible(hoy.plusDays(3)));assertEquals(EstadoSuplencia.ACTIVO,s.getEstado());var vencida=pendiente();assertThrows(BusinessException.class,()->vencida.aprobar(5L,hoy.plusDays(3)));}
}
