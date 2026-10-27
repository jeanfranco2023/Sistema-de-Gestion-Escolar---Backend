package com.colegio.shuji.evaluacion.infrastructure.adapter;

import com.colegio.shuji.evaluacion.application.port.in.ConsultarLibretaNotasUseCase;
import com.colegio.shuji.evaluacion.application.port.in.GenerarLibretaPdfUseCase;
import com.colegio.shuji.matricula.application.dto.out.HijoPortalResponseDto;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReporteLibretaPdfAdapter implements GenerarLibretaPdfUseCase {
  private final ConsultarLibretaNotasUseCase consulta;
  private final EntityManager em;

  @Transactional(readOnly = true)
  public byte[] generar(Long matriculaId) {
    var libreta = consulta.libreta(matriculaId); // Aplica custodia o asignación docente antes de leer datos personales.
    var alumno = em.createQuery("""
        select new com.colegio.shuji.matricula.application.dto.out.HijoPortalResponseDto(
          e.id,m.id,a.id,a.anio,s.id,e.nombres,e.apellidoPaterno,e.apellidoMaterno,e.numeroDocumento,n.nombre,g.nombre,s.letra)
        from MatriculaEntity m, EstudianteEntity e, SeccionEntity s, GradoEntity g, NivelEntity n, AnioLectivoEntity a
        where m.id=:id and m.estudianteId=e.id and m.seccionId=s.id and s.gradoId=g.id
          and g.nivelId=n.id and m.anioLectivoId=a.id
        """, HijoPortalResponseDto.class).setParameter("id", matriculaId).getSingleResult();
    var lineas = new java.util.ArrayList<String>();
    lineas.add("I.E.P. SHUJI KITAMURA"); lineas.add("INFORME DE PROGRESO DEL ESTUDIANTE - CNEB");
    lineas.add("Estudiante: " + alumno.nombres() + " " + alumno.apellidoPaterno() + " " + alumno.apellidoMaterno());
    lineas.add("DNI: " + alumno.numeroDocumento() + " | Año: " + alumno.anio());
    lineas.add(alumno.nivel() + " | " + alumno.grado() + " | Sección " + alumno.seccion());
    lineas.add(" ");
    if (libreta.calificaciones().isEmpty()) lineas.add("Sin calificaciones registradas para esta consulta.");
    else {
      var ids = libreta.calificaciones().stream().map(c -> c.id()).toList();
      var filas = em.createQuery("""
          select a.nombre, comp.nombre, p.nombre, c.calificacionCualitativa, c.conclusionDescriptiva
          from CalificacionCnebEntity c, AreaCurricularEntity a, CompetenciaEntity comp, PeriodoAcademicoEntity p
          where c.id in :ids and c.areaCurricularId=a.id and c.competenciaId=comp.id and c.periodoAcademicoId=p.id
          order by p.numeroPeriodo,a.nombre,comp.numeroOrden
          """, Object[].class).setParameter("ids", ids).getResultList();
      for (var fila : filas) {
        lineas.add(fila[2] + " | " + fila[0] + " | Logro: " + fila[3]);
        lineas.add("Competencia: " + fila[1]);
        lineas.add("Conclusión: " + (fila[4] == null ? "Sin conclusión registrada" : fila[4])); lineas.add(" ");
      }
    }
    lineas.add("Informe generado el " + java.time.LocalDate.now(java.time.ZoneId.of("America/Lima")));
    lineas.add("Documento informativo del sistema. No contiene firma digital institucional.");
    return renderizar(lineas);
  }

  public static byte[] renderizar(List<String> lineas) {
    var fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    try (var doc = new PDDocument(); var salida = new ByteArrayOutputStream()) {
      PDPageContentStream stream = null; float y = 0; int numero = 0;
      try {
        for (String parrafo : lineas) {
          StringBuilder texto = new StringBuilder();
          for (char c : parrafo.toCharArray()) {
            if (c == '\n' || c == '\r') { texto.append('\n'); continue; }
            try { fuente.encode(String.valueOf(c)); texto.append(c); } catch (IllegalArgumentException ex) { texto.append('?'); }
          }
          StringBuilder linea = new StringBuilder();
          var ajustadas = new java.util.ArrayList<String>();
          for (char c : texto.toString().toCharArray()) {
            if (c == '\n') { ajustadas.add(linea.toString()); linea.setLength(0); continue; }
            if (fuente.getStringWidth(linea.toString() + c) * 10 / 1000 > 495) {
              int espacio = linea.lastIndexOf(" ");
              if (espacio > 0) {
                ajustadas.add(linea.substring(0, espacio)); String resto = linea.substring(espacio + 1);
                linea.setLength(0); linea.append(resto);
              } else { ajustadas.add(linea.toString()); linea.setLength(0); }
            }
            linea.append(c);
          }
          ajustadas.add(linea.toString());
          for (String contenido : ajustadas) {
            if (y < 55) {
              if (stream != null) stream.close();
              var pagina = new PDPage(PDRectangle.A4); doc.addPage(pagina);
              stream = new PDPageContentStream(doc, pagina); y = 785; numero++;
              stream.beginText(); stream.setFont(fuente, 8); stream.newLineAtOffset(50, 30);
              stream.showText("Shuji Kitamura | Página " + numero); stream.endText();
            }
            stream.beginText(); stream.setFont(fuente, 10); stream.newLineAtOffset(50, y);
            stream.showText(contenido); stream.endText(); y -= 14;
          }
          y -= 3;
        }
      } finally { if (stream != null) stream.close(); }
      doc.save(salida); return salida.toByteArray();
    } catch (IOException ex) { throw new IllegalStateException("No se pudo generar el informe PDF", ex); }
  }
}
