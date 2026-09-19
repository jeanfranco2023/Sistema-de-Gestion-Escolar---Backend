package com.colegio.shuji.comunicado.infrastructure.entity;

import jakarta.persistence.*;
import java.time.*;
import lombok.*;

@Entity
@Table(name = "comunicado_destinatarios")
@Getter
@Setter
@NoArgsConstructor
public class ComunicadoDestinatarioEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "comunicado_id", nullable = false)
  private Long comunicadoId;

  @Column(name = "apoderado_id", nullable = false)
  private Long apoderadoId;

  @Column(name = "leido", nullable = false)
  private Boolean leido;

  @Column(name = "fecha_lectura")
  private OffsetDateTime fechaLectura;

  @Column(name = "acuse_confirmado", nullable = false)
  private Boolean acuseConfirmado;

  @Column(name = "fecha_acuse")
  private OffsetDateTime fechaAcuse;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "comunicado_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.comunicado.infrastructure.entity.ComunicadoOficialEntity relacion0;

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
    if (leido == null) leido = false;
    if (acuseConfirmado == null) acuseConfirmado = false;
  }
}
