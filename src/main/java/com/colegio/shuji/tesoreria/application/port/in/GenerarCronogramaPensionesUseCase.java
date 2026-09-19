package com.colegio.shuji.tesoreria.application.port.in;

import com.colegio.shuji.tesoreria.application.dto.in.*;
import com.colegio.shuji.tesoreria.application.dto.out.*;
import java.util.*;

public interface GenerarCronogramaPensionesUseCase {
  List<ObligacionResponseDto> generarCronograma(GenerarObligacionesAnualesRequestDto r);

  List<ObligacionResponseDto> obligacionesMatricula(Long id);

  EstadoCuentaEstudianteResponseDto estadoCuenta(Long estudianteId);
}
