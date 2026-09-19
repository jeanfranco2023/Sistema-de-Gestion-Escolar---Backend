package com.colegio.shuji.asistencia.application.port.out;

import com.colegio.shuji.asistencia.domain.model.LoteBiometrico;
import java.util.*;

public interface BiometricoRepositoryPort {
  LoteBiometrico guardar(LoteBiometrico valor);

  Optional<LoteBiometrico> buscarPorId(Long id);

  Optional<LoteBiometrico> buscarPorHashContenido(String hashContenido);

  Optional<LoteBiometrico> bloquearPorId(Long id);

  List<LoteBiometrico> listar();

  List<LoteBiometrico> buscarPorImportadoPorUsuarioId(Long valor);
}
