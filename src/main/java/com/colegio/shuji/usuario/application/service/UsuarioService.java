package com.colegio.shuji.usuario.application.service;

import com.colegio.shuji.shared.application.port.out.AuditContextPort;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.usuario.application.dto.in.ActualizarUsuarioAdminRequestDto;
import com.colegio.shuji.usuario.application.dto.in.CambiarPasswordRequestDto;
import com.colegio.shuji.usuario.application.dto.in.CorregirDniRequestDto;
import com.colegio.shuji.usuario.application.dto.in.RegisterUserRequestDto;
import com.colegio.shuji.usuario.application.dto.in.UpdateUsuarioRequestDto;
import com.colegio.shuji.usuario.application.dto.out.AreaDocenteResponseDto;
import com.colegio.shuji.usuario.application.dto.out.UserResponseDto;
import com.colegio.shuji.usuario.application.mapper.UserMapper;
import com.colegio.shuji.usuario.application.port.in.GestionUsuarioUseCase;
import com.colegio.shuji.usuario.application.port.in.RegisterUserUseCase;
import com.colegio.shuji.usuario.application.port.out.PasswordHashPort;
import com.colegio.shuji.usuario.application.port.out.RoleRepositoryPort;
import com.colegio.shuji.usuario.application.port.out.UserRepositoryPort;
import com.colegio.shuji.usuario.domain.exception.InvalidCredentialsException;
import com.colegio.shuji.usuario.domain.exception.ReglaNegocioException;
import com.colegio.shuji.usuario.domain.exception.UserAlreadyExistsException;
import com.colegio.shuji.usuario.domain.exception.UserNotFoundException;
import com.colegio.shuji.usuario.domain.model.Rol;
import com.colegio.shuji.usuario.domain.model.Usuario;
import com.colegio.shuji.usuario.infrastructure.adapter.DocenteAreasJdbcAdapter;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio orquestador para la gestión institucional del ciclo de vida de usuarios. Implementa el
 * Caso de Uso GestionUsuarioUseCase.
 */
@Slf4j
@Service
@Transactional
public class UsuarioService implements GestionUsuarioUseCase {

  private static final Set<String> ROLES_OPERATIVOS_SECRETARIA = Set.of("DOCENTE", "AUXILIAR", "TUTOR");
  private static final Set<String> ROLES_INSTITUCIONALES_DIRECCION =
      Set.of("DIRECCION", "SECRETARIA", "DOCENTE", "AUXILIAR", "TUTOR");
  private static final Set<String> ESTADOS_LABORALES_VALIDOS =
      Set.of("ACTIVO", "SUSPENDIDO", "CESADO", "INACTIVO");

  private final UserRepositoryPort userRepositoryPort;
  private final PasswordHashPort passwordEncoder;
  private final UserMapper userMapper;
  private final AuditContextPort auditoria;
  private final RegisterUserUseCase registerUserUseCase;

  @Autowired(required = false)
  private RoleRepositoryPort roleRepositoryPort;

  @Autowired(required = false)
  private DocenteAreasJdbcAdapter docenteAreasAdapter;

  @Autowired
  public UsuarioService(
      UserRepositoryPort userRepositoryPort,
      PasswordHashPort passwordEncoder,
      UserMapper userMapper,
      AuditContextPort auditoria,
      RegisterUserUseCase registerUserUseCase) {
    this.userRepositoryPort = userRepositoryPort;
    this.passwordEncoder = passwordEncoder;
    this.userMapper = userMapper;
    this.auditoria = auditoria;
    this.registerUserUseCase = registerUserUseCase;
  }

  public void setRoleRepositoryPort(RoleRepositoryPort roleRepositoryPort) {
    this.roleRepositoryPort = roleRepositoryPort;
  }

  public void setDocenteAreasAdapter(DocenteAreasJdbcAdapter docenteAreasAdapter) {
    this.docenteAreasAdapter = docenteAreasAdapter;
  }

