package com.colegio.shuji.fase3y4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.colegio.shuji.academico.infrastructure.entity.AnioLectivoEntity;
import com.colegio.shuji.academico.infrastructure.entity.GradoEntity;
import com.colegio.shuji.academico.domain.enums.NivelCodigo;
import com.colegio.shuji.academico.infrastructure.entity.NivelEntity;
import com.colegio.shuji.academico.infrastructure.entity.SeccionEntity;
import com.colegio.shuji.academico.infrastructure.repository.JpaAnioLectivoRepository;
import com.colegio.shuji.academico.infrastructure.repository.JpaSeccionRepository;
import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import com.colegio.shuji.config.persistence.SupabaseAuditInterceptor;
import com.colegio.shuji.matricula.application.dto.in.CambiarEstadoApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.in.CambiarEstadoEstudianteRequestDto;
import com.colegio.shuji.matricula.application.dto.in.CrearSolicitudMatriculaPublicaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.ReenviarTokenSolicitudRequestDto;
import com.colegio.shuji.matricula.application.dto.out.ApoderadoResponseDto;
import com.colegio.shuji.matricula.application.dto.out.EstudianteResponseDto;
import com.colegio.shuji.matricula.application.dto.out.ReenviarTokenSolicitudResponseDto;
import com.colegio.shuji.matricula.application.dto.out.SolicitudMatriculaPublicaResponseDto;
import com.colegio.shuji.matricula.application.mapper.MatriculaMapper;
import com.colegio.shuji.matricula.application.port.out.ApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.FirmaDigitalDocumentoPort;
import com.colegio.shuji.matricula.application.port.out.GeminiDocumentoMatriculaPort;
import com.colegio.shuji.matricula.application.port.out.ReniecServicePort;
import com.colegio.shuji.matricula.application.port.out.SeguimientoSolicitudCorreoPort;
import com.colegio.shuji.matricula.application.service.EstudianteService;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.enums.EstadoSolicitudMatriculaPublica;
import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.Parentesco;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import com.colegio.shuji.matricula.domain.model.Apoderado;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import com.colegio.shuji.matricula.infrastructure.entity.DocumentoMatriculaVersionEntity;
import com.colegio.shuji.matricula.infrastructure.entity.EstudianteEntity;
import com.colegio.shuji.matricula.infrastructure.entity.MatriculaEntity;
import com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaPublicaEntity;
import com.colegio.shuji.matricula.infrastructure.repository.JpaApoderadoRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaDocumentoMatriculaVersionRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaEstudianteApoderadoRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaEstudianteRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaMatriculaRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaSolicitudMatriculaDocumentoRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaSolicitudMatriculaPublicaRepository;
import com.colegio.shuji.matricula.infrastructure.service.SolicitudMatriculaPublicaService;
import com.colegio.shuji.shared.application.port.out.AuditContextPort;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.application.port.in.RegistrarCobroMatriculaUseCase;
import com.colegio.shuji.tesoreria.application.port.out.MercadoPagoPort;
import com.colegio.shuji.usuario.application.dto.in.CorregirDniRequestDto;
import com.colegio.shuji.usuario.application.dto.in.RegisterUserRequestDto;
import com.colegio.shuji.usuario.application.dto.out.UserResponseDto;
import com.colegio.shuji.usuario.application.mapper.UserMapper;
import com.colegio.shuji.usuario.application.port.in.RegisterUserUseCase;
import com.colegio.shuji.usuario.application.port.out.PasswordHashPort;
import com.colegio.shuji.usuario.application.port.out.UserRepositoryPort;
import com.colegio.shuji.usuario.application.service.UsuarioService;
import com.colegio.shuji.usuario.domain.exception.ReglaNegocioException;
import com.colegio.shuji.usuario.domain.model.Rol;
import com.colegio.shuji.usuario.domain.model.Usuario;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

class Fase3Y4Test {

  // Solicitud Mocks
  private JpaSolicitudMatriculaPublicaRepository solicitudesRepo;
  private JpaSolicitudMatriculaDocumentoRepository documentosRepo;
  private JpaDocumentoMatriculaVersionRepository versionesRepo;
  private JpaAnioLectivoRepository aniosRepo;
  private JpaSeccionRepository seccionesRepo;
  private JpaEstudianteRepository estudiantesRepo;
  private JpaApoderadoRepository apoderadosRepo;
  private JpaEstudianteApoderadoRepository vinculosRepo;
  private JpaMatriculaRepository matriculasRepo;
  private GeminiDocumentoMatriculaPort geminiPort;
  private MercadoPagoPort mercadoPagoPort;
  private RegistrarCobroMatriculaUseCase cobrosUseCase;
  private SolicitudMatriculaPublicaService solicitudService;

