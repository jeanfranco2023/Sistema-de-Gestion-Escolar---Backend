package com.colegio.shuji.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.colegio.shuji.config.security.JwtAuthenticationFilter;
import com.colegio.shuji.config.security.RateLimitingFilter;
import com.colegio.shuji.config.security.SecurityConfig;
import com.colegio.shuji.shared.infrastructure.audit.AuditoriaAccesoService;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.context.annotation.Import;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.cors.CorsConfigurationSource;

@WebMvcTest(controllers = SecurityErrorDispatchTest.ErrorProbeController.class)
@Import({SecurityConfig.class, SecurityErrorDispatchTest.ErrorProbeController.class})
class SecurityErrorDispatchTest {
  @Autowired private MockMvc mvc;
  @MockitoBean private JwtAuthenticationFilter jwtFilter;
  @MockitoBean private RateLimitingFilter rateFilter;
  @MockitoBean private AuditoriaAccesoService auditoriaAcceso;
  @MockitoBean(name = "corsConfigurationSource") private CorsConfigurationSource cors;

  @BeforeEach
  void passThroughFilters() throws Exception {
    doAnswer(invocation -> {
      FilterChain chain = invocation.getArgument(2);
      chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
      return null;
    }).when(jwtFilter).doFilter(any(), any(), any());
    doAnswer(invocation -> {
      FilterChain chain = invocation.getArgument(2);
      chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
      return null;
    }).when(rateFilter).doFilter(any(), any(), any());
  }

  @Test
  void internalErrorDispatchPreservesTheServerErrorInsteadOfReturningUnauthorized() throws Exception {
    mvc.perform(get("/error").with(request -> {
      request.setDispatcherType(DispatcherType.ERROR);
      return request;
    }))
        .andExpect(status().isInternalServerError());
  }

  @Test
  void normalRequestsRemainProtected() throws Exception {
    mvc.perform(get("/error")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/v1/qa/private")).andExpect(status().isUnauthorized());
  }

  @Test
  void refreshDoesNotRequireAnAccessToken() throws Exception {
    mvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isOk());
  }

  @RestController
  static class ErrorProbeController implements ErrorController {
    @GetMapping("/error")
    ResponseEntity<Void> error() {
      return ResponseEntity.internalServerError().build();
    }

    @GetMapping("/api/v1/qa/private")
    void protectedResource() {}

    @PostMapping("/api/v1/auth/refresh")
    void refresh() {}
  }
}
