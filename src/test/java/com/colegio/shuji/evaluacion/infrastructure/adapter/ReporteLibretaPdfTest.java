package com.colegio.shuji.evaluacion.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
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
      javax.imageio.ImageIO.write(imagen, "png", Path.of("..", ".runtime", "playbook-libreta-preview.png").toFile());
      Files.write(Path.of("..", ".runtime", "playbook-libreta-prueba.pdf"), bytes);
    }
  }
}
