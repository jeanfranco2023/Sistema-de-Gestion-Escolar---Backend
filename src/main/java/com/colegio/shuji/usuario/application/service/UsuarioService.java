package com.colegio.shuji.usuario.application.service;

import com.colegio.shuji.shared.application.port.out.AuditContextPort;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.usuario.application.dto.in.CambiarPasswordRequestDto;
import com.colegio.shuji.usuario.application.dto.in.CorregirDniRequestDto;
import com.colegio.shuji.usuario.application.dto.in.RegisterUserRequestDto;
import com.colegio.shuji.usuario.application.dto.in.UpdateUsuarioRequestDto;
import com.colegio.shuji.usuario.application.dto.out.UserResponseDto;
import com.colegio.shuji.usuario.application.mapper.UserMapper;
import com.colegio.shuji.usuario.application.port.in.GestionUsuarioUseCase;
import com.colegio.shuji.usuario.application.port.in.RegisterUserUseCase;
import com.colegio.shuji.usuario.application.port.out.PasswordHashPort;
import com.colegio.shuji.usuario.application.port.out.UserRepositoryPort;
import com.colegio.shuji.usuario.domain.exception.InvalidCredentialsException;
import com.colegio.shuji.usuario.domain.exception.ReglaNegocioException;
import com.colegio.shuji.usuario.domain.exception.UserAlreadyExistsException;
import com.colegio.shuji.usuario.domain.exception.UserNotFoundException;
import com.colegio.shuji.usuario.domain.model.Usuario;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio orquestador para la gestión institucional del ciclo de vida de usuarios. Implementa el
 * Caso de Uso GestionUsuarioUseCase.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UsuarioService implements GestionUsuarioUseCase {

  private final UserRepositoryPort userRepositoryPort;
  private final PasswordHashPort passwordEncoder;
  private final UserMapper userMapper;
  private final AuditContextPort auditoria;
  private final RegisterUserUseCase registerUserUseCase;

  @Override
  @Transactional(readOnly = true)
  public UserResponseDto obtenerPerfil(Long usuarioId) {
    log.debug("Obteniendo perfil para el usuario ID: {}", usuarioId);
    Usuario usuario =
        userRepositoryPort
            .findByIdWithRoles(usuarioId)
            .orElseThrow(() -> new UserNotFoundException(usuarioId));
    return userMapper.toResponseDto(usuario);
  }

  @Override
  public UserResponseDto actualizarPerfil(
      Long usuarioId, UpdateUsuarioRequestDto updateUsuarioDto) {
    log.info("Actualizando perfil para el usuario ID: {}", usuarioId);

    Usuario usuario =
        userRepositoryPort
            .findByIdWithRoles(usuarioId)
            .orElseThrow(() -> new UserNotFoundException(usuarioId));

    auditoria.syncCurrentUserFromSecurityContext();

    if (updateUsuarioDto.getEmail() != null
        && !updateUsuarioDto.getEmail().equalsIgnoreCase(usuario.getEmail())) {
      if (userRepositoryPort.existsByEmail(updateUsuarioDto.getEmail())) {
        throw new UserAlreadyExistsException("correo electrónico", updateUsuarioDto.getEmail());
      }
      usuario.actualizarPerfil(updateUsuarioDto.getEmail());
    }

    Usuario usuarioActualizado = userRepositoryPort.save(usuario);
    log.info("Perfil actualizado correctamente para usuario ID: {}", usuarioId);
    return userMapper.toResponseDto(usuarioActualizado);
  }

  @Override
  public UserResponseDto revocarRol(Long usuarioId, Short rolId) {
    log.info("Acción transaccional: Revocando rol ID: {} para usuario ID: {}", rolId, usuarioId);

    Usuario usuario =
        userRepositoryPort
            .findByIdWithRoles(usuarioId)
            .orElseThrow(() -> new UserNotFoundException(usuarioId));

    com.colegio.shuji.usuario.domain.model.Rol rolARevocar = usuario.getRoles().stream()
        .filter(r -> r.getId().equals(rolId))
        .findFirst()
        .orElseThrow(() -> new ReglaNegocioException("El usuario no tiene asignado el rol especificado."));

    boolean esRolDireccion = "DIRECCION".equalsIgnoreCase(rolARevocar.getCodigo())
        || "DIRECCION".equalsIgnoreCase(rolARevocar.getNombre())
        || "ROL_DIRECCION".equalsIgnoreCase(rolARevocar.getCodigo());

    if (esRolDireccion) {
      // 1. Bloquear si el director intenta quitarse su propio rol de DIRECCION
      var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
      if (auth != null && auth.getPrincipal() instanceof com.colegio.shuji.config.persistence.SupabaseAuditInterceptor.UserPrincipalHolder holder) {
        if (usuarioId.equals(holder.getUserId())) {
          throw new ReglaNegocioException("Un usuario con rol DIRECCIÓN no puede revocar su propio rol de DIRECCIÓN.", "AUTOREVOCACION_PROHIBIDA");
        }
      }

      // 2. Garantizar que quede al menos 1 administrador activo
      List<Usuario> administradoresActivos = userRepositoryPort.findByRolCodigo("DIRECCION").stream()
          .filter(u -> u.estaActivo() && !u.getId().equals(usuarioId))
          .toList();
      if (administradoresActivos.isEmpty()) {
        throw new ReglaNegocioException("No se puede revocar el rol DIRECCIÓN: debe permanecer al menos un usuario administrador activo en la institución.", "ULTIMO_ADMINISTRADOR");
      }
    }

    if (usuario.getRoles().size() <= 1) {
      throw new ReglaNegocioException("No se puede revocar el único rol del usuario institucional.", "UNICO_ROL");
    }

    usuario.revocarRol(rolARevocar, LocalDateTime.now());

    auditoria.syncCurrentUserFromSecurityContext();

    Usuario usuarioActualizado = userRepositoryPort.save(usuario);
    log.info("Rol ID: {} revocado exitosamente del usuario ID: {}", rolId, usuarioId);

    return userMapper.toResponseDto(usuarioActualizado);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserResponseDto> listarPorRol(String codigoRol) {
    if (codigoRol == null || codigoRol.trim().isEmpty()) {
      return listarTodos();
    }
    log.debug("Listando usuarios con el rol: {}", codigoRol);
    List<Usuario> usuarios = userRepositoryPort.findByRolCodigo(codigoRol.trim());
    return userMapper.toResponseDtoList(usuarios);
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserResponseDto> listarTodos() {
    log.debug("Listando todos los usuarios institucionales");
    List<Usuario> usuarios = userRepositoryPort.findAll();
    return userMapper.toResponseDtoList(usuarios);
  }

  @Override
  public UserResponseDto cambiarEstado(Long usuarioId, Boolean activo) {
    log.info("Cambiando estado del usuario ID: {} a activo: {}", usuarioId, activo);

    Usuario usuario =
        userRepositoryPort
            .findByIdWithRoles(usuarioId)
            .orElseThrow(() -> new UserNotFoundException(usuarioId));

    auditoria.syncCurrentUserFromSecurityContext();

    if (Boolean.TRUE.equals(activo)) {
      usuario.activar();
    } else {
      usuario.desactivar();
    }

    Usuario usuarioModificado = userRepositoryPort.save(usuario);
    log.info(
        "Estado del usuario ID: {} actualizado a activo: {}",
        usuarioId,
        usuarioModificado.estaActivo());
    return userMapper.toResponseDto(usuarioModificado);
  }

  @Override
  public void cambiarPassword(Long usuarioId, CambiarPasswordRequestDto cambiarPasswordDto) {
    log.info("Solicitud de cambio de contraseña para el usuario ID: {}", usuarioId);

    Usuario usuario =
        userRepositoryPort
            .findById(usuarioId)
            .orElseThrow(() -> new UserNotFoundException(usuarioId));

    if (!passwordEncoder.matches(
        cambiarPasswordDto.getPasswordActual(), usuario.getUsuarioPassword())) {
      log.warn("Fallo de coincidencia de contraseña actual para usuario ID: {}", usuarioId);
      throw new InvalidCredentialsException("La contraseña actual proporcionada es incorrecta.");
    }

    if (cambiarPasswordDto.getPasswordActual().equals(cambiarPasswordDto.getPasswordNuevo())) {
      throw new BusinessException(
          "La nueva contraseña no puede ser idéntica a la actual.", "SAME_PASSWORD");
    }

    auditoria.syncCurrentUserFromSecurityContext();

    usuario.cambiarPassword(passwordEncoder.encode(cambiarPasswordDto.getPasswordNuevo()));
    userRepositoryPort.save(usuario);
    log.info("Contraseña actualizada exitosamente para el usuario ID: {}", usuarioId);
  }

  @Override
  public UserResponseDto registrarPorDireccion(RegisterUserRequestDto request) {
    log.info("Delegando alta administrativa de usuario institucional a RegisterUserUseCase");
    auditoria.syncCurrentUserFromSecurityContext();
    return registerUserUseCase.registrarPorDireccion(request);
  }

  @Override
  public UserResponseDto corregirDni(Long usuarioId, CorregirDniRequestDto request) {
    log.info("Acción transaccional: Corrigiendo DNI para usuario ID: {} con motivo: {}", usuarioId, request.motivo());

    Usuario usuario =
        userRepositoryPort
            .findByIdWithRoles(usuarioId)
            .orElseThrow(() -> new UserNotFoundException(usuarioId));

    if (request.nuevoDni().equals(usuario.getDni())) {
      throw new BusinessException("El nuevo DNI es idéntico al DNI actualmente registrado", "DNI_IDENTICO");
    }

    if (userRepositoryPort.existsByDni(request.nuevoDni())) {
      throw new ReglaNegocioException("El DNI ingresado ya se encuentra asignado a otro usuario", "DNI_DUPLICADO");
    }

    auditoria.syncCurrentUserFromSecurityContext();

    usuario.corregirDni(request.nuevoDni(), LocalDateTime.now());

    Usuario usuarioActualizado = userRepositoryPort.save(usuario);
    log.info("DNI del usuario ID: {} corregido exitosamente a {}", usuarioId, request.nuevoDni());

    return userMapper.toResponseDto(usuarioActualizado);
  }
}
