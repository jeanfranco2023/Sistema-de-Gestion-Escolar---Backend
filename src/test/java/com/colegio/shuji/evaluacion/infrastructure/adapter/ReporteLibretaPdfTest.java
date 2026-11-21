package com.colegio.shuji.evaluacion.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import com.colegio.shuji.evaluacion.infrastructure.adapter.ReporteLibretaPdfAdapter.CalificacionLibretaItem;
import com.colegio.shuji.matricula.application.dto.out.HijoPortalResponseDto;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;

class ReporteLibretaPdfTest {
  @Test void generaPdfPaginadoConTextoRealSinRecortarConclusiones() throws Exception {
    var lineas = new ArrayList<String>(); lineas.add("I.E.P. SHUJI KITAMURA - INFORME CNEB");
    for (int i = 0; i < 70; i++) lineas.add("Matemática | Competencia " + i + ": Explica sus procedimientos y requiere apoyo para comprobar resultados. ".repeat(3));
    lineas.add("FIN DEL INFORME DE PRUEBA");
    byte[] bytes = ReporteLibretaPdfAdapter.renderizar(lineas);
    try (var doc = Loader.loadPDF(bytes)) {
      assertTrue(doc.getNumberOfPages() > 1); assertTrue(bytes.length < 150_000);
      String texto = new PDFTextStripper().getText(doc);
      assertTrue(texto.contains("Competencia 69")); assertTrue(texto.contains("FIN DEL INFORME"));
      var imagen = new PDFRenderer(doc).renderImageWithDPI(0, 120);
      Files.createDirectories(Path.of("target", "playbook"));
      javax.imageio.ImageIO.write(imagen, "png", Path.of("target", "playbook", "playbook-libreta-preview.png").toFile());
      Files.write(Path.of("target", "playbook", "playbook-libreta-prueba.pdf"), bytes);
    }
  }

  @Test void generaLibretaFormalEscolarConMatrizYConclusiones() throws Exception {
    var alumno = new HijoPortalResponseDto(
        1L, 91L, (short) 2026, (short) 2026, 37,
        "Lucia Fernanda", "Vargas", "Flores", "85000001",
        "Secundaria", "5° de Secundaria", "A"
    );
    var items = List.of(
        new CalificacionLibretaItem("Matematica", "Resuelve problemas de cantidad", 1, "I Bimestre", "AD",
            "En el I Bimestre, demuestra logro destacado resolviendo situaciones problematicas con autonomia."),
        new CalificacionLibretaItem("Matematica", "Resuelve problemas de cantidad", 2, "II Bimestre", "A",
            "Mantiene desempeno esperado en modelamiento numerico."),
        new CalificacionLibretaItem("Matematica", "Resuelve problemas de forma, movimiento y localizacion", 1, "I Bimestre", "B",
            "Consolida estrategias geometricas de resolucion."),
        new CalificacionLibretaItem("Ciencia y Tecnologia", "Indaga mediante metodos cientificos", 1, "I Bimestre", "C",
            "Requiere acompanamiento para formular hipotesis contrastables."),
        new CalificacionLibretaItem("Comunicacion", "Lee diversos tipos de textos escritos en lengua materna", 1, "I Bimestre", "A",
            "Comprende textos argumentativos e infiere ideas principales.")
    );

    byte[] pdfBytes = ReporteLibretaPdfAdapter.renderizarLibretaOficial(alumno, items, LocalDate.of(2026, 11, 13));
    try (var doc = Loader.loadPDF(pdfBytes)) {
      assertTrue(doc.getNumberOfPages() >= 1);
      String texto = new PDFTextStripper().getText(doc);
      assertTrue(texto.contains("LIBRETA OFICIAL DE NOTAS"));
      assertTrue(texto.contains("Vargas Flores, Lucia Fernanda"));
      assertTrue(texto.contains("MATRIZ OFICIAL DE CALIFICACIONES CNEB"));
      assertTrue(texto.contains("I BIM"));
      assertTrue(texto.contains("IV BIM"));
      assertTrue(texto.contains("CONCLUSIONES DESCRIPTIVAS"));
      assertTrue(texto.contains("REQUIERE REFUERZO"));
      assertTrue(texto.contains("DIRECCIÓN ACADÉMICA"));
      assertTrue(texto.contains("Página 1 de " + doc.getNumberOfPages()));

      var imagen = new PDFRenderer(doc).renderImageWithDPI(0, 140);
      Files.createDirectories(Path.of("target", "playbook"));
      javax.imageio.ImageIO.write(imagen, "png", Path.of("target", "playbook", "playbook-libreta-formal-p1.png").toFile());
      if (doc.getNumberOfPages() > 1) {
        var imagen2 = new PDFRenderer(doc).renderImageWithDPI(1, 140);
        javax.imageio.ImageIO.write(imagen2, "png", Path.of("target", "playbook", "playbook-libreta-formal-p2.png").toFile());
      }
      Files.write(Path.of("target", "playbook", "playbook-libreta-formal.pdf"), pdfBytes);
    }
  }
}
