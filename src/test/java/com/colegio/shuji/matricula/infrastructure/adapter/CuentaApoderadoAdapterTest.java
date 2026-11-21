package com.colegio.shuji.matricula.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.colegio.shuji.usuario.application.port.out.PasswordHashPort;
import com.colegio.shuji.usuario.infrastructure.entity.RolEntity;
import com.colegio.shuji.usuario.infrastructure.entity.UsuarioEntity;
import com.colegio.shuji.usuario.infrastructure.repository.JpaRolRepository;
import com.colegio.shuji.usuario.infrastructure.repository.JpaUsuarioRepository;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CuentaApoderadoAdapterTest {

  private JpaUsuarioRepository usuarioRepository;
  private JpaRolRepository rolRepository;
  private PasswordHashPort passwordEncoder;
  private CuentaApoderadoAdapter adapter;

  private RolEntity rolApoderado;

  @BeforeEach
  void setUp() {
    usuarioRepository = mock(JpaUsuarioRepository.class);
    rolRepository = mock(JpaRolRepository.class);
    passwordEncoder = mock(PasswordHashPort.class);
    adapter = new CuentaApoderadoAdapter(usuarioRepository, rolRepository, passwordEncoder);

    rolApoderado = RolEntity.builder().id((short) 4).codigo("APODERADO").nombre("Apoderado").build();
    when(rolRepository.findByCodigo("APODERADO")).thenReturn(Optional.of(rolApoderado));
    when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "hash_" + inv.getArgument(0));
  }

  @Test
  @DisplayName("Crea un nuevo usuario apoderado con contraseña temporal y debeCambiarPassword=true")
  void creaNuevoUsuarioApoderadoConPasswordTemporalYFlagPrimerLogin() {
    when(usuarioRepository.findByEmail("padre@example.test")).thenReturn(Optional.empty());
    when(usuarioRepository.findByDni("76543210")).thenReturn(Optional.empty());
    when(usuarioRepository.existsByUsername(anyString())).thenReturn(false);
    when(usuarioRepository.save(any(UsuarioEntity.class))).thenAnswer(inv -> {
      UsuarioEntity u = inv.getArgument(0);
      u.setId(99L);
      return u;
    });

    var resultado = adapter.asegurarCuentaApoderado(
        "76543210",
        "padre@example.test",
        "Carlos",
        "Mendoza",
        "Perez",
        "987654321",
        "Av. Siempre Viva 123");

    assertNotNull(resultado);
    assertEquals(99L, resultado.usuarioId());
    assertEquals("padre@example.test", resultado.email());
    assertEquals("padre@example.test", resultado.username());
    assertTrue(resultado.esNuevaCuenta());
    assertNotNull(resultado.passwordTemporal());
    assertTrue(resultado.passwordTemporal().startsWith("Shuji2026!"));

    ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
    verify(usuarioRepository).save(captor.capture());
    UsuarioEntity guardado = captor.getValue();

    assertEquals("padre@example.test", guardado.getUsername());
    assertEquals("padre@example.test", guardado.getEmail());
    assertEquals("Carlos Mendoza Perez", guardado.getNombreCompleto());
    assertEquals("76543210", guardado.getDni());
    assertEquals("987654321", guardado.getTelefono());
    assertTrue(Boolean.TRUE.equals(guardado.getActivo()));
    assertTrue(Boolean.TRUE.equals(guardado.getDebeCambiarPassword()));
    assertTrue(guardado.getUsuarioPassword().startsWith("hash_Shuji2026!"));
    assertTrue(guardado.getRoles().contains(rolApoderado));
  }

  @Test
  @DisplayName("Reutiliza usuario existente por email y no renueva clave si ya no debe cambiarla")
  void reutilizaUsuarioExistenteSinCambioPasswordSiYaNoDebeCambiarla() {
    UsuarioEntity existente = UsuarioEntity.builder()
        .id(55L)
        .username("carlos_mendoza")
        .email("padre@example.test")
        .usuarioPassword("hash_existente")
        .debeCambiarPassword(false)
        .roles(new HashSet<>(Set.of(rolApoderado)))
        .build();

    when(usuarioRepository.findByEmail("padre@example.test")).thenReturn(Optional.of(existente));
    when(usuarioRepository.save(any(UsuarioEntity.class))).thenReturn(existente);

    var resultado = adapter.asegurarCuentaApoderado(
        "76543210",
        "padre@example.test",
        "Carlos",
        "Mendoza",
        "Perez",
        "987654321",
        "Av. Siempre Viva 123");

    assertNotNull(resultado);
    assertEquals(55L, resultado.usuarioId());
    assertFalse(resultado.esNuevaCuenta());
    assertNull(resultado.passwordTemporal());
    assertEquals("hash_existente", existente.getUsuarioPassword());
  }

  @Test
  @DisplayName("No emite una contraseña temporal si el correo no coincide con el de la cuenta")
  void protegeCuentaExistenteSiElCorreoNoCoincide() {
    UsuarioEntity existente = UsuarioEntity.builder()
        .id(77L)
        .username("apod_76543210")
        .email("otro_correo@example.test")
        .dni("76543210")
        .usuarioPassword("hash_anterior")
        .debeCambiarPassword(true)
        .roles(new HashSet<>())
        .build();

    when(usuarioRepository.findByEmail("padre@example.test")).thenReturn(Optional.empty());
    when(usuarioRepository.findByDni("76543210")).thenReturn(Optional.of(existente));

    var resultado = adapter.asegurarCuentaApoderado(
        "76543210",
        "padre@example.test",
        "Carlos",
        "Mendoza",
        "Perez",
        "987654321",
        "Av. Siempre Viva 123");

    assertNull(resultado);
    assertEquals("hash_anterior", existente.getUsuarioPassword());
    assertTrue(existente.getRoles().isEmpty());
  }

  @Test
  @DisplayName("Retorna null si no se proporciona email")
  void retornaNullSinEmail() {
    var resultado = adapter.asegurarCuentaApoderado("76543210", "   ", "Carlos", "M", "P", null, null);
    assertNull(resultado);
  }
}
