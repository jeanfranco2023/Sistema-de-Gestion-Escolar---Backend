package com.colegio.shuji.usuario.infrastructure.adapter;

import com.colegio.shuji.usuario.application.dto.out.AreaDocenteResponseDto;
import com.colegio.shuji.usuario.domain.exception.ReglaNegocioException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Adaptador JDBC para consultar y sincronizar los cursos reales habilitados por docente. */
@Component
@RequiredArgsConstructor
public class DocenteAreasJdbcAdapter {

  private final NamedParameterJdbcTemplate jdbc;

  @Transactional(readOnly = true)
  public Map<Long, List<AreaDocenteResponseDto>> listarPorUsuarios(Collection<Long> usuarioIds) {
    if (usuarioIds == null || usuarioIds.isEmpty()) {
      return Collections.emptyMap();
    }
    List<Long> ids = usuarioIds.stream().filter(Objects::nonNull).distinct().toList();
    if (ids.isEmpty()) {
      return Collections.emptyMap();
    }
    String sql =
        """
        SELECT dac.docente_usuario_id,
               ac.id AS area_id,
               ac.nivel_id,
               n.nombre_nivel,
               ac.codigo_area,
               ac.nombre_area
          FROM public.docente_areas_curriculares dac
          JOIN public.areas_curriculares ac
            ON ac.id = dac.area_curricular_id
           AND ac.nivel_id = dac.nivel_id
          JOIN public.niveles n
            ON n.id = dac.nivel_id
         WHERE dac.docente_usuario_id IN (:ids)
         ORDER BY dac.docente_usuario_id ASC, dac.nivel_id ASC, ac.id ASC
        """;
    Map<Long, List<AreaDocenteResponseDto>> resultado = new LinkedHashMap<>();
    jdbc.query(
        sql,
        new MapSqlParameterSource("ids", ids),
        (ResultSet rs) -> {
          long usuarioId = rs.getLong("docente_usuario_id");
          resultado
              .computeIfAbsent(usuarioId, ignored -> new ArrayList<>())
              .add(mapArea(rs));
        });
    return resultado;
  }

