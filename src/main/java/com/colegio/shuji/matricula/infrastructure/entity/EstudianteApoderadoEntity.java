package com.colegio.shuji.matricula.infrastructure.entity;

import com.colegio.shuji.matricula.domain.enums.Parentesco;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "estudiante_apoderados")
@Getter
@Setter
@NoArgsConstructor
public class EstudianteApoderadoEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "estudiante_id", nullable = false)
  private Long estudianteId;

  @Column(name = "apoderado_id", nullable = false)
  private Long apoderadoId;

  @Enumerated(EnumType.STRING)
  @Column(name = "parentesco", nullable = false, length = 30)
  private Parentesco parentesco;

  @Column(name = "es_responsable_economico", nullable = false)
  private Boolean esResponsableEconomico;

  @Column(name = "tiene_custodia", nullable = false)
  private Boolean tieneCustodia;

  @Column(name = "permite_recojo", nullable = false)
  private Boolean permiteRecojo;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "estudiante_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.matricula.infrastructure.entity.EstudianteEntity relacion0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "apoderado_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.matricula.infrastructure.entity.ApoderadoEntity relacion1;

  @PrePersist
  void inicializar() {
    if (esResponsableEconomico == null) esResponsableEconomico = false;
    if (tieneCustodia == null) tieneCustodia = true;
    if (permiteRecojo == null) permiteRecojo = true;
  }
}
