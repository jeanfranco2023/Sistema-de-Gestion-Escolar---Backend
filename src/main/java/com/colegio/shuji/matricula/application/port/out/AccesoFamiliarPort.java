package com.colegio.shuji.matricula.application.port.out;

public interface AccesoFamiliarPort {
  boolean tieneCustodia(Long estudianteId, Long usuarioId);
}
