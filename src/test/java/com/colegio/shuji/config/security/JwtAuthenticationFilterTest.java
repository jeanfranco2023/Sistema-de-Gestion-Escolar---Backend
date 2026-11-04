package com.colegio.shuji.config.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.colegio.shuji.config.persistence.SupabaseAuditInterceptor;
import com.colegio.shuji.usuario.application.port.out.SessionRepositoryPort;
import com.colegio.shuji.usuario.application.port.out.UserRepositoryPort;
import com.colegio.shuji.usuario.domain.model.Rol;
import com.colegio.shuji.usuario.domain.model.Usuario;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class JwtAuthenticationFilterTest {
  private JwtTokenProvider tokenProvider;

  @BeforeEach
  void setUp() {
    SecurityContextHolder.clearContext();
    tokenProvider = new JwtTokenProvider();
    ReflectionTestUtils.setField(
        tokenProvider, "jwtSecret", Base64.getEncoder().encodeToString(new byte[32]));
    ReflectionTestUtils.setField(tokenProvider, "jwtExpirationMs", 60_000L);
    ReflectionTestUtils.setField(tokenProvider, "jwtRefreshExpirationMs", 60_000L);
    tokenProvider.init();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void refreshTokenContinuesAsUnauthenticatedRequest() throws Exception {
    String refreshToken = tokenProvider.generateRefreshToken(42L, "test-apoderado");
    var userRepository = mock(UserRepositoryPort.class);
    var auditInterceptor = mock(SupabaseAuditInterceptor.class);
    var filter = new JwtAuthenticationFilter(tokenProvider, auditInterceptor, userRepository);
    var request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + refreshToken);
    var response = new MockHttpServletResponse();
    var chainContinued = new AtomicBoolean();

    filter.doFilter(request, response, (req, res) -> chainContinued.set(true));

    assertTrue(chainContinued.get());
    assertNull(SecurityContextHolder.getContext().getAuthentication());
    assertTrue(tokenProvider.validateRefreshToken(refreshToken));
    assertFalse(tokenProvider.validateAccessToken(refreshToken));
    verifyNoInteractions(userRepository, auditInterceptor);
  }

  @Test
  void accessAndRefreshTokensHaveDistinctTypes() {
    String accessToken = tokenProvider.generateToken(42L, "test-apoderado", java.util.List.of("APODERADO"));
    String refreshToken = tokenProvider.generateRefreshToken(42L, "test-apoderado");

    assertTrue(tokenProvider.validateAccessToken(accessToken));
    assertFalse(tokenProvider.validateRefreshToken(accessToken));
    assertTrue(tokenProvider.validateRefreshToken(refreshToken));
    assertFalse(tokenProvider.validateAccessToken(refreshToken));
  }

  @Test
  void usuarioConDebeCambiarPasswordBloqueadoEnOtrosRecursos() throws Exception {
    String accessToken = tokenProvider.generateToken(10L, "docente_carlos", java.util.List.of("DOCENTE"));
    var userRepository = mock(UserRepositoryPort.class);
    var auditInterceptor = mock(SupabaseAuditInterceptor.class);
    Usuario usuario = Usuario.builder()
        .id(10L)
        .username("docente_carlos")
        .activo(true)
        .debeCambiarPassword(true)
        .roles(Set.of(Rol.builder().id((short) 2).codigo("DOCENTE").build()))
        .build();
    when(userRepository.obtenerPorId(10L)).thenReturn(Optional.of(usuario));

    var filter = new JwtAuthenticationFilter(tokenProvider, auditInterceptor, userRepository);
    var request = new MockHttpServletRequest("GET", "/api/v1/usuarios/me");
    request.addHeader("Authorization", "Bearer " + accessToken);
    var response = new MockHttpServletResponse();
    var chainContinued = new AtomicBoolean();

    filter.doFilter(request, response, (req, res) -> chainContinued.set(true));

    assertFalse(chainContinued.get());
    assertEquals(403, response.getStatus());
    assertTrue(response.getContentAsString().contains("PASSWORD_CHANGE_REQUIRED"));
  }

  @Test
  void usuarioConDebeCambiarPasswordPermitidoEnCambiarPasswordInicial() throws Exception {
    String accessToken = tokenProvider.generateToken(10L, "docente_carlos", java.util.List.of("DOCENTE"));
    var userRepository = mock(UserRepositoryPort.class);
    var auditInterceptor = mock(SupabaseAuditInterceptor.class);
    Usuario usuario = Usuario.builder()
        .id(10L)
        .username("docente_carlos")
        .activo(true)
        .debeCambiarPassword(true)
        .roles(Set.of(Rol.builder().id((short) 2).codigo("DOCENTE").build()))
        .build();
    when(userRepository.obtenerPorId(10L)).thenReturn(Optional.of(usuario));

    var filter = new JwtAuthenticationFilter(tokenProvider, auditInterceptor, userRepository);
    var request = new MockHttpServletRequest("POST", "/api/v1/auth/cambiar-password-inicial");
    request.addHeader("Authorization", "Bearer " + accessToken);
    var response = new MockHttpServletResponse();
    var chainContinued = new AtomicBoolean();

    filter.doFilter(request, response, (req, res) -> chainContinued.set(true));

    assertTrue(chainContinued.get());
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
  }

  @Test
  void sesionRevocadaRetorna401Inmediatamente() throws Exception {
    Long sessionId = 999L;
    String accessToken =
        tokenProvider.generateToken(10L, "docente_carlos", java.util.List.of("DOCENTE"), sessionId);
    var userRepository = mock(UserRepositoryPort.class);
    var auditInterceptor = mock(SupabaseAuditInterceptor.class);
    var sessionRepository = mock(SessionRepositoryPort.class);

    when(sessionRepository.existsById(sessionId)).thenReturn(false);

    var filter =
        new JwtAuthenticationFilter(tokenProvider, auditInterceptor, userRepository, sessionRepository);
    var request = new MockHttpServletRequest("GET", "/api/v1/usuarios/me");
    request.addHeader("Authorization", "Bearer " + accessToken);
    var response = new MockHttpServletResponse();
    var chainContinued = new AtomicBoolean();

    filter.doFilter(request, response, (req, res) -> chainContinued.set(true));

    assertFalse(chainContinued.get());
    assertEquals(401, response.getStatus());
    assertTrue(response.getContentAsString().contains("SESSION_REVOKED"));
    assertTrue(response.getContentAsString().contains("Sesión cerrada o token revocado"));
    verifyNoInteractions(userRepository, auditInterceptor);
  }

  @Test
  void sesionActivaPermiteAcceso() throws Exception {
    Long sessionId = 888L;
    String accessToken =
        tokenProvider.generateToken(10L, "docente_carlos", java.util.List.of("DOCENTE"), sessionId);
    var userRepository = mock(UserRepositoryPort.class);
    var auditInterceptor = mock(SupabaseAuditInterceptor.class);
    var sessionRepository = mock(SessionRepositoryPort.class);

    when(sessionRepository.existsById(sessionId)).thenReturn(true);
    Usuario usuario =
        Usuario.builder()
            .id(10L)
            .username("docente_carlos")
            .activo(true)
            .roles(Set.of(Rol.builder().id((short) 2).codigo("DOCENTE").build()))
            .build();
    when(userRepository.obtenerPorId(10L)).thenReturn(Optional.of(usuario));

    var filter =
        new JwtAuthenticationFilter(tokenProvider, auditInterceptor, userRepository, sessionRepository);
    var request = new MockHttpServletRequest("GET", "/api/v1/usuarios/me");
    request.addHeader("Authorization", "Bearer " + accessToken);
    var response = new MockHttpServletResponse();
    var chainContinued = new AtomicBoolean();

    filter.doFilter(request, response, (req, res) -> chainContinued.set(true));

    assertTrue(chainContinued.get());
    assertNotNull(SecurityContextHolder.getContext().getAuthentication());
  }
}
