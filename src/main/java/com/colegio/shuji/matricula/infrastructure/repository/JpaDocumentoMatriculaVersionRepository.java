package com.colegio.shuji.matricula.infrastructure.repository;

import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
import com.colegio.shuji.matricula.infrastructure.entity.DocumentoMatriculaVersionEntity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaDocumentoMatriculaVersionRepository extends JpaRepository<DocumentoMatriculaVersionEntity, UUID> {
  List<DocumentoMatriculaVersionEntity> findBySolicitudIdAndTipoOrderByFechaCargaDesc(UUID solicitudId, TipoDocumentoSolicitud tipo);
}
