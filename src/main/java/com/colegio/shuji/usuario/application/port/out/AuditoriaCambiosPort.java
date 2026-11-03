package com.colegio.shuji.usuario.application.port.out;

import com.colegio.shuji.usuario.domain.model.CambioAuditoria;
import java.util.List;

public interface AuditoriaCambiosPort {
  List<CambioAuditoria> listarRecientes(int limite);
}
