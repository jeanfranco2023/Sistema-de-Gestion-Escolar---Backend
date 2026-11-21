package com.colegio.shuji.shared.infrastructure.audit;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditoriaAccesoRepository extends JpaRepository<AuditoriaAccesoEntity, Long> {
  Page<AuditoriaAccesoEntity> findAllByOrderByFechaEventoDesc(Pageable pageable);

  @Query("""
      select a from AuditoriaAccesoEntity a
      where (:usuarioNombre is null or lower(a.usuarioNombre) like lower(concat('%', :usuarioNombre, '%')))
        and (:metodo is null or upper(a.metodo) = upper(:metodo))
        and (:ruta is null or lower(a.ruta) like lower(concat('%', :ruta, '%')))
      order by a.fechaEvento desc
      """)
  Page<AuditoriaAccesoEntity> filtrar(
      @Param("usuarioNombre") String usuarioNombre,
      @Param("metodo") String metodo,
      @Param("ruta") String ruta,
      Pageable pageable);
}
