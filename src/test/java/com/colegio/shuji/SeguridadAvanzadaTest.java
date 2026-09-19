package com.colegio.shuji;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.colegio.shuji.config.security.JwtTokenProvider;
import com.colegio.shuji.config.security.ProductionSecurityConfigurationValidator;
import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.ApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.model.Apoderado;
import com.colegio.shuji.matricula.domain.model.EstudianteApoderado;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.application.dto.in.CrearPreferenciaMercadoPagoDto;
import com.colegio.shuji.tesoreria.application.mapper.TesoreriaMapper;
import com.colegio.shuji.tesoreria.application.port.in.EmitirComprobanteUseCase;
import com.colegio.shuji.tesoreria.application.port.out.ConceptoCobroRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.MercadoPagoPort;
import com.colegio.shuji.tesoreria.application.port.out.ObligacionRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.PagoRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort;
import com.colegio.shuji.tesoreria.application.service.TesoreriaService;
import com.colegio.shuji.tesoreria.domain.enums.EstadoObligacion;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import com.colegio.shuji.tesoreria.infrastructure.adapter.MercadoPagoHttpAdapter;
import com.colegio.shuji.tesoreria.infrastructure.security.MercadoPagoProductionConfigurationValidator;
import com.colegio.shuji.tesoreria.infrastructure.security.MercadoPagoWebhookSignatureValidator;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;
import com.colegio.shuji.config.security.JwtAuthenticationFilter;
import com.colegio.shuji.config.security.RateLimitingFilter;
import com.colegio.shuji.config.persistence.SupabaseAuditInterceptor;
import com.colegio.shuji.shared.application.port.out.AuditContextPort;
import com.colegio.shuji.usuario.application.dto.in.RegisterUserRequestDto;
import com.colegio.shuji.usuario.application.dto.out.UserResponseDto;
import com.colegio.shuji.usuario.application.mapper.UserMapper;
import com.colegio.shuji.usuario.application.port.out.PasswordHashPort;
import com.colegio.shuji.usuario.application.port.out.RoleRepositoryPort;
import com.colegio.shuji.usuario.application.port.out.SessionRepositoryPort;
import com.colegio.shuji.usuario.application.port.out.TokenPort;
import com.colegio.shuji.usuario.application.port.out.UserRepositoryPort;
import com.colegio.shuji.usuario.application.service.AuthService;
import com.colegio.shuji.usuario.domain.model.Rol;
import com.colegio.shuji.usuario.domain.model.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Set;
import org.mockito.ArgumentCaptor;

/**
 * Suite completa de pruebas de seguridad:
 * 1. Webhooks: Criptografía HMAC-SHA256, tiempo constante, prevención de replay attack.
 * 2. Anti-IDOR: Control de acceso basado en responsabilidad económica familiar.
 * 3. Open Redirect: Validación estricta de esquema HTTPS y lista blanca de dominios en backUrls.
 * 4. Rotación Real de Secretos: Soporte dual-key en JWT y Webhooks.
 * 5. Configuración de producción: Fail-fast ante ausencia o debilidad de secretos.
 */
class SeguridadAvanzadaTest {

  @Nested
  @DisplayName("1. Seguridad de Webhooks (HMAC-SHA256 y Anti-Replay)")
  class WebhookSecurityTests {

    private final String SECRET = "test_webhook_secret_key_1234567890";
    private final long AHORA = 1750000000L;
    private final Clock clock = Clock.fixed(Instant.ofEpochSecond(AHORA), ZoneOffset.UTC);
    private final MercadoPagoWebhookSignatureValidator validator =
        new MercadoPagoWebhookSignatureValidator(SECRET, null, Optional.of(clock));

