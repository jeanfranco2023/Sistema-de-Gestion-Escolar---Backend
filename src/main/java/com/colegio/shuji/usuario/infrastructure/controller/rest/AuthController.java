package com.colegio.shuji.usuario.infrastructure.controller.rest;

import com.colegio.shuji.shared.application.dto.ApiResponse;
import com.colegio.shuji.usuario.application.dto.in.LoginRequestDto;
import com.colegio.shuji.usuario.application.dto.in.RefreshTokenRequestDto;
import com.colegio.shuji.usuario.application.dto.in.RegisterUserRequestDto;
import com.colegio.shuji.usuario.application.dto.out.AuthResponseDto;
import com.colegio.shuji.usuario.application.dto.out.UserResponseDto;
import com.colegio.shuji.usuario.application.port.in.LoginUseCase;
import com.colegio.shuji.usuario.application.port.in.RefreshTokenUseCase;
import com.colegio.shuji.usuario.application.port.in.RegisterUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controlador REST para operaciones de autenticación, registro y renovación de tokens. */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(
    name = "Autenticación",
    description = "Endpoints de autenticación, registro y gestión de sesiones con JWT")
public class AuthController {

  private final LoginUseCase loginUseCase;
  private final RegisterUserUseCase registerUserUseCase;
  private final RefreshTokenUseCase refreshTokenUseCase;

  @Operation(
      summary = "Iniciar sesión",
      description = "Valida credenciales y retorna Access Token y Refresh Token")
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthResponseDto>> login(
      @Valid @RequestBody LoginRequestDto loginRequest, HttpServletRequest request) {

    String ipAddress = extractClientIp(request);
    String userAgent = request.getHeader("User-Agent");

    AuthResponseDto response = loginUseCase.login(loginRequest, ipAddress, userAgent);
    return ResponseEntity.ok(ApiResponse.ok(response, "Inicio de sesión exitoso"));
  }

  @Operation(
      summary = "Registrar nuevo usuario",
      description = "Crea una nueva cuenta de usuario con roles institucionales")
  @PostMapping("/register")
  public ResponseEntity<ApiResponse<UserResponseDto>> register(
      @Valid @RequestBody RegisterUserRequestDto registerRequest) {

    UserResponseDto response = registerUserUseCase.register(registerRequest);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.created(response, "Usuario registrado exitosamente en el sistema"));
  }

  @Operation(
      summary = "Renovar token de acceso",
      description = "Genera un nuevo Access Token a partir de un Refresh Token válido")
  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<AuthResponseDto>> refresh(
      @Valid @RequestBody RefreshTokenRequestDto refreshRequest, HttpServletRequest request) {

    String ipAddress = extractClientIp(request);
    String userAgent = request.getHeader("User-Agent");

    AuthResponseDto response =
        refreshTokenUseCase.refreshToken(refreshRequest, ipAddress, userAgent);
    return ResponseEntity.ok(ApiResponse.ok(response, "Token de acceso renovado exitosamente"));
  }

  private String extractClientIp(HttpServletRequest request) {
    String xfHeader = request.getHeader("X-Forwarded-For");
    if (xfHeader == null || xfHeader.isEmpty() || "unknown".equalsIgnoreCase(xfHeader)) {
      return request.getRemoteAddr();
    }
    return xfHeader.split(",")[0].trim();
  }
}
