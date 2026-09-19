package com.colegio.shuji.asistencia.application.port.in;

import com.colegio.shuji.asistencia.application.dto.in.*;
import com.colegio.shuji.asistencia.application.dto.out.*;

public interface ProcesarBiometricoUseCase {
  LoteBiometricoResponseDto importar(ImportarLoteBiometricoRequestDto r);
}
