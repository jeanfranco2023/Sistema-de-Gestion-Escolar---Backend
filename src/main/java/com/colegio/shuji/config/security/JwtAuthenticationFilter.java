package com.colegio.shuji.config.security;

import com.colegio.shuji.config.persistence.SupabaseAuditInterceptor;
import com.colegio.shuji.usuario.application.port.out.UserRepositoryPort;
import com.colegio.shuji.usuario.domain.model.Rol;
import com.colegio.shuji.usuario.domain.model.Usuario;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filtro de autenticación por solicitud HTTP que intercepta las peticiones, extrae el token Bearer
 * JWT, lo valida y establece el contexto de seguridad.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtTokenProvider tokenProvider;
  private final SupabaseAuditInterceptor supabaseAuditInterceptor;
  private final UserRepositoryPort userRepositoryPort;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {

    try {
      String jwt = getJwtFromRequest(request);

      if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
        String username = tokenProvider.getUsernameFromToken(jwt);
        Long userId = tokenProvider.getUserIdFromToken(jwt);

        if (userId != null) {
          Optional<Usuario> usuarioOpt = userRepositoryPort.obtenerPorId(userId);
          if (usuarioOpt.isEmpty() || !usuarioOpt.get().estaActivo()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"status\":401,\"message\":\"Usuario inactivo o revocado\"}");
            return;
          }

          Usuario usuario = usuarioOpt.get();

          List<SimpleGrantedAuthority> authorities =
              (usuario.getRoles() == null ? Collections.<Rol>emptySet() : usuario.getRoles())
                  .stream()
                  .map(Rol::getCodigo)
                  .filter(Objects::nonNull)
                  .map(c -> c.startsWith("ROLE_") ? c : "ROLE_" + c)
                  .map(SimpleGrantedAuthority::new)
                  .toList();

          UserPrincipal principal =
              UserPrincipal.builder()
                  .id(userId)
                  .username(username)
                  .active(usuario.estaActivo())
                  .authorities(authorities)
                  .build();

          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(principal, null, authorities);
          authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

          SecurityContextHolder.getContext().setAuthentication(authentication);

          // Configura opcionalmente el actor en la persistencia si hay userId disponible
          supabaseAuditInterceptor.setCurrentUserId(userId);
        }
      }
    } catch (Exception ex) {
      log.error(
          "No se pudo establecer la autenticación del usuario en el contexto de seguridad", ex);
    }

    filterChain.doFilter(request, response);
  }

  private String getJwtFromRequest(HttpServletRequest request) {
    String bearerToken = request.getHeader("Authorization");
    if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
      return bearerToken.substring(7);
    }
    return null;
  }
}
