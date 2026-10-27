package com.colegio.shuji.matricula.application.dto.out;

import com.colegio.shuji.matricula.domain.enums.EstadoFirmaDigital;
import java.time.OffsetDateTime;
import java.util.UUID;

public record VersionDocumentoMatriculaResponseDto(
    UUID versionId, String sha256, String mimeType, long tamanoBytes, OffsetDateTime fechaCarga,
    Boolean iaValida, Boolean dniApiCoincide, String observacionIa, EstadoFirmaDigital estadoFirma, String detalleFirma,
    String certificadoSha256, Boolean vinculoParentalCoincide) {}
