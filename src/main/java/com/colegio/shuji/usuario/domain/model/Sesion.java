package com.colegio.shuji.usuario.domain.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Modelo puro del Dominio que representa una sesión activa y su token de refresco. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(of = "id")
public class Sesion {

  private Long id;
  private Long usuarioId;
  private String tokenHash;
  private String ipAddress;
  private String userAgent;
  private LocalDateTime expiraAt;
  private LocalDateTime createdAt;

  public boolean estaExpirada(LocalDateTime ahora) {
    if (expiraAt == null || ahora == null) {
      return true;
    }
    return !ahora.isBefore(expiraAt);
  }

  public void renovar(
      String hash, String ip, String agente, LocalDateTime ahora, LocalDateTime vencimiento) {
    if (estaExpirada(ahora)
        || hash == null
        || hash.isBlank()
        || vencimiento == null
        || !vencimiento.isAfter(ahora))
      throw new com.colegio.shuji.shared.domain.exception.BusinessException(
          "Renovación de sesión inválida");
    tokenHash = hash;
    ipAddress = ip;
    userAgent = agente;
    expiraAt = vencimiento;
  }
}
