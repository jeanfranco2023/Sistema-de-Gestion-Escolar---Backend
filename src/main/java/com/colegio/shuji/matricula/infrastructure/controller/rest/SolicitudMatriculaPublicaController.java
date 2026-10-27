package com.colegio.shuji.matricula.infrastructure.controller.rest;

import com.colegio.shuji.matricula.application.dto.in.CrearSolicitudMatriculaPublicaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.ObservarSolicitudMatriculaPublicaRequestDto;
import com.colegio.shuji.matricula.application.dto.out.CatalogoMatriculaPublicaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.PagoMatriculaPublicaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.SolicitudMatriculaPublicaAdminResponseDto;
import com.colegio.shuji.matricula.application.dto.out.SolicitudMatriculaPublicaResponseDto;
import com.colegio.shuji.matricula.application.port.in.GestionarPagoMatriculaPublicaUseCase;
import com.colegio.shuji.matricula.application.port.out.GeminiDocumentoMatriculaPort;
import com.colegio.shuji.matricula.application.port.out.FirmaDigitalDocumentoPort;
import com.colegio.shuji.matricula.application.dto.out.VersionDocumentoMatriculaResponseDto;
import com.colegio.shuji.matricula.application.port.out.ReniecServicePort;
import com.colegio.shuji.matricula.application.port.out.AlmacenDocumentosMatriculaPort;
import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
import com.colegio.shuji.matricula.infrastructure.service.SolicitudMatriculaPublicaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/solicitudes-matricula")
@Validated
@RequiredArgsConstructor
public class SolicitudMatriculaPublicaController {
  private static final String TOKEN_HEADER = "X-Solicitud-Matricula-Token";
  private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;

  private final SolicitudMatriculaPublicaService solicitudes;
  private final GestionarPagoMatriculaPublicaUseCase pagos;
  private final GeminiDocumentoMatriculaPort gemini;
  private final FirmaDigitalDocumentoPort firmas;
  private final com.colegio.shuji.matricula.application.service.VerificacionIdentidadMatriculaService identidades;
  private final ReniecServicePort consultaDni;
  private final AlmacenDocumentosMatriculaPort almacenDocumentos;

  @GetMapping("/public/catalogo")
  public CatalogoMatriculaPublicaResponseDto catalogo() {
    return solicitudes.catalogo();
  }

