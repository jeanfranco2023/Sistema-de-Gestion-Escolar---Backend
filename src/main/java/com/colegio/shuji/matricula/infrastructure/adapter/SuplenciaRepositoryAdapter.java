package com.colegio.shuji.matricula.infrastructure.adapter;
import com.colegio.shuji.matricula.application.port.out.SuplenciaRepositoryPort;
import com.colegio.shuji.matricula.domain.model.Suplencia;
import com.colegio.shuji.matricula.infrastructure.repository.JpaSuplenciaRepository;
import com.colegio.shuji.shared.application.port.out.AuditContextPort;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component @RequiredArgsConstructor @Transactional
public class SuplenciaRepositoryAdapter implements SuplenciaRepositoryPort {
  private final JpaSuplenciaRepository repository;
  private final SuplenciaPersistenceMapper mapper;
  private final AuditContextPort auditoria;
  private final EntityManager em;
  public Suplencia guardar(Suplencia valor) {
    auditoria.syncCurrentUserFromSecurityContext();
    try { return mapper.toDomain(repository.saveAndFlush(mapper.toEntity(valor))); }
    catch (org.springframework.dao.DataIntegrityViolationException ex) {
      throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
          "La solicitud coincide con una suplencia ya registrada o sus datos no son válidos",ex);
    }
  }
  public Optional<Suplencia> bloquearPorId(Long id) { return repository.bloquearPorId(id).map(e->{em.refresh(e);return mapper.toDomain(e);}); }
  public List<Suplencia> porTitular(Long id) { return repository.findByApoderadoTitularIdOrderByFechaSolicitudDesc(id).stream().map(mapper::toDomain).toList(); }
  public List<Suplencia> listar() { return repository.findAllByOrderByFechaSolicitudDesc().stream().map(mapper::toDomain).toList(); }
  public boolean hayConflicto(Long estudianteId,LocalDate inicio,LocalDate fin,Long excluirId) { return repository.hayConflicto(estudianteId,inicio,fin,excluirId); }
}
