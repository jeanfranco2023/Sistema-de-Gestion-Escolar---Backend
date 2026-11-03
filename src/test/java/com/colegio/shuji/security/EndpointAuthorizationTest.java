package com.colegio.shuji.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.colegio.shuji.evaluacion.infrastructure.controller.rest.EvaluacionController;
import com.colegio.shuji.usuario.domain.enums.RolNombre;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

class EndpointAuthorizationTest {

  private static final Set<String> ROLES =
      Arrays.stream(RolNombre.values()).map(Enum::name).collect(java.util.stream.Collectors.toSet());
  private static final Pattern ROLE_EXPRESSION = Pattern.compile("has(?:Any)?Role\\(([^)]*)\\)");
  private static final Pattern ROLE_NAME = Pattern.compile("'([^']+)'|\"([^\"]+)\"");
  @Test
  void everyRestEndpointDeclaresItsOwnAuthorizationAndOnlyKnownRoles() throws Exception {
    var violations = new java.util.ArrayList<String>();
    int endpoints = 0;

    var sourceRoot = Path.of("src/main/java");
    List<Path> sources;
    try (var files = Files.walk(sourceRoot)) {
      sources = files.filter(path -> path.toString().endsWith(".java")).toList();
    }
    for (Path source : sources) {
      String className = sourceRoot.relativize(source).toString()
          .replace('\\', '.').replace('/', '.').replaceFirst("\\.java$", "");
      Class<?> controller = Class.forName(className, false, getClass().getClassLoader());
      if (!controller.isAnnotationPresent(RestController.class)) continue;
      for (Method method : controller.getDeclaredMethods()) {
        if (!isRestEndpoint(method)) continue;
        endpoints++;
        PreAuthorize authorization = method.getDeclaredAnnotation(PreAuthorize.class);
        if (authorization == null) {
          violations.add(controller.getSimpleName() + "." + method.getName() + " lacks @PreAuthorize");
          continue;
        }

        Matcher expressions = ROLE_EXPRESSION.matcher(authorization.value());
        while (expressions.find()) {
          Matcher roleNames = ROLE_NAME.matcher(expressions.group(1));
          while (roleNames.find()) {
            String role = roleNames.group(1) != null ? roleNames.group(1) : roleNames.group(2);
            if (!ROLES.contains(role)) {
              violations.add(
                  controller.getSimpleName() + "." + method.getName() + " references unknown role " + role);
            }
          }
        }
      }
    }

    assertTrue(endpoints > 0, "No REST endpoints were discovered for the authorization audit");
    assertTrue(violations.isEmpty(), String.join(System.lineSeparator(), violations));
  }

  @Test
  void gradeEditingAndImportEndpointsAreTeacherOnly() {
    for (String methodName :
        List.of("plantilla", "cargarPlanilla", "sugerirConclusionIa", "registrarCalificacionesMasivas")) {
      String authorization =
          Arrays.stream(EvaluacionController.class.getDeclaredMethods())
              .filter(method -> method.getName().equals(methodName))
              .map(method -> method.getDeclaredAnnotation(PreAuthorize.class))
              .map(PreAuthorize::value)
              .findFirst()
              .orElseThrow(() -> new AssertionError("Missing @PreAuthorize for " + methodName));
      assertEquals("hasRole('DOCENTE')", authorization, methodName + " must be teacher-only");
    }
  }

  private static boolean isRestEndpoint(Method method) {
    return Arrays.stream(method.getDeclaredAnnotations())
        .anyMatch(annotation -> annotation.annotationType().getName().endsWith("Mapping"));
  }
}
