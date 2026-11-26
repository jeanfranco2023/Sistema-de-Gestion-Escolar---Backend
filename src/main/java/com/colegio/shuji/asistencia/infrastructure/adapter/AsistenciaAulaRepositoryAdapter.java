package com.colegio.shuji.asistencia.infrastructure.adapter;

import com.colegio.shuji.asistencia.application.port.out.AsistenciaAulaRepositoryPort;
import com.colegio.shuji.asistencia.application.port.out.SeccionAsistencia;
import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import com.colegio.shuji.asistencia.domain.model.AsistenciaAula;
import com.colegio.shuji.asistencia.infrastructure.entity.AsistenciaAulaEntity;
import com.colegio.shuji.asistencia.infrastructure.repository.JpaAsistenciaAulaRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class AsistenciaAulaRepositoryAdapter implements AsistenciaAulaRepositoryPort {
  private final JpaAsistenciaAulaRepository repository;
  private final AsistenciaAulaPersistenceMapper mapper;
  private final EntityManager entityManager;
  private final com.colegio.shuji.config.persistence.SupabaseAuditInterceptor auditoria;

  public AsistenciaAula guardar(AsistenciaAula valor) {
    auditoria.syncCurrentUserFromSecurityContext();
    var entity =
        valor.getId() == null
            ? mapper.toEntity(valor)
            : repository
                .findById(valor.getId())
                .orElseThrow(
                    () ->
                        new com.colegio.shuji.shared.domain.exception.BusinessException(
                            "Registro no encontrado"));
    mapper.actualizar(valor, entity);
    repository.saveAndFlush(entity);
    entityManager.refresh(entity);
    return mapper.toDomain(entity);
  }

  public List<AsistenciaAula> guardarTodos(Collection<AsistenciaAula> valores) {
    if (valores == null || valores.isEmpty()) return List.of();
    auditoria.syncCurrentUserFromSecurityContext();
    var idsExistentes = valores.stream().map(AsistenciaAula::getId).filter(java.util.Objects::nonNull).distinct().toList();
    var entidadesExistentes = repository.findAllById(idsExistentes).stream()
        .collect(Collectors.toMap(AsistenciaAulaEntity::getId, Function.identity()));
    var entidades = valores.stream().map(valor -> {
      var entidad = valor.getId() == null ? mapper.toEntity(valor) : entidadesExistentes.get(valor.getId());
      if (entidad == null) {
        throw new com.colegio.shuji.shared.domain.exception.BusinessException("Registro no encontrado");
      }
      mapper.actualizar(valor, entidad);
      return entidad;
    }).toList();
    return repository.saveAll(entidades).stream().map(mapper::toDomain).toList();
  }

  public Optional<AsistenciaAula> buscarPorId(Long id) {
    return repository.findById(id).map(mapper::toDomain);
  }

  public Optional<AsistenciaAula> bloquearPorId(Long id) {
    return repository
        .bloquearPorId(id)
        .map(
            e -> {
              entityManager.refresh(e);
              return mapper.toDomain(e);
            });
  }

  public List<AsistenciaAula> listar() {
    return repository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<AsistenciaAula> buscarPorMatriculaId(Long valor) {
    return repository.buscarPorMatriculaId(valor).stream().map(mapper::toDomain).toList();
  }

  public List<AsistenciaAula> buscarPorMatriculaIds(Collection<Long> matriculaIds) {
    if (matriculaIds == null || matriculaIds.isEmpty()) return List.of();
    return repository.buscarPorMatriculaIds(matriculaIds).stream().map(mapper::toDomain).toList();
  }

  public List<AsistenciaAula> buscarPorFechaSesion(LocalDate valor) {
    return repository.buscarPorFechaSesion(valor).stream().map(mapper::toDomain).toList();
  }

  public List<SeccionAsistencia> buscarSeccionesPorMatriculaIds(Collection<Long> matriculaIds) {
    if (matriculaIds == null || matriculaIds.isEmpty()) return List.of();
    return repository.buscarSeccionesPorMatriculaIds(matriculaIds);
  }

  @Override
  @Transactional(readOnly = true)
  public ResumenConteoAsistencia contarPorEstadoEnFecha(LocalDate fecha) {
    if (fecha == null) return new ResumenConteoAsistencia(0, 0, 0, 0);
    return mapearConteos(repository.contarPorEstadoEnFecha(fecha));
  }

  @Override
  @Transactional(readOnly = true)
  public ResumenConteoAsistencia contarPorEstadoYMatriculaIdsYRango(
      Collection<Long> matriculaIds, LocalDate desde, LocalDate hasta) {
    if (matriculaIds == null || matriculaIds.isEmpty()) return new ResumenConteoAsistencia(0, 0, 0, 0);
    var listaIds = new java.util.ArrayList<>(matriculaIds);
    long presentes = 0;
    long tardanzas = 0;
    long faltasJust = 0;
    long faltasInjust = 0;
    final int tamanoLote = 500;
    for (int i = 0; i < listaIds.size(); i += tamanoLote) {
      var lote = listaIds.subList(i, Math.min(i + tamanoLote, listaIds.size()));
      List<Object[]> filas = (desde == null || hasta == null)
          ? repository.contarPorEstadoYMatriculaIds(lote)
          : repository.contarPorEstadoYMatriculaIdsYRango(lote, desde, hasta);
      var parcial = mapearConteos(filas);
      presentes += parcial.presentes();
      tardanzas += parcial.tardanzas();
      faltasJust += parcial.faltasJustificadas();
      faltasInjust += parcial.faltasInjustificadas();
    }
    return new ResumenConteoAsistencia(presentes, tardanzas, faltasJust, faltasInjust);
  }

  @Override
  @Transactional(readOnly = true)
  public ResumenConteoAsistencia contarPorEstadoEnAnioYRango(
      Short anioId, LocalDate desde, LocalDate hasta) {
    if (anioId == null) return new ResumenConteoAsistencia(0, 0, 0, 0);
    List<Object[]> filas = (desde == null || hasta == null)
        ? repository.contarPorEstadoEnAnio(anioId)
        : repository.contarPorEstadoEnAnioYRango(anioId, desde, hasta);
    return mapearConteos(filas);
  }

  private ResumenConteoAsistencia mapearConteos(List<Object[]> filas) {
    long presentes = 0;
    long tardanzas = 0;
    long faltasJust = 0;
    long faltasInjust = 0;
    if (filas != null) {
      for (Object[] fila : filas) {
        if (fila != null && fila.length >= 2 && fila[0] instanceof EstadoAsistenciaAula estado && fila[1] instanceof Number n) {
          long c = n.longValue();
          switch (estado) {
            case PRESENTE -> presentes += c;
            case TARDANZA -> tardanzas += c;
            case FALTA_JUSTIFICADA -> faltasJust += c;
            case FALTA_INJUSTIFICADA -> faltasInjust += c;
          }
        }
      }
    }
    return new ResumenConteoAsistencia(presentes, tardanzas, faltasJust, faltasInjust);
  }

  public List<LocalDate> buscarFechasConRegistros(LocalDate desde, LocalDate hasta) {
    return repository.buscarFechasConRegistros(desde, hasta);
  }

  public List<AsistenciaAula> buscarPorFechaSesionYMatriculaIds(
      LocalDate fecha, Collection<Long> matriculaIds) {
    if (matriculaIds == null || matriculaIds.isEmpty()) return List.of();
    return repository.buscarPorFechaSesionYMatriculaIds(fecha, matriculaIds).stream()
        .map(mapper::toDomain)
        .toList();
  }

  public List<AsistenciaAula> buscarPorEstado(EstadoAsistenciaAula valor) {
    return repository.buscarPorEstado(valor).stream().map(mapper::toDomain).toList();
  }

  public List<AsistenciaAula> buscarPorAuxiliarUsuarioId(Long valor) {
    return repository.buscarPorAuxiliarUsuarioId(valor).stream().map(mapper::toDomain).toList();
  }
}
