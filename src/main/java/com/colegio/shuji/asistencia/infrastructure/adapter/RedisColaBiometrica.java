package com.colegio.shuji.asistencia.infrastructure.adapter;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisColaBiometrica {
  private static final String USUARIOS = "shuji:biometrico:rr:usuarios";
  private static final String PENDIENTES = "shuji:biometrico:rr:pendientes";
  private static final String SECUENCIA = "shuji:biometrico:rr:secuencia";
  private static final String TRABAJOS = "shuji:biometrico:rr:trabajos";
  private static final String COLA_USUARIO_PREFIX = "shuji:biometrico:rr:usuario:";

  private static final DefaultRedisScript<Long> ENCOLAR = new DefaultRedisScript<>("""
      if redis.call('SISMEMBER', KEYS[4], ARGV[2]) == 1 then return 0 end
      local pendientes = tonumber(redis.call('GET', KEYS[2]) or '0')
      if pendientes >= tonumber(ARGV[3]) then return -1 end
      local usuario = ARGV[1]
      local cola = ARGV[4] .. usuario
      redis.call('RPUSH', cola, ARGV[2])
      redis.call('SADD', KEYS[4], ARGV[2])
      if redis.call('ZSCORE', KEYS[1], usuario) == false then
        local turno = redis.call('INCR', KEYS[3])
        redis.call('ZADD', KEYS[1], turno, usuario)
      end
      redis.call('INCR', KEYS[2])
      return 1
      """, Long.class);

  private static final DefaultRedisScript<List> TOMAR_SIGUIENTE = new DefaultRedisScript<>("""
      local usuarios = redis.call('ZRANGE', KEYS[1], 0, 0)
      if #usuarios == 0 then return {} end
      local usuario = usuarios[1]
      local cola = ARGV[1] .. usuario
      local trabajo = redis.call('LPOP', cola)
      if not trabajo then
        redis.call('ZREM', KEYS[1], usuario)
        return {}
      end
      redis.call('SREM', KEYS[4], trabajo)
      redis.call('DECR', KEYS[2])
      if redis.call('LLEN', cola) > 0 then
        local turno = redis.call('INCR', KEYS[3])
        redis.call('ZADD', KEYS[1], turno, usuario)
      else
        redis.call('ZREM', KEYS[1], usuario)
      end
      return {usuario, trabajo}
      """, List.class);

  private final StringRedisTemplate redis;

  @Value("${app.biometrico.max-pending:20}")
  private int maxPendientes;

  public boolean encolar(Long usuarioId, UUID trabajoId) {
    Long resultado = redis.execute(
        ENCOLAR,
        List.of(USUARIOS, PENDIENTES, SECUENCIA, TRABAJOS),
        usuarioId.toString(),
        trabajoId.toString(),
        Integer.toString(maxPendientes),
        COLA_USUARIO_PREFIX);
    return resultado != null && resultado >= 0;
  }

  public TrabajoEnCola tomarSiguiente() {
    List<?> resultado = redis.execute(
        TOMAR_SIGUIENTE,
        List.of(USUARIOS, PENDIENTES, SECUENCIA, TRABAJOS),
        COLA_USUARIO_PREFIX);
    if (resultado == null || resultado.size() < 2) return null;
    return new TrabajoEnCola(Long.valueOf(resultado.get(0).toString()), UUID.fromString(resultado.get(1).toString()));
  }

  public record TrabajoEnCola(Long usuarioId, UUID trabajoId) {}
}
