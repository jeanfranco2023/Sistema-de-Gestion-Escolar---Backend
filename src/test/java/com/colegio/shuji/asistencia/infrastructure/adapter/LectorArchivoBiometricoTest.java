package com.colegio.shuji.asistencia.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class LectorArchivoBiometricoTest {
  @Test
  void leeCsvUtf8ConCabeceraYFechaConZona() {
    var filas = LectorArchivoBiometrico.leer(("dni,fecha_hora,dispositivo_codigo\r\n"
        + "12345678,2026-10-28T08:30:00-05:00," + "\"Puerta, principal\"\r\n")
        .getBytes(StandardCharsets.UTF_8));
    assertEquals(1, filas.size());
    assertEquals("12345678", filas.getFirst().dniLeido());
    assertEquals("Puerta, principal", filas.getFirst().dispositivoCodigo());
  }

  @Test
  void rechazaFechaSinZonaHoraria() {
    var bytes = ("dni,fechaHora,dispositivoCodigo\n"
        + "12345678,2026-10-28T08:30:00,puerta1\n").getBytes(StandardCharsets.UTF_8);
    assertThrows(ResponseStatusException.class, () -> LectorArchivoBiometrico.leer(bytes));
  }

  @Test
  void procesaLoteDeQuinientasFilas() {
    var csv = new StringBuilder("dni,fechaHora,dispositivoCodigo\n");
    for (int i = 0; i < 500; i++) {
      csv.append("12345678,2026-10-28T08:30:00-05:00,puerta1\n");
    }
    var filas = LectorArchivoBiometrico.leer(csv.toString().getBytes(StandardCharsets.UTF_8));
    assertEquals(500, filas.size());
  }

  @Test
  void leeXlsxConValoresDeTextoYConservaCerosDelDni() throws Exception {
    var hoja = "<worksheet><sheetData>"
        + "<row r='1'><c r='A1' t='inlineStr'><is><t>dni</t></is></c>"
        + "<c r='B1' t='inlineStr'><is><t>fechaHora</t></is></c>"
        + "<c r='C1' t='inlineStr'><is><t>dispositivoCodigo</t></is></c></row>"
        + "<row r='2'><c r='A2' t='inlineStr'><is><t>01234567</t></is></c>"
        + "<c r='B2' t='inlineStr'><is><t>2026-10-28T08:30:00-05:00</t></is></c>"
        + "<c r='C2' t='inlineStr'><is><t>puerta1</t></is></c></row>"
        + "</sheetData></worksheet>";
    var filas = LectorArchivoBiometrico.leer(xlsx(hoja), "marcas.xlsx");
    assertEquals("01234567", filas.getFirst().dniLeido());
    assertEquals("puerta1", filas.getFirst().dispositivoCodigo());
  }

  @Test
  void rechazaFormulasEnXlsx() throws Exception {
    var hoja = "<worksheet><sheetData><row r='1'><c r='A1'><f>1+1</f><v>2</v></c></row>"
        + "</sheetData></worksheet>";
    assertThrows(ResponseStatusException.class, () -> LectorArchivoBiometrico.leer(xlsx(hoja), "marcas.xlsx"));
  }

  @Test
  void leeXlsxConCadenasCompartidas() throws Exception {
    var cadenas = "<sst><si><t>dni</t></si><si><t>fechaHora</t></si><si><t>dispositivoCodigo</t></si>"
        + "<si><t>00123456</t></si><si><t>2026-10-28T08:30:00-05:00</t></si><si><t>puerta2</t></si></sst>";
    var hoja = "<worksheet><sheetData><row r='1'><c r='A1' t='s'><v>0</v></c><c r='B1' t='s'><v>1</v></c>"
        + "<c r='C1' t='s'><v>2</v></c></row><row r='2'><c r='A2' t='s'><v>3</v></c>"
        + "<c r='B2' t='s'><v>4</v></c><c r='C2' t='s'><v>5</v></c></row></sheetData></worksheet>";
    var filas = LectorArchivoBiometrico.leer(xlsx(hoja, cadenas), "marcas.xlsx");
    assertEquals("00123456", filas.getFirst().dniLeido());
    assertEquals("puerta2", filas.getFirst().dispositivoCodigo());
  }

  private static byte[] xlsx(String hoja) throws Exception {
    return xlsx(hoja, null);
  }

  private static byte[] xlsx(String hoja, String cadenas) throws Exception {
    var bytes = new ByteArrayOutputStream();
    try (var zip = new ZipOutputStream(bytes)) {
      if (cadenas != null) {
        zip.putNextEntry(new ZipEntry("xl/sharedStrings.xml"));
        zip.write(cadenas.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
      }
      zip.putNextEntry(new ZipEntry("xl/worksheets/sheet1.xml"));
      zip.write(hoja.getBytes(StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    return bytes.toByteArray();
  }
}
