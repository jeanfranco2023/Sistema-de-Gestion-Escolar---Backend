package com.colegio.shuji.matricula.infrastructure.adapter;

import com.colegio.shuji.matricula.application.port.out.ConsultaPortalDatosPort;
import com.colegio.shuji.matricula.application.dto.out.HijoPortalResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.AsistenciaAulaResponseDto;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConsultaPortalDatosAdapter implements ConsultaPortalDatosPort {
  private final EntityManager em;

  public List<com.colegio.shuji.evaluacion.application.dto.out.CalificacionPortalResponseDto> calificaciones(Long matriculaId) {
    return em.createQuery("""
        select new com.colegio.shuji.evaluacion.application.dto.out.CalificacionPortalResponseDto(
          a.id,a.nombre,comp.id,comp.nombre,p.id,p.nombre,c.calificacionCualitativa,c.conclusionDescriptiva)
        from CalificacionCnebEntity c, AreaCurricularEntity a, CompetenciaEntity comp, PeriodoAcademicoEntity p
        where c.matriculaId=:id and c.areaCurricularId=a.id and c.competenciaId=comp.id and c.periodoAcademicoId=p.id
        order by p.numeroPeriodo,a.nombre,comp.numeroOrden
        """, com.colegio.shuji.evaluacion.application.dto.out.CalificacionPortalResponseDto.class)
        .setParameter("id", matriculaId).getResultList();
  }

  public List<HijoPortalResponseDto> hijos(Long usuarioId) {
    return em.createQuery("""
        select new com.colegio.shuji.matricula.application.dto.out.HijoPortalResponseDto(
          e.id,m.id,a.id,a.anio,s.id,e.nombres,e.apellidoPaterno,e.apellidoMaterno,e.numeroDocumento,n.nombre,g.nombre,s.letra)
        from MatriculaEntity m, EstudianteEntity e, SeccionEntity s, GradoEntity g, NivelEntity n, AnioLectivoEntity a
        where m.estudianteId=e.id and m.seccionId=s.id and s.gradoId=g.id and g.nivelId=n.id and m.anioLectivoId=a.id
          and e.activo=true and m.estadoMatricula=com.colegio.shuji.matricula.domain.enums.EstadoMatricula.MATRICULADO
          and (:usuarioId is null or exists (select v.id from EstudianteApoderadoEntity v, ApoderadoEntity ap
            where v.apoderadoId=ap.id and v.estudianteId=e.id and v.tieneCustodia=true and ap.usuarioId=:usuarioId))
        order by a.anio desc,e.apellidoPaterno,e.apellidoMaterno,e.nombres
        """, HijoPortalResponseDto.class).setParameter("usuarioId", usuarioId).getResultList();
  }

  public List<AsistenciaAulaResponseDto> asistencia(Long matriculaId) {
    return em.createQuery("""
        select new com.colegio.shuji.asistencia.application.dto.out.AsistenciaAulaResponseDto(
          a.id,a.matriculaId,a.fechaSesion,a.horaRegistro,a.estado,a.auxiliarUsuarioId,a.justificada,a.motivoJustificacion,a.documentoSustentoUrl)
        from AsistenciaAulaEntity a where a.matriculaId=:id order by a.fechaSesion desc
        """, AsistenciaAulaResponseDto.class).setParameter("id", matriculaId).getResultList();
  }
}
