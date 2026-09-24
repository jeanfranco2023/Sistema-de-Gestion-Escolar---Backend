package com.colegio.shuji.matricula.infrastructure.adapter;

import com.colegio.shuji.matricula.application.port.out.ReniecServicePort;
import com.fasterxml.jackson.databind.JsonNode;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptador para proveedores externos de consulta de DNI; no acredita validación oficial.
 */
@Component
public class ReniecHttpAdapter implements ReniecServicePort {
  private final RestClient client;
  private final String url;

  public ReniecHttpAdapter(
      @Value("${integraciones.reniec.url:}") String url,
      @Value("${integraciones.reniec.token:}") String token,
      @Value("${integraciones.reniec.api-key:}") String apiKey) {
    this.url = url.trim();
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
    factory.setReadTimeout(Duration.ofSeconds(5));
    var builder = RestClient.builder().requestFactory(factory);
    String credencial = apiKey.isBlank() ? token.trim() : apiKey.trim();
    if (!credencial.isBlank()) builder.defaultHeader("Authorization", "Bearer " + credencial);
    this.client = builder.build();
  }

  public Optional<Identidad> consultar(String dni) {
    if (url.isBlank() || dni == null || !dni.matches("[0-9]{8}")) return Optional.empty();
    try {
      JsonNode body;
      if (url.contains("{dni}")) {
        body = client.get().uri(url, dni).retrieve().body(JsonNode.class);
      } else {
        body = client.post().uri(url).contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("dni", dni)).retrieve().body(JsonNode.class);
      }
      if (body == null) return Optional.empty();
      JsonNode data = body.has("data") && body.get("data").isObject() ? body.get("data") : body;
      String documento = campo(data, "numeroDocumento", "document_number", "numero", "dni");
      if (documento != null && !dni.equals(documento)) return Optional.empty();
      String nombres = campo(data, "nombres", "first_name", "nombre");
      String paterno = campo(data, "apellidoPaterno", "apellido_paterno", "first_last_name");
      String materno = campo(data, "apellidoMaterno", "apellido_materno", "second_last_name");
      if (nombres == null || paterno == null || materno == null) return Optional.empty();
      return Optional.of(new Identidad(dni, nombres, paterno, materno));
    } catch (RestClientException e) {
      return Optional.empty();
    }
  }

  private static String campo(JsonNode data, String... keys) {
    for (String key : keys) {
      JsonNode value = data.get(key);
      if (value != null && value.isTextual() && !value.asText().isBlank())
        return value.asText().trim();
    }
    return null;
  }
}
