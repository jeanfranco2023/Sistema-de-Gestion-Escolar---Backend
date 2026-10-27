package com.colegio.shuji.matricula.infrastructure.adapter;

import com.colegio.shuji.matricula.application.port.out.AccesoFamiliarPort;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccesoFamiliarAdapter implements AccesoFamiliarPort {
  private final EntityManager em;
  public boolean tieneCustodia(Long estudianteId, Long usuarioId) {
    return em.createQuery("""
        select count(v) from EstudianteApoderadoEntity v, ApoderadoEntity a, EstudianteEntity e
        where v.apoderadoId = a.id and v.estudianteId = e.id and e.id = :estudianteId
          and e.activo = true and v.tieneCustodia = true and a.usuarioId = :usuarioId
        """, Long.class).setParameter("estudianteId", estudianteId).setParameter("usuarioId", usuarioId)
        .getSingleResult() > 0;
  }
}
