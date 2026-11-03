package com.colegio.shuji.matricula.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.colegio.shuji.matricula.application.dto.in.RegistrarEstudianteRequestDto;
import com.colegio.shuji.matricula.application.dto.out.ApoderadoResponseDto;
import com.colegio.shuji.matricula.application.mapper.MatriculaMapper;
import com.colegio.shuji.matricula.application.port.out.ApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.ReniecServicePort;
import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.OrigenRegistro;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import com.colegio.shuji.matricula.domain.model.Apoderado;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class EstudianteServiceTest {
  @Test
  void listarApoderadosConservaSeparadosIdYUsuarioId() {
    var mapper = mock(MatriculaMapper.class);
    var apoderados = mock(ApoderadoRepositoryPort.class);
    var apoderado = mock(Apoderado.class);
    var respuesta = new ApoderadoResponseDto(41L, 900L, TipoDocumento.DNI, "12345678",
        "Ana", "Pérez", "López", "987654321", null, "Calle 1", "150101",
        false, OrigenRegistro.MANUAL_CONTINGENCIA, null);
    when(apoderados.listar()).thenReturn(List.of(apoderado));
    when(mapper.toResponse(apoderado)).thenReturn(respuesta);
    var service = new EstudianteService(mapper, mock(EstudianteRepositoryPort.class), apoderados,
        mock(EstudianteApoderadoRepositoryPort.class), mock(ReniecServicePort.class));

    var resultado = service.listarApoderados();

    assertEquals(1, resultado.size());
    assertEquals(41L, resultado.getFirst().id());
    assertEquals(900L, resultado.getFirst().usuarioId());
  }

  @Test
  void registroConservaNombresRevisadosSinMarcarReniec() {
    var mapper = mock(MatriculaMapper.class);
    var estudiantes = mock(EstudianteRepositoryPort.class);
    var proveedor = mock(ReniecServicePort.class);
    var service = new EstudianteService(mapper, estudiantes,
        mock(ApoderadoRepositoryPort.class), mock(EstudianteApoderadoRepositoryPort.class), proveedor);
    var request = new RegistrarEstudianteRequestDto(TipoDocumento.DNI, "12345678", "Ana",
        "Pérez", "López", LocalDate.of(2015, 5, 12), Genero.F, null, null);
    when(estudiantes.buscarPorNumeroDocumento("12345678")).thenReturn(List.of());
    when(mapper.toDomain(request)).thenReturn(Estudiante.builder().tipoDocumento(TipoDocumento.DNI)
        .numeroDocumento("12345678").nombres("Ana").apellidoPaterno("Pérez")
        .apellidoMaterno("López").build());
    when(estudiantes.guardar(any(Estudiante.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.registrarEstudiante(request);

    var captor = ArgumentCaptor.forClass(Estudiante.class);
    verify(estudiantes).guardar(captor.capture());
    assertEquals("Ana", captor.getValue().getNombres());
    assertNull(captor.getValue().getCodigoEstudianteSiagie());
    assertEquals(OrigenRegistro.MANUAL_CONTINGENCIA, captor.getValue().getOrigenRegistro());
    assertEquals(false, captor.getValue().getValidadoReniec());
    verify(proveedor, never()).consultar("12345678");
  }
}
