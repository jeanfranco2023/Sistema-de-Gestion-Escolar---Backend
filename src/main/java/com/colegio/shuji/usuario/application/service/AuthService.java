package com.colegio.shuji.usuario.application.service;

import com.colegio.shuji.shared.application.port.out.AuditContextPort;
import com.colegio.shuji.shared.domain.exception.ResourceNotFoundException;
import com.colegio.shuji.shared.domain.exception.UnauthorizedException;
import com.colegio.shuji.usuario.application.dto.in.LoginRequestDto;
import com.colegio.shuji.usuario.application.dto.in.RefreshTokenRequestDto;
import com.colegio.shuji.usuario.application.dto.in.RegisterUserRequestDto;
import com.colegio.shuji.usuario.application.dto.out.AuthResponseDto;
import com.colegio.shuji.usuario.application.dto.out.UserResponseDto;
import com.colegio.shuji.usuario.application.mapper.UserMapper;
import com.colegio.shuji.usuario.application.port.in.LoginUseCase;
import com.colegio.shuji.usuario.application.port.in.RefreshTokenUseCase;
import com.colegio.shuji.usuario.application.port.in.RegisterUserUseCase;
import com.colegio.shuji.usuario.application.port.out.PasswordHashPort;
import com.colegio.shuji.usuario.application.port.out.RoleRepositoryPort;
import com.colegio.shuji.usuario.application.port.out.SessionRepositoryPort;
import com.colegio.shuji.usuario.application.port.out.TokenPort;
import com.colegio.shuji.usuario.application.port.out.UserRepositoryPort;
import com.colegio.shuji.usuario.domain.exception.InvalidCredentialsException;
import com.colegio.shuji.usuario.domain.exception.UserAlreadyExistsException;
import com.colegio.shuji.usuario.domain.model.Rol;
import com.colegio.shuji.usuario.domain.model.Sesion;
import com.colegio.shuji.usuario.domain.model.Usuario;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio orquestador central de Autenticación y Seguridad. Implementa los Casos de Uso del módulo
 * y orquesta puertos de salida y criptografía.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AuthService implements LoginUseCase, RegisterUserUseCase, RefreshTokenUseCase {

  private final UserRepositoryPort userRepositoryPort;
  private final RoleRepositoryPort roleRepositoryPort;
  private final SessionRepositoryPort sessionRepositoryPort;
  private final PasswordHashPort passwordEncoder;
  private final TokenPort jwtTokenProvider;
  private final UserMapper userMapper;
  private final AuditContextPort auditoria;

  @Override
  public AuthResponseDto login(LoginRequestDto loginRequest, String ipAddress, String userAgent) {
    log.info("Intento de inicio de sesión para el usuario/email: {}", loginRequest.getUsername());

    Usuario usuario =
        userRepositoryPort
            .findByUsernameOrEmail(loginRequest.getUsername())
            .orElseThrow(InvalidCredentialsException::new);

    if (!passwordEncoder.matches(loginRequest.getPassword(), usuario.getPasswordHash())) {
      log.warn("Contraseña incorrecta para el usuario: {}", loginRequest.getUsername());
      throw new InvalidCredentialsException();
    }

    if (!usuario.estaActivo()) {
      log.warn("Intento de acceso para usuario desactivado: {}", loginRequest.getUsername());
      throw new UnauthorizedException(
          "La cuenta de usuario se encuentra desactivada. Contacte a Dirección.");
    }

    // Configurar variable de auditoría para la transacción
    auditoria.setCurrentUserId(usuario.getId());

    List<String> roles = usuario.getRoles().stream().map(Rol::getCodigo).toList();

    String accessToken =
        jwtTokenProvider.generateToken(usuario.getId(), usuario.getUsername(), roles);
    String refreshToken =
        jwtTokenProvider.generateRefreshToken(usuario.getId(), usuario.getUsername());

    String tokenHash = hashToken(refreshToken);
    long refreshExpirationMs = jwtTokenProvider.getJwtRefreshExpirationMs();
    LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000);

    Sesion sesion =
        Sesion.builder()
            .usuarioId(usuario.getId())
            .tokenHash(tokenHash)
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .expiraAt(expiresAt)
            .createdAt(LocalDateTime.now())
            .build();

    sessionRepositoryPort.save(sesion);

    log.info(
        "Inicio de sesión exitoso para usuario: {} (ID: {})",
        usuario.getUsername(),
        usuario.getId());

    return AuthResponseDto.builder()
        .accessToken(accessToken)
        .refreshToken(refreshToken)
        .tokenType("Bearer")
        .expiresIn(jwtTokenProvider.getJwtExpirationMs())
        .user(userMapper.toResponseDto(usuario))
        .build();
  }

  @Override
  public UserResponseDto register(RegisterUserRequestDto registerRequest) {
    log.info("Registrando nuevo usuario (auto-registro público): {}", registerRequest.getUsername());

    if (userRepositoryPort.existsByUsername(registerRequest.getUsername())) {
      throw new UserAlreadyExistsException("nombre de usuario", registerRequest.getUsername());
    }

    if (userRepositoryPort.existsByEmail(registerRequest.getEmail())) {
      throw new UserAlreadyExistsException("correo electrónico", registerRequest.getEmail());
    }

    // Seguridad crítica: el auto-registro público asigna forzosamente y de forma exclusiva el rol APODERADO
    List<Rol> roles = roleRepositoryPort.findByCodigoIn(List.of("APODERADO"));
    if (roles.isEmpty()) {
      throw new ResourceNotFoundException(
          "El rol obligatorio APODERADO no fue encontrado en el catálogo institucional.");
    }

    Usuario usuario = userMapper.toDomain(registerRequest);
    usuario.registrar(
        passwordEncoder.encode(registerRequest.getPassword()),
        new HashSet<>(roles),
        LocalDateTime.now());

    Usuario usuarioGuardado = userRepositoryPort.save(usuario);
    log.info("Usuario auto-registrado exitosamente con ID: {} y rol APODERADO", usuarioGuardado.getId());

    return userMapper.toResponseDto(usuarioGuardado);
  }

  @Override
  public UserResponseDto registrarPorDireccion(RegisterUserRequestDto request) {
    log.info("Registrando nuevo usuario administrativo por Dirección: {}", request.getUsername());

    if (userRepositoryPort.existsByUsername(request.getUsername())) {
      throw new UserAlreadyExistsException("nombre de usuario", request.getUsername());
    }

    if (userRepositoryPort.existsByEmail(request.getEmail())) {
      throw new UserAlreadyExistsException("correo electrónico", request.getEmail());
    }

    Collection<String> codigosRoles =
        (request.getRoles() == null || request.getRoles().isEmpty())
            ? List.of("APODERADO")
            : request.getRoles();

    List<Rol> roles = roleRepositoryPort.findByCodigoIn(codigosRoles);
    if (roles.isEmpty()) {
      throw new ResourceNotFoundException(
          "Ninguno de los roles solicitados fue encontrado en el catálogo institucional.");
    }

    Usuario usuario = userMapper.toDomain(request);
    usuario.registrar(
        passwordEncoder.encode(request.getPassword()),
        new HashSet<>(roles),
        LocalDateTime.now());

    Usuario usuarioGuardado = userRepositoryPort.save(usuario);
    log.info(
        "Usuario institucional creado por Dirección con ID: {} y roles: {}",
        usuarioGuardado.getId(),
        codigosRoles);

    return userMapper.toResponseDto(usuarioGuardado);
  }

  @Override
  public AuthResponseDto refreshToken(
      RefreshTokenRequestDto refreshTokenRequest, String ipAddress, String userAgent) {
    String refreshToken = refreshTokenRequest.getRefreshToken();

    if (!jwtTokenProvider.validateToken(refreshToken)) {
      throw new UnauthorizedException("El token de refresco no es válido o ha expirado.");
    }

    String tokenHash = hashToken(refreshToken);
    Sesion sesion =
        sessionRepositoryPort
            .findByTokenHash(tokenHash)
            .orElseThrow(() -> new UnauthorizedException("Sesión no encontrada o token revocado."));

    if (sesion.estaExpirada(LocalDateTime.now())) {
      sessionRepositoryPort.deleteByUsuarioId(sesion.getUsuarioId());
      throw new UnauthorizedException("La sesión ha expirado. Por favor inicie sesión nuevamente.");
    }

    Usuario usuario =
        userRepositoryPort
            .findById(sesion.getUsuarioId())
            .orElseThrow(
                () -> new UnauthorizedException("Usuario asociado a la sesión no encontrado."));

    if (!usuario.estaActivo()) {
      throw new UnauthorizedException("La cuenta de usuario se encuentra desactivada.");
    }

    // Configurar variable de auditoría para la transacción
    auditoria.setCurrentUserId(usuario.getId());

    List<String> roles = usuario.getRoles().stream().map(Rol::getCodigo).toList();

    // Rotación segura de refresh token
    String newAccessToken =
        jwtTokenProvider.generateToken(usuario.getId(), usuario.getUsername(), roles);
    String newRefreshToken =
        jwtTokenProvider.generateRefreshToken(usuario.getId(), usuario.getUsername());

    var ahora = LocalDateTime.now();
    sesion.renovar(
        hashToken(newRefreshToken),
        ipAddress,
        userAgent,
        ahora,
        ahora.plusSeconds(jwtTokenProvider.getJwtRefreshExpirationMs() / 1000));
    sessionRepositoryPort.save(sesion);

    log.info("Token renovado exitosamente para usuario: {}", usuario.getUsername());

    return AuthResponseDto.builder()
        .accessToken(newAccessToken)
        .refreshToken(newRefreshToken)
        .tokenType("Bearer")
        .expiresIn(jwtTokenProvider.getJwtExpirationMs())
        .user(userMapper.toResponseDto(usuario))
        .build();
  }

  private String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] encodedHash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
      for (byte b : encodedHash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Error criptográfico al inicializar algoritmo SHA-256", e);
    }
  }
}
