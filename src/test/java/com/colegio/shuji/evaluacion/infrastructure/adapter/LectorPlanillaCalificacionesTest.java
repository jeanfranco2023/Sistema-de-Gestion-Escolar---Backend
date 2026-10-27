package com.colegio.shuji.evaluacion.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LectorPlanillaCalificacionesTest {
  @Test void csvConBomComillasComasYSaltosConservaContenido() {
    var filas = leer("\uFEFFA,B,C,D\r\n00123456,2,C,\"Resuelve, con apoyo\nDice \"\"sí\"\"\"\r\n");
    assertEquals(2, filas.size()); assertEquals("00123456", filas.get(1).get(0));
    assertEquals("Resuelve, con apoyo\nDice \"sí\"", filas.get(1).get(3));
  }
  @Test void rechazaComillasTruncadasColumnasExtrasYExcesoDeFilas() {
    for (String contenido : new String[]{"a,b,c,\"incompleto", "a,b,c,d,e", "a,b,c,d\n".repeat(502)})
      assertThrows(ResponseStatusException.class, () -> leer(contenido));
  }
  @Test void xlsxLeeTextoInlineSinPerderCerosYRechazaFormulas() throws Exception {
    String hoja = "<worksheet><sheetData><row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>00123456</t></is></c><c r=\"B1\"><v>2</v></c></row></sheetData></worksheet>";
    assertEquals("00123456", LectorPlanillaCalificaciones.leer(zip(hoja), "planilla.xlsx").getFirst().getFirst());
    assertThrows(ResponseStatusException.class, () -> LectorPlanillaCalificaciones.leer(zip(hoja.replace("<v>2</v>", "<f>1+1</f><v>2</v>")), "p.xlsx"));
    assertThrows(ResponseStatusException.class, () -> LectorPlanillaCalificaciones.leer(zip("<!DOCTYPE x [<!ENTITY secreto SYSTEM 'file:///archivo'>]>" + hoja), "p.xlsx"));
  }
  private java.util.List<java.util.List<String>> leer(String texto) { return LectorPlanillaCalificaciones.leer(texto.getBytes(StandardCharsets.UTF_8), "p.csv"); }
  private byte[] zip(String hoja) throws Exception {
    var bytes = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(bytes)) { zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml")); zip.write(hoja.getBytes(StandardCharsets.UTF_8)); zip.closeEntry(); }
    return bytes.toByteArray();
  }
}
