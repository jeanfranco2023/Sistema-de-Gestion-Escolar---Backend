package com.colegio.shuji.usuario.infrastructure.adapter;

import com.colegio.shuji.usuario.application.port.out.AuditoriaCambiosPort;
import com.colegio.shuji.usuario.domain.model.CambioAuditoria;
import com.colegio.shuji.usuario.infrastructure.entity.AuditoriaCambiosEntity;
import com.colegio.shuji.usuario.infrastructure.repository.JpaAuditoriaCambiosRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AuditoriaCambiosRepositoryAdapter implements AuditoriaCambiosPort {

  private final JpaAuditoriaCambiosRepository repository;

  @Override
  @Transactional(readOnly = true)
  public List<CambioAuditoria> listarRecientes(int limite) {
    var pageable = PageRequest.of(0, limite);
    return repository.findAllByOrderByFechaCreacionDescIdDesc(pageable).stream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CambioAuditoria> filtrar(
      Instant fechaInicio,
      Instant fechaFin,
      Long usuarioId,
      String tabla,
      Pageable pageable) {
    Pageable ordenado = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
        Sort.by(Sort.Direction.DESC, "fechaCreacion", "id"));
    return repository.findAll((root, query, cb) -> {
      List<Predicate> filtros = new ArrayList<>();
      if (fechaInicio != null) filtros.add(cb.greaterThanOrEqualTo(root.get("fechaCreacion"), fechaInicio));
      if (fechaFin != null) filtros.add(cb.lessThanOrEqualTo(root.get("fechaCreacion"), fechaFin));
      if (usuarioId != null) filtros.add(cb.equal(root.get("usuario").get("id"), usuarioId));
      if (tabla != null && !tabla.isBlank()) {
        filtros.add(cb.equal(cb.lower(root.get("tablaAfectada")), tabla.strip().toLowerCase(Locale.ROOT)));
      }
      return cb.and(filtros.toArray(Predicate[]::new));
    }, ordenado)
        .map(this::toDomain);
  }

  private CambioAuditoria toDomain(AuditoriaCambiosEntity evento) {
    return new CambioAuditoria(
        evento.getId(),
        evento.getTablaAfectada(),
        evento.getRegistroId(),
        evento.getRegistroClave(),
        evento.getAccion(),
        evento.getDatosAnteriores(),
        evento.getDatosNuevos(),
        evento.getUsuario() == null ? null : evento.getUsuario().getId(),
        evento.getUsuarioNombreSnapshot(),
        evento.getRolesUsuarioSnapshot(),
        evento.getFechaCreacion());
  }
}
