package com.colegio.shuji.asistencia.infrastructure.controller.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.colegio.shuji.asistencia.application.dto.in.ImportarLoteBiometricoRequestDto;
import com.colegio.shuji.asistencia.application.port.in.EjecutarConciliacionDiariaUseCase;
import com.colegio.shuji.asistencia.application.port.in.ProcesarBiometricoUseCase;
import com.colegio.shuji.asistencia.application.port.in.RegistrarListaAulaUseCase;
import com.colegio.shuji.asistencia.application.port.out.AlmacenJustificacionPort;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

class AsistenciaControllerImportacionTest {
  private ProcesarBiometricoUseCase biometrico;
  private AsistenciaController controller;

  @BeforeEach
  void preparar() {
    biometrico = mock(ProcesarBiometricoUseCase.class);
    controller = new AsistenciaController(
        biometrico,
        mock(RegistrarListaAulaUseCase.class),
        mock(EjecutarConciliacionDiariaUseCase.class),
        mock(AlmacenJustificacionPort.class));
  }

  @Test
  void rechazaArchivoNuloVacioSinNombreExtensionInvalidaYMuyGrande() {
    assertThrows(ResponseStatusException.class, () -> controller.importarArchivoBiometrico(null));
    assertThrows(ResponseStatusException.class, () -> controller.importarArchivoBiometrico(
        new MockMultipartFile("archivo", "marcas.csv", "text/csv", new byte[0])));
    assertThrows(ResponseStatusException.class, () -> controller.importarArchivoBiometrico(
        new MockMultipartFile("archivo", null, "text/csv", new byte[] {1})));
    assertThrows(ResponseStatusException.class, () -> controller.importarArchivoBiometrico(
        new MockMultipartFile("archivo", "marcas.txt", "text/plain", new byte[] {1})));
    assertThrows(ResponseStatusException.class, () -> controller.importarArchivoBiometrico(
        new MockMultipartFile("archivo", "marcas.csv", "text/csv", new byte[5 * 1024 * 1024 + 1])));
  }

  @Test
  void importaCsvValidoYEntregaElLoteAlCasoDeUso() {
    byte[] csv = ("dni,fecha_hora,dispositivo_codigo\n"
        + "00000000,2026-10-28T08:30:00-05:00,PUERTA1\n").getBytes(StandardCharsets.UTF_8);
    var archivo = new MockMultipartFile("archivo", "marcas.csv", "text/csv", csv);

    controller.importarArchivoBiometrico(archivo);

    var captor = org.mockito.ArgumentCaptor.forClass(ImportarLoteBiometricoRequestDto.class);
    verify(biometrico).importar(captor.capture());
    assertEquals("marcas.csv", captor.getValue().nombreArchivo());
    assertEquals(1, captor.getValue().marcas().size());
    assertEquals("00000000", captor.getValue().marcas().getFirst().dniLeido());
  }

  @Test
  void devuelveBadRequestSiNoPuedeLeerElArchivo() throws IOException {
    MultipartFile archivo = mock(MultipartFile.class);
    when(archivo.getOriginalFilename()).thenReturn("marcas.csv");
    when(archivo.isEmpty()).thenReturn(false);
    when(archivo.getSize()).thenReturn(10L);
    when(archivo.getBytes()).thenThrow(new IOException("lectura de prueba"));

    var error = assertThrows(ResponseStatusException.class,
        () -> controller.importarArchivoBiometrico(archivo));

    assertTrue(error.getReason().contains("No se pudo leer"));
  }
}
