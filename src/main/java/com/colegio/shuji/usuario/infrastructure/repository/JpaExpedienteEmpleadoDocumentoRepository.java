package com.colegio.shuji.usuario.infrastructure.repository;

import com.colegio.shuji.usuario.infrastructure.entity.ExpedienteEmpleadoDocumentoEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaExpedienteEmpleadoDocumentoRepository
    extends JpaRepository<ExpedienteEmpleadoDocumentoEntity, Long> {

  List<ExpedienteEmpleadoDocumentoEntity> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

  List<ExpedienteEmpleadoDocumentoEntity> findByUsuarioId(Long usuarioId);

  Optional<ExpedienteEmpleadoDocumentoEntity> findByIdAndUsuarioId(Long id, Long usuarioId);
}
