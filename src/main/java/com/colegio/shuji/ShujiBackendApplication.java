package com.colegio.shuji;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class ShujiBackendApplication {

  public static void main(String[] args) {
    if (!perfilLocalDockerActivo()) {
      loadDotenvIfPresent();
    }
    SpringApplication.run(ShujiBackendApplication.class, args);
  }

  private static boolean perfilLocalDockerActivo() {
    String perfiles = System.getProperty(
        "spring.profiles.active", System.getenv("SPRING_PROFILES_ACTIVE"));
    if (perfiles == null) return false;
    return java.util.Arrays.stream(perfiles.split(","))
        .map(String::trim)
        .anyMatch("local-docker"::equals);
  }

  /**
   * Carga las variables del archivo .env como propiedades del sistema JVM antes de que Spring
   * resuelva los placeholders ${...}. Busca primero en el working directory y luego en ./backend/.
   * Solo establece variables que aún no estén definidas como variables de entorno del SO ni como
   * propiedades del sistema, respetando cualquier valor inyectado por IntelliJ, Docker o CI.
   */
  private static void loadDotenvIfPresent() {
    Path envFile = Path.of(".env");
    if (!Files.isRegularFile(envFile)) {
      envFile = Path.of("backend", ".env");
    }
    if (!Files.isRegularFile(envFile)) {
      return;
    }
    try (BufferedReader reader = Files.newBufferedReader(envFile)) {
      String line;
      while ((line = reader.readLine()) != null) {
        line = line.strip();
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        int eq = line.indexOf('=');
        if (eq <= 0) {
          continue;
        }
        String key = line.substring(0, eq).strip();
        String value = line.substring(eq + 1).strip();
        // No sobrescribir variables de entorno del SO ni propiedades ya definidas
        if (System.getenv(key) == null && System.getProperty(key) == null) {
          System.setProperty(key, value);
        }
      }
    } catch (IOException e) {
      System.err.println("[ShujiBackend] No se pudo leer .env: " + e.getMessage());
    }
  }
}
