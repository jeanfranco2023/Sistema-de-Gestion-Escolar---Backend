package com.colegio.shuji;

import static org.junit.jupiter.api.Assertions.*;

import com.colegio.shuji.config.security.UserPrincipal;
import com.colegio.shuji.curriculo.application.port.in.ConsultarCatalogoHorariosUseCase;
import com.colegio.shuji.evaluacion.application.port.in.DerivarAlumnosRefuerzoUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.TestConstructor;
import org.springframework.transaction.annotation.Transactional;

/** Verificación de consultas reales del tablero de refuerzos contra PostgreSQL. */
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
@EnabledIfEnvironmentVariable(named = "RUN_MUTATING_POSTGRES_TESTS", matches = "true")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@RequiredArgsConstructor
class RefuerzoTableroPostgresTest {
  private final DerivarAlumnosRefuerzoUseCase refuerzo;
  private final ConsultarCatalogoHorariosUseCase catalogo;

  private void autenticar(String rol) {
    var autoridades = List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    var auth =
        new UsernamePasswordAuthenticationToken(
            new UserPrincipal(1L, "qa_" + rol, "qa@shuji.local", "", true, autoridades),
            null,
            autoridades);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  @AfterEach
  void limpiar() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void tableroEInscripcionesYCatalogoDevuelvenDatosEnriquecidosDeQuintoSecundaria() {
    autenticar("ADMINISTRADOR");

    var opciones = catalogo.catalogo();
    assertFalse(opciones.isEmpty());
    assertTrue(
        opciones.stream()
            .anyMatch(o -> o.docenteNombre() != null && o.docenteNombre().contains(" ")),
        "El catálogo debe mostrar nombre completo docente");

    var tablero = refuerzo.tablero((short) 1, (short) 4);
    assertEquals(42, tablero.totalSesiones());
    assertEquals(442, tablero.totalInscripciones());
    assertTrue(tablero.evaluacionesRiesgoTotal() > 800);

    var primera = tablero.sesiones().getFirst();
    assertNotNull(primera.gradoSeccion());
    assertNotNull(primera.areaNombre());
    assertTrue(primera.docenteNombre().contains(" "), primera.docenteNombre());

    var detalle = refuerzo.inscripciones(primera.id());
    assertFalse(detalle.isEmpty());
    assertTrue(detalle.stream().anyMatch(i -> i.dniEstudiante() != null && i.calificacionOrigen() != null));
  }
}
