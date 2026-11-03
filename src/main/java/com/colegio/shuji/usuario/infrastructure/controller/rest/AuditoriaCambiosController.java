package com.colegio.shuji.usuario.infrastructure.controller.rest;

import com.colegio.shuji.usuario.application.dto.out.CambioAuditoriaResponseDto;
import com.colegio.shuji.usuario.application.port.in.ConsultarAuditoriaCambiosUseCase;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/v1/auditoria/cambios")
@Validated
@RequiredArgsConstructor
public class AuditoriaCambiosController {

  private final ConsultarAuditoriaCambiosUseCase consulta;

  @GetMapping
  @PreAuthorize("hasRole('DIRECCION')")
  public List<CambioAuditoriaResponseDto> listar(
      @RequestParam(defaultValue = "100") @Positive @Max(200) int limite) {
    return consulta.listarRecientes(limite);
  }
}
