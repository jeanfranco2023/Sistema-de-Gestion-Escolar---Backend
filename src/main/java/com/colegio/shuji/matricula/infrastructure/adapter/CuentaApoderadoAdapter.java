package com.colegio.shuji.matricula.infrastructure.adapter;

import com.colegio.shuji.matricula.application.port.out.CuentaApoderadoPort;
import com.colegio.shuji.usuario.application.port.out.PasswordHashPort;
import com.colegio.shuji.usuario.infrastructure.entity.RolEntity;
import com.colegio.shuji.usuario.infrastructure.entity.UsuarioEntity;
import com.colegio.shuji.usuario.infrastructure.repository.JpaRolRepository;
import com.colegio.shuji.usuario.infrastructure.repository.JpaUsuarioRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adaptador para aprovisionar automáticamente la cuenta institucional del apoderado
 * al confirmarse el pago de su matrícula, generando contraseña temporal para primer inicio de sesión.
 */
@Component
@RequiredArgsConstructor
public class CuentaApoderadoAdapter implements CuentaApoderadoPort {

  private static final Logger log = LoggerFactory.getLogger(CuentaApoderadoAdapter.class);

  private final JpaUsuarioRepository usuarioRepository;
  private final JpaRolRepository rolRepository;
  private final PasswordHashPort passwordEncoder;

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public CredencialesApoderado asegurarCuentaApoderado(
      String numeroDocumento,
      String email,
      String nombres,
      String apellidoPaterno,
      String apellidoMaterno,
      String telefono,
      String direccion) {

    if (email == null || email.isBlank()) {
      log.warn("No se puede aprovisionar cuenta de apoderado sin correo electrónico (DNI: {})", numeroDocumento);
      return null;
    }

    String emailNorm = email.trim().toLowerCase();
    String dniNorm = numeroDocumento != null && !numeroDocumento.isBlank() ? numeroDocumento.trim() : null;

    // Buscar si ya existe usuario por email o por DNI
    Optional<UsuarioEntity> usuarioOpt = usuarioRepository.findByEmail(emailNorm);
    if (usuarioOpt.isEmpty() && dniNorm != null) {
      usuarioOpt = usuarioRepository.findByDni(dniNorm);
    }

    RolEntity rolApoderado = rolRepository.findByCodigo("APODERADO")
        .or(() -> rolRepository.findByCodigo("ROL_APODERADO"))
        .orElseThrow(() -> new IllegalStateException("El rol APODERADO no existe en el catálogo institucional"));

    if (usuarioOpt.isPresent()) {
      UsuarioEntity usuario = usuarioOpt.get();
      if (!emailNorm.equalsIgnoreCase(usuario.getEmail())
          || (usuario.getDni() != null && !usuario.getDni().isBlank() && !usuario.getDni().equals(dniNorm))
          || (usuario.getRoles() != null && usuario.getRoles().stream().anyMatch(
              rol -> !"APODERADO".equalsIgnoreCase(rol.getCodigo()) && !"ROL_APODERADO".equalsIgnoreCase(rol.getCodigo())))) {
        log.warn("Se omitió la vinculación de una cuenta de apoderado con identidad incompatible");
        return null;
      }
      log.info("Usuario existente encontrado para apoderado (ID: {}, username: {}, email: {})",
          usuario.getId(), usuario.getUsername(), usuario.getEmail());

      boolean teniaRol = usuario.getRoles() != null && usuario.getRoles().stream()
          .anyMatch(r -> "APODERADO".equalsIgnoreCase(r.getCodigo()) || "ROL_APODERADO".equalsIgnoreCase(r.getCodigo()));
      if (!teniaRol) {
        if (usuario.getRoles() == null) {
          usuario.setRoles(new HashSet<>());
        }
        usuario.getRoles().add(rolApoderado);
      }

      if (dniNorm != null && (usuario.getDni() == null || usuario.getDni().isBlank())) {
        usuario.setDni(dniNorm);
      }

      String passwordTemporal = null;
      if (Boolean.TRUE.equals(usuario.getDebeCambiarPassword())) {
        passwordTemporal = generarPasswordTemporal();
        usuario.setUsuarioPassword(passwordEncoder.encode(passwordTemporal));
      }

      UsuarioEntity actualizado = usuarioRepository.save(usuario);
      return new CredencialesApoderado(
          actualizado.getId(),
          actualizado.getUsername(),
          actualizado.getEmail(),
          passwordTemporal,
          false);
    }

    // Crear nuevo usuario institucional con contraseña temporal
    String passwordTemporal = generarPasswordTemporal();
    String username = generarUsernameParaApoderado(emailNorm);
    String nombreCompleto = Stream.of(nombres, apellidoPaterno, apellidoMaterno)
        .filter(s -> s != null && !s.isBlank())
        .map(String::trim)
        .collect(Collectors.joining(" "));
    if (nombreCompleto.length() > 200) {
      nombreCompleto = nombreCompleto.substring(0, 200);
    }

    UsuarioEntity nuevoUsuario = UsuarioEntity.builder()
        .username(username)
        .email(emailNorm)
        .nombreCompleto(nombreCompleto.isBlank() ? "Apoderado" : nombreCompleto)
        .dni(dniNorm)
        .telefono(telefono != null && telefono.length() > 15 ? telefono.substring(0, 15) : telefono)
        .direccion(direccion != null && direccion.length() > 150 ? direccion.substring(0, 150) : direccion)
        .usuarioPassword(passwordEncoder.encode(passwordTemporal))
        .activo(true)
        .debeCambiarPassword(true) // Obliga a cambiar contraseña en primer inicio de sesión
        .estadoLaboral("ACTIVO")
        .roles(new HashSet<>(List.of(rolApoderado)))
        .build();

    UsuarioEntity guardado = usuarioRepository.save(nuevoUsuario);
    log.info("Usuario creado exitosamente para apoderado (ID: {}, username: {}, email: {}) con flag debeCambiarPassword=true",
        guardado.getId(), guardado.getUsername(), guardado.getEmail());

    return new CredencialesApoderado(
        guardado.getId(),
        guardado.getUsername(),
        guardado.getEmail(),
        passwordTemporal,
        true);
  }

  private String generarPasswordTemporal() {
    return "Shuji2026!" + UUID.randomUUID().toString().replace("-", "").substring(0, 6);
  }

  private String generarUsernameParaApoderado(String email) {
    if (email == null || email.isBlank()) {
      return "apod_" + UUID.randomUUID().toString().substring(0, 8);
    }
    String emailNorm = email.trim().toLowerCase();
    // Si cabe en la columna de nombre_usuario (hasta 30 chars), usar el correo directamente como username
    if (emailNorm.length() <= 30 && !usuarioRepository.existsByUsername(emailNorm)) {
      return emailNorm;
    }
    // Si excede 30 chars o colisiona, usar el correo o base única de hasta 30 caracteres
    String candidato = emailNorm.length() > 30 ? emailNorm.substring(0, 30) : emailNorm;
    int counter = 1;
    while (usuarioRepository.existsByUsername(candidato)) {
      String sufijo = "_" + counter++;
      int maxBaseLen = 30 - sufijo.length();
      String baseAjustada = emailNorm.length() > maxBaseLen ? emailNorm.substring(0, maxBaseLen) : emailNorm;
      candidato = baseAjustada + sufijo;
    }
    return candidato;
  }
}
