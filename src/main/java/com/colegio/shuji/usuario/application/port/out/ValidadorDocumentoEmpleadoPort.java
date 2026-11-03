package com.colegio.shuji.usuario.application.port.out;

import com.colegio.shuji.usuario.domain.enums.TipoDocumentoEmpleado;
import com.colegio.shuji.usuario.domain.model.ResultadoValidacionDocumentoEmpleado;
import com.colegio.shuji.usuario.domain.model.Usuario;

public interface ValidadorDocumentoEmpleadoPort {

  ResultadoValidacionDocumentoEmpleado validar(TipoDocumentoEmpleado tipo, byte[] bytes, Usuario usuario);
}
