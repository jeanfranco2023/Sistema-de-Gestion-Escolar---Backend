package com.colegio.shuji.curriculo.infrastructure.adapter;

import com.colegio.shuji.curriculo.application.port.out.DocenteHabilitadoPort;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocenteHabilitadoAdapter implements DocenteHabilitadoPort {
  private final EntityManager em;

  public boolean estaHabilitado(Long id) {
    return em.createQuery(
                "select count(u) from UsuarioEntity u join u.roles r where u.id=:id and"
                    + " u.activo=true and r.codigo='DOCENTE'",
                Long.class)
            .setParameter("id", id)
            .getSingleResult()
        > 0;
  }
}
