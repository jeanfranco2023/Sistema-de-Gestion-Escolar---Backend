package com.colegio.shuji.matricula.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.colegio.shuji.matricula.application.dto.in.RegistrarEstudianteRequestDto;
import com.colegio.shuji.matricula.application.dto.in.ApoderadoMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.VincularApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.out.ApoderadoResponseDto;
import com.colegio.shuji.matricula.application.mapper.MatriculaMapper;
import com.colegio.shuji.matricula.application.port.out.ApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.ReniecServicePort;
import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.Parentesco;
import com.colegio.shuji.matricula.domain.enums.OrigenRegistro;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import com.colegio.shuji.matricula.domain.model.Apoderado;
import com.colegio.shuji.matricula.domain.model.EstudianteApoderado;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
    when(mapper.toResponse(any(Estudiante.class))).thenReturn(
        new com.colegio.shuji.matricula.application.dto.out.EstudianteResponseDto(
            10L, "ALU-000010", TipoDocumento.DNI, "12345678", "Ana", "Pérez", "López",
            LocalDate.of(2015, 5, 12), Genero.F, null, false, OrigenRegistro.MANUAL_CONTINGENCIA,
            null, null, false, java.time.OffsetDateTime.now()));

    service.registrarEstudiante(request);

    var captor = ArgumentCaptor.forClass(Estudiante.class);
    verify(estudiantes).guardar(captor.capture());
    assertEquals("Ana", captor.getValue().getNombres());
    assertNull(captor.getValue().getCodigoEstudianteSiagie());
    assertEquals(OrigenRegistro.MANUAL_CONTINGENCIA, captor.getValue().getOrigenRegistro());
    assertEquals(false, captor.getValue().getValidadoReniec());
    verify(proveedor, never()).consultar("12345678");
  }

  @Test
  void registraYVinculaElApoderadoEnLaMismaAltaDelEstudiante() {
    var mapper = mock(MatriculaMapper.class);
    var estudiantes = mock(EstudianteRepositoryPort.class);
    var apoderados = mock(ApoderadoRepositoryPort.class);
    var vinculos = mock(EstudianteApoderadoRepositoryPort.class);
    var estudiante = mock(Estudiante.class);
    var apoderado = mock(Apoderado.class);
    var vinculo = new EstudianteApoderado();
    var respuesta = new com.colegio.shuji.matricula.application.dto.out.EstudianteResponseDto(
        10L, "ALU-000010", TipoDocumento.DNI, "12345678", "Ana", "Pérez", "López",
        LocalDate.of(2015, 5, 12), Genero.F, null, false, OrigenRegistro.MANUAL_CONTINGENCIA,
        null, null, true, java.time.OffsetDateTime.now());
    var request = new RegistrarEstudianteRequestDto(TipoDocumento.DNI, "12345678", "Ana",
        "Pérez", "López", LocalDate.of(2015, 5, 12), Genero.F, null, null,
        new ApoderadoMatriculaRequestDto(TipoDocumento.DNI, "87654321", "Rosa", "Pérez",
            "Ruiz", "999888777", null, "Calle 1", "150101", Parentesco.MADRE));
    when(estudiantes.buscarPorNumeroDocumento("12345678")).thenReturn(List.of());
    when(mapper.toDomain(request)).thenReturn(estudiante);
    when(estudiantes.guardar(estudiante)).thenReturn(estudiante);
    when(estudiante.getId()).thenReturn(10L);
    when(apoderados.buscarPorNumeroDocumento("87654321")).thenReturn(List.of());
    when(mapper.toDomain(any(com.colegio.shuji.matricula.application.dto.in.RegistrarApoderadoRequestDto.class)))
        .thenReturn(apoderado);
    when(apoderados.guardar(apoderado)).thenReturn(apoderado);
    when(apoderado.getId()).thenReturn(20L);
    when(estudiantes.bloquearPorId(10L)).thenReturn(Optional.of(estudiante));
    when(apoderados.buscarPorId(20L)).thenReturn(Optional.of(apoderado));
    when(vinculos.buscarPorEstudianteId(10L)).thenReturn(List.of());
    when(mapper.toDomain(any(VincularApoderadoRequestDto.class))).thenAnswer(invocation -> {
      var datos = invocation.getArgument(0, VincularApoderadoRequestDto.class);
      vinculo.setEstudianteId(datos.estudianteId());
      vinculo.setApoderadoId(datos.apoderadoId());
      vinculo.setParentesco(datos.parentesco());
      vinculo.setEsResponsableEconomico(datos.esResponsableEconomico());
      vinculo.setTieneCustodia(datos.tieneCustodia());
      vinculo.setPermiteRecojo(datos.permiteRecojo());
      return vinculo;
    });
    when(vinculos.guardar(vinculo)).thenReturn(vinculo);
    when(mapper.toResponse(estudiante)).thenReturn(respuesta);

    var resultado = new EstudianteService(mapper, estudiantes, apoderados, vinculos,
        mock(ReniecServicePort.class)).registrarEstudiante(request);

    assertEquals(true, resultado.tieneApoderado());
    var captor = ArgumentCaptor.forClass(EstudianteApoderado.class);
    verify(vinculos).guardar(captor.capture());
    assertEquals(20L, captor.getValue().getApoderadoId());
    assertEquals(10L, captor.getValue().getEstudianteId());
    assertEquals(true, captor.getValue().getEsResponsableEconomico());
    assertEquals(Parentesco.MADRE, captor.getValue().getParentesco());
  }
}
