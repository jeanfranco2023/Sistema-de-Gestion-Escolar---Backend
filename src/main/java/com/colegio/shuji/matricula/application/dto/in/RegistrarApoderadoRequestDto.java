package com.colegio.shuji.matricula.application.dto.in;

import com.colegio.shuji.matricula.domain.enums.*;
import jakarta.validation.constraints.*;

public record RegistrarApoderadoRequestDto(
    @NotNull TipoDocumento tipoDocumento,
    @NotBlank @Size(max = 15) String numeroDocumento,
    @NotBlank @Size(max = 100) String nombres,
    @NotBlank @Size(max = 80) String apellidoPaterno,
    @NotBlank @Size(max = 80) String apellidoMaterno,
    @NotBlank @Pattern(regexp = "9[0-9]{8}") String celular,
    @Email @Size(max = 100) String email,
    @NotBlank @Size(max = 200) String direccion,
    @NotBlank @Pattern(regexp = "[0-9]{6}") String ubigeoInei,
    Long usuarioId) {}
