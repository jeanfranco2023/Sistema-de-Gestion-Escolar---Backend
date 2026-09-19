package com.colegio.shuji.asistencia.application.port.in;

import com.colegio.shuji.asistencia.application.dto.in.ImportarLoteBiometricoRequestDto;
import com.colegio.shuji.asistencia.application.dto.out.LoteBiometricoResponseDto;

public interface ProcesarBiometricoUseCase {
  LoteBiometricoResponseDto importar(ImportarLoteBiometricoRequestDto r);
}
