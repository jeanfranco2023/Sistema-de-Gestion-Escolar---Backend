package com.colegio.shuji;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Guardas contra regresiones de las fronteras hexagonales y el contrato JPQL. */
class ArquitecturaTest {
  private List<Path> fuentes() throws IOException {
    try (var archivos = Files.walk(Path.of("src/main/java/com/colegio/shuji"))) {
      return archivos.filter(p -> p.toString().endsWith(".java")).toList();
    }
  }

  @Test
  void aplicacionNoImportaInfraestructuraYDominioNoImportaFrameworks() throws IOException {
    var frontera =
        Pattern.compile("import com\\.colegio\\.shuji\\..*\\.(infrastructure|config)\\.");
    var framework =
        Pattern.compile("import (jakarta\\.persistence|org\\.springframework|org\\.hibernate)");
    for (var path : fuentes()) {
      var nombre = path.toString().replace('\\', '/');
      var codigo = Files.readString(path);
      if (nombre.contains("/application/"))
        assertFalse(frontera.matcher(codigo).find(), path.toString());
      if (nombre.contains("/domain/"))
        assertFalse(framework.matcher(codigo).find(), path.toString());
    }
  }

  @Test
  void serviciosSinComodinesNiSettersDeNegocio() throws IOException {
    var setters = Pattern.compile("\\.set(?!CurrentUserId\\b)[A-Z]\\w*\\(");
    for (var path : fuentes()) {
      if (!path.toString().replace('\\', '/').contains("/application/service/")) continue;
      var codigo = Files.readString(path);
      assertFalse(codigo.contains(".*;"), path.toString());
      assertFalse(setters.matcher(codigo).find(), path.toString());
      assertTrue(codigo.contains("@Transactional"), path.toString());
    }
  }

  @Test
  void noExistenConsultasNativas() throws IOException {
    var sql =
        Pattern.compile(
            "createNativeQuery|nativeQuery\\s*=\\s*true|import java\\.sql|JdbcTemplate");
    for (var path : fuentes())
      assertFalse(sql.matcher(Files.readString(path)).find(), path.toString());
  }

  @Test
  void pojosTienenConstructoresYBuilder() throws IOException {
    for (var path : fuentes()) {
      var nombre = path.toString().replace('\\', '/');
      if (!nombre.contains("/domain/model/") || nombre.endsWith("/Reglas.java")) continue;
      var codigo = Files.readString(path);
      for (var anotacion : List.of("@Builder", "@NoArgsConstructor", "@AllArgsConstructor"))
        assertTrue(codigo.contains(anotacion), path + " requiere " + anotacion);
    }
  }
}
