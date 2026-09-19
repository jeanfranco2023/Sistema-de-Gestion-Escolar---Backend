package com.colegio.shuji.tesoreria.application.port.in;

import com.colegio.shuji.tesoreria.application.dto.in.GenerarObligacionesAnualesRequestDto;
import com.colegio.shuji.tesoreria.application.dto.out.EstadoCuentaEstudianteResponseDto;
import com.colegio.shuji.tesoreria.application.dto.out.ObligacionResponseDto;
import java.util.List;

public interface GenerarCronogramaPensionesUseCase {
  List<ObligacionResponseDto> generarCronograma(GenerarObligacionesAnualesRequestDto r);

  List<ObligacionResponseDto> obligacionesMatricula(Long id);

  EstadoCuentaEstudianteResponseDto estadoCuenta(Long estudianteId);
}
