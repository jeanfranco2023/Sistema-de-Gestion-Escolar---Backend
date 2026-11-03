package com.colegio.shuji.evaluacion.infrastructure.controller.rest;

import com.colegio.shuji.evaluacion.application.dto.out.CuadroHonorResponseDto;
import com.colegio.shuji.evaluacion.application.port.in.ConsultarRankingAcademicoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/evaluacion/ranking")
@Validated
@RequiredArgsConstructor
@Tag(name = "Ranking")
public class RankingController {

  private final ConsultarRankingAcademicoUseCase rankingUseCase;

  @GetMapping("/seccion/{seccionId}")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE','TUTOR')")
  @Operation(summary = "Obtener ranking académico por sección")
  public CuadroHonorResponseDto obtenerRankingSeccion(@PathVariable @Positive Integer seccionId) {
    return rankingUseCase.obtenerRankingSeccion(seccionId);
  }

  @GetMapping("/cuadro-honor")
  @PreAuthorize("hasAnyRole('DIRECCION','SECRETARIA','DOCENTE','TUTOR')")
  @Operation(summary = "Obtener cuadro de honor por grado y/o año lectivo")
  public CuadroHonorResponseDto obtenerCuadroHonor(
      @RequestParam(value = "gradoId", required = false) @Positive Short gradoId,
      @RequestParam("anioLectivoId") @Positive Short anioLectivoId) {
    return rankingUseCase.obtenerCuadroHonor(gradoId, anioLectivoId);
  }
}
