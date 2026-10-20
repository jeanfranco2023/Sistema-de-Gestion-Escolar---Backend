package com.colegio.shuji.matricula.infrastructure.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.colegio.shuji.academico.infrastructure.repository.JpaAnioLectivoRepository;
import com.colegio.shuji.academico.infrastructure.repository.JpaSeccionRepository;
import com.colegio.shuji.matricula.application.port.out.FirmaDigitalDocumentoPort;
import com.colegio.shuji.matricula.application.port.out.GeminiDocumentoMatriculaPort;
import com.colegio.shuji.matricula.domain.enums.*;
import com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaDocumentoEntity;
import com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaPublicaEntity;
import com.colegio.shuji.matricula.infrastructure.repository.*;
import com.colegio.shuji.tesoreria.application.port.out.MercadoPagoPort;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class ValidacionAutomaticaDocumentosTest {
  final UUID id = UUID.randomUUID();
  final String token = "token-prueba";
  final JpaSolicitudMatriculaPublicaRepository solicitudes = mock(JpaSolicitudMatriculaPublicaRepository.class);
  final JpaSolicitudMatriculaDocumentoRepository documentos = mock(JpaSolicitudMatriculaDocumentoRepository.class);
  final JpaDocumentoMatriculaVersionRepository versiones = mock(JpaDocumentoMatriculaVersionRepository.class);
  final MercadoPagoPort mercado = mock(MercadoPagoPort.class);
  final SolicitudMatriculaPublicaService service = new SolicitudMatriculaPublicaService(solicitudes, documentos, versiones,
      mock(JpaAnioLectivoRepository.class), mock(JpaSeccionRepository.class), mock(JpaEstudianteRepository.class),
      mock(JpaApoderadoRepository.class), mock(JpaEstudianteApoderadoRepository.class), mock(JpaMatriculaRepository.class),
      mock(GeminiDocumentoMatriculaPort.class), mercado,
      mock(com.colegio.shuji.tesoreria.application.port.in.RegistrarCobroMatriculaUseCase.class));
  SolicitudMatriculaPublicaEntity solicitud;
  final GeminiDocumentoMatriculaPort.Resultado iaCorrecta = new GeminiDocumentoMatriculaPort.Resultado(true,true,true,"Compatible.");
  final GeminiDocumentoMatriculaPort.Resultado iaNoDisponible = new GeminiDocumentoMatriculaPort.Resultado(false,false,false,"Proveedor no disponible.",false);

  @BeforeEach void preparar() throws Exception {
    solicitud = new SolicitudMatriculaPublicaEntity();
    solicitud.setId(id);
    solicitud.setTokenHash(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))));
    solicitud.setEstado(EstadoSolicitudMatriculaPublica.DOCUMENTOS_PENDIENTES);
    solicitud.setEstadoPartida(EstadoValidacionDocumento.PENDIENTE);
    solicitud.setEstadoDniC4(EstadoValidacionDocumento.PENDIENTE);
    solicitud.setEstadoRecibo(EstadoValidacionDocumento.PENDIENTE);
    when(solicitudes.bloquearPorId(id)).thenReturn(Optional.of(solicitud));
    when(solicitudes.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
  }

  void subir(TipoDocumentoSolicitud tipo, EstadoFirmaDigital firma, GeminiDocumentoMatriculaPort.Resultado ia, Boolean dni) {
    service.guardarResultadoDocumento(id, token, tipo, ia, new FirmaDigitalDocumentoPort.Resultado(firma,"Resultado firma.",null),
        dni, null, "bucket", "ruta-aleatoria.pdf", "application/pdf", 100, "a".repeat(64));
  }

  @Test void partidaSoloSeValidaConFirmaDniEIAConfirmados() {
    subir(TipoDocumentoSolicitud.PARTIDA_NACIMIENTO, EstadoFirmaDigital.VALIDA, iaCorrecta, true);
    assertEquals(EstadoValidacionDocumento.VALIDADO, solicitud.getEstadoPartida());
  }
  @Test void iaNoPuedeAprobarUnaPartidaSinFirmaDigital() {
    subir(TipoDocumentoSolicitud.PARTIDA_NACIMIENTO, EstadoFirmaDigital.NO_APLICA, iaCorrecta, true);
    assertEquals(EstadoValidacionDocumento.OBSERVADO, solicitud.getEstadoPartida());
  }
  @Test void firmaInvalidaRechazaAunqueLaIANoEsteDisponible() {
    subir(TipoDocumentoSolicitud.PARTIDA_NACIMIENTO, EstadoFirmaDigital.INVALIDA, iaNoDisponible, null);
    assertEquals(EstadoValidacionDocumento.OBSERVADO, solicitud.getEstadoPartida());
  }
  @Test void revocacionNoConfirmadaQuedaPendiente() {
    subir(TipoDocumentoSolicitud.PARTIDA_NACIMIENTO, EstadoFirmaDigital.NO_VERIFICABLE, iaCorrecta, true);
    assertEquals(EstadoValidacionDocumento.EN_VALIDACION, solicitud.getEstadoPartida());
  }
  @Test void dniIncorrectoNoSeApruebaPorUnaFirmaCorrecta() {
    subir(TipoDocumentoSolicitud.DNI_C4, EstadoFirmaDigital.NO_APLICA, iaNoDisponible, false);
    assertEquals(EstadoValidacionDocumento.OBSERVADO, solicitud.getEstadoDniC4());
  }
  @Test void apiDeIdentidadCaidaQuedaPendiente() {
    subir(TipoDocumentoSolicitud.DNI_C4, EstadoFirmaDigital.NO_APLICA, iaNoDisponible, null);
    assertEquals(EstadoValidacionDocumento.EN_VALIDACION, solicitud.getEstadoDniC4());
  }
  @Test void iaPositivaNoSustituyeUnDniRechazadoPorLaApi() {
    subir(TipoDocumentoSolicitud.DNI_C4, EstadoFirmaDigital.NO_APLICA, iaCorrecta, false);
    assertEquals(EstadoValidacionDocumento.OBSERVADO, solicitud.getEstadoDniC4());
  }
  @Test void iaPositivaNoSustituyeUnaConsultaDniNoDisponible() {
    subir(TipoDocumentoSolicitud.DNI_C4, EstadoFirmaDigital.NO_APLICA, iaCorrecta, null);
    assertEquals(EstadoValidacionDocumento.EN_VALIDACION, solicitud.getEstadoDniC4());
  }
  @Test void reciboLegibleNoNecesitaFirmaReniecNiConsultaDni() {
    subir(TipoDocumentoSolicitud.RECIBO_SERVICIO, EstadoFirmaDigital.NO_APLICA, iaCorrecta, null);
    assertEquals(EstadoValidacionDocumento.VALIDADO, solicitud.getEstadoRecibo());
  }
  @Test void respuestaTardiaNoSobrescribeUnaVersionMasReciente() {
    var actual = new SolicitudMatriculaDocumentoEntity();
    actual.setVersionId(UUID.randomUUID());
    when(documentos.findById(any())).thenReturn(Optional.of(actual));
    var ex = assertThrows(ResponseStatusException.class,
        () -> subir(TipoDocumentoSolicitud.DNI_C4, EstadoFirmaDigital.NO_APLICA, iaCorrecta, true));
    assertEquals(409, ex.getStatusCode().value());
    verifyNoInteractions(versiones);
  }
  @Test void noPermitePagoSiSoloElEstadoGlobalDiceValidado() {
    solicitud.setEstado(EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS);
    assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.crearPago(id,token)).getStatusCode().value());
    verifyNoInteractions(mercado);
  }
}
