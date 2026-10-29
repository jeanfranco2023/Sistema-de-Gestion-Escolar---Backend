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
public class MallaHorariaGrado {
  private Long id;
  private Short anioLectivoId;
  private Short gradoId;
  private Short areaCurricularId;
  private Short diaSemana;
  private Short bloqueHorarioId;
  private Boolean activo;

  public void programar(DiaSemana dia, Short bloqueId) {
    this.diaSemana = dia.getCodigo();
    this.bloqueHorarioId = bloqueId;
    this.activo = true;
  }

  public void cambiarArea(Short areaId) { this.areaCurricularId = areaId; }

  public void retirar() { this.activo = false; }

  public boolean estaActivo() { return Boolean.TRUE.equals(activo); }
}
