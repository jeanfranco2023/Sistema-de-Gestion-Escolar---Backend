package com.colegio.shuji.comunicado.domain.model;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComunicadoOficial {
  private Long id;
  private UUID uuid;
  private Long remitenteUsuarioId;
  private String titulo;
  private String contenido;
  private Boolean requiereAcuse;
  private OffsetDateTime fechaPublicacion;
  private Short anioLectivoId;
  private Short nivelId;
  private Integer seccionId;

  public void actualizar(String nuevoTitulo, String nuevoContenido) {
    this.titulo = nuevoTitulo;
    this.contenido = nuevoContenido;
  }

  public boolean esGeneral() {
    return nivelId == null && seccionId == null;
  }

  public boolean esPorNivel() {
    return nivelId != null && seccionId == null;
  }

  public boolean esPorSeccion() {
    return seccionId != null;
  }

  public void publicar(Long remitenteId) {
    if (id != null
        || remitenteId == null
        || titulo == null
        || titulo.isBlank()
        || contenido == null
        || contenido.isBlank())
      throw new com.colegio.shuji.shared.domain.exception.BusinessException(
          "Comunicado incompleto o ya publicado");
    remitenteUsuarioId = remitenteId;
    fechaPublicacion = OffsetDateTime.now();
  }
}
