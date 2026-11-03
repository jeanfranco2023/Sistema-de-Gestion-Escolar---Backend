package com.colegio.shuji.config.security;

import com.colegio.shuji.shared.infrastructure.audit.AuditoriaAccesoService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@RequiredArgsConstructor
@Component
public class AuditoriaAccesoFilter extends OncePerRequestFilter {

  private final AuditoriaAccesoService auditoria;

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    return !request.getRequestURI().startsWith("/api/v1/")
        || "OPTIONS".equalsIgnoreCase(request.getMethod());
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    long inicio = System.nanoTime();
    int estado = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
    try {
      filterChain.doFilter(request, response);
      estado = response.getStatus();
    } catch (IOException | ServletException | RuntimeException ex) {
      throw ex;
    } finally {
      long duracionMs = (System.nanoTime() - inicio) / 1_000_000;
      try {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long usuarioId =
            authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal
                ? principal.getId()
                : null;
        auditoria.registrar(
            request.getMethod(),
            request.getRequestURI(),
            estado,
            request.getRemoteAddr(),
            request.getHeader("User-Agent"),
            duracionMs,
            usuarioId,
            authentication);
      } catch (RuntimeException ex) {
        log.error("No se pudo persistir el registro de acceso a la API", ex);
      }
    }
  }
}