  @Transactional
  public List<AreaDocenteResponseDto> sincronizarAreasDocente(
      Long docenteUsuarioId, Collection<Short> areaIdsSolicitadas, boolean requiereAlMenosUnCurso) {
    Set<Short> areaIds =
        areaIdsSolicitadas == null
            ? Set.of()
            : areaIdsSolicitadas.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

    if (requiereAlMenosUnCurso && areaIds.isEmpty()) {
      throw new ReglaNegocioException(
          "Debe seleccionar al menos un curso real para el docente.", "CURSO_DOCENTE_REQUERIDO");
    }

    if (areaIds.isEmpty()) {
      Integer asignacionesActivas =
          jdbc.queryForObject(
              "SELECT COUNT(*) FROM public.asignaciones_docentes WHERE docente_usuario_id = :docenteId",
              new MapSqlParameterSource("docenteId", docenteUsuarioId),
              Integer.class);
      if (asignacionesActivas != null && asignacionesActivas > 0) {
        throw new ReglaNegocioException(
            "No se pueden retirar cursos porque el docente tiene secciones asignadas activas.",
            "CURSO_CON_ASIGNACIONES");
      }
      jdbc.update(
          "DELETE FROM public.docente_areas_curriculares WHERE docente_usuario_id = :docenteId",
          new MapSqlParameterSource("docenteId", docenteUsuarioId));
      return List.of();
    }

    String consultaAreas =
        """
        SELECT ac.id AS area_id,
               ac.nivel_id,
               n.nombre_nivel,
               ac.codigo_area,
               ac.nombre_area
          FROM public.areas_curriculares ac
          JOIN public.niveles n ON n.id = ac.nivel_id
         WHERE ac.id IN (:areaIds)
         ORDER BY ac.nivel_id ASC, ac.id ASC
        """;
    List<AreaDocenteResponseDto> areasValidas =
        jdbc.query(
            consultaAreas,
            new MapSqlParameterSource("areaIds", areaIds),
            (rs, rowNum) -> mapArea(rs));

    if (areasValidas.size() != areaIds.size()) {
      throw new ReglaNegocioException(
          "Uno o más cursos seleccionados no existen en el catálogo curricular.",
          "AREA_CURRICULAR_INVALIDA");
    }

    Integer enUsoFueraDeLista =
        jdbc.queryForObject(
            """
            SELECT COUNT(*)
              FROM public.asignaciones_docentes
             WHERE docente_usuario_id = :docenteId
               AND area_curricular_id NOT IN (:areaIds)
            """,
            new MapSqlParameterSource()
                .addValue("docenteId", docenteUsuarioId)
                .addValue("areaIds", areaIds),
            Integer.class);
    if (enUsoFueraDeLista != null && enUsoFueraDeLista > 0) {
      throw new ReglaNegocioException(
          "No se puede retirar un curso mientras el docente tenga secciones asignadas en dicho curso.",
          "CURSO_CON_ASIGNACIONES");
    }

    // FK compuesta: postulaciones_docentes (APROBADA) debe existir antes de docente_areas_curriculares
    Set<Short> nivelesRequeridos =
        areasValidas.stream()
            .map(AreaDocenteResponseDto::nivelId)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    if (!nivelesRequeridos.isEmpty()) {
      MapSqlParameterSource[] paramsNiveles =
          nivelesRequeridos.stream()
              .map(
                  nivelId ->
                      new MapSqlParameterSource()
                          .addValue("docenteId", docenteUsuarioId)
                          .addValue("nivelId", nivelId))
              .toArray(MapSqlParameterSource[]::new);
      jdbc.batchUpdate(
          """
          INSERT INTO public.postulaciones_docentes (docente_usuario_id, nivel_id, estado)
          VALUES (:docenteId, :nivelId, 'APROBADA')
          ON CONFLICT (docente_usuario_id, nivel_id)
          DO UPDATE SET estado = 'APROBADA'
          """,
          paramsNiveles);
    }

    jdbc.update(
        """
        DELETE FROM public.docente_areas_curriculares
         WHERE docente_usuario_id = :docenteId
           AND area_curricular_id NOT IN (:areaIds)
        """,
        new MapSqlParameterSource()
            .addValue("docenteId", docenteUsuarioId)
            .addValue("areaIds", areaIds));

    MapSqlParameterSource[] paramsAreas =
        areasValidas.stream()
            .map(
                area ->
                    new MapSqlParameterSource()
                        .addValue("docenteId", docenteUsuarioId)
                        .addValue("nivelId", area.nivelId())
                        .addValue("areaId", area.id()))
            .toArray(MapSqlParameterSource[]::new);
    jdbc.batchUpdate(
        """
        INSERT INTO public.docente_areas_curriculares (docente_usuario_id, nivel_id, area_curricular_id)
        VALUES (:docenteId, :nivelId, :areaId)
        ON CONFLICT (docente_usuario_id, area_curricular_id) DO NOTHING
        """,
        paramsAreas);

    return areasValidas;
  }

  public static String resumirEspecialidad(Collection<AreaDocenteResponseDto> areas) {
    if (areas == null || areas.isEmpty()) {
      return null;
    }
    String texto =
        areas.stream()
            .map(AreaDocenteResponseDto::nombre)
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.joining(", "));
    return texto.length() > 100 ? texto.substring(0, 97) + "..." : texto;
  }

  private AreaDocenteResponseDto mapArea(ResultSet rs) throws SQLException {
    return new AreaDocenteResponseDto(
        rs.getShort("area_id"),
        rs.getShort("nivel_id"),
        rs.getString("nombre_nivel"),
        rs.getString("codigo_area"),
        rs.getString("nombre_area"));
  }
}
