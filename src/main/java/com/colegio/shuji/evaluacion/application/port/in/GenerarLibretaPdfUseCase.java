package com.colegio.shuji.evaluacion.application.port.in;

public interface GenerarLibretaPdfUseCase {
  byte[] generar(Long matriculaId);
}
