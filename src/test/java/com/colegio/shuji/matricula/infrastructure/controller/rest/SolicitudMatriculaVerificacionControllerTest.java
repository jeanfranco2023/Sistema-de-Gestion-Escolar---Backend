package com.colegio.shuji.matricula.infrastructure.controller.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.colegio.shuji.matricula.application.port.in.GestionarPagoMatriculaPublicaUseCase;
import com.colegio.shuji.matricula.application.port.out.AlmacenDocumentosMatriculaPort;
import com.colegio.shuji.matricula.application.port.out.FirmaDigitalDocumentoPort;
import com.colegio.shuji.matricula.application.port.out.GeminiDocumentoMatriculaPort;
import com.colegio.shuji.matricula.application.port.out.ReniecServicePort;
import com.colegio.shuji.matricula.application.service.VerificacionIdentidadMatriculaService;
import com.colegio.shuji.matricula.domain.enums.EstadoFirmaDigital;
import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
import com.colegio.shuji.matricula.infrastructure.service.SolicitudMatriculaPublicaService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class SolicitudMatriculaVerificacionControllerTest {
  @Mock SolicitudMatriculaPublicaService solicitudes;
  @Mock GestionarPagoMatriculaPublicaUseCase pagos;
  @Mock GeminiDocumentoMatriculaPort gemini;
  @Mock FirmaDigitalDocumentoPort firmas;
  @Mock VerificacionIdentidadMatriculaService identidades;
  @Mock ReniecServicePort consultaDni;
  @Mock AlmacenDocumentosMatriculaPort almacenDocumentos;
  @InjectMocks SolicitudMatriculaPublicaController controller;
  final UUID id = UUID.randomUUID();
  final String token = "token-prueba";
  final AlmacenDocumentosMatriculaPort.ArchivoGuardado archivo =
      new AlmacenDocumentosMatriculaPort.ArchivoGuardado("privado","ruta-aleatoria.pdf");
  final MockMultipartFile pdf = pdfValido();
  final GeminiDocumentoMatriculaPort.Resultado ia =
      new GeminiDocumentoMatriculaPort.Resultado(true,true,true,"Compatible.");

  private static MockMultipartFile pdfValido() {
    try (var documento = new org.apache.pdfbox.pdmodel.PDDocument(); var salida = new java.io.ByteArrayOutputStream()) {
      documento.addPage(new org.apache.pdfbox.pdmodel.PDPage());
      documento.save(salida);
      return new MockMultipartFile("archivo", "original.pdf", "application/pdf", salida.toByteArray());
    } catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
  }

  void preparar(TipoDocumentoSolicitud tipo) {
    when(solicitudes.prepararValidacion(id,token,tipo))
        .thenReturn(new SolicitudMatriculaPublicaService.PreparacionDocumento("Maria Perez Lopez","01234567",null, new GeminiDocumentoMatriculaPort.ContextoParental("Rosa Perez Lopez", "87654321", com.colegio.shuji.matricula.domain.enums.Parentesco.MADRE)));
    when(almacenDocumentos.guardar(eq(id),eq(tipo),any(),eq("application/pdf"))).thenReturn(archivo);
  }

  void confirmarEvidencia(TipoDocumentoSolicitud tipo, Boolean dni, EstadoFirmaDigital estado, boolean iaDisponible) {
    var analisis = ArgumentCaptor.forClass(GeminiDocumentoMatriculaPort.Resultado.class);
    var firma = ArgumentCaptor.forClass(FirmaDigitalDocumentoPort.Resultado.class);
    verify(solicitudes).guardarResultadoDocumento(eq(id),eq(token),eq(tipo),analisis.capture(),firma.capture(),eq(dni),isNull(),
        eq("privado"),eq("ruta-aleatoria.pdf"),eq("application/pdf"),eq(pdf.getSize()),matches("[0-9a-f]{64}"));
    assertEquals(estado,firma.getValue().estado());
    assertEquals(iaDisponible,analisis.getValue().disponible());
  }

  @Test void partidaIntegraFirmaReniecApiDniEIA() {
    var tipo = TipoDocumentoSolicitud.PARTIDA_NACIMIENTO;
    preparar(tipo);
    when(firmas.verificar(any(),eq("application/pdf"),eq(true)))
        .thenReturn(new FirmaDigitalDocumentoPort.Resultado(EstadoFirmaDigital.VALIDA,"Verificada.",null));
    when(identidades.verificar(anyString(),anyString())).thenReturn(VerificacionIdentidadMatriculaService.Resultado.COINCIDE);
    when(gemini.validar(eq(tipo),any(),eq("application/pdf"),eq("Maria Perez Lopez"),eq("01234567"),any())).thenReturn(ia);
    controller.validarDocumento(id,tipo,token,pdf);
    confirmarEvidencia(tipo,true,EstadoFirmaDigital.VALIDA,true);
    verify(identidades).verificar("Rosa Perez Lopez", "87654321");
  }
  @Test void reciboNoRequierePkiReniecNiConsultaDni() {
    var tipo = TipoDocumentoSolicitud.RECIBO_SERVICIO;
    preparar(tipo);
    when(gemini.validar(eq(tipo),any(),eq("application/pdf"),anyString(),anyString(),any())).thenReturn(ia);
    controller.validarDocumento(id,tipo,token,pdf);
    verifyNoInteractions(firmas,identidades);
    confirmarEvidencia(tipo,null,EstadoFirmaDigital.NO_APLICA,true);
  }
  @Test void dniQueNoCoincideNoSeDelegaALaIaParaQueLoApruebe() {
    var tipo = TipoDocumentoSolicitud.DNI_C4;
    preparar(tipo);
    when(firmas.verificar(any(),eq("application/pdf"),eq(false)))
        .thenReturn(new FirmaDigitalDocumentoPort.Resultado(EstadoFirmaDigital.NO_APLICA,"Sin firma.",null));
    when(identidades.verificar(anyString(),anyString())).thenReturn(VerificacionIdentidadMatriculaService.Resultado.NO_COINCIDE);
    controller.validarDocumento(id,tipo,token,pdf);
    verifyNoInteractions(gemini);
    confirmarEvidencia(tipo,false,EstadoFirmaDigital.NO_APLICA,false);
  }
  @Test void tokenInvalidoSeRechazaAntesDeConsultarProveedores() {
    when(solicitudes.prepararValidacion(id,token,TipoDocumentoSolicitud.PARTIDA_NACIMIENTO))
        .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
    assertThrows(ResponseStatusException.class,()-> controller.validarDocumento(id,TipoDocumentoSolicitud.PARTIDA_NACIMIENTO,token,pdf));
    verifyNoInteractions(almacenDocumentos,gemini,firmas,identidades);
  }
  @Test void iaIndisponibleSeRegistraSinInventarValidacion() {
    var tipo = TipoDocumentoSolicitud.RECIBO_SERVICIO;
    preparar(tipo);
    when(gemini.validar(eq(tipo),any(),anyString(),anyString(),anyString(),any()))
        .thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE));
    controller.validarDocumento(id,tipo,token,pdf);
    confirmarEvidencia(tipo,null,EstadoFirmaDigital.NO_APLICA,false);
  }
  @Test void falloPersistiendoElResultadoLimpiaLaCargaNueva() {
    var tipo = TipoDocumentoSolicitud.RECIBO_SERVICIO;
    preparar(tipo);
    when(gemini.validar(eq(tipo),any(),anyString(),anyString(),anyString(),any())).thenReturn(ia);
    when(solicitudes.guardarResultadoDocumento(eq(id),eq(token),eq(tipo),any(),any(),isNull(),isNull(),
        anyString(),anyString(),anyString(),anyLong(),anyString()))
        .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT));
    assertThrows(ResponseStatusException.class,()-> controller.validarDocumento(id,tipo,token,pdf));
    verify(almacenDocumentos).eliminar(archivo);
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(TipoDocumentoSolicitud.class)
  void rechazaImagenDisfrazadaDePdfEnLosTresDocumentos(TipoDocumentoSolicitud tipo) {
    when(solicitudes.prepararValidacion(id,token,tipo)).thenReturn(
        new SolicitudMatriculaPublicaService.PreparacionDocumento("Maria Perez Lopez","01234567",null,
            new GeminiDocumentoMatriculaPort.ContextoParental("Rosa Perez Lopez","87654321",
                com.colegio.shuji.matricula.domain.enums.Parentesco.MADRE)));
    var imagen = new MockMultipartFile("archivo","falso.pdf","application/pdf",new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff});
    assertEquals(400,assertThrows(ResponseStatusException.class,()->controller.validarDocumento(id,tipo,token,imagen)).getStatusCode().value());
    verifyNoInteractions(gemini,firmas,identidades);
    verify(almacenDocumentos,never()).guardar(any(),any(),any(),any());
  }

  @Test void padreConDniDistintoNoPuedeSerAprobadoPorLaIa() {
    var tipo = TipoDocumentoSolicitud.PARTIDA_NACIMIENTO;
    preparar(tipo);
    when(firmas.verificar(any(),eq("application/pdf"),eq(true)))
        .thenReturn(new FirmaDigitalDocumentoPort.Resultado(EstadoFirmaDigital.VALIDA,"Verificada.",null));
    when(identidades.verificar("Maria Perez Lopez","01234567")).thenReturn(VerificacionIdentidadMatriculaService.Resultado.COINCIDE);
    when(identidades.verificar("Rosa Perez Lopez","87654321")).thenReturn(VerificacionIdentidadMatriculaService.Resultado.NO_COINCIDE);
    controller.validarDocumento(id,tipo,token,pdf);
    verifyNoInteractions(gemini);
    confirmarEvidencia(tipo,false,EstadoFirmaDigital.VALIDA,false);
  }

  @Test void pdfTruncadoSeRechazaAntesDeConsultarProveedores() {
    when(solicitudes.prepararValidacion(id,token,TipoDocumentoSolicitud.RECIBO_SERVICIO)).thenReturn(
        new SolicitudMatriculaPublicaService.PreparacionDocumento("Maria","01234567",null,null));
    var falso = new MockMultipartFile("archivo","truncado.pdf","application/pdf","%PDF-1.7\nfixture".getBytes(StandardCharsets.US_ASCII));
    assertEquals(400,assertThrows(ResponseStatusException.class,()->controller.validarDocumento(id,TipoDocumentoSolicitud.RECIBO_SERVICIO,token,falso)).getStatusCode().value());
    verifyNoInteractions(gemini,firmas,identidades);
  }
}
