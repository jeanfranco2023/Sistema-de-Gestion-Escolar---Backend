package com.colegio.shuji.matricula.infrastructure.entity;

import com.colegio.shuji.matricula.domain.enums.OrigenRegistro;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "apoderados")
@Getter
@Setter
@NoArgsConstructor
public class ApoderadoEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;


  @Column(name = "usuario_id")
  private Long usuarioId;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_documento", nullable = false, length = 3)
  private TipoDocumento tipoDocumento;

  @Column(name = "numero_documento", nullable = false, length = 8)
  private String numeroDocumento;

  @Column(name = "nombres_apoderado", nullable = false, length = 20)
  private String nombres;

  @Column(name = "apellido_paterno_apoderado", nullable = false, length = 20)
  private String apellidoPaterno;

  @Column(name = "apellido_materno_apoderado", nullable = false, length = 20)
  private String apellidoMaterno;

  @Column(name = "celular", nullable = false, length = 9)
  private String celular;

  @Column(name = "correo_electronico", length = 100)
  private String email;

  @Column(name = "direccion", nullable = false, length = 150)
  private String direccion;

  @Column(name = "ubigeo_inei", nullable = false, length = 6)
  private String ubigeoInei;

  @Column(name = "validado_reniec", nullable = false)
  private Boolean validadoReniec;

  @Enumerated(EnumType.STRING)
  @Column(name = "origen_registro", nullable = false, length = 20)
  private OrigenRegistro origenRegistro;

  @Column(name = "fecha_creacion", nullable = false)
  private OffsetDateTime fechaCreacion;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumns({
    @JoinColumn(
        name = "usuario_id",
        referencedColumnName = "id",
        insertable = false,
        updatable = false)
  })
  private com.colegio.shuji.usuario.infrastructure.entity.UsuarioEntity relacion0;

  @PrePersist
  void inicializar() {
    if (tipoDocumento == null) tipoDocumento = TipoDocumento.DNI;
    if (validadoReniec == null) validadoReniec = false;
    if (origenRegistro == null) origenRegistro = OrigenRegistro.RENIEC_API;
    if (fechaCreacion == null) fechaCreacion = OffsetDateTime.now(ZoneOffset.UTC);
  }
}
