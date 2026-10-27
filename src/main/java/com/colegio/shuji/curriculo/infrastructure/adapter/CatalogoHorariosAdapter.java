package com.colegio.shuji.curriculo.infrastructure.adapter;
import com.colegio.shuji.curriculo.application.dto.out.AsignacionHorarioOpcionDto;
import com.colegio.shuji.curriculo.application.port.in.ConsultarCatalogoHorariosUseCase;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
@Component @RequiredArgsConstructor @Transactional(readOnly=true)
public class CatalogoHorariosAdapter implements ConsultarCatalogoHorariosUseCase {
  private final EntityManager em;
  private final ActorActualPort actor;
  public List<AsignacionHorarioOpcionDto> catalogo() {
    Long docente=actor.tieneRol("DIRECCION")||actor.tieneRol("SECRETARIA")?null:actor.usuarioId();
    return em.createQuery("""
        select new com.colegio.shuji.curriculo.application.dto.out.AsignacionHorarioOpcionDto(
          a.id,an.id,an.anio,s.id,n.nombre,g.nombre,s.letra,ar.id,ar.nombre,u.id,u.username,au.codigo)
        from AsignacionDocenteEntity a, AnioLectivoEntity an, SeccionEntity s, GradoEntity g,
          NivelEntity n, AreaCurricularEntity ar, UsuarioEntity u, AulaEntity au
        where a.anioLectivoId=an.id and a.seccionId=s.id and s.gradoId=g.id and g.nivelId=n.id
          and a.areaCurricularId=ar.id and a.docenteUsuarioId=u.id and s.aulaId=au.id
          and (:docente is null or exists(select propia.id from AsignacionDocenteEntity propia
            where propia.docenteUsuarioId=:docente and propia.seccionId=a.seccionId and propia.anioLectivoId=a.anioLectivoId))
        order by an.anio desc,n.nombre,g.numeroGrado,s.letra,ar.nombre
        """,AsignacionHorarioOpcionDto.class).setParameter("docente",docente).getResultList();
  }
}