    @Test
    @DisplayName("Firma válida y reciente es aceptada")
    void firmaValidaYRecienteEsAceptada() throws Exception {
      String requestId = "req-abc-123";
      String dataId = "99887766";
      String ts = String.valueOf(AHORA - 30); // 30 segundos de antigüedad

      String firmaHex = calcularHmac(SECRET, "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";");
      String signatureHeader = "ts=" + ts + ",v1=" + firmaHex;

      assertTrue(validator.validar(signatureHeader, requestId, dataId));
    }

    @Test
    @DisplayName("Firma falsificada o alterada es rechazada (Anti-Tampering)")
    void firmaFalsificadaEsRechazada() {
      String requestId = "req-abc-123";
      String dataId = "99887766";
      String ts = String.valueOf(AHORA - 10);
      String firmaFalsa = "a".repeat(64);
      String signatureHeader = "ts=" + ts + ",v1=" + firmaFalsa;

      assertFalse(validator.validar(signatureHeader, requestId, dataId));
    }

    @Test
    @DisplayName("Ataque de repetición (Replay Attack): Timestamp > 300 segundos es rechazado")
    void timestampExpiradoEsRechazadoComoReplayAttack() throws Exception {
      String requestId = "req-replay";
      String dataId = "99887766";
      String tsExpirado = String.valueOf(AHORA - 301); // 301 segundos atrás (> 5 minutos)

      String firmaHex = calcularHmac(SECRET, "id:" + dataId + ";request-id:" + requestId + ";ts:" + tsExpirado + ";");
      String signatureHeader = "ts=" + tsExpirado + ",v1=" + firmaHex;

      assertFalse(validator.validar(signatureHeader, requestId, dataId), "Debe rechazar timestamps fuera de la ventana de 5 min");
    }

    @Test
    @DisplayName("Firma malformada o formato no hex es rechazada de inmediato")
    void firmaMalformadaEsRechazada() {
      assertFalse(validator.validar("ts=invalido,v1=not-hex", "req", "123"));
      assertFalse(validator.validar("", "req", "123"));
      assertFalse(validator.validar(null, "req", "123"));
    }
  }

  @Nested
  @DisplayName("2. Prevención de Open Redirect en Back URLs")
  class OpenRedirectSecurityTests {

    @Test
    @DisplayName("Rechaza protocolo HTTP inseguro")
    void rechazaProtocoloInseguroHttp() {
      var adapter = new MercadoPagoHttpAdapter(
          "https://api.mercadopago.com", "token", "key", "colegio.edu.pe,localhost");

      var obligacion = crearObligacionEjemplo();

      var ex = assertThrows(BusinessException.class, () ->
          adapter.crearPreferencia(
              obligacion,
              "Pension Escolar",
              "http://colegio.edu.pe/exito",
              "https://colegio.edu.pe/fallo"));

      assertTrue(ex.getMessage().contains("HTTPS"));
    }

    @Test
    @DisplayName("Rechaza dominios no autorizados fuera de la lista blanca")
    void rechazaDominioNoAutorizado() {
      var adapter = new MercadoPagoHttpAdapter(
          "https://api.mercadopago.com", "token", "key", "colegio.edu.pe");

      var obligacion = crearObligacionEjemplo();

      var ex = assertThrows(BusinessException.class, () ->
          adapter.crearPreferencia(
              obligacion,
              "Pension Escolar",
              "https://sitio-malicioso.com/robo",
              "https://colegio.edu.pe/fallo"));

      assertTrue(ex.getMessage().contains("Dominio no permitido"));
    }

    @Test
    @DisplayName("Rechaza URLs que incluyan credenciales o user-info en la URL")
    void rechazaUserInfoEnUrl() {
      var adapter = new MercadoPagoHttpAdapter(
          "https://api.mercadopago.com", "token", "key", "colegio.edu.pe,localhost");

      var obligacion = crearObligacionEjemplo();

      assertThrows(BusinessException.class, () ->
          adapter.crearPreferencia(
              obligacion,
              "Pension Escolar",
              "https://usuario:password@colegio.edu.pe/exito",
              "https://colegio.edu.pe/fallo"));
    }
  }

  @Nested
  @DisplayName("3. Prevención de IDOR (Insecure Direct Object Reference) en Tesorería")
  class AntiIdorSecurityTests {

