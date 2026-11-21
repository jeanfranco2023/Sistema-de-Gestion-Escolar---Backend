package com.colegio.shuji.usuario.infrastructure.controller.rest;

import com.colegio.shuji.usuario.application.dto.out.CambioAuditoriaResponseDto;
import com.colegio.shuji.usuario.application.port.in.ConsultarAuditoriaCambiosUseCase;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auditoria/cambios")
@Validated
@RequiredArgsConstructor
public class AuditoriaCambiosController {

  private final ConsultarAuditoriaCambiosUseCase consulta;

  @GetMapping
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public Page<CambioAuditoriaResponseDto> listar(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fechaInicio,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant fechaFin,
      @RequestParam(required = false) Long usuarioId,
      @RequestParam(required = false) String tabla,
      @RequestParam(defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(defaultValue = "50") @Positive @Max(200) int size,
      @RequestParam(required = false) Integer limite) {
    int pageSize = (limite != null && limite > 0) ? Math.min(limite, 200) : size;
    return consulta.filtrar(fechaInicio, fechaFin, usuarioId, tabla, PageRequest.of(page, pageSize));
  }

  @GetMapping("/recientes")
  @PreAuthorize("hasRole('ADMINISTRADOR')")
  public List<CambioAuditoriaResponseDto> listarRecientes(
      @RequestParam(defaultValue = "100") @Positive @Max(200) int limite) {
    return consulta.listarRecientes(limite);
  }
}
