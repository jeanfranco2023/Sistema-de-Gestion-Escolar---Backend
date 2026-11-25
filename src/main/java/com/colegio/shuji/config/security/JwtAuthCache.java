package com.colegio.shuji.config.security;

import com.colegio.shuji.usuario.domain.model.Usuario;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Caché TTL ligera en memoria para evitar consultas repetitivas a Supabase (sesión y usuario) en
 * cada petición HTTP autenticada. Se invalida de inmediato ante logout, revocación o cambios del
 * usuario.
 */
@Component
public class JwtAuthCache {
  private static final long TTL_MS = 30_000L;

  private record Entrada<T>(T valor, long expiraEnMs) {
    boolean vigente() {
      return System.currentTimeMillis() < expiraEnMs;
    }
  }

  private final ConcurrentHashMap<Long, Entrada<Boolean>> sesiones = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Long, Entrada<Optional<Usuario>>> usuarios =
      new ConcurrentHashMap<>();

  public Boolean getSessionExists(Long sessionId) {
    if (sessionId == null) return null;
    Entrada<Boolean> e = sesiones.get(sessionId);
    return (e != null && e.vigente()) ? e.valor() : null;
  }

  public void putSessionExists(Long sessionId, boolean exists) {
    if (sessionId == null) return;
    if (exists) {
      sesiones.put(sessionId, new Entrada<>(true, System.currentTimeMillis() + TTL_MS));
    } else {
      sesiones.remove(sessionId);
    }
  }

  public Optional<Usuario> getUser(Long userId) {
    if (userId == null) return null;
    Entrada<Optional<Usuario>> e = usuarios.get(userId);
    return (e != null && e.vigente()) ? e.valor() : null;
  }

  public void putUser(Long userId, Optional<Usuario> usuario) {
    if (userId == null || usuario == null) return;
    if (usuario.isPresent() && usuario.get().estaActivo()) {
      usuarios.put(userId, new Entrada<>(usuario, System.currentTimeMillis() + TTL_MS));
    } else {
      usuarios.remove(userId);
    }
  }

  public void evictUser(Long userId) {
    if (userId != null) {
      usuarios.remove(userId);
    }
  }

  public void evictAllSessions() {
    sesiones.clear();
  }

  public void clearAll() {
    sesiones.clear();
    usuarios.clear();
  }
}
