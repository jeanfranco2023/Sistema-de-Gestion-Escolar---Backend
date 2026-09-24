package com.colegio.shuji.matricula.application.dto.in;

import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegistrarApoderadoRequestDto(
    @NotNull TipoDocumento tipoDocumento,
    @NotBlank @Size(max = 15) String numeroDocumento,
    @NotBlank @Size(max = 100) String nombres,
    @NotBlank @Size(max = 20) String apellidoPaterno,
    @NotBlank @Size(max = 20) String apellidoMaterno,
    @NotBlank @Pattern(regexp = "9[0-9]{8}") String celular,
    @Email @Size(max = 254) String email,
    @NotBlank @Size(max = 200) String direccion,
    @NotBlank @Pattern(regexp = "[0-9]{6}") String ubigeoInei,
    Long usuarioId) {}
