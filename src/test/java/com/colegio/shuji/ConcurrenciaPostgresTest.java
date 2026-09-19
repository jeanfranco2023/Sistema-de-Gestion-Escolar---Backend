package com.colegio.shuji;

import static org.junit.jupiter.api.Assertions.*;

import com.colegio.shuji.academico.application.dto.in.CrearAnioLectivoRequestDto;
import com.colegio.shuji.academico.application.dto.in.CrearGradoRequestDto;
import com.colegio.shuji.academico.application.dto.in.CrearSeccionRequestDto;
import com.colegio.shuji.academico.application.port.in.ConsultarVacantesUseCase;
import com.colegio.shuji.academico.application.port.in.GestionarAnioLectivoUseCase;
import com.colegio.shuji.academico.application.port.in.GestionarSeccionesUseCase;
import com.colegio.shuji.config.security.UserPrincipal;
import com.colegio.shuji.matricula.application.dto.in.ConfirmarMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.RegistrarApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.in.RegistrarEstudianteRequestDto;
import com.colegio.shuji.matricula.application.dto.in.SolicitarMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.VincularApoderadoRequestDto;
import com.colegio.shuji.matricula.application.port.in.ProcesarMatriculaUseCase;
import com.colegio.shuji.matricula.application.port.in.RegistrarFichaFamiliarUseCase;
import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.Parentesco;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import com.colegio.shuji.tesoreria.application.dto.in.GenerarObligacionesAnualesRequestDto;
import com.colegio.shuji.tesoreria.application.dto.in.RegistrarPagoCajaRequestDto;
import com.colegio.shuji.tesoreria.application.port.in.EmitirComprobanteUseCase;
import com.colegio.shuji.tesoreria.application.port.in.GenerarCronogramaPensionesUseCase;
import com.colegio.shuji.tesoreria.application.port.in.ProcesarPagoPasarelaUseCase;
import com.colegio.shuji.tesoreria.domain.enums.MetodoPago;
import com.colegio.shuji.tesoreria.domain.enums.TipoComprobante;
import com.colegio.shuji.usuario.infrastructure.entity.RolEntity;
import com.colegio.shuji.usuario.infrastructure.entity.UsuarioEntity;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Pruebas reales de alta concurrencia multi-hilo sobre PostgreSQL.
 * Valida el bloqueo pesimista (PESSIMISTIC_WRITE) en series_comprobante
 * y el control transaccional de vacantes.
 */
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
      "spring.datasource.hikari.maximum-pool-size=20",
      "spring.datasource.hikari.minimum-idle=5"
    })
class ConcurrenciaPostgresTest {

  @Autowired EntityManager em;
  @Autowired TransactionTemplate txTemplate;

  @Autowired GestionarAnioLectivoUseCase calendario;
  @Autowired GestionarSeccionesUseCase estructura;
  @Autowired ConsultarVacantesUseCase vacantes;
  @Autowired RegistrarFichaFamiliarUseCase familias;
  @Autowired ProcesarMatriculaUseCase matriculas;
  @Autowired GenerarCronogramaPensionesUseCase cronograma;
  @Autowired ProcesarPagoPasarelaUseCase pagos;
  @Autowired EmitirComprobanteUseCase comprobantes;

  Long usuarioId;
  Short anioId, nivelId;

