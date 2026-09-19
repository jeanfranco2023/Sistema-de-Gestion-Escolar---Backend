package com.colegio.shuji.usuario.infrastructure.controller.rest;

import com.colegio.shuji.config.security.UserPrincipal;
import com.colegio.shuji.shared.application.dto.ApiResponse;
import com.colegio.shuji.usuario.application.dto.in.CambiarEstadoRequestDto;
import com.colegio.shuji.usuario.application.dto.in.CambiarPasswordRequestDto;
import com.colegio.shuji.usuario.application.dto.in.UpdateUsuarioRequestDto;
import com.colegio.shuji.usuario.application.dto.out.UserResponseDto;
import com.colegio.shuji.usuario.application.port.in.GestionUsuarioUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Controlador REST para la gestión integral de cuentas de usuarios institucionales. */
@Slf4j
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(
    name = "Usuarios",
    description =
        "Endpoints de administración, perfiles y control de acceso de usuarios institucionales")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

  private final GestionUsuarioUseCase gestionUsuarioUseCase;

  @Operation(
      summary = "Obtener perfil propio",
      description = "Retorna la información del usuario actualmente autenticado")
  @GetMapping("/me")
  public ResponseEntity<ApiResponse<UserResponseDto>> obtenerMiPerfil(
      @AuthenticationPrincipal UserPrincipal principal) {

    UserResponseDto perfil = gestionUsuarioUseCase.obtenerPerfil(principal.getId());
    return ResponseEntity.ok(ApiResponse.ok(perfil, "Perfil obtenido exitosamente"));
  }

  @Operation(
      summary = "Actualizar perfil propio",
      description = "Actualiza los datos básicos del usuario autenticado")
  @PutMapping("/me")
  public ResponseEntity<ApiResponse<UserResponseDto>> actualizarMiPerfil(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody UpdateUsuarioRequestDto requestDto) {

    UserResponseDto actualizado =
        gestionUsuarioUseCase.actualizarPerfil(principal.getId(), requestDto);
    return ResponseEntity.ok(ApiResponse.ok(actualizado, "Perfil actualizado exitosamente"));
  }

  @Operation(
      summary = "Listar o filtrar usuarios por rol",
      description =
          "Lista todos los usuarios o filtra según el código de rol institucional (ej: DOCENTE,"
              + " SECRETARIA)")
  @GetMapping
  @PreAuthorize("hasAnyRole('DIRECCION', 'SECRETARIA')")
  public ResponseEntity<ApiResponse<List<UserResponseDto>>> listarUsuarios(
      @Parameter(description = "Código del rol (opcional)", example = "DOCENTE")
          @RequestParam(name = "rol", required = false)
          String rol) {

    List<UserResponseDto> usuarios = gestionUsuarioUseCase.listarPorRol(rol);
    return ResponseEntity.ok(ApiResponse.ok(usuarios, "Usuarios obtenidos exitosamente"));
  }

  @Operation(
      summary = "Cambiar estado activo/inactivo de un usuario",
      description = "Permite a Dirección o Secretaría activar o suspender una cuenta")
  @PatchMapping("/{id}/estado")
  @PreAuthorize("hasAnyRole('DIRECCION', 'SECRETARIA')")
  public ResponseEntity<ApiResponse<UserResponseDto>> cambiarEstado(
      @Parameter(description = "ID del usuario a modificar", required = true) @PathVariable("id")
          Long id,
      @Valid @RequestBody(required = false) CambiarEstadoRequestDto body,
      @RequestParam(name = "activo", required = false) Boolean activoParam) {

    Boolean nuevoEstado =
        (body != null && body.getActivo() != null) ? body.getActivo() : activoParam;
    if (nuevoEstado == null) {
      throw new IllegalArgumentException(
          "Debe indicar el estado 'activo' en el cuerpo o como parámetro de consulta.");
    }

    UserResponseDto usuario = gestionUsuarioUseCase.cambiarEstado(id, nuevoEstado);
    return ResponseEntity.ok(
        ApiResponse.ok(usuario, "Estado del usuario actualizado exitosamente"));
  }

  @Operation(
      summary = "Cambiar contraseña del usuario autenticado",
      description = "Permite al usuario actual modificar su contraseña validando la anterior")
  @PutMapping("/password")
  public ResponseEntity<ApiResponse<Void>> cambiarPassword(
      @AuthenticationPrincipal UserPrincipal principal,
      @Valid @RequestBody CambiarPasswordRequestDto requestDto) {

    gestionUsuarioUseCase.cambiarPassword(principal.getId(), requestDto);
    return ResponseEntity.ok(ApiResponse.ok(null, "Contraseña actualizada exitosamente"));
  }
}
