package com.colegio.shuji.matricula.application.service;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.colegio.shuji.matricula.application.port.out.ConsultaPortalDatosPort;
import com.colegio.shuji.matricula.application.port.out.AccesoFamiliarPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.academico.application.port.in.GestionarAnioLectivoUseCase;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class PortalFamiliarServiceTest {
  @Mock ConsultaPortalDatosPort datos; @Mock AccesoFamiliarPort acceso; @Mock MatriculaRepositoryPort matriculas;
  @Mock GestionarAnioLectivoUseCase calendario; @Mock ActorActualPort actor; @InjectMocks PortalFamiliarService servicio;
  @Test void sinCustodiaNoLeeNotasAsistenciaNiPeriodosDeOtroAlumno() {
    when(actor.usuarioId()).thenReturn(5L);when(matriculas.buscarPorId(9L)).thenReturn(Optional.of(Matricula.builder().id(9L).estudianteId(8L).build()));
    assertThrows(AccessDeniedException.class,()->servicio.calificaciones(9L));
    assertThrows(AccessDeniedException.class,()->servicio.asistencia(9L));
    assertThrows(AccessDeniedException.class,()->servicio.periodos(9L));
    verifyNoInteractions(datos,calendario);
  }
  @Test void hijosFamiliaresSeFiltranEnBaseDeDatosPorUsuario() {
    when(actor.usuarioId()).thenReturn(5L);servicio.hijos();verify(datos).hijos(5L);
  }
  @Test void hijosPropiosNoAmplianAccesoPorTenerAdemasRolAdministrativo() {
    when(actor.usuarioId()).thenReturn(5L);servicio.hijosPropios();verify(datos).hijos(5L);
  }
  @Test void apoderadoNoPuedeConsultarElPadronDeSeguimiento() {
    assertThrows(AccessDeniedException.class,()->servicio.alumnosSeguimiento());verifyNoInteractions(datos);
  }
}
