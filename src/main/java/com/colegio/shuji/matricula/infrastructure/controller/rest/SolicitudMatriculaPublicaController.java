package com.colegio.shuji.matricula.infrastructure.controller.rest;

import com.colegio.shuji.matricula.application.dto.in.CrearSolicitudMatriculaPublicaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.ObservarSolicitudMatriculaPublicaRequestDto;
import com.colegio.shuji.matricula.application.dto.out.CatalogoMatriculaPublicaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.PagoMatriculaPublicaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.SolicitudMatriculaPublicaAdminResponseDto;
import com.colegio.shuji.matricula.application.dto.out.SolicitudMatriculaPublicaResponseDto;
import com.colegio.shuji.matricula.application.port.out.GeminiDocumentoMatriculaPort;
import com.colegio.shuji.matricula.application.service.PagoMatriculaPublicaService;
import com.colegio.shuji.matricula.application.service.SolicitudMatriculaPublicaService;
import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
  private final PagoMatriculaPublicaService pagos;
  private final GeminiDocumentoMatriculaPort gemini;

  @GetMapping("/public/catalogo")
  public CatalogoMatriculaPublicaResponseDto catalogo() {
    return solicitudes.catalogo();
  }

  @PostMapping("/public")
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
    final byte[] bytes;
    try {
      bytes = archivo.getBytes();
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el documento");
    }
    try {
      String mimeType = detectarMime(bytes);
      String declarados = solicitudes.prepararValidacion(id, token, tipo);
      int separador = declarados.lastIndexOf('|');
      var resultado = gemini.validar(
          tipo, bytes, mimeType, declarados.substring(0, separador), declarados.substring(separador + 1));
      return solicitudes.guardarResultadoDocumento(id, token, tipo, resultado);
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

  @PostMapping("/admin/{id}/rechazar")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  public void rechazarAdmin(@PathVariable UUID id) {
    solicitudes.rechazarAdmin(id);
  }

  private String detectarMime(byte[] bytes) {
    if (bytes.length >= 5 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D' && bytes[3] == 'F' && bytes[4] == '-') {
      return MediaType.APPLICATION_PDF_VALUE;
    }
    if (bytes.length >= 3 && (bytes[0] & 0xff) == 0xff && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff) {
      return MediaType.IMAGE_JPEG_VALUE;
    }
    if (bytes.length >= 8 && (bytes[0] & 0xff) == 0x89 && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G') {
      return MediaType.IMAGE_PNG_VALUE;
    }
    if (bytes.length >= 12 && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
        && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
      return "image/webp";
    }
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formato no permitido. Usa PDF, JPG, PNG o WebP");
  }

  public record ConfirmarPagoPublicoRequest(@NotBlank String paymentId) {}
}
