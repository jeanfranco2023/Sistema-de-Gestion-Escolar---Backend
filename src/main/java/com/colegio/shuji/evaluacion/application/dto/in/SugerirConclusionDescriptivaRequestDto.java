package com.colegio.shuji.evaluacion.application.dto.in;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SugerirConclusionDescriptivaRequestDto(
    @Size(max = 20) String estudianteNombre,
    @NotBlank @Size(max = 50) String areaCurricular,
    @NotBlank @Size(max = 200) String competenciaDescripcion,
    @NotBlank @Pattern(regexp = "AD|A|B|C") String nivelLogro,
    @NotBlank @Size(max = 300) String desempenoObservado) {}
