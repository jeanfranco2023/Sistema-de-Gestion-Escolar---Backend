package com.colegio.shuji.curriculo.domain.model;

import com.colegio.shuji.curriculo.domain.enums.DiaSemana;
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
public class HorarioSeccion {
  private Long id;
  private Short anioLectivoId;
  private Integer seccionId;
  private Long docenteUsuarioId;
  private Long asignacionDocenteId;
  private Short diaSemana;
  private Short bloqueHorarioId;

  public void reprogramar(Short nuevoDia, Short nuevoBloqueId) {
    this.diaSemana = nuevoDia;
    this.bloqueHorarioId = nuevoBloqueId;
  }

  public void reprogramar(DiaSemana dia, Short nuevoBloqueId) {
    this.diaSemana = dia != null ? dia.getCodigo() : null;
    this.bloqueHorarioId = nuevoBloqueId;
  }
}
