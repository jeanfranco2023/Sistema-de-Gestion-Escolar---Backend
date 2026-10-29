package com.colegio.shuji.asistencia.infrastructure.controller.rest;

import com.colegio.shuji.asistencia.application.dto.in.ImportarLoteBiometricoRequestDto;
import com.colegio.shuji.asistencia.application.dto.in.RegistrarAsistenciaAulaRequestDto;
import com.colegio.shuji.asistencia.application.dto.out.AsistenciaAulaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.ConciliacionAsistenciaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.DiscrepanciaAlertaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.DocenteAsistenciaDiariaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.DocenteMatriculaAsistenciaDto;
import com.colegio.shuji.asistencia.application.dto.out.LoteBiometricoResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.ReporteAsistenciaDiariaResponseDto;
import com.colegio.shuji.asistencia.application.port.in.EjecutarConciliacionDiariaUseCase;
import com.colegio.shuji.asistencia.application.port.in.ProcesarBiometricoUseCase;
import com.colegio.shuji.asistencia.application.port.in.RegistrarListaAulaUseCase;
import com.colegio.shuji.asistencia.application.port.out.AlmacenJustificacionPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import java.io.IOException;
import java.util.Locale;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/asistencia")
@Validated
@RequiredArgsConstructor
@Tag(name = "Asistencia")
public class AsistenciaController {
  private final ProcesarBiometricoUseCase biometrico;
  private final RegistrarListaAulaUseCase aula;
  private final EjecutarConciliacionDiariaUseCase conciliacion;
  private final AlmacenJustificacionPort almacenamiento;

  @PostMapping("/biometrico/importar")
  @PreAuthorize("hasAnyRole('SECRETARIA','AUXILIAR')")
  @Operation(summary = "biometrico.importar")
  public LoteBiometricoResponseDto importarLoteBiometrico(
      @Valid @RequestBody ImportarLoteBiometricoRequestDto r) {
    return biometrico.importar(r);
  }

  @PostMapping(value = "/biometrico/importar-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('SECRETARIA','AUXILIAR')")
  @Operation(summary = "biometrico.importarCsv")
  public LoteBiometricoResponseDto importarArchivoBiometrico(@org.springframework.web.bind.annotation.RequestPart("archivo") MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty() || archivo.getSize() > 5L * 1024 * 1024
        || archivo.getOriginalFilename() == null
        || !(archivo.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".csv")
            || archivo.getOriginalFilename().toLowerCase(Locale.ROOT).endsWith(".xlsx"))) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adjunta un CSV o XLSX biométrico de hasta 5 MB");
    }
    try {
      var marcas = com.colegio.shuji.asistencia.infrastructure.adapter.LectorArchivoBiometrico.leer(
          archivo.getBytes(), archivo.getOriginalFilename());
      var nombre = archivo.getOriginalFilename().replaceAll("[\\r\\n\\\\/]+", "_");
      return biometrico.importar(new ImportarLoteBiometricoRequestDto(nombre, marcas));
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el CSV biométrico");
    }
  }

  @PostMapping("/aula")
  @PreAuthorize("hasAnyRole('AUXILIAR','TUTOR','DOCENTE')")
  @Operation(summary = "aula.registrar")
  public AsistenciaAulaResponseDto registrarAsistenciaAula(
      @Valid @RequestBody RegistrarAsistenciaAulaRequestDto r) {
    return aula.registrar(r);
  }

  @GetMapping("/aula/docente/matriculas")
  @PreAuthorize("hasAnyRole('DOCENTE','TUTOR')")
  @Operation(summary = "aula.docente.matriculas")
  public List<DocenteMatriculaAsistenciaDto> listarMatriculasDelDocente() {
    return aula.matriculasDelDocente();
  }

  @GetMapping("/aula/docente")
  @PreAuthorize("hasAnyRole('DOCENTE','TUTOR')")
  @Operation(summary = "aula.docente.reporte")
  public List<DocenteAsistenciaDiariaResponseDto> reporteDocente(@RequestParam LocalDate fecha) {
    return aula.reporteDelDocente(fecha);
  }

  @PostMapping(value = "/aula/justificacion", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  @PreAuthorize("hasAnyRole('SECRETARIA','AUXILIAR','TUTOR')")
  @Operation(summary = "aula.justificar")
  public AsistenciaAulaResponseDto justificarInasistencia(
      @RequestParam @jakarta.validation.constraints.Positive Long asistenciaId,
      @RequestParam @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 4000) String motivo,
      @RequestPart("archivo") MultipartFile archivo) {
    if (archivo == null || archivo.isEmpty() || archivo.getSize() > 5L * 1024 * 1024) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adjunta un PDF de hasta 5 MB");
    }
    byte[] bytes = null;
    String clave = null;
    try {
      bytes = archivo.getBytes();
      if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F' || bytes[4] != '-') {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El sustento debe ser un PDF válido");
      }
      try (var pdf = org.apache.pdfbox.Loader.loadPDF(bytes)) {
        if (pdf.isEncrypted() || pdf.getNumberOfPages() == 0) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El PDF debe ser legible, tener páginas y no usar contraseña");
        }
      }
      clave = almacenamiento.guardar(asistenciaId, bytes).clave();
      return aula.justificar(asistenciaId, motivo, clave);
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer el PDF de sustento");
    } catch (RuntimeException ex) {
      if (clave != null) {
        try { almacenamiento.eliminar(clave); }
        catch (RuntimeException limpieza) { ex.addSuppressed(limpieza); }
      }
      throw ex;
    } finally {
      if (bytes != null) Arrays.fill(bytes, (byte) 0);
    }
  }

  @GetMapping("/aula/{asistenciaId}/justificacion/documento")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "aula.documentoSustentoPrivado")
  public DocumentoSustentoResponse descargarSustentoPrivado(@PathVariable Long asistenciaId) {
    return new DocumentoSustentoResponse(almacenamiento.urlFirmada(aula.documentoSustentoKey(asistenciaId)));
  }

  public record DocumentoSustentoResponse(String url) {}

  @GetMapping("/aula")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR')")
  @Operation(summary = "aula.reporte")
  public ReporteAsistenciaDiariaResponseDto obtenerReporteDiario(@RequestParam LocalDate fecha) {
    return aula.reporte(fecha);
  }

  @PostMapping("/conciliacion")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR')")
  @Operation(summary = "conciliacion.conciliar")
  public List<ConciliacionAsistenciaResponseDto> conciliarAsistencia(
      @RequestParam Short anioId,
      @RequestParam LocalDate fecha,
      @RequestParam boolean turnoCerrado) {
    return conciliacion.conciliar(anioId, fecha, turnoCerrado);
  }

  @GetMapping("/conciliacion/alertas")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','AUXILIAR')")
  @Operation(summary = "conciliacion.alertas")
  public List<DiscrepanciaAlertaResponseDto> consultarAlertas(@RequestParam LocalDate fecha) {
    return conciliacion.alertas(fecha);
  }
}
