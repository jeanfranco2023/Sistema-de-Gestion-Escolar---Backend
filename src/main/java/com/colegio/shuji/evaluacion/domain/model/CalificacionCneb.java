package com.colegio.shuji.evaluacion.domain.model;

import com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa;
import java.time.OffsetDateTime;
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
public class CalificacionCneb {
  private Long id;
  private Long matriculaId;
  private Short anioLectivoId;
  private Integer seccionId;
  private Short periodoAcademicoId;
  private Long asignacionDocenteId;
  private Short areaCurricularId;
  private Long docenteUsuarioId;
  private Short competenciaId;
  private CalificacionCualitativa calificacionCualitativa;
  private String conclusionDescriptiva;
  private Boolean sugerenciaIaUtilizada;
  private Boolean requiereRefuerzo;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;

  public boolean necesitaRefuerzo() {
    return calificacionCualitativa == CalificacionCualitativa.C;
  }

  public void actualizar(
      CalificacionCualitativa calificacion, String conclusion, Boolean sugerenciaIa) {
    if (calificacion == null || sugerenciaIa == null)
      throw new com.colegio.shuji.evaluacion.domain.exception.CalificacionInvalidaException(
          "Calificación incompleta");
    this.calificacionCualitativa = calificacion;
    this.conclusionDescriptiva = conclusion;
    this.sugerenciaIaUtilizada = sugerenciaIa;
    this.requiereRefuerzo = necesitaRefuerzo();
  }
}
