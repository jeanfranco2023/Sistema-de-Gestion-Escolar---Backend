package com.colegio.shuji.asistencia.infrastructure.adapter;

import com.colegio.shuji.asistencia.application.dto.in.MarcaBiometricaRequestDto;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.zip.ZipInputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Lee CSV UTF-8 exportados por el reloj biométrico, con formato controlado y límites de memoria. */
public final class LectorArchivoBiometrico {
  private static final int MAX_BYTES = 5 * 1024 * 1024;
  private static final int MAX_XLSX_BYTES_DESCOMPRIMIDOS = 10 * 1024 * 1024;
  private static final int MAX_FILAS = 10_000;
  private static final Pattern DNI = Pattern.compile("[0-9]{8}");

  private LectorArchivoBiometrico() {}

  public static List<MarcaBiometricaRequestDto> leer(byte[] bytes) {
    return leer(bytes, "marcas.csv");
  }

  public static List<MarcaBiometricaRequestDto> leer(byte[] bytes, String nombre) {
    if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) throw invalido();
    if (nombre == null) throw invalido();
    try (var reader = new PushbackReader(
        new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)), 1)) {
      String formato = nombre.toLowerCase(Locale.ROOT);
      List<List<String>> filas;
      if (formato.endsWith(".xlsx")) filas = leerXlsx(bytes);
      else if (formato.endsWith(".csv")) filas = parsear(reader);
      else throw invalido();
      return validarFilas(filas);
    } catch (ResponseStatusException ex) {
      throw ex;
    } catch (Exception ex) {
      throw invalido();
    }
  }

  private static List<MarcaBiometricaRequestDto> validarFilas(List<List<String>> filas) {
    if (filas.size() < 2 || !esCabecera(filas.getFirst())) throw invalido();
    if (filas.size() - 1 > MAX_FILAS) throw invalido();
    var resultado = new ArrayList<MarcaBiometricaRequestDto>(filas.size() - 1);
    for (int i = 1; i < filas.size(); i++) {
      var fila = filas.get(i);
      if (fila.size() != 3) throw invalido();
      String dni = fila.get(0).trim();
      String fecha = fila.get(1).trim();
      String dispositivo = fila.get(2).trim();
      if (!DNI.matcher(dni).matches() || dispositivo.isBlank() || dispositivo.length() > 30) throw invalido();
      try {
        resultado.add(new MarcaBiometricaRequestDto(dni, OffsetDateTime.parse(fecha), dispositivo));
      } catch (RuntimeException ex) {
        throw invalido();
      }
    }
    return List.copyOf(resultado);
  }

  private static List<List<String>> leerXlsx(byte[] bytes) throws Exception {
    var archivos = new HashMap<String, byte[]>();
    int total = 0;
    int cantidad = 0;
    try (var zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
      java.util.zip.ZipEntry entrada;
      while ((entrada = zip.getNextEntry()) != null) {
        if (++cantidad > 100) throw invalido();
        byte[] contenido = zip.readNBytes(MAX_XLSX_BYTES_DESCOMPRIMIDOS - total + 1);
        total += contenido.length;
        if (total > MAX_XLSX_BYTES_DESCOMPRIMIDOS) throw invalido();
        String nombreEntrada = entrada.getName();
        if (nombreEntrada.equals("xl/sharedStrings.xml") || nombreEntrada.equals("xl/worksheets/sheet1.xml")) {
          if (archivos.put(nombreEntrada, contenido) != null) throw invalido();
        }
      }
    }
    byte[] hoja = archivos.get("xl/worksheets/sheet1.xml");
    if (hoja == null) throw invalido();
    var factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    var textos = leerCadenasCompartidas(factory, archivos.get("xl/sharedStrings.xml"));
    var xml = factory.createXMLStreamReader(new ByteArrayInputStream(hoja));
    var filas = new ArrayList<List<String>>();
    ArrayList<String> fila = null;
    boolean[] asignada = null;
    String tipo = "";
    int columna = -1;
    while (xml.hasNext()) {
      int evento = xml.next();
      if (evento == XMLStreamConstants.DTD) throw invalido();
      if (evento == XMLStreamConstants.START_ELEMENT) {
        switch (xml.getLocalName()) {
          case "row" -> { fila = new ArrayList<>(List.of("", "", "")); asignada = new boolean[3]; }
          case "c" -> {
            String referencia = xml.getAttributeValue(null, "r");
            tipo = xml.getAttributeValue(null, "t");
            if (fila == null || referencia == null || !referencia.matches("[A-C][1-9][0-9]*")) throw invalido();
            columna = referencia.charAt(0) - 'A';
            if (asignada[columna]) throw invalido();
            asignada[columna] = true;
          }
          case "f" -> throw invalido();
          case "v", "t" -> {
            String valor = xml.getElementText();
            if (valor.length() > 128 || fila == null || columna < 0) throw invalido();
            if ("s".equals(tipo)) valor = textos.get(Integer.parseInt(valor));
            fila.set(columna, fila.get(columna) + valor);
          }
          default -> {}
        }
      } else if (evento == XMLStreamConstants.END_ELEMENT && xml.getLocalName().equals("row")) {
        agregar(filas, fila);
        fila = null;
        asignada = null;
      }
    }
    xml.close();
    return filas;
  }

  private static List<String> leerCadenasCompartidas(XMLInputFactory factory, byte[] contenido) throws Exception {
    if (contenido == null) return List.of();
    var xml = factory.createXMLStreamReader(new ByteArrayInputStream(contenido));
    var textos = new ArrayList<String>();
    StringBuilder texto = null;
    while (xml.hasNext()) {
      int evento = xml.next();
      if (evento == XMLStreamConstants.DTD) throw invalido();
      if (evento == XMLStreamConstants.START_ELEMENT && xml.getLocalName().equals("si")) texto = new StringBuilder();
      if (evento == XMLStreamConstants.START_ELEMENT && xml.getLocalName().equals("t") && texto != null) {
        texto.append(xml.getElementText());
        if (texto.length() > 128) throw invalido();
      }
      if (evento == XMLStreamConstants.END_ELEMENT && xml.getLocalName().equals("si")) {
        textos.add(texto.toString());
        texto = null;
        if (textos.size() > MAX_FILAS * 3 + 3) throw invalido();
      }
    }
    xml.close();
    return textos;
  }

  private static boolean esCabecera(List<String> columnas) {
    if (columnas.size() != 3) return false;
    return normalizar(columnas.get(0)).equals("dni")
        && normalizar(columnas.get(1)).equals("fechahora")
        && normalizar(columnas.get(2)).equals("dispositivocodigo");
  }

  private static String normalizar(String valor) {
    return valor.strip().replace("_", "").toLowerCase(Locale.ROOT);
  }

  private static List<List<String>> parsear(PushbackReader reader) throws Exception {
    var filas = new ArrayList<List<String>>();
    var fila = new ArrayList<String>(3);
    var campo = new StringBuilder();
    boolean entreComillas = false;
    boolean comillaCerrada = false;
    boolean primero = true;
    int leido;
    while ((leido = reader.read()) != -1) {
      char c = (char) leido;
      if (primero && c == '\uFEFF') { primero = false; continue; }
      primero = false;
      if (entreComillas) {
        if (c != '"') campo.append(c);
        else {
          int siguiente = reader.read();
          if (siguiente == '"') campo.append('"');
          else {
            entreComillas = false;
            comillaCerrada = true;
            if (siguiente != -1) reader.unread(siguiente);
          }
        }
      } else if (c == '"') {
        if (!campo.isEmpty() || comillaCerrada) throw invalido();
        entreComillas = true;
      } else if (c == ',' || c == '\r' || c == '\n') {
        fila.add(campo.toString()); campo.setLength(0); comillaCerrada = false;
        if (c != ',') {
          agregar(filas, fila); fila = new ArrayList<>(3);
          if (c == '\r') { int siguiente = reader.read(); if (siguiente != '\n' && siguiente != -1) reader.unread(siguiente); }
        }
      } else {
        if (comillaCerrada) throw invalido();
        campo.append(c);
      }
      if (campo.length() > 128 || fila.size() > 3 || filas.size() > MAX_FILAS + 1) throw invalido();
    }
    if (entreComillas) throw invalido();
    if (!campo.isEmpty() || !fila.isEmpty() || comillaCerrada) {
      fila.add(campo.toString());
      agregar(filas, fila);
    }
    return filas;
  }

  private static void agregar(List<List<String>> filas, List<String> fila) {
    if (fila.stream().allMatch(String::isBlank)) return;
    if (fila.size() > 3 || filas.size() >= MAX_FILAS + 1) throw invalido();
    filas.add(List.copyOf(fila));
  }

  private static ResponseStatusException invalido() {
    return new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "Archivo biométrico inválido: CSV UTF-8 o XLSX, cabecera dni,fechaHora,dispositivoCodigo, hasta 10 000 filas y fechas ISO-8601 con zona horaria");
  }
}
