package com.colegio.shuji.security;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.colegio.shuji.academico.infrastructure.controller.rest.AcademicoController;
import com.colegio.shuji.asistencia.infrastructure.controller.rest.AsistenciaController;
import com.colegio.shuji.comunicado.infrastructure.controller.rest.ComunicadoController;
import com.colegio.shuji.convivencia.infrastructure.controller.rest.ConvivenciaController;
import com.colegio.shuji.curriculo.infrastructure.controller.rest.CurriculoController;
import com.colegio.shuji.curriculo.infrastructure.controller.rest.HorarioController;
import com.colegio.shuji.evaluacion.infrastructure.controller.rest.EvaluacionController;
import com.colegio.shuji.evaluacion.infrastructure.controller.rest.RefuerzoController;
import com.colegio.shuji.matricula.infrastructure.controller.rest.ApoderadoController;
import com.colegio.shuji.matricula.infrastructure.controller.rest.EstudianteController;
import com.colegio.shuji.matricula.infrastructure.controller.rest.MatriculaController;
import com.colegio.shuji.matricula.infrastructure.controller.rest.PortalFamiliarController;
import com.colegio.shuji.matricula.infrastructure.controller.rest.SolicitudMatriculaPublicaController;
import com.colegio.shuji.matricula.infrastructure.controller.rest.SuplenciaController;
import com.colegio.shuji.shared.infrastructure.controller.DashboardController;
import com.colegio.shuji.tesoreria.infrastructure.controller.rest.ComprobanteController;
import com.colegio.shuji.tesoreria.infrastructure.controller.rest.RetornoPagoController;
import com.colegio.shuji.tesoreria.infrastructure.controller.rest.TesoreriaController;
import com.colegio.shuji.tesoreria.infrastructure.controller.rest.WebhookController;
import com.colegio.shuji.usuario.domain.enums.RolNombre;
import com.colegio.shuji.usuario.infrastructure.controller.rest.AuthController;
import com.colegio.shuji.usuario.infrastructure.controller.rest.UsuarioController;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class EndpointAuthorizationTest {

  private static final Set<String> ROLES =
      Arrays.stream(RolNombre.values()).map(Enum::name).collect(java.util.stream.Collectors.toSet());
  private static final Pattern ROLE_EXPRESSION = Pattern.compile("has(?:Any)?Role\\(([^)]*)\\)");
  private static final Pattern ROLE_NAME = Pattern.compile("'([^']+)'|\"([^\"]+)\"");
  private static final List<Class<?>> CONTROLLERS =
      List.of(
          AcademicoController.class,
          AsistenciaController.class,
          ComunicadoController.class,
          ConvivenciaController.class,
          CurriculoController.class,
          HorarioController.class,
          EvaluacionController.class,
          RefuerzoController.class,
          ApoderadoController.class,
          EstudianteController.class,
          MatriculaController.class,
          PortalFamiliarController.class,
          SolicitudMatriculaPublicaController.class,
          SuplenciaController.class,
          DashboardController.class,
          ComprobanteController.class,
          RetornoPagoController.class,
          TesoreriaController.class,
          WebhookController.class,
          AuthController.class,
          UsuarioController.class);

  @Test
  void everyRestEndpointDeclaresItsOwnAuthorizationAndOnlyKnownRoles() {
    var violations = new java.util.ArrayList<String>();
    int endpoints = 0;

    for (Class<?> controller : CONTROLLERS) {
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

  private static boolean isRestEndpoint(Method method) {
    return Arrays.stream(method.getDeclaredAnnotations())
        .anyMatch(annotation -> annotation.annotationType().getName().endsWith("Mapping"));
  }
}