    @Test
    @DisplayName("Apoderado sin responsabilidad económica es bloqueado al intentar pagar (IDOR)")
    void apoderadoSinResponsabilidadEconomicaEsBloqueado() {
      var mapper = mock(TesoreriaMapper.class);
      var obligaciones = mock(ObligacionRepositoryPort.class);
      var pagos = mock(PagoRepositoryPort.class);
      var conceptos = mock(ConceptoCobroRepositoryPort.class);
      var matriculas = mock(MatriculaRepositoryPort.class);
      var anios = mock(AnioLectivoRepositoryPort.class);
      var verificador = mock(VerificarPagoPort.class);
      var mercadoPago = mock(MercadoPagoPort.class);
      var comprobantes = mock(EmitirComprobanteUseCase.class);
      var actor = mock(ActorActualPort.class);
      var apoderados = mock(ApoderadoRepositoryPort.class);
      var estudianteApoderados = mock(EstudianteApoderadoRepositoryPort.class);

      var service = new TesoreriaService(
          mapper, obligaciones, pagos, conceptos, matriculas,
          anios, verificador, mercadoPago, comprobantes, actor,
          apoderados, estudianteApoderados);

      Long obligacionId = 55L;
      Long matriculaId = 10L;
      Long estudianteId = 20L;
      Long usuarioApoderadoId = 99L;
      Long apoderadoId = 44L;

      var obligacion = crearObligacionEjemplo();
      ReflectionTestUtils.setField(obligacion, "id", obligacionId);
      ReflectionTestUtils.setField(obligacion, "matriculaId", matriculaId);

      var matricula = Matricula.builder().id(matriculaId).estudianteId(estudianteId).build();
      var apoderado = Apoderado.builder().id(apoderadoId).usuarioId(usuarioApoderadoId).build();

      // Vínculo existente pero sin responsabilidad económica
      var vinculoSinPoderEconomico = EstudianteApoderado.builder()
          .estudianteId(estudianteId)
          .apoderadoId(apoderadoId)
          .esResponsableEconomico(false)
          .build();

      when(obligaciones.buscarPorId(obligacionId)).thenReturn(Optional.of(obligacion));
      when(actor.tieneRol("APODERADO")).thenReturn(true);
      when(actor.tieneRol("DIRECCION")).thenReturn(false);
      when(actor.tieneRol("SECRETARIA")).thenReturn(false);
      when(actor.usuarioId()).thenReturn(usuarioApoderadoId);
      when(apoderados.buscarPorUsuarioId(usuarioApoderadoId)).thenReturn(List.of(apoderado));
      when(matriculas.buscarPorId(matriculaId)).thenReturn(Optional.of(matricula));
      when(estudianteApoderados.buscarPorApoderadoId(apoderadoId)).thenReturn(List.of(vinculoSinPoderEconomico));

      var dto = new CrearPreferenciaMercadoPagoDto(
          obligacionId, "https://colegio.edu.pe/exito", "https://colegio.edu.pe/fallo");

      var ex = assertThrows(AccessDeniedException.class, () -> service.crearPreferenciaMercadoPago(dto));
      assertTrue(ex.getMessage().contains("responsable económico"), "Debe rechazar IDOR si no es responsable económico");
    }

    @Test
    @DisplayName("Apoderado ajeno al estudiante es bloqueado de inmediato")
    void apoderadoAjenoEsBloqueado() {
      var mapper = mock(TesoreriaMapper.class);
      var obligaciones = mock(ObligacionRepositoryPort.class);
      var pagos = mock(PagoRepositoryPort.class);
      var conceptos = mock(ConceptoCobroRepositoryPort.class);
      var matriculas = mock(MatriculaRepositoryPort.class);
      var anios = mock(AnioLectivoRepositoryPort.class);
      var verificador = mock(VerificarPagoPort.class);
      var mercadoPago = mock(MercadoPagoPort.class);
      var comprobantes = mock(EmitirComprobanteUseCase.class);
      var actor = mock(ActorActualPort.class);
      var apoderados = mock(ApoderadoRepositoryPort.class);
      var estudianteApoderados = mock(EstudianteApoderadoRepositoryPort.class);

      var service = new TesoreriaService(
          mapper, obligaciones, pagos, conceptos, matriculas,
          anios, verificador, mercadoPago, comprobantes, actor,
          apoderados, estudianteApoderados);

      Long obligacionId = 55L;
      Long matriculaId = 10L;
      var obligacion = crearObligacionEjemplo();
      ReflectionTestUtils.setField(obligacion, "id", obligacionId);
      ReflectionTestUtils.setField(obligacion, "matriculaId", matriculaId);
      var matricula = Matricula.builder().id(matriculaId).estudianteId(20L).build();
      var apoderado = Apoderado.builder().id(44L).usuarioId(99L).build();

      when(obligaciones.buscarPorId(obligacionId)).thenReturn(Optional.of(obligacion));
      when(actor.tieneRol("APODERADO")).thenReturn(true);
      when(actor.tieneRol("DIRECCION")).thenReturn(false);
      when(actor.tieneRol("SECRETARIA")).thenReturn(false);
      when(actor.usuarioId()).thenReturn(99L);
      when(apoderados.buscarPorUsuarioId(99L)).thenReturn(List.of(apoderado));
      when(matriculas.buscarPorId(matriculaId)).thenReturn(Optional.of(matricula));
      // Sin vínculos con el estudiante
      when(estudianteApoderados.buscarPorApoderadoId(44L)).thenReturn(List.of());

      var dto = new CrearPreferenciaMercadoPagoDto(
          obligacionId, "https://colegio.edu.pe/exito", "https://colegio.edu.pe/fallo");

      assertThrows(AccessDeniedException.class, () -> service.crearPreferenciaMercadoPago(dto));
    }
  }

