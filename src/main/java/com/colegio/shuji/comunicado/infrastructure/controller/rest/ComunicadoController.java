package com.colegio.shuji.comunicado.infrastructure.controller.rest;

import com.colegio.shuji.comunicado.application.dto.in.ConfirmarAcuseReciboRequestDto;
import com.colegio.shuji.comunicado.application.dto.in.EmitirComunicadoRequestDto;
import com.colegio.shuji.comunicado.application.dto.out.BandejaApoderadoResponseDto;
import com.colegio.shuji.comunicado.application.dto.out.ComunicadoDestinatarioResponseDto;
import com.colegio.shuji.comunicado.application.dto.out.ComunicadoResponseDto;
import com.colegio.shuji.comunicado.application.dto.out.MetricasLecturaResponseDto;
import com.colegio.shuji.comunicado.application.port.in.ConfirmarLecturaUseCase;
import com.colegio.shuji.comunicado.application.port.in.ConsultarBandejaUseCase;
import com.colegio.shuji.comunicado.application.port.in.PublicarComunicadoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/comunicados")
@Validated
@RequiredArgsConstructor
@Tag(name = "Comunicado")
public class ComunicadoController {
  private final PublicarComunicadoUseCase publicacion;
  private final ConfirmarLecturaUseCase lectura;
  private final ConsultarBandejaUseCase consulta;

  @PostMapping("")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "publicacion.publicar")
  public ComunicadoResponseDto publicarComunicado(@Valid @RequestBody EmitirComunicadoRequestDto r) {
    return publicacion.publicar(r);
  }

  @GetMapping("")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  public List<ComunicadoResponseDto> listarPublicados() {
    return publicacion.listarPublicados();
  }

  @GetMapping("/{id}/metricas")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "publicacion.metricas")
  public MetricasLecturaResponseDto consultarMetricas(@PathVariable @Positive Long id) {
    return publicacion.metricas(id);
  }

  @GetMapping("/apoderado/bandeja")
  @PreAuthorize("hasRole('APODERADO')")
  @Operation(summary = "consulta.bandeja")
  public BandejaApoderadoResponseDto consultarBandeja(
      @RequestParam(name = "page", defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(name = "size", defaultValue = "20") @Positive @Max(100) int size) {
    BandejaApoderadoResponseDto bandeja = consulta.bandeja();
    if (bandeja == null || bandeja.comunicados() == null || bandeja.comunicados().isEmpty()) {
      return bandeja;
    }
    int start = Math.min(Math.max(0, page) * Math.max(1, size), bandeja.comunicados().size());
    int end = Math.min(start + Math.max(1, size), bandeja.comunicados().size());
    return new BandejaApoderadoResponseDto(
        bandeja.apoderadoId(), bandeja.comunicados().subList(start, end));
  }

  @PostMapping("/{id}/acuse")
  @PreAuthorize("hasRole('APODERADO')")
  @Operation(summary = "lectura.confirmar")
  public ComunicadoDestinatarioResponseDto confirmarAcuse(
      @PathVariable @Positive Long id, @Valid @RequestBody ConfirmarAcuseReciboRequestDto r) {
    return lectura.confirmar(id, r);
  }
}
