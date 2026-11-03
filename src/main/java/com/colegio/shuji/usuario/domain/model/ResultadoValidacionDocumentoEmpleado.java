package com.colegio.shuji.usuario.domain.model;

import com.colegio.shuji.usuario.domain.enums.EstadoValidacionIaEmpleado;

public record ResultadoValidacionDocumentoEmpleado(
    EstadoValidacionIaEmpleado estado,
    String observacion,
    boolean identidadCoincide,
    String motorValidacion,
    int paginas) {}
