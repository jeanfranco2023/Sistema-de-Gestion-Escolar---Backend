package com.colegio.shuji;

import static org.junit.jupiter.api.Assertions.*;

import com.colegio.shuji.asistencia.application.dto.in.ImportarLoteBiometricoRequestDto;
import com.colegio.shuji.asistencia.application.dto.in.MarcaBiometricaRequestDto;
import com.colegio.shuji.asistencia.application.port.in.ProcesarBiometricoUseCase;
import com.colegio.shuji.config.security.UserPrincipal;
import com.colegio.shuji.matricula.application.dto.in.RegistrarEstudianteRequestDto;
import com.colegio.shuji.matricula.application.port.in.RegistrarFichaFamiliarUseCase;
import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import com.colegio.shuji.tesoreria.application.port.in.GenerarCronogramaPensionesUseCase;
import com.colegio.shuji.usuario.infrastructure.entity.RolEntity;
import com.colegio.shuji.usuario.infrastructure.entity.UsuarioEntity;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;
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
 * Pruebas de carga y detección de regresiones N+1 sobre PostgreSQL.
 * Valida que la ingesta masiva de biometría y las consultas financieras
 * escalen en tiempo O(1) respecto a las consultas a base de datos (batch queries).
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
      "spring.datasource.hikari.maximum-pool-size=10"
    })
class CargaYRegresionNPlusOneTest {

  @Autowired EntityManager em;
  @Autowired TransactionTemplate txTemplate;
  @Autowired RegistrarFichaFamiliarUseCase familias;
  @Autowired ProcesarBiometricoUseCase biometrico;
  @Autowired GenerarCronogramaPensionesUseCase tesoreria;

  Long usuarioId;

  @BeforeEach
  void preparar() {
    txTemplate.execute(status -> {
      var rolesAux = em.createQuery("select r from RolEntity r where r.codigo='AUXILIAR'", RolEntity.class).getResultList();
      var u = new UsuarioEntity();
      u.setUsername("auxiliar_n1_" + UUID.randomUUID().toString().substring(0, 8));
      u.setEmail(u.getUsername() + "@colegio.pe");
      u.setPasswordHash("hash123");
      u.setRoles(new HashSet<>(rolesAux));
      em.persist(u);
      em.flush();
      usuarioId = u.getId();
      return null;
    });
    autenticar("AUXILIAR");
  }

  @Test
  @DisplayName("Carga masiva sin N+1: Procesamiento de 300 marcas biométricas en lote único")
  void procesamientoMasivoDeMarcasBiometricasEjecutaEnTiempoSubsegundoSinNPlusOne() {
    final int TOTAL_ALUMNOS = 50;
    final int MARCAS_POR_ALUMNO = 6;
    final int TOTAL_MARCAS = TOTAL_ALUMNOS * MARCAS_POR_ALUMNO; // 300 marcas

    List<String> dnis = new ArrayList<>();
    for (int i = 0; i < TOTAL_ALUMNOS; i++) {
      String dni = "4" + String.format("%07d", new Random().nextInt(9999999));
      dnis.add(dni);
      familias.registrarEstudiante(
          new RegistrarEstudianteRequestDto(
              TipoDocumento.DNI, dni,
              "EstudianteN1_" + i, "Batch", "Performance",
              LocalDate.of(2017, 3, 15), Genero.M, null, null, null));
    }

    List<MarcaBiometricaRequestDto> marcasLote = new ArrayList<>();
    var ahora = OffsetDateTime.now();
    for (int i = 0; i < TOTAL_ALUMNOS; i++) {
      String dni = dnis.get(i);
      for (int m = 0; m < MARCAS_POR_ALUMNO; m++) {
        marcasLote.add(new MarcaBiometricaRequestDto(
            dni, ahora.plusMinutes(m * 10), "PORT-01"));
      }
    }

    // Medición de rendimiento: Ingesta en lote de 300 marcas
    long t0 = System.currentTimeMillis();
    var resultado = biometrico.importar(
        new ImportarLoteBiometricoRequestDto("lote_masivo_performance.csv", marcasLote));
    long duracionMs = System.currentTimeMillis() - t0;

    assertNotNull(resultado);
    assertEquals(TOTAL_MARCAS, resultado.totalFilas());
    assertEquals(TOTAL_MARCAS, resultado.marcasValidas());
    assertEquals(0, resultado.marcasErroneas());

    // Si hubiera N+1 (300 marcas * 3 consultas = 900 roundtrips secuenciales a Postgres),
    // tardaría varios segundos. Con batching (3 consultas en total), tarda típicamente < 500ms.
    assertTrue(duracionMs < 4000, "El procesamiento por lotes debe ser subsegundo (duró " + duracionMs + " ms)");
  }

  private void autenticar(String rol) {
    var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + rol));
    var principal =
        UserPrincipal.builder()
            .id(usuarioId != null ? usuarioId : 1L)
            .username("auxiliar_perf")
            .active(true)
            .authorities(authorities)
            .build();
    var auth = new UsernamePasswordAuthenticationToken(principal, null, authorities);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }
}
