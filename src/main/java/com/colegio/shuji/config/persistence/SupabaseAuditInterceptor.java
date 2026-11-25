package com.colegio.shuji.config.persistence;

import jakarta.persistence.EntityManager;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Contexto transaccional PostgreSQL invocado mediante FUNCTION de JPQL. */
@Component
@RequiredArgsConstructor
public class SupabaseAuditInterceptor
    implements com.colegio.shuji.shared.application.port.out.AuditContextPort {
  private final EntityManager entityManager;

  public void setCurrentUserId(Long userId) {
    if (userId == null
        || !TransactionSynchronizationManager.isActualTransactionActive()
        || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) return;
    establecer(userId, userId.toString());
  }

  private void establecer(Long id, String valor) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    String nombre = auth == null || !auth.isAuthenticated() || valor.isBlank() ? "" : auth.getName();
    String roles =
        auth == null || !auth.isAuthenticated() || valor.isBlank()
            ? ""
            : auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(rol -> rol.startsWith("ROLE_") ? rol.substring(5) : rol)
                .filter(Objects::nonNull)
                .sorted()
                .reduce((primero, segundo) -> primero + "," + segundo)
                .orElse("");
    entityManager
        .createQuery(
            "select function('set_config', 'app.current_user_id', :valor, true), "
                + "function('set_config', 'app.current_username', :nombre, true), "
                + "function('set_config', 'app.current_roles', :roles, true) "
                + "from UsuarioEntity u where u.id=:id")
        .setParameter("valor", valor)
        .setParameter("nombre", nombre)
        .setParameter("roles", roles)
        .setParameter("id", id)
        .getResultList();
  }

  public void clearCurrentUserId() {
    if (!TransactionSynchronizationManager.isActualTransactionActive()
        || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) return;
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof UserPrincipalHolder holder)
      establecer(holder.getUserId(), "");
  }

  public void syncCurrentUserFromSecurityContext() {
    if (!TransactionSynchronizationManager.isActualTransactionActive()
        || TransactionSynchronizationManager.isCurrentTransactionReadOnly()) return;
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null
        && auth.isAuthenticated()
        && auth.getPrincipal() instanceof UserPrincipalHolder holder)
      setCurrentUserId(holder.getUserId());
  }

  public interface UserPrincipalHolder {
    Long getUserId();
  }
}
