package com.colegio.shuji.config.security;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Configuración central de Spring Security 6. Establece la política STATELESS, deshabilita CSRF,
 * configura CORS, cabeceras HTTP de seguridad, declara endpoints públicos y agrega filtros.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final RateLimitingFilter rateLimitingFilter;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final CorsConfigurationSource corsConfigurationSource;

  @Value("${springdoc.swagger-ui.enabled:true}")
  private boolean swaggerEnabled;

  private static final String[] PUBLIC_ENDPOINTS = {
    "/auth/**",
    "/api/v1/auth/**",
    "/api/v1/solicitudes-matricula/public/**",
    "/actuator/health",
    "/actuator/health/**",
    "/actuator/info"
  };

  private static final String[] SWAGGER_ENDPOINTS = {
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/v3/api-docs/**",
    "/api-docs/**"
  };

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource))
        .csrf(AbstractHttpConfigurer::disable)
        .headers(headers -> headers
            .contentTypeOptions(Customizer.withDefaults())
            .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
            .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000))
            .contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'self'; frame-ancestors 'none';")))
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .exceptionHandling(
            exceptions ->
                exceptions
                    .authenticationEntryPoint(authenticationEntryPoint())
                    .accessDeniedHandler(accessDeniedHandler()))
        .authorizeHttpRequests(
            auth -> {
              auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                  .permitAll()
                  .requestMatchers(HttpMethod.POST, "/api/v1/pagos/webhook/**")
                  .permitAll()
                  .requestMatchers("/actuator/prometheus", "/actuator/metrics", "/actuator/metrics/**")
                  .hasAnyRole("DIRECCION", "ACTUATOR")
                  .requestMatchers(PUBLIC_ENDPOINTS)
                  .permitAll();
              if (swaggerEnabled) {
                auth.requestMatchers(SWAGGER_ENDPOINTS).permitAll();
              } else {
                auth.requestMatchers(SWAGGER_ENDPOINTS).denyAll();
              }
              auth.anyRequest().authenticated();
            })
        .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(10);
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
      throws Exception {
    return authConfig.getAuthenticationManager();
  }

  @Bean
  public AuthenticationEntryPoint authenticationEntryPoint() {
    return (request, response, authException) -> {
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response
          .getOutputStream()
          .println(
              String.format(
                  "{\"success\":false,\"status\":401,\"errorCode\":\"UNAUTHORIZED\",\"message\":\"%s\",\"path\":\"%s\"}",
                  authException.getMessage(), request.getRequestURI()));
    };
  }

  @Bean
  public AccessDeniedHandler accessDeniedHandler() {
    return (request, response, accessDeniedException) -> {
      response.setContentType(MediaType.APPLICATION_JSON_VALUE);
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response
          .getOutputStream()
          .println(
              String.format(
                  "{\"success\":false,\"status\":403,\"errorCode\":\"ACCESS_DENIED\",\"message\":\"Acceso"
                      + " denegado al recurso\",\"path\":\"%s\"}",
                  request.getRequestURI()));
    };
  }
}
