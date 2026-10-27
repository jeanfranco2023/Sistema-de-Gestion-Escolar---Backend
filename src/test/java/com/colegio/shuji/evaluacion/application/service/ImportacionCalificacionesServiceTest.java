package com.colegio.shuji.evaluacion.application.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.colegio.shuji.academico.application.port.out.PeriodoRepositoryPort;
import com.colegio.shuji.academico.domain.model.PeriodoAcademico;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.CompetenciaRepositoryPort;
import com.colegio.shuji.curriculo.domain.model.AsignacionDocente;
import com.colegio.shuji.curriculo.domain.model.Competencia;
import com.colegio.shuji.evaluacion.application.port.in.RegistrarEvaluacionCnebUseCase;
import com.colegio.shuji.evaluacion.domain.exception.ImportacionCalificacionesException;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ImportacionCalificacionesServiceTest {
  @Mock PeriodoRepositoryPort periodos; @Mock AsignacionRepositoryPort asignaciones; @Mock CompetenciaRepositoryPort competencias;
  @Mock MatriculaRepositoryPort matriculas; @Mock EstudianteRepositoryPort estudiantes; @Mock ActorActualPort actor;
  @Mock RegistrarEvaluacionCnebUseCase registro; @InjectMocks ImportacionCalificacionesService servicio;
  private void contexto() {
    when(asignaciones.buscarPorId(9L)).thenReturn(Optional.of(AsignacionDocente.builder().id(9L).docenteUsuarioId(5L).seccionId(2).anioLectivoId((short)1).areaCurricularId((short)3).build()));
  }
  private void datos() {
    contexto(); when(periodos.buscarPorId((short)1)).thenReturn(Optional.of(PeriodoAcademico.builder().id((short)1).anioLectivoId((short)1).activo(true).cerrado(false).build()));
    when(estudiantes.buscarPorNumerosDocumento(any())).thenReturn(List.of(Estudiante.builder().id(4L).numeroDocumento("00123456").nombres("Ana").apellidoPaterno("Perez").apellidoMaterno("Ruiz").activo(true).build()));
    when(matriculas.buscarPorSeccionId(2)).thenReturn(List.of(Matricula.builder().id(7L).estudianteId(4L).anioLectivoId((short)1).estadoMatricula(EstadoMatricula.MATRICULADO).build()));
    when(competencias.buscarPorAreaId((short)3)).thenReturn(List.of(Competencia.builder().id((short)8).build()));
  }
  private List<List<String>> filas(String nota) { return List.of(ImportacionCalificacionesService.CABECERAS,List.of("00123456","8",nota,"Explica con apoyo")); }
  @Test void previsualizaSinPersistirYConservaElDniConCeros() {
    datos(); var r=servicio.importar(filas("B"),(short)1,2,9L,false);
    assertEquals(1,r.filasValidadas()); assertEquals(0,r.filasProcesadas()); assertEquals("00123456",r.filas().getFirst().dni()); verifyNoInteractions(registro);
  }
  @Test void erroresDeFilasRechazanElLoteCompleto() {
    datos(); var filas=new java.util.ArrayList<>(filas("X")); filas.add(List.of("00123456","8","C","Otra conclusión"));
    var error=assertThrows(ImportacionCalificacionesException.class,()->servicio.importar(filas,(short)1,2,9L,true));
    assertTrue(error.errores().containsKey("fila[2].NOTA_CNEB")); assertTrue(error.errores().containsKey("fila[3].duplicado")); verifyNoInteractions(registro);
  }
  @Test void confirmaConElCasoDeUsoTransaccionalExistente() {
    datos(); var r=servicio.importar(filas("C"),(short)1,2,9L,true);
    assertEquals(1,r.filasProcesadas()); verify(registro).registrar(argThat(rq->rq.asignacionDocenteId()==9L&&rq.calificaciones().getFirst().matriculaId()==7L));
  }
  @Test void docenteAjenoNoPuedeInspeccionarAlumnosNiImportar() {
    contexto(); doThrow(new org.springframework.security.access.AccessDeniedException("Ajeno")).when(actor).verificarDocente(5L);
    assertThrows(org.springframework.security.access.AccessDeniedException.class,()->servicio.importar(filas("C"),(short)1,2,9L,true)); verifyNoInteractions(estudiantes,matriculas,registro,competencias,periodos);
  }
}
