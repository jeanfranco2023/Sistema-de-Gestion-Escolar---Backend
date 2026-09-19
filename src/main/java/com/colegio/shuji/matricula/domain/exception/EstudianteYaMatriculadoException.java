package com.colegio.shuji.matricula.domain.exception;

import com.colegio.shuji.shared.domain.exception.BusinessException;

public class EstudianteYaMatriculadoException extends BusinessException {
  public EstudianteYaMatriculadoException(String mensaje) {
    super(mensaje, "ESTUDIANTE_YA_MATRICULADO");
  }
}
