package com.colegio.shuji.matricula.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import com.colegio.shuji.matricula.application.port.out.ReniecServicePort;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class VerificacionIdentidadMatriculaServiceTest {
  final ReniecServicePort api = mock(ReniecServicePort.class);
  final VerificacionIdentidadMatriculaService service = new VerificacionIdentidadMatriculaService(api);
  @Test void contrastaDniYNombresSinDependerDeTildes() {
    when(api.consultar("01234567")).thenReturn(Optional.of(new ReniecServicePort.Identidad("01234567","María","Pérez","López")));
    assertEquals(VerificacionIdentidadMatriculaService.Resultado.COINCIDE, service.verificar("MARIA PEREZ LOPEZ","01234567"));
  }
  @Test void rechazaIdentidadDistinta() {
    when(api.consultar("01234567")).thenReturn(Optional.of(new ReniecServicePort.Identidad("01234567","Otro","Pérez","López")));
    assertEquals(VerificacionIdentidadMatriculaService.Resultado.NO_COINCIDE, service.verificar("Maria Perez Lopez","01234567"));
  }
  @Test void proveedorCaidoNoApruebaIdentidad() {
    when(api.consultar("01234567")).thenReturn(Optional.empty());
    assertEquals(VerificacionIdentidadMatriculaService.Resultado.NO_DISPONIBLE, service.verificar("Maria Perez Lopez","01234567"));
  }
  @Test void documentoInvalidoNoConsultaApi() {
    assertEquals(VerificacionIdentidadMatriculaService.Resultado.NO_COINCIDE, service.verificar("Maria","1234567"));
    verifyNoInteractions(api);
  }

  @Test void noConfundeNombresConPalabrasDistintasAlQuitarEspacios() {
    when(api.consultar("01234567")).thenReturn(Optional.of(new ReniecServicePort.Identidad("01234567","Ana María","Pérez","López")));
    assertEquals(VerificacionIdentidadMatriculaService.Resultado.NO_COINCIDE,
        service.verificar("Anamaría Pérez López", "01234567"));
  }

  @Test void admiteEspaciosAdicionalesSinFusionarLasPalabras() {
    when(api.consultar("01234567")).thenReturn(Optional.of(new ReniecServicePort.Identidad("01234567","Ana María","Pérez","López")));
    assertEquals(VerificacionIdentidadMatriculaService.Resultado.COINCIDE,
        service.verificar("  ANA\tMARIA  PEREZ\nLOPEZ  ", "01234567"));
  }
}
