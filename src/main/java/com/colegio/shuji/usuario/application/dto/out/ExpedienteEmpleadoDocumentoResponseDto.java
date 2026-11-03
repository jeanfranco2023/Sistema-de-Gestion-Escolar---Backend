package com.colegio.shuji.usuario.application.dto.out;

import com.colegio.shuji.usuario.domain.enums.EstadoValidacionIaEmpleado;
import com.colegio.shuji.usuario.domain.enums.TipoDocumentoEmpleado;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "Detalle de documento del expediente de empleado y su dictamen IA")
public record ExpedienteEmpleadoDocumentoResponseDto(
    Long id,
    Long usuarioId,
    TipoDocumentoEmpleado tipoDocumento,
    String nombreArchivo,
    String rutaObjeto,
    String tipoContenido,
    Long tamanoBytes,
    EstadoValidacionIaEmpleado estadoIa,
    String observacionesIa,
    String metadatosIa,
    OffsetDateTime fechaCreacion,
    OffsetDateTime fechaValidacionIa
) {}