  @Nested
  @DisplayName("4. Rotación Real de Secretos (Mecanismo Dual-Key)")
  class SecretRotationTests {

    @Test
    @DisplayName("JWT: Token generado con clave previa es válido durante la ventana de rotación")
    void jwtTokenGeneradoConClavePreviaEsValido() {
      String secretViejo = "clave_secreta_antigua_de_rotacion_32bytes!!";
      String secretNuevo = "clave_secreta_nueva_de_rotacion_32bytes!!";

      // Instancia previa que firma con la clave antigua
      var providerAntiguo = new JwtTokenProvider();
      ReflectionTestUtils.setField(providerAntiguo, "jwtSecret", secretViejo);
      ReflectionTestUtils.setField(providerAntiguo, "jwtExpirationMs", 3600000L);
      ReflectionTestUtils.setField(providerAntiguo, "jwtRefreshExpirationMs", 7200000L);
      providerAntiguo.init();

      String tokenConClaveAntigua = providerAntiguo.generateToken(1L, "usuario.rotacion", List.of("APODERADO"));

      // Instancia rotada: clave primaria es secretNuevo, clave previa es secretViejo
      var providerRotado = new JwtTokenProvider();
      ReflectionTestUtils.setField(providerRotado, "jwtSecret", secretNuevo);
      ReflectionTestUtils.setField(providerRotado, "jwtPreviousSecret", secretViejo);
      ReflectionTestUtils.setField(providerRotado, "jwtExpirationMs", 3600000L);
      ReflectionTestUtils.setField(providerRotado, "jwtRefreshExpirationMs", 7200000L);
      providerRotado.init();

      // Debe validar exitosamente sin desconectar al usuario
      assertTrue(providerRotado.validateToken(tokenConClaveAntigua), "El token firmado con clave previa debe validarse exitosamente");
      assertEquals("usuario.rotacion", providerRotado.getUsernameFromToken(tokenConClaveAntigua));

      // Nuevos tokens se firman con secretNuevo
      String tokenNuevo = providerRotado.generateToken(2L, "usuario.nuevo", List.of("DIRECCION"));
      assertTrue(providerRotado.validateToken(tokenNuevo));
    }

    @Test
    @DisplayName("Webhook: Webhook firmado con secreto previo es aceptado durante rotación")
    void webhookFirmadoConSecretoPrevioEsAceptado() throws Exception {
      String secretPrincipal = "webhook_secret_nuevo_1234567890";
      String secretPrevio = "webhook_secret_antiguo_0987654321";
      long ahora = 1750000000L;
      Clock clock = Clock.fixed(Instant.ofEpochSecond(ahora), ZoneOffset.UTC);

      var validatorDual = new MercadoPagoWebhookSignatureValidator(secretPrincipal, secretPrevio, java.util.Optional.of(clock));

      String requestId = "req-rot";
      String dataId = "445566";
      String ts = String.valueOf(ahora - 15);

      // Firmado con el secreto previo
      String firmaHex = calcularHmac(secretPrevio, "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";");
      String header = "ts=" + ts + ",v1=" + firmaHex;

      assertTrue(validatorDual.validar(header, requestId, dataId), "Debe aceptar webhook con clave previa");
    }
  }

  @Nested
  @DisplayName("5. Configuración y Validadores Fail-Fast en Producción")
  class ProductionSecretsTests {

