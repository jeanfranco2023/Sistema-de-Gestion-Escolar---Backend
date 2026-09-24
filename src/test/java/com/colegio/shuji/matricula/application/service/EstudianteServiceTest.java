package com.colegio.shuji.matricula.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.colegio.shuji.matricula.application.dto.in.RegistrarEstudianteRequestDto;
import com.colegio.shuji.matricula.application.mapper.MatriculaMapper;
import com.colegio.shuji.matricula.application.port.out.ApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.ReniecServicePort;
import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.OrigenRegistro;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EstudianteServiceTest {
  @Test
  void codigoSiagieVacioSeGuardaComoNull() {
    assertCodigoNormalizado("", null);
    assertCodigoNormalizado("   ", null);
  }

  @Test
  void codigoSiagieConEspaciosSeRecorta() {
    assertCodigoNormalizado("  SIAGIE-123  ", "SIAGIE-123");
  }

  @Test
  void registroConservaNombresRevisadosSinMarcarReniec() {
    var mapper = mock(MatriculaMapper.class);
    var estudiantes = mock(EstudianteRepositoryPort.class);
    var proveedor = mock(ReniecServicePort.class);
    var service = new EstudianteService(mapper, estudiantes,
        mock(ApoderadoRepositoryPort.class), mock(EstudianteApoderadoRepositoryPort.class), proveedor);
    var request = new RegistrarEstudianteRequestDto(TipoDocumento.DNI, "12345678", "Ana",
        "Pérez", "López", LocalDate.of(2015, 5, 12), Genero.F, null, null, null);
    when(estudiantes.buscarPorNumeroDocumento("12345678")).thenReturn(List.of());
    when(mapper.toDomain(request)).thenReturn(Estudiante.builder().tipoDocumento(TipoDocumento.DNI)
        .numeroDocumento("12345678").nombres("Ana").apellidoPaterno("Pérez")
        .apellidoMaterno("López").build());
    when(estudiantes.guardar(any(Estudiante.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.registrarEstudiante(request);

    var captor = ArgumentCaptor.forClass(Estudiante.class);
    verify(estudiantes).guardar(captor.capture());
    assertEquals("Ana", captor.getValue().getNombres());
    assertEquals(OrigenRegistro.MANUAL_CONTINGENCIA, captor.getValue().getOrigenRegistro());
    assertEquals(false, captor.getValue().getValidadoReniec());
    verify(proveedor, never()).consultar("12345678");
  }

  private void assertCodigoNormalizado(String recibido, String esperado) {
    var mapper = mock(MatriculaMapper.class);
    var estudiantes = mock(EstudianteRepositoryPort.class);
    var service = new EstudianteService(
        mapper,
        estudiantes,
        mock(ApoderadoRepositoryPort.class),
        mock(EstudianteApoderadoRepositoryPort.class),
        mock(ReniecServicePort.class));
    var request = new RegistrarEstudianteRequestDto(
        TipoDocumento.CE, "12345678", "Prueba", "Local", "Integracion",
        LocalDate.of(2015, 5, 12), Genero.F, recibido, null, null);
    when(estudiantes.buscarPorNumeroDocumento("12345678")).thenReturn(List.of());
    when(mapper.toDomain(request)).thenReturn(Estudiante.builder().codigoEstudianteSiagie(recibido).build());
    when(estudiantes.guardar(any(Estudiante.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.registrarEstudiante(request);

    var captor = ArgumentCaptor.forClass(Estudiante.class);
    verify(estudiantes).guardar(captor.capture());
    if (esperado == null) {
      assertNull(captor.getValue().getCodigoEstudianteSiagie());
    } else {
      assertEquals(esperado, captor.getValue().getCodigoEstudianteSiagie());
    }
  }
}
