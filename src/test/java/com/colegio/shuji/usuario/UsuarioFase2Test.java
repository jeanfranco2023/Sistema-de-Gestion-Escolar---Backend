package com.colegio.shuji.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.colegio.shuji.shared.application.port.out.AuditContextPort;
import com.colegio.shuji.usuario.application.dto.in.CambiarPasswordInicialRequestDto;
import com.colegio.shuji.usuario.application.dto.in.CorregirDniRequestDto;
import com.colegio.shuji.usuario.application.dto.in.LoginRequestDto;
import com.colegio.shuji.usuario.application.dto.in.RegisterUserRequestDto;
import com.colegio.shuji.usuario.application.dto.out.AuthResponseDto;
import com.colegio.shuji.usuario.application.dto.out.ExpedienteEmpleadoDocumentoResponseDto;
import com.colegio.shuji.usuario.application.dto.out.UserResponseDto;
import com.colegio.shuji.usuario.application.mapper.UserMapper;
import com.colegio.shuji.usuario.application.port.in.RegisterUserUseCase;
import com.colegio.shuji.usuario.application.port.out.ExpedienteEmpleadoRepositoryPort;
import com.colegio.shuji.usuario.application.port.out.ExpedienteEmpleadoStoragePort;
import com.colegio.shuji.usuario.application.port.out.PasswordHashPort;
import com.colegio.shuji.usuario.application.port.out.RoleRepositoryPort;
import com.colegio.shuji.usuario.application.port.out.SessionRepositoryPort;
import com.colegio.shuji.usuario.application.port.out.TokenPort;
import com.colegio.shuji.usuario.application.port.out.UserRepositoryPort;
import com.colegio.shuji.usuario.application.port.out.ValidadorDocumentoEmpleadoPort;
import com.colegio.shuji.usuario.application.service.AuthService;
import com.colegio.shuji.usuario.application.service.ExpedienteEmpleadoService;
import com.colegio.shuji.usuario.application.service.UsuarioService;
import com.colegio.shuji.usuario.domain.enums.EstadoValidacionIaEmpleado;
import com.colegio.shuji.usuario.domain.enums.TipoDocumentoEmpleado;
import com.colegio.shuji.usuario.domain.exception.ReglaNegocioException;
import com.colegio.shuji.shared.domain.exception.ResourceNotFoundException;
import com.colegio.shuji.usuario.domain.model.Rol;
import com.colegio.shuji.usuario.domain.model.ExpedienteEmpleadoDocumento;
import com.colegio.shuji.usuario.domain.model.ResultadoValidacionDocumentoEmpleado;
import com.colegio.shuji.usuario.domain.model.Usuario;
import com.colegio.shuji.usuario.infrastructure.adapter.PdfBoxGeminiValidadorDocumentoEmpleadoAdapter;
import com.colegio.shuji.usuario.infrastructure.controller.rest.UsuarioController;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

class UsuarioFase2Test {

  private UserRepositoryPort userRepositoryPort;
  private RoleRepositoryPort roleRepositoryPort;
  private SessionRepositoryPort sessionRepositoryPort;
  private PasswordHashPort passwordEncoder;
  private TokenPort jwtTokenProvider;
  private UserMapper userMapper;
  private AuditContextPort auditoria;
  private RegisterUserUseCase registerUserUseCase;

  private AuthService authService;
  private UsuarioService usuarioService;

  private ExpedienteEmpleadoRepositoryPort expedienteRepo;
  private ExpedienteEmpleadoStoragePort expedienteStorage;
  private ValidadorDocumentoEmpleadoPort validadorDocumento;
  private ExpedienteEmpleadoService expedienteEmpleadoService;