    @Test
    @DisplayName("Falla al iniciar si MERCADOPAGO_WEBHOOK_SECRET está vacío en perfil prod")
    void fallaInicioSiWebhookSecretFaltaEnProduccion() {
      var validator = new MercadoPagoProductionConfigurationValidator("", "colegio.edu.pe", "APP_USR-token");
      var ex = assertThrows(IllegalStateException.class, () -> invocarValidar(validator));
      assertTrue(ex.getMessage().contains("MERCADOPAGO_WEBHOOK_SECRET"));
    }

    @Test
    @DisplayName("Falla al iniciar si MERCADOPAGO_RETURN_URL_HOSTS está vacío en perfil prod")
    void fallaInicioSiHostsFaltanEnProduccion() {
      var validator = new MercadoPagoProductionConfigurationValidator("secret123", "", "APP_USR-token");
      var ex = assertThrows(IllegalStateException.class, () -> invocarValidar(validator));
      assertTrue(ex.getMessage().contains("MERCADOPAGO_RETURN_URL_HOSTS"));
    }

    @Test
    @DisplayName("Falla al iniciar si MERCADOPAGO_ACCESS_TOKEN está vacío o es credencial de test en prod")
    void fallaInicioSiAccessTokenEsInvalidoEnProduccion() {
      var validatorTest = new MercadoPagoProductionConfigurationValidator("secret123", "colegio.edu.pe", "TEST-1234567890");
      var ex1 = assertThrows(IllegalStateException.class, () -> invocarValidar(validatorTest));
      assertTrue(ex1.getMessage().contains("MERCADOPAGO_ACCESS_TOKEN"));

      var validatorVacio = new MercadoPagoProductionConfigurationValidator("secret123", "colegio.edu.pe", "");
      var ex2 = assertThrows(IllegalStateException.class, () -> invocarValidar(validatorVacio));
      assertTrue(ex2.getMessage().contains("MERCADOPAGO_ACCESS_TOKEN"));
    }

    @Test
    @DisplayName("Falla al iniciar si JWT_SECRET usa valor default o es menor a 256 bits en prod")
    void fallaInicioSiJwtSecretEsInseguroEnProduccion() {
      var valDefault = new ProductionSecurityConfigurationValidator("c2h1amlfa2l0YW11cmFfcHJveWVjdG9faW50ZWdyYWRvcl91dHBfMjAyNg==");
      assertThrows(IllegalStateException.class, () -> invocarValidar(valDefault));

      var valCorto = new ProductionSecurityConfigurationValidator("clave_corta_de_16b!");
      assertThrows(IllegalStateException.class, () -> invocarValidar(valCorto));

      var valValido = new ProductionSecurityConfigurationValidator("clave_segura_de_produccion_con_mas_de_32_bytes_de_longitud!");
      assertDoesNotThrow(() -> invocarValidar(valValido));
    }

    private void invocarValidar(Object target) throws Throwable {
      var m = target.getClass().getDeclaredMethod("validar");
      m.setAccessible(true);
      try {
        m.invoke(target);
      } catch (java.lang.reflect.InvocationTargetException ite) {
        throw ite.getCause();
      }
    }
  }

