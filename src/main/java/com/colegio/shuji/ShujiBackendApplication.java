package com.colegio.shuji;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@org.springframework.scheduling.annotation.EnableScheduling
public class ShujiBackendApplication {

  public static void main(String[] args) {
    cargarVariablesEntorno();
    SpringApplication.run(ShujiBackendApplication.class, args);
  }

  private static void cargarVariablesEntorno() {
    java.nio.file.Path backendEnv = java.nio.file.Path.of("backend", ".env");
    if (java.nio.file.Files.exists(backendEnv)) {
      try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(backendEnv)) {
        String line;
        while ((line = reader.readLine()) != null) {
          line = line.trim();
          if (line.isEmpty() || line.startsWith("#")) {
            continue;
          }
          int idx = line.indexOf('=');
          if (idx > 0) {
            String key = line.substring(0, idx).trim();
            String val = line.substring(idx + 1).trim();
            if (System.getProperty(key) == null && System.getenv(key) == null) {
              System.setProperty(key, val);
            }
          }
        }
      } catch (Exception ignored) {
      }
    }
  }
}
