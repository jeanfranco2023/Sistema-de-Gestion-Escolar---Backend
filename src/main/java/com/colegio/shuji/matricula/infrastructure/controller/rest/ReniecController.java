package com.colegio.shuji.matricula.infrastructure.controller.rest;

import com.colegio.shuji.matricula.application.dto.out.ConsultaReniecResponseDto;
import com.colegio.shuji.matricula.application.port.out.ReniecServicePort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/personas")
@Validated
@RequiredArgsConstructor
@Tag(name = "Personas", description = "Consultas de personas e identidades oficiales")
public class ReniecController {

  private final ReniecServicePort reniecServicePort;

  @GetMapping("/reniec/{dni}")
  @PreAuthorize("permitAll()")
  @Operation(summary = "Consultar datos de persona en RENIEC por DNI")
  public ConsultaReniecResponseDto consultarPorDni(
      @Parameter(description = "Número de DNI de 8 dígitos", example = "12345678")
      @PathVariable String dni) {

    if (dni == null || !dni.matches("^[0-9]{8}$")) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "El DNI debe tener exactamente 8 dígitos numéricos");
    }

    return reniecServicePort
        .consultar(dni)
        .map(identidad -> new ConsultaReniecResponseDto(
            identidad.numeroDocumento(),
            identidad.nombres(),
            identidad.apellidoPaterno(),
            identidad.apellidoMaterno()))
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Persona no encontrada en RENIEC"));
  }
}
