package com.colegio.shuji.usuario.infrastructure.adapter;

import com.colegio.shuji.usuario.application.port.out.ValidadorDocumentoEmpleadoPort;
import com.colegio.shuji.usuario.domain.enums.EstadoValidacionIaEmpleado;
import com.colegio.shuji.usuario.domain.enums.TipoDocumentoEmpleado;
import com.colegio.shuji.usuario.domain.exception.ReglaNegocioException;
import com.colegio.shuji.usuario.domain.model.ResultadoValidacionDocumentoEmpleado;
import com.colegio.shuji.usuario.domain.model.Usuario;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class PdfBoxGeminiValidadorDocumentoEmpleadoAdapter implements ValidadorDocumentoEmpleadoPort {

  private static final String MOTOR_GEMINI = "Gemini-IA";
  private static final String MOTOR_NO_DISPONIBLE = "No disponible";

  private final ObjectMapper json;
  private final RestClient restClient;
  private final List<String> geminiApiKeys;
  private final AtomicInteger apiKeyCursor = new AtomicInteger();
  private final String geminiModel;

  @Autowired
  public PdfBoxGeminiValidadorDocumentoEmpleadoAdapter(
      ObjectMapper json,
      @Value("${app.matricula-publica.gemini.api-key:}") String apiKey,
      @Value("${app.matricula-publica.gemini.api-keys:}") String configuredApiKeys,
      @Value("${app.matricula-publica.gemini.model:gemini-2.5-flash}") String geminiModel) {
    this(json, apiKey, configuredApiKeys, geminiModel, crearRestClient());
  }

  PdfBoxGeminiValidadorDocumentoEmpleadoAdapter(
      ObjectMapper json,
      String apiKey,
      String configuredApiKeys,
      String geminiModel,
      RestClient restClient) {
    this.json = json;
    this.geminiModel = geminiModel;
    this.restClient = restClient;
    var keys = new LinkedHashSet<String>();
    if (configuredApiKeys != null && !configuredApiKeys.isBlank()) {
      for (String key : configuredApiKeys.split(",")) {
        if (!key.trim().isBlank()) keys.add(key.trim());
      }
    }
    if (apiKey != null && !apiKey.isBlank()) keys.add(apiKey.trim());
    this.geminiApiKeys = List.copyOf(keys);

  }

  private static RestClient crearRestClient() {
    var factory = new JdkClientHttpRequestFactory(
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build());
    factory.setReadTimeout(Duration.ofSeconds(30));
    return RestClient.builder().requestFactory(factory).build();
  }

  @Override
  public ResultadoValidacionDocumentoEmpleado validar(TipoDocumentoEmpleado tipo, byte[] bytes, Usuario usuario) {
    int paginas = contarPaginas(bytes);
    if (geminiApiKeys.isEmpty()) {
      return observado(paginas, "La validación IA no está configurada; requiere revisión manual.", MOTOR_NO_DISPONIBLE);
    }
    try {
      return consultarGemini(tipo, bytes, usuario, paginas);
    } catch (Exception ex) {
      log.warn("La validación IA del expediente falló; el documento requiere revisión manual.");
      return observado(paginas, "No se pudo completar la validación IA; requiere revisión manual.", MOTOR_NO_DISPONIBLE);
    }
  }

  private int contarPaginas(byte[] bytes) {
    try (PDDocument documento = Loader.loadPDF(bytes)) {
      int paginas = documento.getNumberOfPages();
      if (paginas < 1) throw new IOException("PDF sin páginas");
      return paginas;
    } catch (Exception ex) {
      throw new ReglaNegocioException("El documento no tiene una estructura PDF válida o está dañado", "PDF_INVALIDO");
    }
  }

  private ResultadoValidacionDocumentoEmpleado consultarGemini(
      TipoDocumentoEmpleado tipo, byte[] bytes, Usuario usuario, int paginas) throws IOException {
    String dni = usuario.getDni() != null ? usuario.getDni() : "No registrado";
    String nombre = usuario.getUsername() != null ? usuario.getUsername() : "";
    String prompt = """
        Evalúa este documento laboral de empleado para el colegio I.E.P. Shuji Kitamura.
        Documento esperado: %s.
        DNI declarado del empleado: %s; Nombre de usuario: %s.
        Responde exclusivamente un objeto JSON con tipoCorrecto, legible, identidadCoincide y observacion.
        Verifica si el documento corresponde al tipo solicitado y si la identidad o DNI coincide razonablemente.
        """.formatted(tipo.name(), dni, nombre);

    Map<String, Object> inline = Map.of(
        "mime_type", "application/pdf",
        "data", Base64.getEncoder().encodeToString(bytes));
    Map<String, Object> body = Map.of(
        "contents", List.of(Map.of("parts", List.of(Map.of("inline_data", inline), Map.of("text", prompt)))),
        "generationConfig", Map.of(
            "responseMimeType", "application/json",
            "responseSchema", Map.of(
                "type", "OBJECT",
                "properties", Map.of(
                    "tipoCorrecto", Map.of("type", "BOOLEAN"),
                    "legible", Map.of("type", "BOOLEAN"),
                    "identidadCoincide", Map.of("type", "BOOLEAN"),
                    "observacion", Map.of("type", "STRING")),
                "required", List.of("tipoCorrecto", "legible", "identidadCoincide", "observacion"))));

    Map<String, Object> response = restClient.post()
        .uri("https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent", geminiModel)
        .header("x-goog-api-key", siguienteApiKey())
        .contentType(MediaType.APPLICATION_JSON)
        .body(body)
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});
    if (response == null) return observado(paginas, "La IA no devolvió un resultado; requiere revisión manual.", MOTOR_GEMINI);

    JsonNode raiz = json.valueToTree(response);
    String rawJson = raiz.path("candidates").path(0).path("content").path("parts").path(0).path("text").textValue();
    if (rawJson == null || rawJson.isBlank()) {
      return observado(paginas, "La IA devolvió una respuesta vacía; requiere revisión manual.", MOTOR_GEMINI);
    }

    JsonNode resultado = json.readTree(rawJson);
    JsonNode tipoCorrecto = resultado.get("tipoCorrecto");
    JsonNode legible = resultado.get("legible");
    JsonNode identidad = resultado.get("identidadCoincide");
    if (!esBooleano(tipoCorrecto) || !esBooleano(legible) || !esBooleano(identidad)) {
      return observado(paginas, "La respuesta IA es incompleta; requiere revisión manual.", MOTOR_GEMINI);
    }

    boolean valida = tipoCorrecto.booleanValue() && legible.booleanValue() && identidad.booleanValue();
    String observacion = resultado.path("observacion").asText();
    if (observacion.isBlank()) {
      observacion = valida
          ? "La IA validó tipo documental, legibilidad e identidad."
          : "La IA observó el documento; requiere revisión manual.";
    }
    return new ResultadoValidacionDocumentoEmpleado(
        valida ? EstadoValidacionIaEmpleado.VALIDADO_IA : EstadoValidacionIaEmpleado.OBSERVADO_IA,
        observacion,
        identidad.booleanValue(),
        MOTOR_GEMINI,
        paginas);
  }

  private static boolean esBooleano(JsonNode node) {
    return node != null && node.isBoolean();
  }

  private String siguienteApiKey() {
    return geminiApiKeys.get(Math.floorMod(apiKeyCursor.getAndIncrement(), geminiApiKeys.size()));
  }

  private static ResultadoValidacionDocumentoEmpleado observado(int paginas, String observacion, String motor) {
    return new ResultadoValidacionDocumentoEmpleado(
        EstadoValidacionIaEmpleado.OBSERVADO_IA, observacion, false, motor, paginas);
  }
}
