package com.colegio.shuji.asistencia.application.service;

import com.colegio.shuji.asistencia.application.dto.in.ImportarLoteBiometricoRequestDto;
import java.util.List;

public record BiometricoImportacionPayload(
    ImportarLoteBiometricoRequestDto solicitud,
    Long usuarioId,
    String nombreUsuario,
    String correoUsuario,
    List<String> roles) {}