  @Override
  @Transactional(readOnly = true)
  public UserResponseDto obtenerPerfil(Long usuarioId) {
    log.debug("Obteniendo perfil para el usuario ID: {}", usuarioId);
    Usuario usuario =
        userRepositoryPort
            .findByIdWithRoles(usuarioId)
            .orElseThrow(() -> new UserNotFoundException(usuarioId));
    return enriquecerAreas(userMapper.toResponseDto(usuario));
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

    validarJerarquiaActorSobreUsuario(usuario);

    com.colegio.shuji.usuario.domain.model.Rol rolARevocar = usuario.getRoles().stream()
        .filter(r -> r.getId().equals(rolId))
        .findFirst()
        .orElseThrow(() -> new ReglaNegocioException("El usuario no tiene asignado el rol especificado."));

    if ("ADMINISTRADOR".equalsIgnoreCase(normalizarCodigoRol(rolARevocar.getCodigo()))) {
      exigirAdministrador();
      asegurarOtroAdministradorActivo(usuario);
    }

    boolean esRolDireccion = "DIRECCION".equalsIgnoreCase(rolARevocar.getCodigo())
        || "DIRECCION".equalsIgnoreCase(rolARevocar.getNombre())
        || "ROL_DIRECCION".equalsIgnoreCase(rolARevocar.getCodigo());

    if (esRolDireccion) {
      Long actorId = obtenerActorId();
      if (actorId != null && usuarioId.equals(actorId)) {
        throw new ReglaNegocioException("Un usuario con rol DIRECCIÓN no puede revocar su propio rol de DIRECCIÓN.", "AUTOREVOCACION_PROHIBIDA");
      }

      // Garantizar que quede al menos 1 usuario de Dirección activo
      asegurarOtroUsuarioConRolActivo(usuarioId, "DIRECCION",
          "No se puede revocar el rol DIRECCIÓN: debe permanecer al menos un usuario administrador activo en la institución.");
    }

    if (usuario.getRoles().size() <= 1) {
      throw new ReglaNegocioException("No se puede revocar el único rol del usuario institucional.", "UNICO_ROL");
    }

    usuario.revocarRol(rolARevocar, LocalDateTime.now());

    auditoria.syncCurrentUserFromSecurityContext();

    Usuario usuarioActualizado = userRepositoryPort.save(usuario);
    log.info("Rol ID: {} revocado exitosamente del usuario ID: {}", rolId, usuarioId);

    return enriquecerAreas(userMapper.toResponseDto(usuarioActualizado));
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserResponseDto> listarPorRol(String codigoRol) {
    if (codigoRol == null || codigoRol.trim().isEmpty()) {
      return listarTodos();
    }
    log.debug("Listando usuarios con el rol: {}", codigoRol);
    List<Usuario> usuarios = userRepositoryPort.findByRolCodigo(codigoRol.trim());
    return enriquecerAreasList(userMapper.toResponseDtoList(usuarios));
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserResponseDto> listarTodos() {
    log.debug("Listando todos los usuarios institucionales");
    List<Usuario> usuarios = userRepositoryPort.findAll();
    return enriquecerAreasList(userMapper.toResponseDtoList(usuarios));
  }

  @Override
  public UserResponseDto cambiarEstado(Long usuarioId, Boolean activo) {
    log.info("Cambiando estado del usuario ID: {} a activo: {}", usuarioId, activo);

    Usuario usuario =
        userRepositoryPort
            .findByIdWithRoles(usuarioId)
            .orElseThrow(() -> new UserNotFoundException(usuarioId));

    validarJerarquiaActorSobreUsuario(usuario);

    if (usuario.getRoles().stream().anyMatch(rol -> "ADMINISTRADOR".equalsIgnoreCase(normalizarCodigoRol(rol.getCodigo())))) {
      exigirAdministrador();
      if (!Boolean.TRUE.equals(activo)) asegurarOtroAdministradorActivo(usuario);
    }
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
    return enriquecerAreas(userMapper.toResponseDto(usuarioModificado));
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
    if (request != null && request.getRoles() != null) {
      validarJerarquiaActorSobreRoles(request.getRoles());
    }
    boolean incluyeDocente = request != null && request.getRoles() != null && request.getRoles().stream()
        .anyMatch(r -> r != null && ("DOCENTE".equalsIgnoreCase(normalizarCodigoRol(r))));
    if (incluyeDocente) {
      if (request.getDni() == null || !request.getDni().trim().matches("^\\d{8}$")) {
        throw new ReglaNegocioException(
            "El DNI de 8 dígitos numéricos es obligatorio para registrar un docente.",
            "DNI_OBLIGATORIO_DOCENTE");
      }
      if (docenteAreasAdapter != null
          && (request.getAreaCurricularIds() == null || request.getAreaCurricularIds().isEmpty())
          && (request.getEspecialidad() == null || request.getEspecialidad().isBlank())) {
        throw new ReglaNegocioException(
            "Debe seleccionar al menos un curso real de la malla curricular para registrar al docente.",
            "CURSO_DOCENTE_REQUERIDO");
      }
    }
    if (request != null) {
      request.setDni(normalizarTextoOpcional(request.getDni()));
      request.setTelefono(normalizarTextoOpcional(request.getTelefono()));
      request.setDireccion(normalizarTextoOpcional(request.getDireccion()));
      request.setEspecialidad(normalizarTextoOpcional(request.getEspecialidad()));
      request.setCodigoModular(normalizarTextoOpcional(request.getCodigoModular()));
      request.setNumeroColegiatura(normalizarTextoOpcional(request.getNumeroColegiatura()));
      request.setTipoContrato(normalizarTextoOpcional(request.getTipoContrato()));
    }
    auditoria.syncCurrentUserFromSecurityContext();
    UserResponseDto creado = registerUserUseCase.registrarPorDireccion(request);
    if (docenteAreasAdapter != null
        && request != null
        && request.getAreaCurricularIds() != null
        && !request.getAreaCurricularIds().isEmpty()) {
      List<AreaDocenteResponseDto> areas =
          docenteAreasAdapter.sincronizarAreasDocente(
              creado.getId(), request.getAreaCurricularIds(), incluyeDocente);
      String resumenEsp = DocenteAreasJdbcAdapter.resumirEspecialidad(areas);
      if (resumenEsp != null) {
        userRepositoryPort
            .findByIdWithRoles(creado.getId())
            .ifPresent(
                u -> {
                  u.setEspecialidad(resumenEsp);
                  userRepositoryPort.save(u);
                });
        creado.setEspecialidad(resumenEsp);
      }
      creado.setAreasCurriculares(areas);
      return creado;
    }
    return enriquecerAreas(creado);
  }

  @Override
  public UserResponseDto actualizarUsuario(Long usuarioId, ActualizarUsuarioAdminRequestDto request) {
    if (request == null) {
      throw new BusinessException("Solicitud de actualización vacía");
    }
    Usuario usuario =
        userRepositoryPort
            .findByIdWithRoles(usuarioId)
            .orElseThrow(() -> new UserNotFoundException(usuarioId));

    Set<String> rolesActuales =
        usuario.getRoles().stream()
            .map(r -> normalizarCodigoRol(r.getCodigo()))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    validarJerarquiaActorSobreRoles(rolesActuales);

    if (request.getRoles() != null) {
      Set<String> rolesObjetivo =
          request.getRoles().stream()
              .filter(Objects::nonNull)
              .map(this::normalizarCodigoRol)
              .filter(s -> !s.isBlank())
              .collect(Collectors.toCollection(LinkedHashSet::new));
      if (rolesObjetivo.isEmpty()) {
        throw new ReglaNegocioException("Debe mantener al menos un rol institucional.");
      }
      if (rolesObjetivo.contains("APODERADO") && !rolesActuales.contains("APODERADO")) {
        throw new ReglaNegocioException(
            "El rol APODERADO solo se asigna automáticamente desde matrícula.");
      }
      validarJerarquiaActorSobreRoles(rolesObjetivo);

      Long actorId = obtenerActorId();
      if (rolesActuales.contains("ADMINISTRADOR") && !rolesObjetivo.contains("ADMINISTRADOR")) {
        if (actorId != null && Objects.equals(usuarioId, actorId)) {
          throw new ReglaNegocioException(
              "No puede revocar su propio rol Administrador.", "AUTOREVOCACION_PROHIBIDA");
        }
        asegurarOtroAdministradorActivo(usuario);
      }
      if (rolesActuales.contains("DIRECCION") && !rolesObjetivo.contains("DIRECCION")) {
        if (actorId != null && Objects.equals(usuarioId, actorId)) {
          throw new ReglaNegocioException(
              "Un usuario con rol DIRECCIÓN no puede revocar su propio rol.",
              "AUTOREVOCACION_PROHIBIDA");
        }
        asegurarOtroUsuarioConRolActivo(
            usuarioId,
            "DIRECCION",
            "Debe permanecer al menos un usuario con rol DIRECCIÓN activo en la institución.");
      }

      if (!rolesObjetivo.equals(rolesActuales)) {
        if (roleRepositoryPort == null) {
          throw new IllegalStateException("RoleRepositoryPort no disponible para actualizar roles");
        }
        Map<String, Rol> catalogoPorCodigo =
            roleRepositoryPort.findAll().stream()
                .collect(
                    Collectors.toMap(
                        r -> normalizarCodigoRol(r.getCodigo()), r -> r, (a, b) -> a));
        Set<Rol> nuevosRoles = new HashSet<>();
        for (String codigo : rolesObjetivo) {
          Rol rol = catalogoPorCodigo.get(codigo);
          if (rol == null) {
            throw new ReglaNegocioException("Rol institucional no encontrado: " + codigo);
          }
          nuevosRoles.add(rol);
        }
        usuario.setRoles(nuevosRoles);
      }
    }

    Set<String> rolesResultantes =
        usuario.getRoles().stream()
            .map(r -> normalizarCodigoRol(r.getCodigo()))
            .collect(Collectors.toSet());
    boolean incluyeDocente = rolesResultantes.contains("DOCENTE");
    LocalDateTime ahora = LocalDateTime.now();

    if (request.getEmail() != null && !request.getEmail().isBlank()) {
      String emailNuevo = request.getEmail().trim().toLowerCase(Locale.ROOT);
      if (!emailNuevo.equalsIgnoreCase(usuario.getEmail())) {
        if (userRepositoryPort.existsByEmail(emailNuevo)) {
          throw new UserAlreadyExistsException("correo electrónico", emailNuevo);
        }
        usuario.actualizarPerfil(emailNuevo);
      }
    }

    if (request.getNombreCompleto() != null) {
      usuario.actualizarNombreCompleto(request.getNombreCompleto(), ahora);
    }

    if (request.getDni() != null) {
      String dniNuevo = request.getDni().trim();
      if (dniNuevo.isEmpty()) {
        if (incluyeDocente) {
          throw new ReglaNegocioException(
              "El DNI de 8 dígitos numéricos es obligatorio para un docente.",
              "DNI_OBLIGATORIO_DOCENTE");
        }
        usuario.corregirDni(null, ahora);
      } else {
        if (!dniNuevo.equals(usuario.getDni()) && userRepositoryPort.existsByDni(dniNuevo)) {
          throw new UserAlreadyExistsException("DNI", dniNuevo);
        }
        usuario.corregirDni(dniNuevo, ahora);
      }
    } else if (incluyeDocente && (usuario.getDni() == null || !usuario.getDni().matches("^\\d{8}$"))) {
      throw new ReglaNegocioException(
          "El DNI de 8 dígitos numéricos es obligatorio para un docente.",
          "DNI_OBLIGATORIO_DOCENTE");
    }

    if (request.getTelefono() != null) {
      usuario.setTelefono(normalizarTextoOpcional(request.getTelefono()));
    }
    if (request.getDireccion() != null) {
      usuario.setDireccion(normalizarTextoOpcional(request.getDireccion()));
    }
    if (request.getCodigoModular() != null) {
      usuario.setCodigoModular(normalizarTextoOpcional(request.getCodigoModular()));
    }
    if (request.getNumeroColegiatura() != null) {
      usuario.setNumeroColegiatura(normalizarTextoOpcional(request.getNumeroColegiatura()));
    }
    if (request.getTipoContrato() != null) {
      usuario.setTipoContrato(normalizarTextoOpcional(request.getTipoContrato()));
    }
    if (request.getEspecialidad() != null) {
      usuario.setEspecialidad(normalizarTextoOpcional(request.getEspecialidad()));
    }
    if (request.getEstadoLaboral() != null && !request.getEstadoLaboral().isBlank()) {
      String estadoNorm = request.getEstadoLaboral().trim().toUpperCase(Locale.ROOT);
      if (!ESTADOS_LABORALES_VALIDOS.contains(estadoNorm)) {
        throw new ReglaNegocioException(
            "Estado laboral inválido. Permitidos: ACTIVO, SUSPENDIDO, CESADO o INACTIVO.");
      }
      usuario.setEstadoLaboral(estadoNorm);
    }
    if (request.getPasswordNuevo() != null && !request.getPasswordNuevo().isBlank()) {
      String pass = request.getPasswordNuevo();
      if (pass.length() < 8 || pass.length() > 100) {
        throw new ReglaNegocioException("La nueva contraseña debe tener entre 8 y 100 caracteres.");
      }
      usuario.cambiarPassword(passwordEncoder.encode(pass));
      usuario.marcarCambioPasswordInicialPendiente();
    }

    auditoria.syncCurrentUserFromSecurityContext();

    List<AreaDocenteResponseDto> areasActualizadas = null;
    if (docenteAreasAdapter != null) {
      if (incluyeDocente && request.getAreaCurricularIds() != null) {
        areasActualizadas =
            docenteAreasAdapter.sincronizarAreasDocente(
                usuarioId, request.getAreaCurricularIds(), true);
        String resumen = DocenteAreasJdbcAdapter.resumirEspecialidad(areasActualizadas);
        if (resumen != null) {
          usuario.setEspecialidad(resumen);
        }
      } else if (!incluyeDocente && rolesActuales.contains("DOCENTE")) {
        areasActualizadas = docenteAreasAdapter.sincronizarAreasDocente(usuarioId, List.of(), false);
        if (request.getEspecialidad() == null) {
          usuario.setEspecialidad(null);
        }
      }
    }

    usuario.setFechaActualizacion(ahora);
    Usuario actualizado = userRepositoryPort.save(usuario);
    UserResponseDto dto = userMapper.toResponseDto(actualizado);
    if (areasActualizadas != null) {
      dto.setAreasCurriculares(areasActualizadas);
      return dto;
    }
    return enriquecerAreas(dto);
  }

  private void validarJerarquiaActorSobreUsuario(Usuario usuario) {
    if (usuario == null) {
      return;
    }
    Set<String> rolesUsuario =
        usuario.getRoles().stream()
            .map(r -> normalizarCodigoRol(r.getCodigo()))
            .collect(Collectors.toSet());
    validarJerarquiaActorSobreRoles(rolesUsuario);
  }

  private void validarJerarquiaActorSobreRoles(Collection<String> rolesObjetivo) {
    if (rolesObjetivo == null || rolesObjetivo.isEmpty()) {
      return;
    }
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    Set<String> normObjetivo =
        rolesObjetivo.stream()
            .filter(Objects::nonNull)
            .map(this::normalizarCodigoRol)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toSet());

    if (auth == null || !auth.isAuthenticated()) {
      if (normObjetivo.contains("ADMINISTRADOR")) {
        throw new AccessDeniedException("Esta operación requiere el rol Administrador.");
      }
      return;
    }

    boolean esAdmin =
        auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMINISTRADOR".equals(a.getAuthority()));
    if (esAdmin) {
      return;
    }

    boolean esDireccion =
        auth.getAuthorities().stream().anyMatch(a -> "ROLE_DIRECCION".equals(a.getAuthority()));
    if (esDireccion) {
      if (normObjetivo.contains("ADMINISTRADOR")) {
        throw new AccessDeniedException("Solo Administrador puede crear o editar cuentas Administrador.");
      }
      for (String r : normObjetivo) {
        if (!ROLES_INSTITUCIONALES_DIRECCION.contains(r) && !"APODERADO".equals(r)) {
          throw new AccessDeniedException("Rol no autorizado para Dirección: " + r);
        }
      }
      return;
    }

    boolean esSecretaria =
        auth.getAuthorities().stream().anyMatch(a -> "ROLE_SECRETARIA".equals(a.getAuthority()));
    if (esSecretaria) {
      for (String r : normObjetivo) {
        if (!ROLES_OPERATIVOS_SECRETARIA.contains(r)) {
          throw new AccessDeniedException(
              "Secretaría solo puede crear o editar docentes, auxiliares o tutores.");
        }
      }
      return;
    }

    throw new AccessDeniedException("No cuenta con permisos suficientes para gestionar usuarios.");
  }

  private String normalizarCodigoRol(String codigo) {
    if (codigo == null) {
      return "";
    }
    String limpio = codigo.trim().toUpperCase(Locale.ROOT);
    return limpio.startsWith("ROL_") ? limpio.substring(4) : limpio;
  }

  private String normalizarTextoOpcional(String valor) {
    if (valor == null) {
      return null;
    }
    String limpio = valor.trim();
    return limpio.isEmpty() ? null : limpio;
  }

  private Long obtenerActorId() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null
        && auth.getPrincipal()
            instanceof
            com.colegio.shuji.config.persistence.SupabaseAuditInterceptor.UserPrincipalHolder holder) {
      return holder.getUserId();
    }
    return null;
  }

  private void asegurarOtroUsuarioConRolActivo(Long usuarioIdExcluido, String codigoRol, String mensaje) {
    List<Usuario> otrosActivos =
        userRepositoryPort.findByRolCodigo(codigoRol).stream()
            .filter(u -> u.estaActivo() && !Objects.equals(u.getId(), usuarioIdExcluido))
            .toList();
    if (otrosActivos.isEmpty()) {
      throw new ReglaNegocioException(mensaje, "ULTIMO_ADMINISTRADOR");
    }
  }

  private UserResponseDto enriquecerAreas(UserResponseDto dto) {
    if (dto == null || dto.getId() == null || docenteAreasAdapter == null) {
      return dto;
    }
    Map<Long, List<AreaDocenteResponseDto>> porUsuario =
        docenteAreasAdapter.listarPorUsuarios(List.of(dto.getId()));
    dto.setAreasCurriculares(porUsuario.getOrDefault(dto.getId(), List.of()));
    return dto;
  }

  private List<UserResponseDto> enriquecerAreasList(List<UserResponseDto> lista) {
    if (lista == null || lista.isEmpty() || docenteAreasAdapter == null) {
      return lista;
    }
    List<Long> ids = lista.stream().map(UserResponseDto::getId).filter(Objects::nonNull).toList();
    if (ids.isEmpty()) {
      return lista;
    }
    Map<Long, List<AreaDocenteResponseDto>> porUsuario = docenteAreasAdapter.listarPorUsuarios(ids);
    for (UserResponseDto dto : lista) {
      dto.setAreasCurriculares(porUsuario.getOrDefault(dto.getId(), List.of()));
    }
    return lista;
  }

  private void exigirAdministrador() {
    var autenticacion = SecurityContextHolder.getContext().getAuthentication();
    if (autenticacion == null || autenticacion.getAuthorities().stream()
        .noneMatch(autoridad -> "ROLE_ADMINISTRADOR".equals(autoridad.getAuthority()))) {
      throw new AccessDeniedException("Esta operación requiere el rol Administrador.");
    }
  }

  private void asegurarOtroAdministradorActivo(Usuario usuario) {
    if (usuario.estaActivo() && userRepositoryPort.findByRolCodigo("ADMINISTRADOR").stream()
        .noneMatch(otro -> otro.estaActivo() && !otro.getId().equals(usuario.getId()))) {
      throw new ReglaNegocioException(
          "Debe quedar al menos un Administrador activo.", "ULTIMO_ADMINISTRADOR");
    }
  }

  @Override
  public UserResponseDto corregirDni(Long usuarioId, CorregirDniRequestDto request) {
    log.info("Acción transaccional: Corrigiendo DNI para usuario ID: {} con motivo: {}", usuarioId, request.motivo());

    Usuario usuario =
        userRepositoryPort
            .findByIdWithRoles(usuarioId)
            .orElseThrow(() -> new UserNotFoundException(usuarioId));

    validarJerarquiaActorSobreUsuario(usuario);

    boolean cambiaDni = !java.util.Objects.equals(request.nuevoDni(), usuario.getDni());
    boolean actualizaNombre = request.nombreCompleto() != null && !request.nombreCompleto().isBlank();
    if (!cambiaDni && !actualizaNombre) {
      throw new BusinessException("Consulta el DNI en RENIEC para actualizar el nombre", "SIN_CAMBIOS");
    }

    if (cambiaDni && userRepositoryPort.existsByDni(request.nuevoDni())) {
      throw new ReglaNegocioException("El DNI ingresado ya se encuentra asignado a otro usuario", "DNI_DUPLICADO");
    }

    auditoria.syncCurrentUserFromSecurityContext();

    LocalDateTime ahora = LocalDateTime.now();
    if (cambiaDni) usuario.corregirDni(request.nuevoDni(), ahora);
    if (actualizaNombre) usuario.actualizarNombreCompleto(request.nombreCompleto(), ahora);

    Usuario usuarioActualizado = userRepositoryPort.save(usuario);
    log.info("DNI del usuario ID: {} corregido exitosamente a {}", usuarioId, request.nuevoDni());

    return enriquecerAreas(userMapper.toResponseDto(usuarioActualizado));
  }
}
