package com.colegio.shuji.matricula.infrastructure.repository;

import com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaDocumentoEntity;
import com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaDocumentoId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaSolicitudMatriculaDocumentoRepository
    extends JpaRepository<SolicitudMatriculaDocumentoEntity, SolicitudMatriculaDocumentoId> {}