  @BeforeEach
  void setUp() {
    userRepositoryPort = mock(UserRepositoryPort.class);
    roleRepositoryPort = mock(RoleRepositoryPort.class);
    sessionRepositoryPort = mock(SessionRepositoryPort.class);
    passwordEncoder = mock(PasswordHashPort.class);
    jwtTokenProvider = mock(TokenPort.class);
    userMapper = mock(UserMapper.class);
    auditoria = mock(AuditContextPort.class);
    registerUserUseCase = mock(RegisterUserUseCase.class);

    authService =
        new AuthService(
            userRepositoryPort,
            roleRepositoryPort,
            sessionRepositoryPort,
            passwordEncoder,
            jwtTokenProvider,
            userMapper,
            auditoria);

    usuarioService =
        new UsuarioService(
            userRepositoryPort,
            passwordEncoder,
            userMapper,
            auditoria,
            registerUserUseCase);

    expedienteRepo = mock(ExpedienteEmpleadoRepositoryPort.class);
    expedienteStorage = mock(ExpedienteEmpleadoStoragePort.class);
    validadorDocumento = mock(ValidadorDocumentoEmpleadoPort.class);
    when(validadorDocumento.validar(any(TipoDocumentoEmpleado.class), any(byte[].class), any(Usuario.class)))
        .thenReturn(new ResultadoValidacionDocumentoEmpleado(
            EstadoValidacionIaEmpleado.OBSERVADO_IA,
            "La validación IA no está configurada; requiere revisión manual.",
            false,
            "No disponible",
            1));
    when(expedienteRepo.guardar(any())).thenAnswer(inv -> {
      ExpedienteEmpleadoDocumento documento = inv.getArgument(0);
      return new ExpedienteEmpleadoDocumento(
          101L,
          documento.usuarioId(),
          documento.tipoDocumento(),
          documento.nombreArchivo(),
          documento.rutaObjeto(),
          documento.tipoContenido(),
          documento.tamanoBytes(),
          documento.estadoIa(),
          documento.observacionesIa(),
          documento.metadatosIa(),
          documento.fechaCreacion(),
          documento.fechaValidacionIa());
    });
    expedienteEmpleadoService =
        new ExpedienteEmpleadoService(
            expedienteRepo,
            expedienteStorage,
            validadorDocumento,
            userRepositoryPort,
            auditoria,
            new ObjectMapper());
  }

  // --- REGLA 1: Bloqueo del rol APODERADO en creación de usuarios ---

  @Test
  @DisplayName("register() rechaza rol APODERADO con ReglaNegocioException")
  void registerRechazaRolApoderado() {
    RegisterUserRequestDto request =
        RegisterUserRequestDto.builder()
            .username("apoderado_juan")
            .email("juan@test.com")
            .password("Pass1234!")
            .roles(Set.of("APODERADO"))
            .build();

    ReglaNegocioException ex =
        assertThrows(ReglaNegocioException.class, () -> authService.register(request));
    assertTrue(ex.getMessage().contains("Los usuarios con rol APODERADO solo se originan automáticamente"));
  }

  @Test
  @DisplayName("registrarPorDireccion() rechaza rol APODERADO con ReglaNegocioException")
  void registrarPorDireccionRechazaRolApoderado() {
    RegisterUserRequestDto request =
        RegisterUserRequestDto.builder()
            .username("apoderado_maria")
            .email("maria@test.com")
            .roles(Set.of("ROL_APODERADO"))
            .build();

    ReglaNegocioException ex =
        assertThrows(ReglaNegocioException.class, () -> authService.registrarPorDireccion(request));
    assertTrue(ex.getMessage().contains("Los usuarios con rol APODERADO solo se originan automáticamente"));

    RegisterUserRequestDto requestTrim =
        RegisterUserRequestDto.builder()
            .username("apoderado_luis")
            .email("luis@test.com")
            .roles(Set.of("  APODERADO  "))
            .build();

    ReglaNegocioException exTrim =
        assertThrows(ReglaNegocioException.class, () -> authService.registrarPorDireccion(requestTrim));
    assertTrue(exTrim.getMessage().contains("Los usuarios con rol APODERADO solo se originan automáticamente"));
  }

  // --- REGLA 2: Contraseña temporal y primer login ---

