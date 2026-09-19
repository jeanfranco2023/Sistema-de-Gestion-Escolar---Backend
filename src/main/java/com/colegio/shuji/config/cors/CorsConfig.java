package com.colegio.shuji.config.cors;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración central de Cross-Origin Resource Sharing (CORS). Permite conexiones seguras desde
 * clientes Angular, React o Vue (puertos 4200, 3000, 5173).
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

  @Value(
      "${app.cors.allowed-origins:http://localhost:4200,http://localhost:3000,http://localhost:5173}")
  private String allowedOrigins;

  @Value("${app.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}")
  private String allowedMethods;

  @Value("${app.cors.allowed-headers:*}")
  private String allowedHeaders;

  @Value("${app.cors.allow-credentials:true}")
  private boolean allowCredentials;

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();

    List<String> origins =
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    configuration.setAllowedOrigins(origins);

    List<String> methods =
        Arrays.stream(allowedMethods.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toList();
    configuration.setAllowedMethods(methods);

    if ("*".equals(allowedHeaders.trim())) {
      configuration.addAllowedHeader("*");
    } else {
      List<String> headers =
          Arrays.stream(allowedHeaders.split(","))
              .map(String::trim)
              .filter(s -> !s.isEmpty())
              .toList();
      configuration.setAllowedHeaders(headers);
    }

    configuration.setAllowCredentials(allowCredentials);
    configuration.addExposedHeader("Authorization");
    configuration.addExposedHeader("Content-Disposition");
    configuration.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    String[] origins =
        Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toArray(String[]::new);

    String[] methods =
        Arrays.stream(allowedMethods.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .toArray(String[]::new);

    registry
        .addMapping("/**")
        .allowedOrigins(origins)
        .allowedMethods(methods)
        .allowedHeaders("*")
        .exposedHeaders("Authorization", "Content-Disposition")
        .allowCredentials(allowCredentials)
        .maxAge(3600);
  }
}