  // Estudiante & Apoderado Mocks
  private MatriculaMapper matriculaMapper;
  private EstudianteRepositoryPort estudianteRepoPort;
  private ApoderadoRepositoryPort apoderadoRepoPort;
  private EstudianteApoderadoRepositoryPort vinculoRepoPort;
  private ReniecServicePort reniecPort;
  private AuditContextPort auditoriaContextPort;
  private EstudianteService estudianteService;

  // Usuario Mocks
  private UserRepositoryPort userRepoPort;
  private PasswordHashPort passwordEncoder;
  private UserMapper userMapper;
  private RegisterUserUseCase registerUserUseCase;
  private UsuarioService usuarioService;

  @BeforeEach
  void setUp() {
    // 1. Solicitud setup
    solicitudesRepo = mock(JpaSolicitudMatriculaPublicaRepository.class);
    documentosRepo = mock(JpaSolicitudMatriculaDocumentoRepository.class);
    versionesRepo = mock(JpaDocumentoMatriculaVersionRepository.class);
    aniosRepo = mock(JpaAnioLectivoRepository.class);
    seccionesRepo = mock(JpaSeccionRepository.class);
    estudiantesRepo = mock(JpaEstudianteRepository.class);
    apoderadosRepo = mock(JpaApoderadoRepository.class);
    vinculosRepo = mock(JpaEstudianteApoderadoRepository.class);
    matriculasRepo = mock(JpaMatriculaRepository.class);
    geminiPort = mock(GeminiDocumentoMatriculaPort.class);
    mercadoPagoPort = mock(MercadoPagoPort.class);
    cobrosUseCase = mock(RegistrarCobroMatriculaUseCase.class);

    solicitudService = new SolicitudMatriculaPublicaService(
        solicitudesRepo, documentosRepo, versionesRepo, aniosRepo, seccionesRepo,
        estudiantesRepo, apoderadosRepo, vinculosRepo, matriculasRepo, geminiPort,
        mercadoPagoPort, cobrosUseCase);

    // 2. Estudiante setup
    matriculaMapper = mock(MatriculaMapper.class);
    estudianteRepoPort = mock(EstudianteRepositoryPort.class);
    apoderadoRepoPort = mock(ApoderadoRepositoryPort.class);
    vinculoRepoPort = mock(EstudianteApoderadoRepositoryPort.class);
    reniecPort = mock(ReniecServicePort.class);
    auditoriaContextPort = mock(AuditContextPort.class);

    estudianteService = new EstudianteService(
        matriculaMapper, estudianteRepoPort, apoderadoRepoPort, vinculoRepoPort,
        reniecPort, auditoriaContextPort);

    // 3. Usuario setup
    userRepoPort = mock(UserRepositoryPort.class);
    passwordEncoder = mock(PasswordHashPort.class);
    userMapper = mock(UserMapper.class);
    registerUserUseCase = mock(RegisterUserUseCase.class);

    usuarioService = new UsuarioService(
        userRepoPort, passwordEncoder, userMapper, auditoriaContextPort, registerUserUseCase);

  }

  // =========================================================================
  // FASE 3: ADMISIÓN RESILIENTE Y UNICIDAD DE DNI
  // =========================================================================