  @Test
  @DisplayName("registrarPorDireccion() genera password temporal y marca debeCambiarPassword=true")
  void registrarPorDireccionGeneraPasswordTemporal() {
    RegisterUserRequestDto request =
        RegisterUserRequestDto.builder()
            .username("docente_carlos")
            .email("carlos@test.com")
            .roles(Set.of("DOCENTE"))
            .build();

    when(userRepositoryPort.existsByUsername("docente_carlos")).thenReturn(false);
    when(userRepositoryPort.existsByEmail("carlos@test.com")).thenReturn(false);
    when(roleRepositoryPort.findByCodigoIn(any())).thenReturn(List.of(Rol.builder().id((short) 2).codigo("DOCENTE").build()));
    when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashedTemporalPassword");
    when(userMapper.toDomain(any())).thenReturn(Usuario.builder().username("docente_carlos").email("carlos@test.com").build());
    when(userRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(userMapper.toResponseDto(any())).thenReturn(UserResponseDto.builder().username("docente_carlos").debeCambiarPassword(true).build());

    UserResponseDto response = authService.registrarPorDireccion(request);

    assertNotNull(response);
    assertTrue(Boolean.TRUE.equals(response.getDebeCambiarPassword()));
    verify(passwordEncoder).encode(anyString());
  }

  @Test
  @DisplayName("login() incluye debeCambiarPassword desde el usuario")
  void loginIncluyeDebeCambiarPassword() {
    LoginRequestDto request = new LoginRequestDto("docente_carlos", "TempPass123!");
    Usuario usuario =
        Usuario.builder()
            .id(10L)
            .username("docente_carlos")
            .usuarioPassword("$2a$10$hashed")
            .activo(true)
            .debeCambiarPassword(true)
            .roles(new HashSet<>(List.of(Rol.builder().id((short) 2).codigo("DOCENTE").build())))
            .build();

    when(userRepositoryPort.findByUsernameOrEmail("docente_carlos")).thenReturn(Optional.of(usuario));
    when(passwordEncoder.matches("TempPass123!", "$2a$10$hashed")).thenReturn(true);
    when(jwtTokenProvider.generateToken(any(), any(), any())).thenReturn("mock.jwt.token");
    when(jwtTokenProvider.generateRefreshToken(any(), any())).thenReturn("mock.refresh.token");
    when(jwtTokenProvider.getJwtRefreshExpirationMs()).thenReturn(86400000L);
    when(jwtTokenProvider.getJwtExpirationMs()).thenReturn(3600000L);
    when(userMapper.toResponseDto(usuario)).thenReturn(UserResponseDto.builder().username("docente_carlos").build());

    AuthResponseDto authResponse = authService.login(request, "127.0.0.1", "JUnit");

    assertNotNull(authResponse);
    assertTrue(authResponse.isDebeCambiarPassword());
  }

  @Test
  @DisplayName("cambiarPasswordInicial() valida contraseña actual y quita flag debeCambiarPassword")
  void cambiarPasswordInicialExitoso() {
    Usuario usuario =
        Usuario.builder()
            .id(10L)
            .username("docente_carlos")
            .usuarioPassword("$2a$10$hashedOldPassword")
            .activo(true)
            .debeCambiarPassword(true)
            .build();

    when(userRepositoryPort.findById(10L)).thenReturn(Optional.of(usuario));
    when(passwordEncoder.matches("Shuji2026!temp", "$2a$10$hashedOldPassword")).thenReturn(true);
    when(passwordEncoder.matches("NuevaPass2026!", "$2a$10$hashedOldPassword")).thenReturn(false);
    when(passwordEncoder.encode("NuevaPass2026!")).thenReturn("$2a$10$hashedNewPassword");

    CambiarPasswordInicialRequestDto request =
        new CambiarPasswordInicialRequestDto("Shuji2026!temp", "NuevaPass2026!");

    authService.cambiarPasswordInicial(10L, request);

    assertFalse(usuario.getDebeCambiarPassword());
    assertEquals("$2a$10$hashedNewPassword", usuario.getUsuarioPassword());
    verify(userRepositoryPort).save(usuario);
  }

  // --- REGLA 5: Borrado lógico y Corregir DNI transaccional ---

  @Test
  @DisplayName("cambiarEstado() aplica borrado lógico desactivando la cuenta")
  void cambiarEstadoAplicaBorradoLogico() {
    Usuario usuario =
        Usuario.builder()
            .id(5L)
            .username("usuario_test")
            .activo(true)
            .build();

    when(userRepositoryPort.findByIdWithRoles(5L)).thenReturn(Optional.of(usuario));
    when(userRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(userMapper.toResponseDto(any())).thenReturn(UserResponseDto.builder().activo(false).build());

    UserResponseDto resultado = usuarioService.cambiarEstado(5L, false);

    assertNotNull(resultado);
    assertFalse(resultado.getActivo());
    assertFalse(usuario.estaActivo());
  }

  @Test
  @DisplayName("corregirDni() actualiza DNI y registra auditoría")
  void corregirDniExitoso() {
    Usuario usuario =
        Usuario.builder()
            .id(5L)
            .username("usuario_test")
            .dni("12345678")
            .activo(true)
            .build();

    when(userRepositoryPort.findByIdWithRoles(5L)).thenReturn(Optional.of(usuario));
    when(userRepositoryPort.existsByDni("87654321")).thenReturn(false);
    when(userRepositoryPort.save(any())).thenAnswer(inv -> inv.getArgument(0));
    when(userMapper.toResponseDto(any())).thenReturn(UserResponseDto.builder().dni("87654321").build());

    CorregirDniRequestDto request =
        new CorregirDniRequestDto("87654321", "Error de digitación en contrato inicial");

    UserResponseDto response = usuarioService.corregirDni(5L, request);

    assertNotNull(response);
    assertEquals("87654321", response.getDni());
    assertEquals("87654321", usuario.getDni());
    verify(auditoria).syncCurrentUserFromSecurityContext();
  }

  @Test
  @DisplayName("corregirDni() rechaza DNI duplicado con ReglaNegocioException")
  void corregirDniRechazaDuplicado() {
    Usuario usuario =
        Usuario.builder()
            .id(5L)
            .username("usuario_test")
            .dni("12345678")
            .build();

    when(userRepositoryPort.findByIdWithRoles(5L)).thenReturn(Optional.of(usuario));
    when(userRepositoryPort.existsByDni("87654321")).thenReturn(true);

    CorregirDniRequestDto request =
        new CorregirDniRequestDto("87654321", "Corrección");

    assertThrows(ReglaNegocioException.class, () -> usuarioService.corregirDni(5L, request));
  }

  // --- REGLA 3: Expediente de Empleado y Validación IA ---

  @Test
  @DisplayName("subirYValidarDocumento() conserva el archivo y no aprueba solo por encontrar el DNI")
  void subirYValidarDocumentoConDniRequiereRevisionManualSinIa() throws IOException {
    Usuario usuario =
        Usuario.builder()
            .id(7L)
            .username("profesor_luis")
            .dni("44556677")
            .build();

    when(userRepositoryPort.findById(7L)).thenReturn(Optional.of(usuario));

    byte[] pdfBytes = generarPdf("CURRICULUM VITAE\nEmpleado: profesor_luis\nDNI: 44556677\nTitulo: Licenciado en Educacion");
    MockMultipartFile archivo = new MockMultipartFile("archivo", "cv.pdf", "application/pdf", pdfBytes);

    ExpedienteEmpleadoDocumentoResponseDto respuesta =
        subirDocumento(7L, TipoDocumentoEmpleado.CV, archivo);

    assertNotNull(respuesta);
    assertEquals(EstadoValidacionIaEmpleado.OBSERVADO_IA, respuesta.estadoIa());
    assertTrue(respuesta.observacionesIa().contains("revisión manual"));
    verify(expedienteStorage).guardar(anyString(), any(byte[].class));
  }

  @Test
  @DisplayName("subirYValidarDocumento() observa PDF que no contiene el DNI esperado")
  void subirYValidarDocumentoSinDniObservaIA() throws IOException {
    Usuario usuario =
        Usuario.builder()
            .id(7L)
            .username("profesor_luis")
            .dni("44556677")
            .build();

    when(userRepositoryPort.findById(7L)).thenReturn(Optional.of(usuario));

    byte[] pdfBytes = generarPdf("DOCUMENTO AJENO\nDNI: 99999999\nNombre: Juan Perez");
    MockMultipartFile archivo = new MockMultipartFile("archivo", "titulo.pdf", "application/pdf", pdfBytes);

    ExpedienteEmpleadoDocumentoResponseDto respuesta =
        subirDocumento(7L, TipoDocumentoEmpleado.TITULO_PROFESIONAL, archivo);

    assertNotNull(respuesta);
    assertEquals(EstadoValidacionIaEmpleado.OBSERVADO_IA, respuesta.estadoIa());
    assertTrue(respuesta.observacionesIa().contains("revisión manual"));
  }

  @Test
  @DisplayName("El validador sin Gemini no acepta un PDF por texto coincidente")
  void validadorSinGeminiNoApruebaDniEncontrado() throws IOException {
    Usuario usuario = Usuario.builder().id(7L).username("profesor_luis").dni("44556677").build();
    var validador = new PdfBoxGeminiValidadorDocumentoEmpleadoAdapter(new ObjectMapper(), "", "", "test-model");

    var resultado = validador.validar(
        TipoDocumentoEmpleado.CV,
        generarPdf("Curriculum vitae DNI 44556677"),
        usuario);

    assertEquals(EstadoValidacionIaEmpleado.OBSERVADO_IA, resultado.estado());
    assertEquals("No disponible", resultado.motorValidacion());
  }

  @Test
  @DisplayName("Si falla la persistencia del expediente, se elimina el PDF guardado")
  void persistenciaFallidaLimpiaArchivo() throws IOException {
    Usuario usuario = Usuario.builder().id(7L).username("empleado_test").dni("44556677").build();
    when(userRepositoryPort.findById(7L)).thenReturn(Optional.of(usuario));
    doThrow(new IllegalStateException("persistencia simulada"))
        .when(expedienteRepo).guardar(any());

    MockMultipartFile archivo = new MockMultipartFile(
        "archivo", "cv.pdf", "application/pdf", generarPdf("Documento de prueba"));

    assertThrows(IllegalStateException.class,
        () -> subirDocumento(7L, TipoDocumentoEmpleado.CV, archivo));

    verify(expedienteStorage).guardar(anyString(), any(byte[].class));
    verify(expedienteStorage).eliminar(anyString());
  }

  @Test
  @DisplayName("Si el almacenamiento privado falla, no se crea metadata del documento")
  void almacenamientoFallidoNoGuardaMetadata() throws IOException {
    Usuario usuario = Usuario.builder().id(7L).username("empleado_test").dni("44556677").build();
    when(userRepositoryPort.findById(7L)).thenReturn(Optional.of(usuario));
    doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE))
        .when(expedienteStorage).guardar(anyString(), any(byte[].class));
    MockMultipartFile archivo = new MockMultipartFile(
        "archivo", "cv.pdf", "application/pdf", generarPdf("Documento de prueba"));

    assertThrows(ResponseStatusException.class,
        () -> subirDocumento(7L, TipoDocumentoEmpleado.CV, archivo));

    verify(expedienteRepo, never()).guardar(any());
  }

