package com.colegio.shuji.shared.infrastructure.adapter;

import com.colegio.shuji.config.security.UserPrincipal;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class ActorActualAdapter implements ActorActualPort {
  public Long usuarioId() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal p))
      throw new AccessDeniedException("Se requiere autenticación");
    return p.getId();
  }

  public boolean tieneRol(String rol) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    return auth != null
        && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_" + rol));
  }

  public void verificarDocente(Long docenteId) {
    if (!tieneRol("DIRECCION") && !tieneRol("SECRETARIA") && !usuarioId().equals(docenteId))
      throw new AccessDeniedException("La asignación pertenece a otro docente");
  }
}
