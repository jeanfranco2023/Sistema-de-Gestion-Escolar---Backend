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
import com.colegio.shuji.usuario.infrastructure.entity.RolEntity;
import com.colegio.shuji.usuario.infrastructure.entity.UsuarioEntity;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/** Requiere DDL oficial en una base desechable local; nunca usa las credenciales de .env. */
@EnabledIfSystemProperty(named = "shuji.integration", matches = "true")
@SpringBootTest(
    properties = {
      "spring.datasource.url=jdbc:postgresql://127.0.0.1:55439/postgres",
      "spring.datasource.username=postgres",
      "spring.datasource.password=",
      "spring.jpa.hibernate.ddl-auto=validate",
      "integraciones.reniec.url=",
      "integraciones.pagos.verificador-url=",
      "logging.level.org.hibernate.SQL=WARN",
      "spring.datasource.hikari.minimum-idle=0"
    })
@Transactional
class PersistenciaPostgresTest {
  @Autowired EntityManager em;
  @Autowired GestionarAnioLectivoUseCase calendario;
  @Autowired GestionarSeccionesUseCase estructura;
  @Autowired ConsultarVacantesUseCase vacantes;
  @Autowired RegistrarFichaFamiliarUseCase familias;
  @Autowired ProcesarMatriculaUseCase matriculas;
  @Autowired GenerarCronogramaPensionesUseCase cronograma;
  @Autowired ProcesarPagoPasarelaUseCase pagos;
  @Autowired EmitirComprobanteUseCase comprobantes;
  @Autowired RevertirPagoUseCase reversiones;
  @Autowired GestionarCurriculoUseCase curriculo;
  @Autowired AsignarCargaDocenteUseCase carga;
  @Autowired GenerarMallaHorariaUseCase horarios;
  @Autowired RegistrarEvaluacionCnebUseCase evaluacion;
  @Autowired DerivarAlumnosRefuerzoUseCase refuerzo;
  @Autowired ProcesarBiometricoUseCase biometrico;
  @Autowired RegistrarListaAulaUseCase aula;
  @Autowired EjecutarConciliacionDiariaUseCase conciliacion;
  @Autowired PublicarComunicadoUseCase publicacion;
  @Autowired ConfirmarLecturaUseCase lectura;
  @Autowired ConsultarBandejaUseCase bandeja;

  Long usuarioId, estudianteId, matriculaId;
  Short anioId, nivelId;
  Integer seccionId;
  String dniEstudiante;

  @BeforeEach
  void preparar() {
    var u = new UsuarioEntity();
    u.setUsername("prueba" + System.nanoTime());
    u.setEmail("prueba" + System.nanoTime() + "@example.test");
    u.setPasswordHash("hash-de-prueba");
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
    var r =
        new RegistrarPagoCajaRequestDto(
            plan.getFirst().id(), "caja-001", MetodoPago.EFECTIVO, new BigDecimal("350.00"));
    var pago = pagos.registrarCaja(r);
    assertEquals(pago.id(), pagos.registrarCaja(r).id());
    var comprobante =
        comprobantes.emitir(
            new EmitirComprobanteRequestDto(pago.id(), TipoComprobante.RECIBO_INTERNO, "E001"));
    em.clear();
    assertEquals(
        EstadoObligacion.PAGADO_TOTAL,
        cronograma.obligacionesMatricula(matriculaId).getFirst().estado());
    reversiones.revertir(new RevertirPagoRequestDto(pago.id(), "Error de caja"));
    em.clear();
    assertEquals(
        EstadoComprobante.ANULADO, comprobantes.consultar(comprobante.id()).estadoComprobante());
    assertEquals(
        new BigDecimal("350.00"),
        cronograma.obligacionesMatricula(matriculaId).getFirst().saldoPendiente());
  }

  @Test
  void clavesCompuestasNotasYRefuerzo() {
    var p =
        calendario.crearPeriodo(
            new CrearPeriodoRequestDto(
                anioId,
                (short) 1,
                "Bimestre 1",
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 5, 1)));
    var area = curriculo.crearArea(new CrearAreaRequestDto(nivelId, "MAT", "Matemática"));
    var comp =
        curriculo.crearCompetencia(
            new CrearCompetenciaRequestDto(area.id(), (short) 1, "Resuelve problemas", null));
    var asignacion =
        carga.asignarDocente(
            new AsignarDocenteRequestDto(usuarioId, seccionId, anioId, nivelId, area.id()));
    var bloque =
        curriculo.crearBloque(
            new CrearBloqueRequestDto((short) 1, LocalTime.of(8, 0), LocalTime.of(9, 0), false));
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
                "R1"));
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
    assertEquals(
        TipoDiscrepancia.ASISTENCIA_CONCILIADA,
        conciliacion.conciliar(anioId, fecha, true).getFirst().tipoDiscrepancia());
    assertTrue(conciliacion.alertas(fecha).isEmpty());
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
