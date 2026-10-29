package com.colegio.shuji;

import static org.junit.jupiter.api.Assertions.*;

import com.colegio.shuji.academico.application.dto.in.*;
import com.colegio.shuji.academico.application.port.in.*;
import com.colegio.shuji.asistencia.application.dto.in.*;
import com.colegio.shuji.asistencia.application.port.in.*;
import com.colegio.shuji.asistencia.domain.enums.*;
import com.colegio.shuji.comunicado.application.dto.in.*;
import com.colegio.shuji.comunicado.application.port.in.*;
import com.colegio.shuji.config.security.UserPrincipal;
import com.colegio.shuji.curriculo.application.dto.in.*;
import com.colegio.shuji.curriculo.application.port.in.*;
import com.colegio.shuji.curriculo.domain.enums.DiaSemana;
import com.colegio.shuji.evaluacion.application.dto.in.*;
import com.colegio.shuji.evaluacion.application.port.in.*;
import com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa;
import com.colegio.shuji.matricula.application.dto.in.*;
import com.colegio.shuji.matricula.application.port.in.*;
import com.colegio.shuji.matricula.domain.enums.*;
import com.colegio.shuji.tesoreria.application.dto.in.*;
import com.colegio.shuji.tesoreria.application.port.in.*;
import com.colegio.shuji.tesoreria.domain.enums.*;
import com.colegio.shuji.usuario.infrastructure.entity.AuditoriaCambiosEntity;
import com.colegio.shuji.usuario.infrastructure.entity.RolEntity;
import com.colegio.shuji.usuario.infrastructure.entity.UsuarioEntity;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;

/** Requiere un esquema PostgreSQL desechable configurado mediante variables de entorno. */
@SpringBootTest(
    properties = {
      "spring.datasource.url=${TEST_DB_URL:jdbc:postgresql://${TEST_DB_HOST:127.0.0.1}:${TEST_DB_PORT:55439}/shuji_audit}",
      "spring.datasource.username=${TEST_DB_USERNAME:postgres}",
      "spring.datasource.password=${TEST_DB_PASSWORD:}",
      "spring.flyway.schemas=${TEST_DB_SCHEMA:public}",
      "spring.flyway.default-schema=${TEST_DB_SCHEMA:public}",
      "spring.flyway.create-schemas=false",
      "spring.flyway.locations=${TEST_DB_FLYWAY_LOCATIONS:classpath:db/migration}",
      "spring.jpa.properties.hibernate.default_schema=${TEST_DB_SCHEMA:public}",
      "spring.jpa.hibernate.ddl-auto=validate",
      "integraciones.reniec.url=",
      "integraciones.pagos.verificador-url=",
      "logging.level.org.hibernate.SQL=WARN",
      "spring.datasource.hikari.maximum-pool-size=10",
      "spring.datasource.hikari.minimum-idle=0"
    })
