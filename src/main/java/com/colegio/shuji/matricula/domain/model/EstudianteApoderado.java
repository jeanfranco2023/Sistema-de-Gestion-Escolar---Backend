package com.colegio.shuji.matricula.domain.model;

import com.colegio.shuji.matricula.domain.enums.Parentesco;
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
public class EstudianteApoderado {
  private Long id;
  private Long estudianteId;
  private Long apoderadoId;
  private Parentesco parentesco;
  private Boolean esResponsableEconomico;
  private Boolean tieneCustodia;
  private Boolean permiteRecojo;

  public void actualizarPermisos(
      Parentesco parentesco, Boolean esResponsable, Boolean tieneCustodia, Boolean permiteRecojo) {
    this.parentesco = parentesco;
    this.esResponsableEconomico = esResponsable;
    this.tieneCustodia = tieneCustodia;
    this.permiteRecojo = permiteRecojo;
  }
}
