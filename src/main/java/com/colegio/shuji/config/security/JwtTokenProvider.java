package com.colegio.shuji.config.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Proveedor de servicios criptográficos JWT (JSON Web Tokens) basado en JJWT 0.12.6. Genera, firma
 * y valida tokens mediante algoritmo HMAC-SHA256.
 */
@Slf4j
@Component
public class JwtTokenProvider implements com.colegio.shuji.usuario.application.port.out.TokenPort {

  @Value("${app.jwt.secret:c2h1amlfa2l0YW11cmFfcHJveWVjdG9faW50ZWdyYWRvcl91dHBfMjAyNg==}")
  private String jwtSecret;

  @Value("${app.jwt.expiration-ms:86400000}")
  private long jwtExpirationMs;

  @Value("${app.jwt.refresh-expiration-ms:604800000}")
  private long jwtRefreshExpirationMs;

  private SecretKey key;

  @PostConstruct
  public void init() {
    try {
      byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
      this.key = Keys.hmacShaKeyFor(keyBytes);
    } catch (Exception e) {
      log.warn("El secreto JWT no está en formato Base64 estándar, derivando bytes UTF-8");
      byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
      this.key = Keys.hmacShaKeyFor(keyBytes);
    }
  }

  /**
   * Genera un token de acceso (Access Token) con claims personalizados de usuario y roles.
   *
   * @param userId ID del usuario en base de datos.
   * @param username Nombre de usuario.
   * @param roles Lista de nombres o códigos de roles asignados.
   * @return Token JWT firmado.
   */
  public String generateToken(Long userId, String username, List<String> roles) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

    return Jwts.builder()
        .subject(username)
        .claim("userId", userId)
        .claim("roles", roles)
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(key)
        .compact();
  }

  /**
   * Genera un token de refresco (Refresh Token) de larga duración.
   *
   * @param userId ID del usuario.
   * @param username Nombre de usuario.
   * @return Token JWT de refresco firmado.
   */
  public String generateRefreshToken(Long userId, String username) {
    Date now = new Date();
    Date expiryDate = new Date(now.getTime() + jwtRefreshExpirationMs);

    return Jwts.builder()
        .subject(username)
        .claim("userId", userId)
        .claim("type", "REFRESH")
        .issuedAt(now)
        .expiration(expiryDate)
        .signWith(key)
        .compact();
  }

  /** Obtiene el username a partir de las claims del token. */
  public String getUsernameFromToken(String token) {
    return extractAllClaims(token).getSubject();
  }

  /** Obtiene el ID numérico del usuario del token. */
  public Long getUserIdFromToken(String token) {
    Claims claims = extractAllClaims(token);
    Object userIdObj = claims.get("userId");
    if (userIdObj instanceof Number number) {
      return number.longValue();
    } else if (userIdObj instanceof String str) {
      return Long.parseLong(str);
    }
    return null;
  }

  /** Extrae los roles codificados en el token. */
  @SuppressWarnings("unchecked")
  public List<String> getRolesFromToken(String token) {
    Claims claims = extractAllClaims(token);
    Object rolesObj = claims.get("roles");
    if (rolesObj instanceof List<?> list) {
      return (List<String>) list;
    }
    return Collections.emptyList();
  }

  /** Extrae la fecha de expiración del token. */
  public Date getExpirationDateFromToken(String token) {
    return extractAllClaims(token).getExpiration();
  }

  /**
   * Valida la firma criptográfica y la vigencia temporal del token JWT.
   *
   * @param token Cadena JWT.
   * @return true si es válido, false en caso contrario.
   */
  public boolean validateToken(String token) {
    try {
      Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
      return true;
    } catch (SecurityException | MalformedJwtException e) {
      log.error("Firma JWT inválida o token malformado: {}", e.getMessage());
    } catch (ExpiredJwtException e) {
      log.warn("El token JWT ha expirado: {}", e.getMessage());
    } catch (UnsupportedJwtException e) {
      log.error("Token JWT no soportado: {}", e.getMessage());
    } catch (IllegalArgumentException e) {
      log.error("La cadena de claims JWT está vacía o es nula: {}", e.getMessage());
    } catch (JwtException e) {
      log.error("Error al procesar el token JWT: {}", e.getMessage());
    }
    return false;
  }

  /** Extrae todas las claims verificando la firma con la clave simétrica. */
  public Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public long getJwtExpirationMs() {
    return jwtExpirationMs;
  }

  public long getJwtRefreshExpirationMs() {
    return jwtRefreshExpirationMs;
  }
}
