package com.colegio.shuji.config.persistence;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
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
    if (userId == null || !TransactionSynchronizationManager.isActualTransactionActive()) return;
    establecer(userId, userId.toString());
  }

  private void establecer(Long id, String valor) {
    entityManager
        .createQuery(
            "select function('set_config', 'app.current_user_id', :valor, true) from UsuarioEntity"
                + " u where u.id=:id",
            String.class)
        .setParameter("valor", valor)
        .setParameter("id", id)
        .getResultList();
  }

  public void clearCurrentUserId() {
    if (!TransactionSynchronizationManager.isActualTransactionActive()) return;
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof UserPrincipalHolder holder)
      establecer(holder.getUserId(), "");
  }

  public void syncCurrentUserFromSecurityContext() {
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