  @Test
  @DisplayName("Fase 3: Rechazar 409 Conflict si el estudiante ya cuenta con matrícula activa para el año lectivo")
  void crearSolicitudRechazaSiEstudianteTieneMatriculaActiva() {
    var anio = new AnioLectivoEntity();
    anio.setId((short) 2027);
    anio.setAnio((short) 2027);
    anio.setAbierto(true);
    when(aniosRepo.findById((short) 2027)).thenReturn(Optional.of(anio));

    var nivel = new NivelEntity();
    nivel.setCodigo(NivelCodigo.PRIMARIA);
    nivel.setNombre("Primaria");

    var grado = new GradoEntity();
    grado.setId((short) 1);
    grado.setNumeroGrado((short) 1);
    grado.setNombre("Primer Grado");
    grado.setRelacion0(nivel);

    var seccion = new SeccionEntity();
    seccion.setId(10);
    seccion.setGradoId((short) 1);
    seccion.setCupoMaximo((short) 30);
    seccion.setVacantesOcupadas((short) 10);
    seccion.setRelacion1(grado);
    when(seccionesRepo.buscarPorAnioLectivoId((short) 2027)).thenReturn(List.of(seccion));

    var estudiante = new EstudianteEntity();
    estudiante.setId(100L);
    estudiante.setNumeroDocumento("71234567");
    when(estudiantesRepo.buscarPorNumeroDocumento("71234567")).thenReturn(List.of(estudiante));
    when(matriculasRepo.existsByEstudianteIdAndAnioLectivoId(100L, (short) 2027)).thenReturn(true);

    var req = new CrearSolicitudMatriculaPublicaRequestDto(
        (short) 2027, (short) 1, "71234567", "Juan", "Perez", "Gomez",
        LocalDate.of(2020, 5, 10), Genero.M, "40123456", "Pedro", "Perez", "Soto",
        "987654321", "pedro@gmail.com", "Av Principal 123", null, Parentesco.PADRE, true);

    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> solicitudService.crear(req));
    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    assertTrue(ex.getReason().contains("vacante confirmada"));
  }

  @Test
  @DisplayName("Fase 3: Rechazar 409 Conflict si ya existe una solicitud en trámite para el DNI")
  void crearSolicitudRechazaSiYaExisteSolicitudActiva() {
    var anio = new AnioLectivoEntity();
    anio.setId((short) 2027);
    anio.setAnio((short) 2027);
    anio.setAbierto(true);
    when(aniosRepo.findById((short) 2027)).thenReturn(Optional.of(anio));

    var nivel = new NivelEntity();
    nivel.setCodigo(NivelCodigo.PRIMARIA);
    nivel.setNombre("Primaria");

    var grado = new GradoEntity();
    grado.setId((short) 1);
    grado.setNumeroGrado((short) 1);
    grado.setNombre("Primer Grado");
    grado.setRelacion0(nivel);

    var seccion = new SeccionEntity();
    seccion.setId(10);
    seccion.setGradoId((short) 1);
    seccion.setCupoMaximo((short) 30);
    seccion.setVacantesOcupadas((short) 10);
    seccion.setRelacion1(grado);
    when(seccionesRepo.buscarPorAnioLectivoId((short) 2027)).thenReturn(List.of(seccion));

    when(estudiantesRepo.buscarPorNumeroDocumento("71234567")).thenReturn(List.of());
    when(solicitudesRepo.existsByNumeroDocumentoEstudianteAndEstadoIn(eq("71234567"), any()))
        .thenReturn(true);

    var req = new CrearSolicitudMatriculaPublicaRequestDto(
        (short) 2027, (short) 1, "71234567", "Juan", "Perez", "Gomez",
        LocalDate.of(2020, 5, 10), Genero.M, "40123456", "Pedro", "Perez", "Soto",
        "987654321", "pedro@gmail.com", "Av Principal 123", null, Parentesco.PADRE, true);

    ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> solicitudService.crear(req));
    assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
    assertTrue(ex.getReason().contains("para este DNI"));
  }

  @Test
  @DisplayName("Fase 3: Reenviar token sólo tras enviar correo y sin enumerar solicitudes")
  void reenviarTokenEnviaAntesDeRotarYRespuestaNoEnumera() {
    var solicitud = new SolicitudMatriculaPublicaEntity();
    solicitud.setId(UUID.randomUUID());
    solicitud.setNumeroDocumentoEstudiante("71234567");
    solicitud.setAnioLectivoId((short) 2027);
    solicitud.setEmailApoderado("pedro.alvarez@gmail.com");
    solicitud.setTokenHash("oldhash123");

    when(solicitudesRepo.buscarActivasPorDniYAnio(eq("71234567"), eq((short) 2027), any()))
        .thenReturn(List.of(solicitud));
    when(solicitudesRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
    var correo = mock(SeguimientoSolicitudCorreoPort.class);
    ReflectionTestUtils.setField(solicitudService, "correoSeguimiento", correo);

    var req = new ReenviarTokenSolicitudRequestDto("71234567", (short) 2027);
    ReenviarTokenSolicitudResponseDto resp = solicitudService.reenviarToken(req);

    assertNotNull(resp);
    assertEquals("Si existe una solicitud activa con esos datos, enviaremos un enlace al correo registrado.", resp.mensaje());
    assertNotEquals("oldhash123", solicitud.getTokenHash());
    verify(correo).enviarEnlaceSeguimiento(eq("pedro.alvarez@gmail.com"), eq(solicitud.getId()), anyString());
  }

  @Test
  @DisplayName("Fase 3: Sin transporte de correo responde 503 antes de consultar o rotar tokens")
  void reenviarTokenSinTransporteNoBuscaSolicitud() {
    var ex = assertThrows(ResponseStatusException.class,
        () -> solicitudService.reenviarToken(new ReenviarTokenSolicitudRequestDto("71234567", (short) 2027)));

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    verifyNoInteractions(solicitudesRepo);
  }

  @Test
  @DisplayName("Fase 3: Si el envío falla conserva el hash de seguimiento vigente")
  void reenviarTokenSiFallaCorreoConservaTokenVigente() {
    var solicitud = new SolicitudMatriculaPublicaEntity();
    solicitud.setId(UUID.randomUUID());
    solicitud.setNumeroDocumentoEstudiante("71234567");
    solicitud.setAnioLectivoId((short) 2027);
    solicitud.setEmailApoderado("apoderado@example.test");
    solicitud.setTokenHash("oldhash123");
    when(solicitudesRepo.buscarActivasPorDniYAnio(eq("71234567"), eq((short) 2027), any()))
        .thenReturn(List.of(solicitud));
    var correo = mock(SeguimientoSolicitudCorreoPort.class);
    org.mockito.Mockito.doThrow(new IllegalStateException("mail transport unavailable"))
        .when(correo).enviarEnlaceSeguimiento(anyString(), any(), anyString());
    ReflectionTestUtils.setField(solicitudService, "correoSeguimiento", correo);

    var ex = assertThrows(ResponseStatusException.class,
        () -> solicitudService.reenviarToken(new ReenviarTokenSolicitudRequestDto("71234567", (short) 2027)));

    assertEquals(HttpStatus.SERVICE_UNAVAILABLE, ex.getStatusCode());
    assertEquals("oldhash123", solicitud.getTokenHash());
    verify(solicitudesRepo, never()).saveAndFlush(any());
  }

  @Test
  @DisplayName("Fase 3: Consultar por token compara con MessageDigest.isEqual en tiempo constante")
  void consultarPorTokenComparaHashEnTiempoConstante() throws Exception {
    String token = "miTokenSeguroDePrueba123456789";
    var digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
    String hash = java.util.HexFormat.of().formatHex(digest);

    var solicitud = new SolicitudMatriculaPublicaEntity();
    solicitud.setId(UUID.randomUUID());
    solicitud.setTokenHash(hash);
    solicitud.setEstado(EstadoSolicitudMatriculaPublica.DOCUMENTOS_OBSERVADOS);
    solicitud.setNombresEstudiante("Carlos");
    solicitud.setApellidoPaternoEstudiante("Perez");

    when(solicitudesRepo.findByTokenHash(hash)).thenReturn(Optional.of(solicitud));

    SolicitudMatriculaPublicaResponseDto resp = solicitudService.consultarPorToken(token);
    assertNotNull(resp);
    assertEquals(solicitud.getId(), resp.id());
    assertEquals(EstadoSolicitudMatriculaPublica.DOCUMENTOS_OBSERVADOS, resp.estado());
  }

  // =========================================================================
  // FASE 3: CORREGIR DNI Y BORRADO LÓGICO EN ESTUDIANTES Y APODERADOS
  // =========================================================================

  @Test
  @DisplayName("Fase 3: Corregir DNI de estudiante valida unicidad y sincroniza auditoría")
  void corregirDniEstudianteValidaUnicidad() {
    var estudiante = new Estudiante();
    estudiante.setId(5L);
    estudiante.setNumeroDocumento("11112222");

    when(estudianteRepoPort.buscarPorId(5L)).thenReturn(Optional.of(estudiante));
    when(estudianteRepoPort.buscarPorNumeroDocumento("88889999")).thenReturn(List.of());
    when(estudianteRepoPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

    var dto = new EstudianteResponseDto(5L, "ALU-000005", TipoDocumento.DNI, "88889999", "Luis",
        "Gomez", "Rios", LocalDate.of(2015, 1, 1), Genero.M, null, true, null, null, null, true, OffsetDateTime.now());
    when(matriculaMapper.toResponse(any(Estudiante.class))).thenReturn(dto);

    var req = new CorregirDniRequestDto("88889999", "Error de digitación");
    EstudianteResponseDto resp = estudianteService.corregirDniEstudiante(5L, req);

    assertNotNull(resp);
    assertEquals("88889999", resp.numeroDocumento());
    verify(auditoriaContextPort).syncCurrentUserFromSecurityContext();
  }

  @Test
  @DisplayName("Fase 3: Cambiar estado de estudiante realiza borrado lógico formal")
  void cambiarEstadoEstudianteRealizaBorradoLogico() {
    var estudiante = new Estudiante();
    estudiante.setId(5L);
    estudiante.setActivo(true);
    estudiante.setEstado("ACTIVO");

    when(estudianteRepoPort.buscarPorId(5L)).thenReturn(Optional.of(estudiante));
    when(estudianteRepoPort.guardar(any())).thenAnswer(inv -> inv.getArgument(0));

    var dto = new EstudianteResponseDto(5L, "ALU-000005", TipoDocumento.DNI, "11112222", "Luis",
        "Gomez", "Rios", LocalDate.of(2015, 1, 1), Genero.M, null, true, null, null, null, false, "SUSPENDIDO", OffsetDateTime.now());
    when(matriculaMapper.toResponse(any(Estudiante.class))).thenReturn(dto);

    var req = new CambiarEstadoEstudianteRequestDto("SUSPENDIDO", "Sanción disciplinaria temporal");
    EstudianteResponseDto resp = estudianteService.cambiarEstadoEstudiante(5L, req);

    assertNotNull(resp);
    assertEquals("SUSPENDIDO", resp.estado());
    assertFalse(resp.activo());
    verify(auditoriaContextPort).syncCurrentUserFromSecurityContext();
  }

  @Test
  @DisplayName("Fase 3: Corregir DNI de apoderado valida colisión")
  void corregirDniApoderadoRechazaSiColisiona() {
    var apoderado = new Apoderado();
    apoderado.setId(8L);
    apoderado.setNumeroDocumento("44445555");

    var otro = new Apoderado();
    otro.setId(99L);
    otro.setNumeroDocumento("77778888");

    when(apoderadoRepoPort.buscarPorId(8L)).thenReturn(Optional.of(apoderado));
    when(apoderadoRepoPort.buscarPorNumeroDocumento("77778888")).thenReturn(List.of(otro));

    var req = new CorregirDniRequestDto("77778888", "Corrección");
    assertThrows(ReglaNegocioException.class, () -> estudianteService.corregirDniApoderado(8L, req));
  }

  // =========================================================================
  // FASE 3: REVOCACIÓN DE ROLES EN USUARIO
  // =========================================================================

  @Test
  @DisplayName("Fase 3: Revocación de rol bloquea auto-revocación del rol DIRECCIÓN")
  void revocarRolBloqueaAutoRevocacionDireccion() {
    var rolDir = Rol.builder().id((short) 1).codigo("DIRECCION").nombre("DIRECCION").build();
    var rolSec = Rol.builder().id((short) 2).codigo("SECRETARIA").nombre("SECRETARIA").build();

    Set<Rol> roles = new HashSet<>(List.of(rolDir, rolSec));
    var usuario = Usuario.builder().id(7L).username("director_juan").roles(roles).activo(true).build();

    when(userRepoPort.findByIdWithRoles(7L)).thenReturn(Optional.of(usuario));

    // Mock usuario autenticado en SecurityContext con el mismo ID (7L)
    var principalMock = mock(SupabaseAuditInterceptor.UserPrincipalHolder.class);
    when(principalMock.getUserId()).thenReturn(7L);
    var auth = new UsernamePasswordAuthenticationToken(principalMock, "cred", List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);

    try {
      ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> usuarioService.revocarRol(7L, (short) 1));
      assertEquals("AUTOREVOCACION_PROHIBIDA", ex.getErrorCode());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  @Test
  @DisplayName("Fase 3: Revocación de rol bloquea si es el último administrador activo en la institución")
  void revocarRolBloqueaUltimoAdministrador() {
    var rolDir = Rol.builder().id((short) 1).codigo("DIRECCION").nombre("DIRECCION").build();
    var rolSec = Rol.builder().id((short) 2).codigo("SECRETARIA").nombre("SECRETARIA").build();

    Set<Rol> roles = new HashSet<>(List.of(rolDir, rolSec));
    var usuario = Usuario.builder().id(12L).username("admin_dos").roles(roles).activo(true).build();

    when(userRepoPort.findByIdWithRoles(12L)).thenReturn(Optional.of(usuario));

    // Actor autenticado es otro usuario (ID 99)
    var principalMock = mock(SupabaseAuditInterceptor.UserPrincipalHolder.class);
    when(principalMock.getUserId()).thenReturn(99L);
    var auth = new UsernamePasswordAuthenticationToken(principalMock, "cred", List.of());
    SecurityContextHolder.getContext().setAuthentication(auth);

    try {
      // No hay otros administradores activos
      when(userRepoPort.findByRolCodigo("DIRECCION")).thenReturn(List.of(usuario));

      ReglaNegocioException ex = assertThrows(ReglaNegocioException.class, () -> usuarioService.revocarRol(12L, (short) 1));
      assertEquals("ULTIMO_ADMINISTRADOR", ex.getErrorCode());
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

}
