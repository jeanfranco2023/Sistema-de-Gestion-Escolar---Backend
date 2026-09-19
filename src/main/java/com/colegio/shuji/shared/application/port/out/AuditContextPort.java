package com.colegio.shuji.shared.application.port.out;

/** Contexto del actor para la transacción actual, sin dependencia de persistencia. */
public interface AuditContextPort {
  void setCurrentUserId(Long userId);

  void syncCurrentUserFromSecurityContext();
}
