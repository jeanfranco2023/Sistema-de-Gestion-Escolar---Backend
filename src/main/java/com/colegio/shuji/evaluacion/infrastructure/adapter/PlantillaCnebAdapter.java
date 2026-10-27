package com.colegio.shuji.evaluacion.infrastructure.adapter;

import com.colegio.shuji.evaluacion.application.port.in.DescargarPlantillaCnebUseCase;
import com.colegio.shuji.evaluacion.application.port.in.RegistrarEvaluacionCnebUseCase;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PlantillaCnebAdapter implements DescargarPlantillaCnebUseCase {
  private final RegistrarEvaluacionCnebUseCase evaluacion;
  private final EntityManager em;
  @Transactional(readOnly = true)
  public byte[] descargar(Long asignacionId, Short periodoId, Short competenciaId) {
    var matriz = evaluacion.listarMatriz(asignacionId, periodoId, competenciaId);
    var texto = new StringBuilder("DNI_ESTUDIANTE,COMPETENCIA_ID,NOTA_CNEB,CONCLUSION_DESCRIPTIVA\r\n");
    if (!matriz.isEmpty()) {
      var ids = matriz.stream().map(f -> f.matriculaId()).toList();
      var documentos = em.createQuery("""
          select e.numeroDocumento from MatriculaEntity m, EstudianteEntity e
          where m.id in :ids and m.estudianteId=e.id order by e.apellidoPaterno,e.apellidoMaterno,e.nombres
          """, String.class).setParameter("ids", ids).getResultList();
      for (String dni : documentos) texto.append(dni).append(',').append(competenciaId).append(",,\r\n");
    }
    return texto.toString().getBytes(StandardCharsets.UTF_8);
  }
}
