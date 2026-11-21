package com.colegio.shuji.curriculo.infrastructure.adapter;

import com.colegio.shuji.curriculo.application.dto.out.DocenteOpcionResponseDto;
import com.colegio.shuji.curriculo.application.port.out.DocenteHabilitadoPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocenteHabilitadoAdapter implements DocenteHabilitadoPort {
  private final JdbcTemplate jdbc;

  @Override
  public List<DocenteOpcionResponseDto> listarPorArea(Short areaId) {
    return jdbc.query("""
        select u.id, u.nombre_usuario, u.nombre_completo
        from public.docente_areas_curriculares d
        join public.postulaciones_docentes p
          on p.docente_usuario_id = d.docente_usuario_id and p.nivel_id = d.nivel_id
        join public.usuarios u on u.id = d.docente_usuario_id
        join public.usuario_roles ur on ur.usuario_id = u.id
        join public.roles r on r.id = ur.rol_id
        where d.area_curricular_id = ? and p.estado = 'APROBADA'
          and u.activo and r.codigo_rol = 'DOCENTE'
        order by coalesce(nullif(u.nombre_completo, ''), u.nombre_usuario), u.id
        """,
        (fila, numero) -> new DocenteOpcionResponseDto(
            fila.getLong("id"), fila.getString("nombre_usuario"), fila.getString("nombre_completo")),
        areaId);
  }
}
