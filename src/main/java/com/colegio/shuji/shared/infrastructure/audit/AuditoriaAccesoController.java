package com.colegio.shuji.shared.infrastructure.audit;

import com.colegio.shuji.shared.application.dto.out.AuditoriaAccesoResponseDto;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auditoria/accesos")
@Validated
@RequiredArgsConstructor
public class AuditoriaAccesoController {

  private final AuditoriaAccesoService auditoria;

  @GetMapping
  @PreAuthorize("hasRole('DIRECCION')")
  public Page<AuditoriaAccesoResponseDto> listar(
      @RequestParam(defaultValue = "0") @PositiveOrZero int page,
      @RequestParam(defaultValue = "50") @Positive @Max(100) int size) {
    return auditoria
        .listar(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaEvento")))
        .map(AuditoriaAccesoController::aDto);
  }

  private static AuditoriaAccesoResponseDto aDto(AuditoriaAccesoEntity evento) {
    return new AuditoriaAccesoResponseDto(
        evento.getId(),
        evento.getSolicitudId(),
        evento.getUsuarioId(),
        evento.getUsuarioNombre(),
        evento.getRoles(),
        evento.getMetodo(),
        evento.getRuta(),
        evento.getEstadoHttp(),
        evento.getDireccionIp(),
        evento.getAgenteUsuario(),
        evento.getDuracionMs(),
        evento.getFechaEvento());
  }
}