  private static String calcularHmac(String secret, String data) throws Exception {
    Mac hmac = Mac.getInstance("HmacSHA256");
    hmac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return java.util.HexFormat.of().formatHex(hmac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
  }

  private static ObligacionPago crearObligacionEjemplo() {
    return ObligacionPago.builder()
        .id(100L)
        .matriculaId(200L)
        .conceptoId((short) 1)
        .numeroCuota((short) 1)
        .montoBase(new BigDecimal("350.00"))
        .montoMora(BigDecimal.ZERO)
        .montoDescuento(BigDecimal.ZERO)
        .totalPagado(BigDecimal.ZERO)
        .saldoPendiente(new BigDecimal("350.00"))
        .fechaVencimiento(LocalDate.now().plusDays(30))
        .estado(EstadoObligacion.PENDIENTE)
        .build();
  }

  @Nested
  @DisplayName("6. Control de Acceso, Anti-Elevación de Privilegios y Mitigación de Abuso")
  class ControlAccesoYAbusoTests {

    @Test
    @DisplayName("Auto-registro público rechaza elevación de privilegios y fuerza exclusivamente rol APODERADO")
    void autoRegistroFuerzaRolApoderadoRechazandoElevacionDePrivilegios() {
      var userRepo = mock(UserRepositoryPort.class);
      var roleRepo = mock(RoleRepositoryPort.class);
      var sessionRepo = mock(SessionRepositoryPort.class);
      var passEncoder = mock(PasswordHashPort.class);
      var tokenPort = mock(TokenPort.class);
      var userMapper = mock(UserMapper.class);
      var auditPort = mock(AuditContextPort.class);

      var authService = new AuthService(userRepo, roleRepo, sessionRepo, passEncoder, tokenPort, userMapper, auditPort);

      var request = RegisterUserRequestDto.builder()
          .username("atacante")
          .email("atacante@shuji.edu.pe")
          .password("Password123!")
          .roles(Set.of("DIRECCION", "ADMINISTRADOR"))
          .build();

      when(userRepo.existsByUsername("atacante")).thenReturn(false);
      when(userRepo.existsByEmail("atacante@shuji.edu.pe")).thenReturn(false);
      when(passEncoder.encode("Password123!")).thenReturn("encoded_pass");

      var rolApoderado = Rol.builder().id((short) 1).codigo("APODERADO").nombre("Apoderado").build();
      when(roleRepo.findByCodigoIn(List.of("APODERADO"))).thenReturn(List.of(rolApoderado));

      when(userMapper.toDomain(request)).thenReturn(
          Usuario.builder()
              .username(request.getUsername())
              .email(request.getEmail())
              .build());

      var usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
      when(userRepo.save(usuarioCaptor.capture())).thenAnswer(inv -> {
        Usuario u = inv.getArgument(0);
        u.setId(50L);
        return u;
      });

      when(userMapper.toResponseDto(any(Usuario.class))).thenAnswer(inv -> {
        Usuario u = inv.getArgument(0);
        return UserResponseDto.builder().id(u.getId()).username(u.getUsername()).email(u.getEmail()).build();
      });

      var result = authService.register(request);

      assertNotNull(result);
      assertEquals(50L, result.getId());

      Usuario usuarioGuardado = usuarioCaptor.getValue();
      assertNotNull(usuarioGuardado);
      assertEquals(1, usuarioGuardado.getRoles().size());
      assertTrue(usuarioGuardado.getRoles().stream().anyMatch(r -> "APODERADO".equals(r.getCodigo())));
      assertFalse(usuarioGuardado.getRoles().stream().anyMatch(r -> "DIRECCION".equals(r.getCodigo())));
      assertFalse(usuarioGuardado.getRoles().stream().anyMatch(r -> "ADMINISTRADOR".equals(r.getCodigo())));

      verify(roleRepo, never()).findByCodigoIn(argThat(roles -> roles != null && roles.contains("DIRECCION")));
      verify(roleRepo, times(1)).findByCodigoIn(List.of("APODERADO"));
    }

    @Test
    @DisplayName("Petición con token válido pero usuario desactivado recibe HTTP 401 Unauthorized")
    void revocacionInmediataUsuarioDesactivadoRetorna401() throws Exception {
      var tokenProvider = mock(JwtTokenProvider.class);
      var supabaseAudit = mock(SupabaseAuditInterceptor.class);
      var userRepo = mock(UserRepositoryPort.class);

      var filter = new JwtAuthenticationFilter(tokenProvider, supabaseAudit, userRepo);

      var request = mock(HttpServletRequest.class);
      var response = mock(HttpServletResponse.class);
      var filterChain = mock(FilterChain.class);

      when(request.getServletPath()).thenReturn("/api/v1/usuarios");
      when(request.getHeader("Authorization")).thenReturn("Bearer token_jwt_valido");
      when(tokenProvider.validateToken("token_jwt_valido")).thenReturn(true);
      when(tokenProvider.getUserIdFromToken("token_jwt_valido")).thenReturn(88L);
      when(tokenProvider.getUsernameFromToken("token_jwt_valido")).thenReturn("usuario_bloqueado");
      when(tokenProvider.getRolesFromToken("token_jwt_valido")).thenReturn(List.of("APODERADO"));

      var usuarioInactivo = Usuario.builder()
          .id(88L)
          .username("usuario_bloqueado")
          .activo(false)
          .build();
      when(userRepo.obtenerPorId(88L)).thenReturn(Optional.of(usuarioInactivo));

      var stringWriter = new StringWriter();
      var printWriter = new PrintWriter(stringWriter);
      when(response.getWriter()).thenReturn(printWriter);

      filter.doFilter(request, response, filterChain);

      verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      verify(response).setContentType("application/json");
      verify(filterChain, never()).doFilter(request, response);
      assertTrue(stringWriter.toString().contains("Usuario inactivo o revocado"));
    }

    @Test
    @DisplayName("Petición con token válido pero usuario eliminado de BD recibe HTTP 401 Unauthorized")
    void revocacionInmediataUsuarioInexistenteRetorna401() throws Exception {
      var tokenProvider = mock(JwtTokenProvider.class);
      var supabaseAudit = mock(SupabaseAuditInterceptor.class);
      var userRepo = mock(UserRepositoryPort.class);

      var filter = new JwtAuthenticationFilter(tokenProvider, supabaseAudit, userRepo);

      var request = mock(HttpServletRequest.class);
      var response = mock(HttpServletResponse.class);
      var filterChain = mock(FilterChain.class);

      when(request.getServletPath()).thenReturn("/api/v1/usuarios");
      when(request.getHeader("Authorization")).thenReturn("Bearer token_jwt_valido");
      when(tokenProvider.validateToken("token_jwt_valido")).thenReturn(true);
      when(tokenProvider.getUserIdFromToken("token_jwt_valido")).thenReturn(999L);
      when(tokenProvider.getUsernameFromToken("token_jwt_valido")).thenReturn("usuario_borrado");
      when(tokenProvider.getRolesFromToken("token_jwt_valido")).thenReturn(List.of("APODERADO"));

      when(userRepo.obtenerPorId(999L)).thenReturn(Optional.empty());

      var stringWriter = new StringWriter();
      var printWriter = new PrintWriter(stringWriter);
      when(response.getWriter()).thenReturn(printWriter);

      filter.doFilter(request, response, filterChain);

      verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      verify(response).setContentType("application/json");
      verify(filterChain, never()).doFilter(request, response);
    }

    @Test
    @DisplayName("Rate limiting bloquea solicitudes excesivas a /api/v1/auth/login retornando HTTP 429 Too Many Requests")
    void rateLimitingBloqueaPeticionesExcesivasCon429() throws Exception {
      var rateLimitingFilter = new RateLimitingFilter();
      var filterChain = mock(FilterChain.class);
      String testIp = "192.168.100.25";

      // Hasta 15 peticiones permitidas por minuto en /login
      for (int i = 1; i <= 15; i++) {
        var req = mock(HttpServletRequest.class);
        var res = mock(HttpServletResponse.class);
        when(req.getRequestURI()).thenReturn("/api/v1/auth/login");
        when(req.getRemoteAddr()).thenReturn(testIp);

        rateLimitingFilter.doFilter(req, res, filterChain);
      }
      verify(filterChain, times(15)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

      // Petición 16 supera el umbral y recibe HTTP 429
      var reqBloqueada = mock(HttpServletRequest.class);
      var resBloqueada = mock(HttpServletResponse.class);
      when(reqBloqueada.getRequestURI()).thenReturn("/api/v1/auth/login");
      when(reqBloqueada.getRemoteAddr()).thenReturn(testIp);

      var stringWriter = new StringWriter();
      var printWriter = new PrintWriter(stringWriter);
      when(resBloqueada.getWriter()).thenReturn(printWriter);

      rateLimitingFilter.doFilter(reqBloqueada, resBloqueada, filterChain);

      verify(resBloqueada).setStatus(429);
      verify(resBloqueada).setHeader("Retry-After", "60");
      verify(resBloqueada).setContentType("application/json");
      assertTrue(stringWriter.toString().contains("Demasiadas solicitudes"));

      // El filterChain no se ejecuta para la petición bloqueada
      verify(filterChain, times(15)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    @DisplayName("Cambio de rol se aplica inmediatamente en BD sin esperar la expiración del token JWT")
    void cambioDeRolSeAplicaInmediatamenteSinEsperarExpiracionDelToken() throws Exception {
      var tokenProvider = mock(JwtTokenProvider.class);
      var supabaseAudit = mock(SupabaseAuditInterceptor.class);
      var userRepo = mock(UserRepositoryPort.class);

      var filter = new JwtAuthenticationFilter(tokenProvider, supabaseAudit, userRepo);

      var request = mock(HttpServletRequest.class);
      var response = mock(HttpServletResponse.class);
      var filterChain = mock(FilterChain.class);

      when(request.getServletPath()).thenReturn("/api/v1/usuarios");
      when(request.getHeader("Authorization")).thenReturn("Bearer token_con_rol_antiguo");
      when(tokenProvider.validateToken("token_con_rol_antiguo")).thenReturn(true);
      when(tokenProvider.getUserIdFromToken("token_con_rol_antiguo")).thenReturn(77L);
      when(tokenProvider.getUsernameFromToken("token_con_rol_antiguo")).thenReturn("docente_degradado");
      // El token aún declara DIRECCION en los claims desactualizados
      when(tokenProvider.getRolesFromToken("token_con_rol_antiguo")).thenReturn(List.of("DIRECCION", "DOCENTE"));

      // En BD, Dirección le revocó el rol DIRECCION y ahora sólo tiene DOCENTE
      var rolDocente = Rol.builder().id((short) 2).codigo("DOCENTE").nombre("Docente").build();
      var usuarioActualizado = Usuario.builder()
          .id(77L)
          .username("docente_degradado")
          .activo(true)
          .roles(Set.of(rolDocente))
          .build();
      when(userRepo.obtenerPorId(77L)).thenReturn(Optional.of(usuarioActualizado));

      try {
        filter.doFilter(request, response, filterChain);

        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth, "La autenticación debe haber sido establecida en el contexto");
        var authorities = auth.getAuthorities().stream()
            .map(org.springframework.security.core.GrantedAuthority::getAuthority)
            .toList();

        // Debe contener el rol vivo en BD (DOCENTE)
        assertTrue(authorities.contains("ROLE_DOCENTE"), "Debe tener ROLE_DOCENTE de la BD");
        // NO debe contener el rol revocado (DIRECCION) a pesar de estar en el JWT
        assertFalse(authorities.contains("ROLE_DIRECCION"), "No debe conservar ROLE_DIRECCION revocado en BD");

        verify(filterChain, times(1)).doFilter(request, response);
      } finally {
        org.springframework.security.core.context.SecurityContextHolder.clearContext();
      }
    }

    @Test
    @DisplayName("RateLimitingFilter no confía en X-Forwarded-For si behindTrustedProxy es false (anti-spoofing)")
    void rateLimitingNoConfiaEnXForwardedForSinProxyConfiable() {
      var filter = new RateLimitingFilter(false);
      var req = mock(HttpServletRequest.class);
      when(req.getHeader("X-Forwarded-For")).thenReturn("203.0.113.195");
      when(req.getRemoteAddr()).thenReturn("192.168.1.10");

      String ipResuelta = filter.extractClientIp(req);
      assertEquals("192.168.1.10", ipResuelta, "Debe ignorar X-Forwarded-For cuando behindTrustedProxy es false");

      filter.setBehindTrustedProxy(true);
      String ipProxy = filter.extractClientIp(req);
      assertEquals("203.0.113.195", ipProxy, "Debe extraer la IP de X-Forwarded-For cuando behindTrustedProxy es true");
    }

    @Test
    @DisplayName("Rate limiting protege endpoints de pagos y tesorería con umbral de 30 req/min")
    void rateLimitingProtegeEndpointsDePagosYTesoreria() throws Exception {
      var rateLimitingFilter = new RateLimitingFilter(false);
      var filterChain = mock(FilterChain.class);
      String testIp = "10.10.10.5";

      for (int i = 1; i <= 30; i++) {
        var req = mock(HttpServletRequest.class);
        var res = mock(HttpServletResponse.class);
        when(req.getRequestURI()).thenReturn("/api/v1/pagos/transacciones");
        when(req.getRemoteAddr()).thenReturn(testIp);

        rateLimitingFilter.doFilter(req, res, filterChain);
      }
      verify(filterChain, times(30)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));

      var reqExcedente = mock(HttpServletRequest.class);
      var resExcedente = mock(HttpServletResponse.class);
      when(reqExcedente.getRequestURI()).thenReturn("/api/v1/pagos/transacciones");
      when(reqExcedente.getRemoteAddr()).thenReturn(testIp);

      var stringWriter = new StringWriter();
      var printWriter = new PrintWriter(stringWriter);
      when(resExcedente.getWriter()).thenReturn(printWriter);

      rateLimitingFilter.doFilter(reqExcedente, resExcedente, filterChain);

      verify(resExcedente).setStatus(429);
      verify(filterChain, times(30)).doFilter(any(HttpServletRequest.class), any(HttpServletResponse.class));
    }
  }
}
