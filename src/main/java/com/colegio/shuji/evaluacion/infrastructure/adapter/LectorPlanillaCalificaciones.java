package com.colegio.shuji.evaluacion.infrastructure.adapter;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class LectorPlanillaCalificaciones {
  private LectorPlanillaCalificaciones() {}

  public static List<List<String>> leer(byte[] bytes, String nombre) {
    if (bytes.length == 0 || bytes.length > 5 * 1024 * 1024) throw invalido();
    try {
      return nombre != null && nombre.toLowerCase(Locale.ROOT).endsWith(".xlsx") ? xlsx(bytes) : csv(bytes);
    } catch (ResponseStatusException ex) { throw ex; }
    catch (Exception ex) { throw invalido(); }
  }

  private static List<List<String>> csv(byte[] bytes) throws Exception {
    var decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT);
    try (var reader = new PushbackReader(new InputStreamReader(new ByteArrayInputStream(bytes), decoder), 1)) {
      var filas = new ArrayList<List<String>>(); var fila = new ArrayList<String>();
      var campo = new StringBuilder(); boolean citado = false, cerrado = false; int caracter;
      boolean primero = true;
      while ((caracter = reader.read()) != -1) {
        char c = (char) caracter;
        if (primero && c == '\uFEFF') { primero = false; continue; } primero = false;
        if (citado) {
          if (c == '"') {
            int siguiente = reader.read();
            if (siguiente == '"') campo.append('"');
            else { citado = false; cerrado = true; if (siguiente != -1) reader.unread(siguiente); }
          } else campo.append(c);
        } else if (c == '"') {
          if (!campo.isEmpty() || cerrado) throw invalido(); citado = true;
        } else if (c == ',' || c == '\n' || c == '\r') {
          fila.add(campo.toString()); campo.setLength(0); cerrado = false;
          if (c != ',') {
            agregar(filas, fila); fila = new ArrayList<>();
            if (c == '\r') { int siguiente = reader.read(); if (siguiente != '\n' && siguiente != -1) reader.unread(siguiente); }
          }
        } else {
          if (cerrado) throw invalido(); campo.append(c);
        }
        if (campo.length() > 4000 || fila.size() > 4) throw invalido();
      }
      if (citado) throw invalido();
      if (!campo.isEmpty() || !fila.isEmpty() || cerrado) { fila.add(campo.toString()); agregar(filas, fila); }
      return filas;
    }
  }

  private static List<List<String>> xlsx(byte[] bytes) throws Exception {
    var archivos = new HashMap<String, byte[]>(); int total = 0, cantidad = 0;
    try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
      java.util.zip.ZipEntry entrada;
      while ((entrada = zip.getNextEntry()) != null) {
        if (++cantidad > 100) throw invalido();
        byte[] contenido = zip.readNBytes(10 * 1024 * 1024 - total + 1); total += contenido.length;
        if (total > 10 * 1024 * 1024) throw invalido();
        String nombre = entrada.getName();
        if (nombre.equals("xl/sharedStrings.xml") || nombre.equals("xl/worksheets/sheet1.xml")) {
          if (archivos.put(nombre, contenido) != null) throw invalido();
        }
      }
    }
    var factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    var textos = new ArrayList<String>();
    if (archivos.containsKey("xl/sharedStrings.xml")) {
      var xml = factory.createXMLStreamReader(new ByteArrayInputStream(archivos.get("xl/sharedStrings.xml")));
      StringBuilder texto = null;
      while (xml.hasNext()) {
        int evento = xml.next();
        if (evento == XMLStreamConstants.DTD) throw invalido();
        if (evento == XMLStreamConstants.START_ELEMENT && xml.getLocalName().equals("si")) texto = new StringBuilder();
        if (evento == XMLStreamConstants.START_ELEMENT && xml.getLocalName().equals("t") && texto != null) texto.append(xml.getElementText());
        if (texto != null && texto.length() > 4000) throw invalido();
        if (evento == XMLStreamConstants.END_ELEMENT && xml.getLocalName().equals("si")) { textos.add(texto.toString()); texto = null; if (textos.size() > 10000) throw invalido(); }
      }
      xml.close();
    }
    if (!archivos.containsKey("xl/worksheets/sheet1.xml")) throw invalido();
    var filas = new ArrayList<List<String>>();
    var xml = factory.createXMLStreamReader(new ByteArrayInputStream(archivos.get("xl/worksheets/sheet1.xml")));
    ArrayList<String> fila = null; String tipo = ""; int columna = -1;
    while (xml.hasNext()) {
      int evento = xml.next();
      if (evento == XMLStreamConstants.DTD) throw invalido();
      if (evento == XMLStreamConstants.START_ELEMENT) {
        switch (xml.getLocalName()) {
          case "row" -> fila = new ArrayList<>(List.of("", "", "", ""));
          case "c" -> {
            String referencia = xml.getAttributeValue(null, "r"); tipo = xml.getAttributeValue(null, "t");
            if (referencia == null || !referencia.matches("[A-D][1-9][0-9]*")) throw invalido();
            columna = referencia.charAt(0) - 'A';
          }
          case "f" -> throw invalido();
          case "v", "t" -> {
            String valor = xml.getElementText();
            if (valor.length() > 4000 || fila == null || columna < 0) throw invalido();
            if ("s".equals(tipo)) valor = textos.get(Integer.parseInt(valor));
            fila.set(columna, fila.get(columna) + valor);
          }
          default -> {}
        }
      } else if (evento == XMLStreamConstants.END_ELEMENT && xml.getLocalName().equals("row")) { agregar(filas, fila); fila = null; }
    }
    xml.close(); return filas;
  }

  private static void agregar(List<List<String>> filas, List<String> fila) {
    if (fila.stream().allMatch(String::isBlank)) return;
    if (fila.size() > 4 || filas.size() >= 501) throw invalido();
    filas.add(List.copyOf(fila));
  }

  private static ResponseStatusException invalido() {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "Planilla inválida: usa CSV UTF-8 o XLSX sin fórmulas, cuatro columnas, hasta 500 filas y 5 MB");
  }
}
