package com.colegio.shuji.matricula.infrastructure.controller.rest;
import com.colegio.shuji.matricula.application.port.in.GestionarSuplenciasUseCase;
import com.colegio.shuji.matricula.application.dto.in.CrearSuplenciaRequestDto;
import com.colegio.shuji.matricula.application.dto.out.SuplenciaResponseDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequestMapping("/api/v1/suplencias") @RequiredArgsConstructor
public class SuplenciaController {
  private final GestionarSuplenciasUseCase suplencias;
  public record Motivo(@NotBlank @Size(max=500) String motivo) {}
  @PostMapping @PreAuthorize("hasRole('APODERADO')")
  public SuplenciaResponseDto solicitar(@Valid @RequestBody CrearSuplenciaRequestDto request) { return suplencias.solicitar(request); }
  @GetMapping("/mis-solicitudes") @PreAuthorize("hasRole('APODERADO')")
  public List<SuplenciaResponseDto> misSolicitudes() { return suplencias.misSolicitudes(); }
  @GetMapping("/pendientes") @PreAuthorize("hasRole('DIRECCION')")
  public List<SuplenciaResponseDto> pendientes() { return suplencias.bandeja(); }
  @PutMapping("/{id}/aprobar") @PreAuthorize("hasRole('DIRECCION')")
  public SuplenciaResponseDto aprobar(@PathVariable Long id) { return suplencias.aprobar(id); }
  @PutMapping("/{id}/rechazar") @PreAuthorize("hasRole('DIRECCION')")
  public SuplenciaResponseDto rechazar(@PathVariable Long id,@Valid @RequestBody Motivo request) { return suplencias.rechazar(id,request.motivo()); }
  @PutMapping("/{id}/revocar") @PreAuthorize("hasRole('DIRECCION')")
  public SuplenciaResponseDto revocar(@PathVariable Long id,@Valid @RequestBody Motivo request) { return suplencias.revocar(id,request.motivo()); }
}