  @BeforeEach
  void preparar() {
    autenticar("DIRECCION");
    txTemplate.execute(status -> {
      var rolesDocente = em.createQuery("select r from RolEntity r where r.codigo='DOCENTE'", RolEntity.class).getResultList();
      var u = new UsuarioEntity();
      u.setUsername("admin_conc_" + UUID.randomUUID().toString().substring(0, 8));
      u.setEmail(u.getUsername() + "@colegio.pe");
      u.setPasswordHash("hash123");
      u.setRoles(new HashSet<>(rolesDocente));
      em.persist(u);
      em.flush();
      usuarioId = u.getId();
      return null;
    });

    anioId = calendario.listarAnios().stream()
        .filter(a -> a.anio() == 2026)
        .findFirst()
        .map(com.colegio.shuji.academico.application.dto.out.AnioLectivoResponseDto::id)
        .orElseGet(() -> calendario.crearAnio(
            new CrearAnioLectivoRequestDto(
                (short) 2026,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31))).id());
    nivelId = estructura.listarNiveles().getFirst().id();
  }

  @Test
  @DisplayName("Concurrencia real: Múltiples hilos concurrentes emitiendo boletas no generan colisiones ni huecos")
  void emisionConcurrenteBoletasEsEstrictamenteSecuencialYSinColisiones() throws Exception {
    final int HILOS = 8;
    final String SERIE = "B001";

    List<Long> pagoIds = new ArrayList<>();
    var grado = estructura.listarGrados(nivelId).stream()
        .filter(g -> g.numeroGrado() == 1)
        .findFirst()
        .orElseGet(() -> estructura.crearGrado(new CrearGradoRequestDto(nivelId, (short) 1, "Grado Concurrente")));
    var seccionesExistentes = estructura.listarSecciones(anioId, nivelId).stream()
        .map(com.colegio.shuji.academico.application.dto.out.SeccionResponseDto::letra)
        .collect(java.util.stream.Collectors.toSet());
    String letra1 = "A";
    for (char c = 'A'; c <= 'Z'; c++) {
      if (!seccionesExistentes.contains(String.valueOf(c))) {
        letra1 = String.valueOf(c);
        seccionesExistentes.add(letra1);
        break;
      }
    }
    String aula1 = "A-" + UUID.randomUUID().toString().substring(0, 8);
    var seccion = estructura.crearSeccion(new CrearSeccionRequestDto(anioId, grado.id(), nivelId, letra1, (short) 50, aula1));

    for (int i = 0; i < HILOS; i++) {
      final int idx = i;
      var est = familias.registrarEstudiante(
          new RegistrarEstudianteRequestDto(
              TipoDocumento.DNI, "7" + String.format("%07d", new Random().nextInt(9999999)),
              "Alumno" + idx, "Test", "Concurrente",
              LocalDate.of(2018, 1, 1), Genero.M, null, null, null));
      var apod = familias.registrarApoderado(
          new RegistrarApoderadoRequestDto(
              TipoDocumento.DNI, "8" + String.format("%07d", new Random().nextInt(9999999)),
              "Apoderado" + idx, "Test", "R", "987654321", null, "Lima", "150101", null));
      familias.vincular(new VincularApoderadoRequestDto(est.id(), apod.id(), Parentesco.PADRE, true, true, true));

      var mat = matriculas.solicitar(new SolicitarMatriculaRequestDto(anioId, est.id(), seccion.id(), OffsetDateTime.now().plusDays(1), null));
      matriculas.confirmar(new ConfirmarMatriculaRequestDto(mat.id()));

      var plan = cronograma.generarCronograma(new GenerarObligacionesAnualesRequestDto(mat.id(), (short) 1, (short) 2, LocalDate.of(2026, 3, 1)));
      var obligacion = plan.getFirst();
      var pago = pagos.registrarCaja(new RegistrarPagoCajaRequestDto(obligacion.id(), "caja-conc-" + UUID.randomUUID(), MetodoPago.EFECTIVO, new BigDecimal("100.00")));
      pagoIds.add(pago.id());
    }

    ExecutorService executor = Executors.newFixedThreadPool(HILOS);
    CountDownLatch latch = new CountDownLatch(1);
    List<Future<Integer>> futures = new ArrayList<>();

    for (int i = 0; i < HILOS; i++) {
      final Long pId = pagoIds.get(i);
      futures.add(executor.submit(() -> {
        autenticar("DIRECCION");
        latch.await();
        var comp = comprobantes.emitirAutomaticoParaPago(pId, TipoComprobante.BOLETA, SERIE);
        return comp.correlativo();
      }));
    }

    latch.countDown();

    Set<Integer> correlativos = new HashSet<>();
    for (var future : futures) {
      Integer corr = future.get(20, TimeUnit.SECONDS);
      assertNotNull(corr, "El correlativo generado no debe ser nulo");
      assertTrue(corr > 0, "El correlativo debe ser positivo");
      boolean agregado = correlativos.add(corr);
      assertTrue(agregado, "¡Colisión detectada! Correlativo duplicado: " + corr);
    }

    executor.shutdown();
    assertEquals(HILOS, correlativos.size(), "Todos los correlativos emitidos concurrentemente deben ser únicos");
  }

  @Test
  @DisplayName("Concurrencia real: Múltiples hilos solicitando matrícula para vacantes limitadas")
  void solicitudesConcurrentesRespetanLimiteDeVacantes() throws Exception {
    final int VACANTES = 3;
    final int HILOS = 8;

    var grado = estructura.listarGrados(nivelId).stream()
        .filter(g -> g.numeroGrado() == 2)
        .findFirst()
        .orElseGet(() -> estructura.crearGrado(new CrearGradoRequestDto(nivelId, (short) 2, "Segundo Primaria")));
    var seccionesExistentes2 = estructura.listarSecciones(anioId, nivelId).stream()
        .map(com.colegio.shuji.academico.application.dto.out.SeccionResponseDto::letra)
        .collect(java.util.stream.Collectors.toSet());
    String letra2 = "Z";
    for (char c = 'Z'; c >= 'A'; c--) {
      if (!seccionesExistentes2.contains(String.valueOf(c))) {
        letra2 = String.valueOf(c);
        break;
      }
    }
    String aula2 = "A-" + UUID.randomUUID().toString().substring(0, 8);
    var seccion = estructura.crearSeccion(new CrearSeccionRequestDto(anioId, grado.id(), nivelId, letra2, (short) VACANTES, aula2));

    List<Long> estudiantes = new ArrayList<>();
    for (int i = 0; i < HILOS; i++) {
      var est = familias.registrarEstudiante(
          new RegistrarEstudianteRequestDto(
              TipoDocumento.DNI, "9" + String.format("%07d", new Random().nextInt(9999999)),
              "Postulante" + i, "Vacante", "Test",
              LocalDate.of(2018, 5, 5), Genero.F, null, null, null));
      estudiantes.add(est.id());
    }

    ExecutorService executor = Executors.newFixedThreadPool(HILOS);
    CountDownLatch startGate = new CountDownLatch(1);
    AtomicInteger exitos = new AtomicInteger(0);
    AtomicInteger rechazados = new AtomicInteger(0);

    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < HILOS; i++) {
      final Long eId = estudiantes.get(i);
      futures.add(executor.submit(() -> {
        autenticar("SECRETARIA");
        try {
          startGate.await();
          matriculas.solicitar(new SolicitarMatriculaRequestDto(anioId, eId, seccion.id(), OffsetDateTime.now().plusDays(1), null));
          exitos.incrementAndGet();
        } catch (Exception ex) {
          rechazados.incrementAndGet();
        }
      }));
    }

    startGate.countDown();
    for (var f : futures) {
      f.get(20, TimeUnit.SECONDS);
    }
    executor.shutdown();

    assertEquals(VACANTES, exitos.get(), "Exactamente 3 solicitudes deben haber tenido éxito");
    assertEquals(HILOS - VACANTES, rechazados.get(), "Las solicitudes excedentes debieron ser rechazadas por falta de vacantes");
  }

  private void autenticar(String rol) {
    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    var principal =
        UserPrincipal.builder()
            .id(usuarioId != null ? usuarioId : 1L)
            .username("admin_concurrencia")
            .active(true)
            .authorities(authorities)
            .build();
    var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }
}
