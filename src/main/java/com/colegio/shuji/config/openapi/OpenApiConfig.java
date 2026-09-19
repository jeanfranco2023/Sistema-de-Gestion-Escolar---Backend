package com.colegio.shuji.config.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Configuración de OpenAPI 3 / Swagger UI con soporte para autenticación Bearer JWT. */
@Configuration
public class OpenApiConfig {

  private static final String SECURITY_SCHEME_NAME = "BearerAuth";

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("I.E.P. Shuji Kitamura - API Backend")
                .description(
                    "Plataforma Web Institucional con Arquitectura Hexagonal y Spring Boot 3.3."
                        + " Gestión académica, matrículas, control financiero y calificaciones bajo"
                        + " normativa CNEB - MINEDU.")
                .version("1.0.0")
                .contact(
                    new Contact()
                        .name("Equipo de Desarrollo Shuji Kitamura")
                        .email("soporte@shujikitamura.edu.pe"))
                .license(
                    new License()
                        .name("Uso Académico e Institucional")
                        .url("https://www.shujikitamura.edu.pe/terms")))
        .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
        .components(
            new Components()
                .addSecuritySchemes(
                    SECURITY_SCHEME_NAME,
                    new SecurityScheme()
                        .name(SECURITY_SCHEME_NAME)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description(
                            "Ingrese el token JWT obtenido del endpoint de inicio de sesión"
                                + " (/api/v1/auth/login).")));
  }
}