@Transactional
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class PersistenciaPostgresTest {
  private final EntityManager em;
  private final GestionarAnioLectivoUseCase calendario;
  private final GestionarSeccionesUseCase estructura;
  private final ConsultarVacantesUseCase vacantes;
  private final RegistrarFichaFamiliarUseCase familias;
  private final ProcesarMatriculaUseCase matriculas;
  private final GenerarCronogramaPensionesUseCase cronograma;
  private final ProcesarPagoPasarelaUseCase pagos;
  private final EmitirComprobanteUseCase comprobantes;
  private final RevertirPagoUseCase reversiones;
  private final GestionarCurriculoUseCase curriculo;
  private final AsignarCargaDocenteUseCase carga;
  private final GenerarMallaHorariaUseCase horarios;
  private final RegistrarEvaluacionCnebUseCase evaluacion;
  private final DerivarAlumnosRefuerzoUseCase refuerzo;
  private final ProcesarBiometricoUseCase biometrico;
  private final RegistrarListaAulaUseCase aula;
  private final EjecutarConciliacionDiariaUseCase conciliacion;
  private final PublicarComunicadoUseCase publicacion;
  private final ConfirmarLecturaUseCase lectura;
  private final ConsultarBandejaUseCase bandeja;
  private final com.colegio.shuji.matricula.infrastructure.service.SolicitudMatriculaPublicaService solicitudesPublicas;
  private final com.colegio.shuji.academico.infrastructure.repository.JpaSeccionRepository seccionesJpa;
  private final com.colegio.shuji.matricula.application.port.in.ConsultarPortalFamiliarUseCase portalFamiliar;
  private final com.colegio.shuji.matricula.application.port.in.GestionarSuplenciasUseCase suplencias;
  private final com.colegio.shuji.evaluacion.application.port.in.ImportarCalificacionesUseCase importacionCneb;
  private final com.colegio.shuji.evaluacion.application.port.in.GenerarLibretaPdfUseCase pdfLibreta;
  private final com.colegio.shuji.evaluacion.application.port.in.ConsultarLibretaNotasUseCase libretas;
  private final com.colegio.shuji.curriculo.application.port.in.ConsultarCatalogoHorariosUseCase catalogoHorarios;
  private final com.colegio.shuji.convivencia.application.port.in.RegistrarIncidenciaUseCase registrarIncidencia;

  Long usuarioId, estudianteId, matriculaId;
  Short anioId, nivelId;
  Integer seccionId;
  String dniEstudiante;

  @BeforeEach
  void preparar() {
    var u = new UsuarioEntity();
    u.setUsername("prueba" + System.nanoTime());
    u.setEmail("prueba" + System.nanoTime() + "@example.test");
    u.setUsuarioPassword("hash-de-prueba");
    u.setRoles(
        new HashSet<>(
            em.createQuery("select r from RolEntity r where r.codigo='DOCENTE'", RolEntity.class)
                .getResultList()));
    em.persist(u);
    em.flush();
    usuarioId = u.getId();
    autenticar("DIRECCION");
    var anio =
        calendario.listarAnios().stream()
            .filter(a -> a.anio() == 2026)
            .findFirst()
            .orElseGet(
                () ->
                    calendario.crearAnio(
                        new CrearAnioLectivoRequestDto(
                            (short) 2026, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31))));
    anioId = anio.id();
    nivelId = estructura.listarNiveles().getFirst().id();
    var grado =
        estructura.listarGrados(nivelId).stream()
            .filter(g -> g.numeroGrado() == 1)
            .findFirst()
            .orElseGet(
                () -> estructura.crearGrado(new CrearGradoRequestDto(nivelId, (short) 1, "Primero")));
    var seccion =
        estructura.listarSecciones(anioId, nivelId).stream()
            .filter(s -> s.letra().equals("A"))
            .findFirst()
            .orElseGet(
                () ->
                    estructura.crearSeccion(
                        new CrearSeccionRequestDto(anioId, grado.id(), nivelId, "A", (short) 20, "A101")));
    seccionId = seccion.id();
    dniEstudiante = "1" + String.format("%07d", new Random().nextInt(9999999));
    var estudiante =
        familias.registrarEstudiante(
            new RegistrarEstudianteRequestDto(
                TipoDocumento.DNI,
                dniEstudiante,
                "Ana",
                "Perez",
                "Lopez",
                LocalDate.of(2018, 1, 1),
                Genero.F,
                null,
                null,
                null));
    estudianteId = estudiante.id();
    String dniApoderado = "8" + String.format("%07d", new Random().nextInt(9999999));
    var apoderado =
        familias.registrarApoderado(
            new RegistrarApoderadoRequestDto(
                TipoDocumento.DNI,
                dniApoderado,
                "Luis",
                "Perez",
                "Ruiz",
                "999888777",
                null,
                "Lima",
                "150101",
                usuarioId));
    familias.vincular(
        new VincularApoderadoRequestDto(
            estudianteId, apoderado.id(), Parentesco.PADRE, true, true, true));
    var matricula =
        matriculas.solicitar(
            new SolicitarMatriculaRequestDto(
                anioId, estudianteId, seccionId, OffsetDateTime.now().plusDays(1), null));
    matriculaId = matricula.id();
    var plan =
        cronograma.generarCronograma(
            new GenerarObligacionesAnualesRequestDto(
                matriculaId, (short) 1, (short) 2, LocalDate.of(2026, 3, 1)));
    var obligacionMatricula =
        plan.stream()
            .filter(o -> o.tipoConcepto() == TipoConcepto.MATRICULA && o.numeroCuota() == 0)
            .findFirst()
            .orElseThrow();
    pagos.registrarCaja(
        new RegistrarPagoCajaRequestDto(
            obligacionMatricula.id(),
            "pago-matricula-test-" + UUID.randomUUID(),
            MetodoPago.EFECTIVO,
            obligacionMatricula.saldoPendiente()));
    matriculas.confirmar(new ConfirmarMatriculaRequestDto(matriculaId));
  }

  private void autenticar(String rol) {
    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    var p =
        UserPrincipal.builder()
            .id(usuarioId)
            .username("prueba")
            .active(true)
            .authorities(authorities)
            .build();
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken(p, null, authorities));
  }

  private com.colegio.shuji.matricula.infrastructure.entity.ApoderadoEntity vincularCuentaFamiliar() {
    var apoderado = em.createQuery("select a from ApoderadoEntity a, EstudianteApoderadoEntity v where v.apoderadoId=a.id and v.estudianteId=:id", com.colegio.shuji.matricula.infrastructure.entity.ApoderadoEntity.class)
        .setParameter("id", estudianteId).getSingleResult();
    apoderado.setUsuarioId(usuarioId); em.flush(); return apoderado;
  }

  @Test
  void portalFamiliarConsultaSoloHijosBajoCustodiaYEmitePdfReal() throws Exception {
    vincularCuentaFamiliar(); autenticar("APODERADO");
    var hijos = portalFamiliar.hijos();
    assertEquals(1,hijos.size()); assertEquals(estudianteId,hijos.getFirst().estudianteId());
    assertEquals(dniEstudiante,hijos.getFirst().numeroDocumento());
    assertTrue(portalFamiliar.calificaciones(matriculaId).isEmpty());
    assertTrue(portalFamiliar.asistencia(matriculaId).isEmpty());
    assertFalse(portalFamiliar.periodos(matriculaId).isEmpty());
    try (var documento=org.apache.pdfbox.Loader.loadPDF(pdfLibreta.generar(matriculaId))) {
      String contenido=new org.apache.pdfbox.text.PDFTextStripper().getText(documento);
      assertTrue(contenido.contains(dniEstudiante)); assertTrue(contenido.contains("SHUJI KITAMURA"));
    }
    assertThrows(org.springframework.security.access.AccessDeniedException.class,()->portalFamiliar.alumnosSeguimiento());
  }

  @Test
  void custodiaRevocadaBloqueaLibretaPdfYPortal() {
    vincularCuentaFamiliar();
    var vinculo=em.createQuery("select v from EstudianteApoderadoEntity v where v.estudianteId=:id",com.colegio.shuji.matricula.infrastructure.entity.EstudianteApoderadoEntity.class)
        .setParameter("id",estudianteId).getSingleResult();
    vinculo.setTieneCustodia(false);em.flush();autenticar("APODERADO");
    assertTrue(portalFamiliar.hijos().isEmpty());
    assertThrows(org.springframework.security.access.AccessDeniedException.class,()->libretas.libreta(matriculaId));
    assertThrows(org.springframework.security.access.AccessDeniedException.class,()->pdfLibreta.generar(matriculaId));
    assertThrows(org.springframework.security.access.AccessDeniedException.class,()->portalFamiliar.calificaciones(matriculaId));
  }

  @Test
  void suplenciaPersisteApruebaRevocaYEvitaSolapamiento() {
    vincularCuentaFamiliar();autenticar("APODERADO");
    var hoy=LocalDate.now(java.time.ZoneId.of("America/Lima"));
    var primera=suplencias.solicitar(new com.colegio.shuji.matricula.application.dto.in.CrearSuplenciaRequestDto(
        estudianteId,"34567891","Elena","Perez","Ruiz","TIA","912345678",hoy,hoy.plusDays(3)));
    assertEquals("PENDIENTE_APROBACION",primera.estado());assertEquals(1,suplencias.misSolicitudes().size());
    assertThrows(org.springframework.security.access.AccessDeniedException.class,()->suplencias.aprobar(primera.id()));
    var segunda=suplencias.solicitar(new com.colegio.shuji.matricula.application.dto.in.CrearSuplenciaRequestDto(
        estudianteId,"34567892","Luis","Perez","Ruiz","TIO","912345678",hoy,hoy.plusDays(3)));
    autenticar("DIRECCION");
    var aprobada=suplencias.aprobar(primera.id());assertEquals("ACTIVO",aprobada.estado());assertEquals(usuarioId,aprobada.autorizadoPorUsuarioId());
    assertThrows(com.colegio.shuji.shared.domain.exception.BusinessException.class,()->suplencias.aprobar(segunda.id()));
    assertEquals("REVOCADO",suplencias.revocar(primera.id(),"Titular retomó el recojo").estado());
    assertEquals("ACTIVO",suplencias.aprobar(segunda.id()).estado());
    assertEquals(2,suplencias.bandeja().stream().filter(s -> s.estudianteId().equals(estudianteId)).count());
  }

  @Test
  void importacionCnebValidaAntesDeGuardarYConfirmaConPersistenciaReal() {
    var periodo=calendario.listarPeriodos(anioId).stream().filter(p->p.activo()&&!p.cerrado()).findFirst().orElseThrow();
    var area=curriculo.crearArea(new CrearAreaRequestDto(nivelId,"PBK","Matemática"));
    var competencia=curriculo.crearCompetencia(new CrearCompetenciaRequestDto(area.id(),(short)1,"Resuelve problemas de cantidad",null));
    var asignacion=carga.asignarDocente(new AsignarDocenteRequestDto(usuarioId,seccionId,anioId,nivelId,area.id()));
    autenticar("DOCENTE");
    assertTrue(evaluacion.listarMatriz(asignacion.id(), periodo.id(), competencia.id()).stream()
        .filter(fila -> fila.matriculaId().equals(matriculaId)).findFirst().orElseThrow()
        .nombreEstudiante().contains("Ana"));
    var opcion=catalogoHorarios.catalogo().stream().filter(a->a.asignacionId().equals(asignacion.id())).findFirst().orElseThrow();
    assertEquals("Matemática",opcion.areaNombre());assertEquals((short)2026,opcion.anio());assertNotNull(opcion.aulaCodigo());
    autenticar("DIRECCION");
    var cabecera=List.of("DNI_ESTUDIANTE","COMPETENCIA_ID","NOTA_CNEB","CONCLUSION_DESCRIPTIVA");
    var malas=List.of(cabecera,List.of(dniEstudiante,competencia.id().toString(),"Z","Error de nota"));
    assertThrows(com.colegio.shuji.evaluacion.domain.exception.ImportacionCalificacionesException.class,
        ()->importacionCneb.importar(malas,periodo.id(),seccionId,asignacion.id(),true));
    assertTrue(evaluacion.listar(asignacion.id(),periodo.id()).isEmpty());
    var filas=List.of(cabecera,List.of(dniEstudiante,competencia.id().toString(),"B","Explica procedimientos con apoyo"));
    assertEquals(0,importacionCneb.importar(filas,periodo.id(),seccionId,asignacion.id(),false).filasProcesadas());
    assertTrue(evaluacion.listar(asignacion.id(),periodo.id()).isEmpty());
    assertEquals(1,importacionCneb.importar(filas,periodo.id(),seccionId,asignacion.id(),true).filasProcesadas());
    em.flush();em.clear();assertEquals(1,evaluacion.listar(asignacion.id(),periodo.id()).size());
    var aula=em.find(com.colegio.shuji.academico.infrastructure.entity.SeccionEntity.class,seccionId).getAula().getCodigo();
    var sesion=refuerzo.programar(new com.colegio.shuji.evaluacion.application.dto.in.ProgramarRefuerzoRequestDto(
        anioId,periodo.id(),area.id(),usuarioId,"Refuerzo de cantidades",LocalDate.now(java.time.ZoneId.of("America/Lima")).plusDays(1),
        LocalTime.of(15,0),LocalTime.of(16,0),aula));
    assertEquals(1,refuerzo.derivar(sesion.id()).size());
    assertTrue(refuerzo.inscripciones(sesion.id()).getFirst().nombreEstudiante().contains("Ana"));
  }

  @AfterEach
  void limpiar() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void triggerVacantesNoDuplicaReservaAlConfirmarYAuditaActor() {
    em.clear();
    int ocupadas = vacantes.vacantes(seccionId).vacantesOcupadas();
    matriculas.confirmar(new ConfirmarMatriculaRequestDto(matriculaId));
    em.clear();
    assertEquals(ocupadas, vacantes.vacantes(seccionId).vacantesOcupadas());
    var actor =
        em.createQuery(
                "select function('current_setting', 'app.current_user_id', true) from UsuarioEntity"
                    + " u where u.id=:id",
                String.class)
            .setParameter("id", usuarioId)
            .getSingleResult();
    assertEquals(usuarioId.toString(), actor);
  }

  @Test
  void auditoriaTransaccionalRegistraActorEnAuditoriaCambios() {
    em.clear();
    matriculas.confirmar(new ConfirmarMatriculaRequestDto(matriculaId));
    em.flush();
    em.clear();

    var registros =
        em.createQuery(
                "select a from AuditoriaCambiosEntity a where a.usuario.id = :uid order by a.id desc",
                AuditoriaCambiosEntity.class)
            .setParameter("uid", usuarioId)
            .getResultList();

    assertFalse(
        registros.isEmpty(),
        "La auditoría de cambios debe registrar el actor autenticado en la transacción");
    assertEquals(usuarioId, registros.getFirst().getUsuario().getId());
  }

  @Test
  void finanzasConTriggersIdempotenciaYAnulacion() {
    var plan =
        cronograma.generarCronograma(
            new GenerarObligacionesAnualesRequestDto(
                matriculaId, (short) 1, (short) 2, LocalDate.of(2026, 3, 1)));
    assertEquals(11, plan.size());
    assertEquals(
        11,
        cronograma
            .generarCronograma(
                new GenerarObligacionesAnualesRequestDto(
                    matriculaId, (short) 1, (short) 2, LocalDate.of(2026, 3, 1)))
            .size());
    var obligacionPension =
        plan.stream()
            .filter(o -> o.tipoConcepto() == TipoConcepto.PENSION)
            .findFirst()
            .orElseThrow();
    var saldoInicial = obligacionPension.saldoPendiente();
    var r =
        new RegistrarPagoCajaRequestDto(
            obligacionPension.id(), "caja-001", MetodoPago.EFECTIVO, saldoInicial);
    var pago = pagos.registrarCaja(r);
    assertEquals(pago.id(), pagos.registrarCaja(r).id());
    var comprobante =
        comprobantes.emitir(
            new EmitirComprobanteRequestDto(pago.id(), TipoComprobante.RECIBO_INTERNO, "E001"));
    em.clear();
    var pensionActual =
        cronograma.obligacionesMatricula(matriculaId).stream()
            .filter(o -> o.id().equals(obligacionPension.id()))
            .findFirst()
            .orElseThrow();
    assertEquals(EstadoObligacion.PAGADO_TOTAL, pensionActual.estado());
    reversiones.revertir(new RevertirPagoRequestDto(pago.id(), "Error de caja"));
    em.clear();
    assertEquals(
        EstadoComprobante.ANULADO, comprobantes.consultar(comprobante.id()).estadoComprobante());
    assertEquals(
        saldoInicial,
        cronograma.obligacionesMatricula(matriculaId).stream()
            .filter(o -> o.id().equals(obligacionPension.id()))
            .findFirst()
            .orElseThrow()
            .saldoPendiente());
  }

  @org.junit.jupiter.params.ParameterizedTest
  @org.junit.jupiter.params.provider.EnumSource(value = MetodoPago.class, names = {"YAPE", "PAGO_EFECTIVO"})
  void pagoPublicoDe350RegistraObligacionTransaccionYBoletaUnaSolaVez(MetodoPago metodo) {
    String dniNuevo = "5" + String.format("%07d", new Random().nextInt(9999999));
    String dniTutor = "6" + String.format("%07d", new Random().nextInt(9999999));
    var nueva = solicitudesPublicas.crear(new CrearSolicitudMatriculaPublicaRequestDto(
        anioId, seccionId, dniNuevo, "Maria", "Perez", "Ruiz", nacimientoAdmisible(), Genero.F,
        dniTutor, "Luis", "Perez", "Ruiz", "999888777", "familia@example.test", "Lima", "150101", Parentesco.PADRE, true));
    var s = em.find(com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaPublicaEntity.class, nueva.id());
    s.setEstado(metodo == MetodoPago.PAGO_EFECTIVO ? EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS
        : EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE);
    for (var tipo : com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud.values()) {
      var analisis = new com.colegio.shuji.matricula.application.port.out.GeminiDocumentoMatriculaPort.Resultado(
          true,true,true,"Evidencia sintética del test",true,
          tipo == com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud.PARTIDA_NACIMIENTO ? true : null);
      solicitudesPublicas.guardarResultadoDocumento(nueva.id(), nueva.token(), tipo, analisis,
          new com.colegio.shuji.matricula.application.port.out.FirmaDigitalDocumentoPort.Resultado(
              com.colegio.shuji.matricula.domain.enums.EstadoFirmaDigital.VALIDA,"Firma sintética de test",null),
          tipo == com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud.RECIBO_SERVICIO ? null : true,
          null,"privado","test/"+UUID.randomUUID()+".pdf","application/pdf",100,"a".repeat(64));
    }
    if (metodo == MetodoPago.YAPE) {
      assertEquals(1, seccionesJpa.reservarVacante(seccionId));
      s.setVacanteReservada(true);
      s.setFechaExpiracionPago(OffsetDateTime.now().plusMinutes(30));
    }
    em.flush();
    String transaccionId = "it-publico-" + UUID.randomUUID();
    var pago = new com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort.PagoVerificado(
        transaccionId, null, new BigDecimal("350.00"), "PEN", metodo, true,
        com.colegio.shuji.matricula.infrastructure.service.SolicitudMatriculaPublicaService.REFERENCIA_PAGO_PREFIX + nueva.id());
    var confirmada = solicitudesPublicas.finalizarPagoVerificado(nueva.id(), nueva.token(), pago);
    em.flush();
    em.clear();
    assertEquals(EstadoSolicitudMatriculaPublica.MATRICULADA, confirmada.estado());
    var deuda = cronograma.obligacionesMatricula(confirmada.matriculaId()).getFirst();
    assertEquals(new BigDecimal("350.00"), deuda.totalPagado());
    assertEquals(EstadoObligacion.PAGADO_TOTAL, deuda.estado());
    var codigo = em.find(com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaPublicaEntity.class, nueva.id()).getComprobantePagoCodigo();
    assertTrue(codigo.matches("B001-[0-9]{8}"));
    assertEquals(confirmada.matriculaId(), solicitudesPublicas.finalizarPagoVerificado(nueva.id(), nueva.token(), pago).matriculaId());
    assertEquals(1L, em.createQuery("select count(p) from PagoTransaccionEntity p where p.pasarelaTransaccionId=:id", Long.class)
        .setParameter("id", transaccionId).getSingleResult());
  }

  @Test
  void conservaHistorialParentalYBloqueaPagoMientrasFaltenOtrosPdf() {
    var nueva = solicitudesPublicas.crear(new CrearSolicitudMatriculaPublicaRequestDto(
        anioId,seccionId,"7"+String.format("%07d",new Random().nextInt(9999999)),"Maria","Perez","Ruiz",
        nacimientoAdmisible(),Genero.F,"8"+String.format("%07d",new Random().nextInt(9999999)),
        "Rosa","Perez","Ruiz","999888777","familia@example.test","Lima","150101",Parentesco.MADRE,true));
    var tipo = TipoDocumentoSolicitud.PARTIDA_NACIMIENTO;
    var firma = new com.colegio.shuji.matricula.application.port.out.FirmaDigitalDocumentoPort.Resultado(
        EstadoFirmaDigital.VALIDA,"Firma sintética del test",null);
    var rechazo = new com.colegio.shuji.matricula.application.port.out.GeminiDocumentoMatriculaPort.Resultado(
        true,true,true,"Progenitor distinto",true,false);
    var observada = solicitudesPublicas.guardarResultadoDocumento(nueva.id(),nueva.token(),tipo,rechazo,firma,true,
        null,"privado","test/"+UUID.randomUUID()+".pdf","application/pdf",100,"a".repeat(64));
    var anterior = observada.documentos().stream().filter(d -> d.tipo()==tipo).findFirst().orElseThrow();
    assertEquals(EstadoValidacionDocumento.OBSERVADO,anterior.estado());
    assertFalse(anterior.vinculoParentalCoincide());
    var aprobacion = new com.colegio.shuji.matricula.application.port.out.GeminiDocumentoMatriculaPort.Resultado(
        true,true,true,"Progenitor coincidente",true,true);
    var corregida = solicitudesPublicas.guardarResultadoDocumento(nueva.id(),nueva.token(),tipo,aprobacion,firma,true,
        anterior.versionId(),"privado","test/"+UUID.randomUUID()+".pdf","application/pdf",100,"b".repeat(64));
    var actual = corregida.documentos().stream().filter(d -> d.tipo()==tipo).findFirst().orElseThrow();
    assertEquals(EstadoValidacionDocumento.VALIDADO,actual.estado());
    assertTrue(actual.vinculoParentalCoincide());
    assertNotEquals(anterior.versionId(),actual.versionId());
    em.flush(); em.clear();
    assertFalse(em.find(com.colegio.shuji.matricula.infrastructure.entity.DocumentoMatriculaVersionEntity.class,
        anterior.versionId()).getVinculoParentalCoincide());
    assertEquals(409,assertThrows(org.springframework.web.server.ResponseStatusException.class,
        () -> solicitudesPublicas.crearPago(nueva.id(),nueva.token())).getStatusCode().value());
  }

  private LocalDate nacimientoAdmisible() {
    var seccion = em.find(com.colegio.shuji.academico.infrastructure.entity.SeccionEntity.class, seccionId);
    var grado = seccion.getRelacion1();
    var rango = com.colegio.shuji.matricula.domain.service.ReglaEdadMatricula.paraGrado(
        grado.getRelacion0().getCodigo().name(), grado.getNumeroGrado());
    return LocalDate.of(2026 - rango.minima(), 2, 1);
  }

  @Test
  void clavesCompuestasNotasYRefuerzo() {
    var p =
        calendario.listarPeriodos(anioId).stream()
            .filter(periodo -> periodo.activo() && !periodo.cerrado())
            .findFirst()
            .orElseThrow();
    var area = curriculo.crearArea(new CrearAreaRequestDto(nivelId, "MATTEST", "Matemática de prueba"));
    var comp =
        curriculo.crearCompetencia(
            new CrearCompetenciaRequestDto(area.id(), (short) 1, "Resuelve problemas", null));
    String aulaCodigo =
        estructura.listarSecciones(anioId, nivelId).stream()
            .filter(seccion -> seccion.id().equals(seccionId))
            .map(com.colegio.shuji.academico.application.dto.out.SeccionResponseDto::aulaFisica)
            .findFirst()
            .orElseThrow();
    var asignacion =
        carga.asignarDocente(
            new AsignarDocenteRequestDto(usuarioId, seccionId, anioId, nivelId, area.id()));
    var bloque =
        curriculo.crearBloque(
            new CrearBloqueRequestDto(
                (short) 9, LocalTime.of(13, 45), LocalTime.of(14, 30), false));
    horarios.programar(
        new ProgramarHorarioRequestDto(asignacion.id(), DiaSemana.LUNES, bloque.id()));
    var notas =
        evaluacion.registrar(
            new RegistrarCalificacionesMasivasRequestDto(
                p.id(),
                asignacion.id(),
                List.of(
                    new CalificacionItemRequestDto(
                        matriculaId,
                        comp.id(),
                        CalificacionCualitativa.C,
                        "Necesita apoyo",
                        false))));
    assertTrue(notas.getFirst().requiereRefuerzo());
    var sesion =
        refuerzo.programar(
            new ProgramarRefuerzoRequestDto(
                anioId,
                p.id(),
                area.id(),
                usuarioId,
                "Refuerzo",
                LocalDate.of(2026, 12, 1),
                LocalTime.of(15, 0),
                LocalTime.of(16, 0),
                aulaCodigo));
    assertEquals(1, refuerzo.derivar(sesion.id()).size());
    assertEquals(0, refuerzo.derivar(sesion.id()).size());
    calendario.cerrarPeriodo(p.id());
    assertThrows(
        com.colegio.shuji.evaluacion.domain.exception.PeriodoEvaluacionCerradoException.class,
        () ->
            evaluacion.registrar(
                new RegistrarCalificacionesMasivasRequestDto(
                    p.id(),
                    asignacion.id(),
                    List.of(
                        new CalificacionItemRequestDto(
                            matriculaId, comp.id(), CalificacionCualitativa.A, null, false)))));
  }

  @Test
  void conciliaPorteriaYAulaYDetectaDuplicados() {
    var fecha = LocalDate.of(2026, 9, 18);
    var marca =
        new MarcaBiometricaRequestDto(
            dniEstudiante, fecha.atTime(8, 0).atOffset(ZoneOffset.ofHours(-5)), "P01");
    var lote =
        biometrico.importar(
            new ImportarLoteBiometricoRequestDto("asistencia.csv", List.of(marca, marca)));
    assertEquals(1, lote.marcasValidas());
    assertEquals(1, lote.marcasErroneas());
    aula.registrar(
        new RegistrarAsistenciaAulaRequestDto(
            matriculaId, fecha, LocalTime.of(8, 10), EstadoAsistenciaAula.PRESENTE));
    assertTrue(aula.reporte(fecha).asistencias().stream()
        .filter(fila -> fila.matriculaId().equals(matriculaId)).findFirst().orElseThrow()
        .nombreEstudiante().contains("Ana"));
    assertEquals(
        TipoDiscrepancia.ASISTENCIA_CONCILIADA,
        conciliacion.conciliar(anioId, fecha, true).getFirst().tipoDiscrepancia());
    assertTrue(conciliacion.alertas(fecha).isEmpty());
  }

  @Test
  void incidenciasDevuelvenElNombreDelEstudianteDeLaMatricula() {
    autenticar("DIRECCION");
    var incidencia = registrarIncidencia.registrar(
        new com.colegio.shuji.convivencia.application.dto.in.RegistrarIncidenciaRequestDto(
            matriculaId, LocalDate.of(2026, 9, 18),
            com.colegio.shuji.convivencia.domain.enums.TipoFalta.LEVE,
            "Observación de prueba", false));
    assertTrue(incidencia.nombreEstudiante().contains("Ana"));
  }

  @Test
  void mallaPorGradoAdmiteTodosLosGradosYLimitaLosCursosAlNivel() {
    autenticar("DIRECCION");
    var otroNivel = estructura.listarNiveles().stream()
        .filter(n -> !n.id().equals(nivelId)).findFirst().orElseThrow();
    var grado = estructura.listarGrados(otroNivel.id()).getFirst();
    var sufijo = Long.toUnsignedString(System.nanoTime(), 36).toUpperCase(Locale.ROOT);
    var areaDelNivel = curriculo.crearArea(
        new CrearAreaRequestDto(otroNivel.id(), "M" + sufijo, "Área malla prueba"));
    var areaDeOtroNivel = curriculo.crearArea(
        new CrearAreaRequestDto(nivelId, "N" + sufijo, "Área nivel distinto"));
    var bloques = curriculo.listarBloques().stream().filter(b -> !b.esRecreo()).toList();
    var existentes = horarios.porGrado(grado.id(), anioId);
    var espacio = Arrays.stream(DiaSemana.values()).flatMap(dia -> bloques.stream()
        .map(b -> Map.entry(dia, b)))
        .filter(celda -> existentes.stream().noneMatch(h -> h.diaSemana() == celda.getKey().getCodigo()
            && h.bloqueHorarioId().equals(celda.getValue().id())))
        .findFirst().orElseThrow();
    var solicitud = new ProgramarMallaHorariaGradoRequestDto(
        anioId, grado.id(), areaDelNivel.id(), espacio.getKey(), espacio.getValue().id());
    assertEquals(grado.id(), horarios.programarGrado(solicitud).gradoId());

    var cursoIncompatible = new ProgramarMallaHorariaGradoRequestDto(
        anioId, grado.id(), areaDeOtroNivel.id(), DiaSemana.MARTES, espacio.getValue().id());
    assertThrows(com.colegio.shuji.shared.domain.exception.BusinessException.class,
        () -> horarios.programarGrado(cursoIncompatible));
  }

  @Test
  void comunicadoUsaApoderadoAutenticadoYAcuseIdempotente() {
    var c =
        publicacion.publicar(
            new EmitirComunicadoRequestDto(
                "Reunión", "Asistencia de familias", true, anioId, nivelId, seccionId));
    autenticar("APODERADO");
    assertEquals(1, bandeja.bandeja().comunicados().size());
    var primero = lectura.confirmar(c.id(), new ConfirmarAcuseReciboRequestDto(true));
    assertEquals(
        primero.fechaAcuse(),
        lectura.confirmar(c.id(), new ConfirmarAcuseReciboRequestDto(true)).fechaAcuse());
    autenticar("DIRECCION");
    assertEquals(1, publicacion.metricas(c.id()).acuses());
  }
}
