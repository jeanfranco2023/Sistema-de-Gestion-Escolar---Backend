package com.colegio.shuji.config.persistence;

import com.colegio.shuji.shared.application.port.out.AuditContextPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Aspecto AOP para la sincronización automática del actor de auditoría en PostgreSQL (Supabase)
 * al inicio de cualquier transacción orquestada por los servicios de aplicación.
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
@RequiredArgsConstructor
public class TransactionalAuditAspect {

  private final AuditContextPort auditContextPort;

  @Before(
      "within(@org.springframework.stereotype.Service *) && "
          + "(@annotation(org.springframework.transaction.annotation.Transactional) || "
          + "@within(org.springframework.transaction.annotation.Transactional))")
  public void synchronizeAuditActor() {
    try {
      auditContextPort.syncCurrentUserFromSecurityContext();
    } catch (Exception ex) {
      log.debug("No se pudo sincronizar el actor de auditoría en la transacción activa: {}", ex.getMessage());
    }
  }
}
