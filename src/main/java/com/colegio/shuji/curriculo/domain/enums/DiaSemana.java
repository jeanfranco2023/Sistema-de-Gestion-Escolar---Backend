package com.colegio.shuji.curriculo.domain.enums;

public enum DiaSemana {
  LUNES(1),
  MARTES(2),
  MIERCOLES(3),
  JUEVES(4),
  VIERNES(5);
  private final short codigo;

  DiaSemana(int codigo) {
    this.codigo = (short) codigo;
  }

  public short getCodigo() {
    return codigo;
  }
}
