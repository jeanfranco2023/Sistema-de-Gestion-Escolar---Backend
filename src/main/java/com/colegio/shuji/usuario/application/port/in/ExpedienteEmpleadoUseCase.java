package com.colegio.shuji.usuario.application.port.in;

import com.colegio.shuji.usuario.application.dto.out.ExpedienteEmpleadoDocumentoResponseDto;
import com.colegio.shuji.usuario.domain.enums.TipoDocumentoEmpleado;
import java.util.List;

public interface ExpedienteEmpleadoUseCase {

  ExpedienteEmpleadoDocumentoResponseDto subirYValidarDocumento(
      Long usuarioId,
      TipoDocumentoEmpleado tipoDocumento,
      String nombreArchivo,
      String tipoContenido,
      byte[] contenido);

  List<ExpedienteEmpleadoDocumentoResponseDto> listarDocumentos(Long usuarioId);

  String urlDescargaDocumento(Long usuarioId, Long documentoId);
}
