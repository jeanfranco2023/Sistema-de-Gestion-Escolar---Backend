package com.colegio.shuji.curriculo.domain.model;

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
public class AsignacionDocente {
  private Long id;
  private Long docenteUsuarioId;
  private Integer seccionId;
  private Short anioLectivoId;
  private Short nivelId;
  private Short areaCurricularId;

  public void reasignarDocente(Long nuevoDocenteUsuarioId) {
    this.docenteUsuarioId = nuevoDocenteUsuarioId;
  }
}