  @Test
  @DisplayName("El rollback de base elimina el archivo privado aun después de guardar metadatos")
  void rollbackEliminaArchivoDespuesDelSave() throws IOException {
    Usuario usuario = Usuario.builder().id(7L).username("empleado_test").dni("44556677").build();
    when(userRepositoryPort.findById(7L)).thenReturn(Optional.of(usuario));
    TransactionSynchronizationManager.initSynchronization();
    try {
      MockMultipartFile archivo = new MockMultipartFile(
          "archivo", "cv.pdf", "application/pdf", generarPdf("Documento de prueba"));
      subirDocumento(7L, TipoDocumentoEmpleado.CV, archivo);

      TransactionSynchronizationManager.getSynchronizations().forEach(
          synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

      verify(expedienteStorage).guardar(anyString(), any(byte[].class));
      verify(expedienteStorage).eliminar(anyString());
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  @DisplayName("Solo se emite el enlace firmado para un documento del empleado solicitado")
  void descargaBuscaDocumentoDentroDelExpedienteDelEmpleado() {
    var documento = new ExpedienteEmpleadoDocumento(
        101L, 7L, TipoDocumentoEmpleado.CV, "cv.pdf", "expedientes/7/cv/id.pdf", "application/pdf",
        500L, EstadoValidacionIaEmpleado.OBSERVADO_IA, "Revisión manual", "{}", null, null);
    when(expedienteRepo.buscarPorIdYUsuario(101L, 7L)).thenReturn(Optional.of(documento));
    when(expedienteStorage.urlFirmada(documento.rutaObjeto())).thenReturn("https://local.test/signed");

    assertEquals("https://local.test/signed", expedienteEmpleadoService.urlDescargaDocumento(7L, 101L));
    verify(expedienteStorage).urlFirmada(documento.rutaObjeto());
  }

  @Test
  @DisplayName("Un documento de otro empleado no obtiene URL firmada")
  void descargaRechazaDocumentoDeOtroEmpleado() {
    when(expedienteRepo.buscarPorIdYUsuario(101L, 7L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class,
        () -> expedienteEmpleadoService.urlDescargaDocumento(7L, 101L));
    verify(expedienteStorage, never()).urlFirmada(anyString());
  }

  @Test
  @DisplayName("El endpoint de descarga firmada requiere el rol Dirección")
  void descargaFirmadaRestringidaADireccion() throws NoSuchMethodException {
    var method = UsuarioController.class.getMethod(
        "obtenerEnlaceDescargaDocumento", Long.class, Long.class);
    var authorization = method.getAnnotation(PreAuthorize.class);

    assertNotNull(authorization);
    assertEquals("hasRole('DIRECCION')", authorization.value());
  }

  @Test
  @DisplayName("subirYValidarDocumento() rechaza archivos que no sean PDF estrictos")
  void subirYValidarDocumentoRechazaFormatoNoPdf() throws IOException {
    MockMultipartFile archivoTxt =
        new MockMultipartFile("archivo", "documento.txt", "text/plain", "texto plano".getBytes());

    assertThrows(
        ReglaNegocioException.class,
        () -> subirDocumento(7L, TipoDocumentoEmpleado.CV, archivoTxt));

    MockMultipartFile archivoExtensionFalsa =
        new MockMultipartFile("archivo", "malicioso.exe", "application/pdf", "binario".getBytes());

    assertThrows(
        ReglaNegocioException.class,
        () -> subirDocumento(7L, TipoDocumentoEmpleado.CV, archivoExtensionFalsa));
  }

  private byte[] generarPdf(String texto) throws IOException {
    try (PDDocument doc = new PDDocument()) {
      PDPage page = new PDPage();
      doc.addPage(page);
      try (PDPageContentStream stream = new PDPageContentStream(doc, page)) {
        stream.beginText();
        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
        stream.newLineAtOffset(50, 700);
        for (String linea : texto.split("\n")) {
          stream.showText(linelineSafe(linea));
          stream.newLineAtOffset(0, -15);
        }
        stream.endText();
      }
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      doc.save(baos);
      return baos.toByteArray();
    }
  }

  private ExpedienteEmpleadoDocumentoResponseDto subirDocumento(
      Long usuarioId, TipoDocumentoEmpleado tipoDocumento, MockMultipartFile archivo) throws IOException {
    return expedienteEmpleadoService.subirYValidarDocumento(
        usuarioId,
        tipoDocumento,
        archivo.getOriginalFilename(),
        archivo.getContentType(),
        archivo.getBytes());
  }

  private String linelineSafe(String input) {
    return input.replaceAll("[^\\x20-\\x7E]", "");
  }
}
