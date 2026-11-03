package com.colegio.shuji.usuario.infrastructure.adapter;

import com.colegio.shuji.usuario.application.port.out.ExpedienteEmpleadoRepositoryPort;
import com.colegio.shuji.usuario.domain.model.ExpedienteEmpleadoDocumento;
import com.colegio.shuji.usuario.infrastructure.entity.ExpedienteEmpleadoDocumentoEntity;
import com.colegio.shuji.usuario.infrastructure.repository.JpaExpedienteEmpleadoDocumentoRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaExpedienteEmpleadoRepositoryAdapter implements ExpedienteEmpleadoRepositoryPort {

  private final JpaExpedienteEmpleadoDocumentoRepository repository;

  public JpaExpedienteEmpleadoRepositoryAdapter(JpaExpedienteEmpleadoDocumentoRepository repository) {
    this.repository = repository;
  }

  @Override
  public ExpedienteEmpleadoDocumento guardar(ExpedienteEmpleadoDocumento documento) {
    return toDomain(repository.save(toEntity(documento)));
  }

  @Override
  public List<ExpedienteEmpleadoDocumento> listarPorUsuarioId(Long usuarioId) {
    return repository.findByUsuarioIdOrderByFechaCreacionDesc(usuarioId).stream()
        .map(JpaExpedienteEmpleadoRepositoryAdapter::toDomain)
        .toList();
  }

  @Override
  public Optional<ExpedienteEmpleadoDocumento> buscarPorIdYUsuario(Long documentoId, Long usuarioId) {
    return repository.findByIdAndUsuarioId(documentoId, usuarioId).map(JpaExpedienteEmpleadoRepositoryAdapter::toDomain);
  }

  private static ExpedienteEmpleadoDocumentoEntity toEntity(ExpedienteEmpleadoDocumento documento) {
    return ExpedienteEmpleadoDocumentoEntity.builder()
        .id(documento.id())
        .usuarioId(documento.usuarioId())
        .tipoDocumento(documento.tipoDocumento())
        .nombreArchivo(documento.nombreArchivo())
        .rutaObjeto(documento.rutaObjeto())
        .tipoContenido(documento.tipoContenido())
        .tamanoBytes(documento.tamanoBytes())
        .estadoIa(documento.estadoIa())
        .observacionesIa(documento.observacionesIa())
        .metadatosIa(documento.metadatosIa())
        .fechaCreacion(documento.fechaCreacion())
        .fechaValidacionIa(documento.fechaValidacionIa())
        .build();
  }

  private static ExpedienteEmpleadoDocumento toDomain(ExpedienteEmpleadoDocumentoEntity entity) {
    return new ExpedienteEmpleadoDocumento(
        entity.getId(),
        entity.getUsuarioId(),
        entity.getTipoDocumento(),
        entity.getNombreArchivo(),
        entity.getRutaObjeto(),
        entity.getTipoContenido(),
        entity.getTamanoBytes(),
        entity.getEstadoIa(),
        entity.getObservacionesIa(),
        entity.getMetadatosIa(),
        entity.getFechaCreacion(),
        entity.getFechaValidacionIa());
  }
}
