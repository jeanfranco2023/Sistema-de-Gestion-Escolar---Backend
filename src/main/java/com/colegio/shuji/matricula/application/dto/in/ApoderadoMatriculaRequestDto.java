package com.colegio.shuji.matricula.application.dto.in;

import com.colegio.shuji.matricula.domain.enums.Parentesco;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ApoderadoMatriculaRequestDto(
    @NotNull TipoDocumento tipoDocumento,
    @NotBlank @Size(min = 8, max = 8) @Pattern(regexp = "[0-9]{8}") String numeroDocumento,
    @NotBlank @Size(max = 30) @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "Los nombres solo deben contener letras y espacios") String nombres,
    @NotBlank @Size(max = 30) @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "El apellido paterno solo debe contener letras y espacios") String apellidoPaterno,
    @NotBlank @Size(max = 30) @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s'-]+$", message = "El apellido materno solo debe contener letras y espacios") String apellidoMaterno,
    @NotBlank @Pattern(regexp = "9[0-9]{8}") String celular,
    @Email @Size(max = 100) String email,
    @NotBlank @Size(max = 150) @Pattern(regexp = "^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑüÜ\\s#.,\\-_/°º]+$", message = "La dirección no permite caracteres especiales extraños") String direccion,
    @NotBlank @Pattern(regexp = "[0-9]{6}") String ubigeoInei,
    @NotNull Parentesco parentesco) {}