  @GetMapping("/public/dni/{dni}")
  public ReniecServicePort.Identidad consultarDni(@PathVariable @jakarta.validation.constraints.Pattern(regexp = "[0-9]{8}") String dni) {
    return consultaDni.consultar(dni)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "No se encontraron nombres para el DNI o el proveedor no está disponible"));
  }

  @PostMapping("/public")
  @PreAuthorize("permitAll()")
  public SolicitudMatriculaPublicaResponseDto crear(
      @Valid @RequestBody CrearSolicitudMatriculaPublicaRequestDto request) {
    return solicitudes.crear(request);
  }

  @GetMapping("/public/{id}")
  public SolicitudMatriculaPublicaResponseDto consultar(
      @PathVariable UUID id,
      @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
    return solicitudes.consultar(id, token);
  }

  @PostMapping(value = "/public/{id}/documentos/{tipo}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public SolicitudMatriculaPublicaResponseDto validarDocumento(
      @PathVariable UUID id,
      @PathVariable TipoDocumentoSolicitud tipo,
      @RequestHeader(value = TOKEN_HEADER, required = false) String token,
      @RequestPart("archivo") MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty() || archivo.getSize() > MAX_FILE_BYTES) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El documento debe pesar hasta 5 MB");
    }
    var declarados = solicitudes.prepararValidacion(id, token, tipo);
    final byte[] bytes;
    try {
      bytes = archivo.getBytes();
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el documento");
    }
    AlmacenDocumentosMatriculaPort.ArchivoGuardado referenciaArchivo = null;
    try {
      almacenDocumentos.validarConfiguracion();
      String mimeType = detectarMime(bytes);
      var firma = tipo == TipoDocumentoSolicitud.RECIBO_SERVICIO
          ? new FirmaDigitalDocumentoPort.Resultado(com.colegio.shuji.matricula.domain.enums.EstadoFirmaDigital.NO_APLICA,
              "La comprobación de firma RENIEC no aplica al recibo de servicio.", null)
          : firmas.verificar(bytes, mimeType, tipo == TipoDocumentoSolicitud.PARTIDA_NACIMIENTO);
      GeminiDocumentoMatriculaPort.Resultado resultado;
      Boolean dniApiCoincide = null;
      try {
        var identidad = tipo == TipoDocumentoSolicitud.RECIBO_SERVICIO
            ? com.colegio.shuji.matricula.application.service.VerificacionIdentidadMatriculaService.Resultado.COINCIDE
            : identidades.verificar(declarados.nombres(), declarados.dni());
        if (tipo == TipoDocumentoSolicitud.PARTIDA_NACIMIENTO
            && identidad == com.colegio.shuji.matricula.application.service.VerificacionIdentidadMatriculaService.Resultado.COINCIDE) {
          identidad = identidades.verificar(declarados.progenitor().nombres(), declarados.progenitor().dni());
        }
        if (tipo != TipoDocumentoSolicitud.RECIBO_SERVICIO) {
          dniApiCoincide = identidad == com.colegio.shuji.matricula.application.service.VerificacionIdentidadMatriculaService.Resultado.NO_DISPONIBLE
              ? null : identidad == com.colegio.shuji.matricula.application.service.VerificacionIdentidadMatriculaService.Resultado.COINCIDE;
        }
        if (firma.estado() == com.colegio.shuji.matricula.domain.enums.EstadoFirmaDigital.INVALIDA) {
          resultado = new GeminiDocumentoMatriculaPort.Resultado(false, false, false, "Archivo o firma inválida.", false);
        } else if (identidad == com.colegio.shuji.matricula.application.service.VerificacionIdentidadMatriculaService.Resultado.NO_DISPONIBLE) {
          resultado = new GeminiDocumentoMatriculaPort.Resultado(false, false, false,
              "La API de DNI no confirmó la identidad; reintenta cuando el proveedor esté disponible.", false);
        } else if (identidad == com.colegio.shuji.matricula.application.service.VerificacionIdentidadMatriculaService.Resultado.NO_COINCIDE) {
          resultado = new GeminiDocumentoMatriculaPort.Resultado(true, true, false,
              "El DNI y los nombres declarados no coinciden con la API de identidad.", false);
        } else {
          resultado = gemini.validar(tipo, bytes, mimeType, declarados.nombres(), declarados.dni(), declarados.progenitor());
        }
      } catch (ResponseStatusException ex) {
        if (!ex.getStatusCode().is5xxServerError()) throw ex;
        resultado = new GeminiDocumentoMatriculaPort.Resultado(false, false, false,
            "Análisis automático no disponible; vuelve a intentar la verificación.", false);
      } catch (com.colegio.shuji.shared.domain.exception.BusinessException ex) {
        resultado = new GeminiDocumentoMatriculaPort.Resultado(false, false, false,
            "La respuesta automática no pudo interpretarse; vuelve a intentar la verificación.", false);
      }
      referenciaArchivo = almacenDocumentos.guardar(id, tipo, bytes, mimeType);
      String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
      return solicitudes.guardarResultadoDocumento(
          id, token, tipo, resultado, firma, dniApiCoincide, declarados.versionActual(), referenciaArchivo.bucket(), referenciaArchivo.objectKey(), mimeType, bytes.length, sha256);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 no está disponible", ex);
    } catch (RuntimeException ex) {
      if (referenciaArchivo != null) {
        try { almacenDocumentos.eliminar(referenciaArchivo); }
        catch (RuntimeException limpieza) { ex.addSuppressed(limpieza); }
      }
      throw ex;
    } finally {
      java.util.Arrays.fill(bytes, (byte) 0);
    }
  }

  @PostMapping("/public/{id}/pago")
  public PagoMatriculaPublicaResponseDto crearPago(
      @PathVariable UUID id,
      @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
    return solicitudes.crearPago(id, token);
  }

  @PostMapping("/public/{id}/confirmar-pago")
  public SolicitudMatriculaPublicaResponseDto confirmarPago(
      @PathVariable UUID id,
      @RequestHeader(value = TOKEN_HEADER, required = false) String token,
      @Valid @RequestBody ConfirmarPagoPublicoRequest request) {
    return pagos.confirmarRetorno(id, token, request.paymentId());
  }

  @PostMapping("/public/{id}/consultar-pago")
  public SolicitudMatriculaPublicaResponseDto consultarPago(
      @PathVariable UUID id, @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
    return pagos.consultarPago(id, token);
  }

  @GetMapping("/public/{id}/comprobante")
  public ResponseEntity<byte[]> descargarComprobante(
      @PathVariable UUID id,
      @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
    return documentoHtml("comprobante-matricula-" + id + ".html", solicitudes.generarComprobanteHtml(id, token));
  }

  @GetMapping("/public/{id}/ficha")
  public ResponseEntity<byte[]> descargarFicha(
      @PathVariable UUID id,
      @RequestHeader(value = TOKEN_HEADER, required = false) String token) {
    return documentoHtml("ficha-matricula-" + id + ".html", solicitudes.generarFichaMatriculaHtml(id, token));
  }

  @GetMapping("/admin")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  public List<SolicitudMatriculaPublicaAdminResponseDto> listarAdmin() {
    return solicitudes.listarAdmin();
  }

  @PutMapping("/admin/{id}/observar")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  public SolicitudMatriculaPublicaAdminResponseDto observarAdmin(
      @PathVariable UUID id,
      @Valid @RequestBody ObservarSolicitudMatriculaPublicaRequestDto request) {
    return solicitudes.observarAdmin(id, request);
  }

  @GetMapping("/admin/{id}/documentos/{tipo}/historial")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  public List<VersionDocumentoMatriculaResponseDto> historialDocumento(@PathVariable UUID id,
      @PathVariable TipoDocumentoSolicitud tipo) {
    return solicitudes.historialDocumento(id, tipo);
  }

  @PostMapping("/admin/{id}/rechazar")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  public void rechazarAdmin(@PathVariable UUID id) {
    solicitudes.rechazarAdmin(id);
  }

  @GetMapping("/admin/{id}/documentos/{tipo}/url")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  public DocumentoAdminDescarga descargarDocumentoAdmin(
      @PathVariable UUID id, @PathVariable TipoDocumentoSolicitud tipo) {
    var documento = solicitudes.documentoDescargaAdmin(id, tipo);
    return new DocumentoAdminDescarga(
        almacenDocumentos.urlFirmada(documento.bucket(), documento.objectKey()),
        documento.mimeType(), documento.tamanoBytes(), documento.fechaActualizacion(), documento.versionId());
  }

  public record DocumentoAdminDescarga(String url, String mimeType, long tamanoBytes,
                                       java.time.OffsetDateTime fechaActualizacion, UUID versionId) {}

  private String detectarMime(byte[] bytes) {
    if (bytes.length >= 5 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F' && bytes[4] == '-') {
      try (var pdf = org.apache.pdfbox.Loader.loadPDF(bytes)) {
        if (pdf.isEncrypted() || pdf.getNumberOfPages() == 0) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adjunta un PDF legible, con páginas y sin contraseña");
        }
        return MediaType.APPLICATION_PDF_VALUE;
      } catch (IOException ex) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo no es un PDF válido o está protegido");
      }
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los tres documentos deben ser PDF; no se admiten imágenes");
  }

  public record ConfirmarPagoPublicoRequest(@NotBlank String paymentId) {}

  private ResponseEntity<byte[]> documentoHtml(String nombre, String contenido) {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/html;charset=UTF-8"))
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(nombre, StandardCharsets.UTF_8).build().toString())
        .body(contenido.getBytes(StandardCharsets.UTF_8));
  }
}
