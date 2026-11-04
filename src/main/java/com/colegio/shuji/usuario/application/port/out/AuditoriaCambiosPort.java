package com.colegio.shuji.usuario.application.port.out;

import com.colegio.shuji.usuario.domain.model.CambioAuditoria;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditoriaCambiosPort {
  List<CambioAuditoria> listarRecientes(int limite);

  Page<CambioAuditoria> filtrar(
      Instant fechaInicio,
      Instant fechaFin,
      Long usuarioId,
      String tabla,
      Pageable pageable);
}
