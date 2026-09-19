package com.colegio.shuji.comunicado.infrastructure.controller.rest;

import com.colegio.shuji.comunicado.application.dto.in.*;
import com.colegio.shuji.comunicado.application.dto.out.*;
import com.colegio.shuji.comunicado.application.port.in.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

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
  public ComunicadoResponseDto operacion0(@Valid @RequestBody EmitirComunicadoRequestDto r) {
    return publicacion.publicar(r);
  }

  @GetMapping("/{id}/metricas")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA')")
  @Operation(summary = "publicacion.metricas")
  public MetricasLecturaResponseDto operacion1(@PathVariable @Positive Long id) {
    return publicacion.metricas(id);
  }

  @GetMapping("/apoderado/bandeja")
  @PreAuthorize("hasRole('APODERADO')")
  @Operation(summary = "consulta.bandeja")
  public BandejaApoderadoResponseDto operacion2() {
    return consulta.bandeja();
  }

  @PostMapping("/{id}/acuse")
  @PreAuthorize("hasRole('APODERADO')")
  @Operation(summary = "lectura.confirmar")
  public ComunicadoDestinatarioResponseDto operacion3(
      @PathVariable @Positive Long id, @Valid @RequestBody ConfirmarAcuseReciboRequestDto r) {
    return lectura.confirmar(id, r);
  }
}
