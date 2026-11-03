package com.colegio.shuji.usuario.domain.model;

import com.colegio.shuji.usuario.domain.enums.EstadoValidacionIaEmpleado;
import com.colegio.shuji.usuario.domain.enums.TipoDocumentoEmpleado;
import java.time.OffsetDateTime;

public record ExpedienteEmpleadoDocumento(
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
    OffsetDateTime fechaValidacionIa) {}
