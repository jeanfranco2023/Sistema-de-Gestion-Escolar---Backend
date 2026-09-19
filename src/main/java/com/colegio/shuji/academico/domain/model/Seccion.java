package com.colegio.shuji.academico.domain.model;

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
public class Seccion {
  private Integer id;
  private Short anioLectivoId;
  private Short gradoId;
  private Short nivelId;
  private String letra;
  private Short cupoMaximo;
  private Short vacantesOcupadas;
  private String aulaFisica;

  public int vacantesDisponibles() {
    return Math.max(0, cupoMaximo - (vacantesOcupadas == null ? 0 : vacantesOcupadas));
  }

  public void verificarCupo() {
    if (vacantesDisponibles() <= 0) {
      throw new com.colegio.shuji.academico.domain.exception.CupoAgotadoException(
          "Cupo agotado para la sección seleccionada");
    }
  }

  public void ocuparVacante() {
    verificarCupo();
    this.vacantesOcupadas =
        (short) ((this.vacantesOcupadas == null ? 0 : this.vacantesOcupadas) + 1);
  }

  public void liberarVacante() {
    if (this.vacantesOcupadas != null && this.vacantesOcupadas > 0) {
      this.vacantesOcupadas = (short) (this.vacantesOcupadas - 1);
    }
  }

  public boolean tieneDisponibilidad() {
    return vacantesDisponibles() > 0;
  }

  public void actualizar(String nuevaLetra, Short nuevoCupo, String nuevaAula) {
    this.letra = nuevaLetra;
    this.cupoMaximo = nuevoCupo;
    this.aulaFisica = nuevaAula;
  }

  public void normalizarAula() {
    aulaFisica = aulaFisica == null || aulaFisica.isBlank() ? null : aulaFisica.trim();
  }
}
