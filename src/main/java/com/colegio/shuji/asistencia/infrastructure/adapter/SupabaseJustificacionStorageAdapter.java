package com.colegio.shuji.asistencia.infrastructure.adapter;

import com.colegio.shuji.asistencia.application.port.out.AlmacenJustificacionPort;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SupabaseJustificacionStorageAdapter implements AlmacenJustificacionPort {
  private final String url;
  private final String serviceRoleKey;
  private final String bucket;
  private final int signedUrlSeconds;
  private final RestClient client = RestClient.builder().build();

  public SupabaseJustificacionStorageAdapter(
      @Value("${supabase.storage.url:}") String url,
      @Value("${supabase.storage.service-role-key:}") String serviceRoleKey,
      @Value("${supabase.storage.justifications-bucket:asistencia-justificaciones}") String bucket,
      @Value("${supabase.storage.signed-url-seconds:60}") int signedUrlSeconds) {
    this.url = url == null ? "" : url.trim().replaceAll("/+$", "");
    this.serviceRoleKey = serviceRoleKey == null ? "" : serviceRoleKey.trim();
    this.bucket = bucket == null ? "" : bucket.trim();
    this.signedUrlSeconds = Math.max(30, Math.min(600, signedUrlSeconds));
  }

  @Override
  public Archivo guardar(Long asistenciaId, byte[] contenido) {
    validarConfiguracion();
    asegurarBucketPrivado();
    String clave = "asistencia/justificaciones/" + asistenciaId + "/" + UUID.randomUUID() + ".pdf";
    try {
      client.post().uri(url + "/storage/v1/object/" + bucket + "/" + clave)
          .header("apikey", serviceRoleKey).header("Authorization", "Bearer " + serviceRoleKey)
          .header("x-upsert", "false").contentType(MediaType.APPLICATION_PDF).body(contenido)
          .retrieve().toBodilessEntity();
      return new Archivo(clave);
    } catch (RestClientException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
          "No se pudo guardar el sustento en almacenamiento privado");
    }
  }

  @Override
  public String urlFirmada(String clave) {
    validarClave(clave);
    validarConfiguracion();
    asegurarBucketPrivado();
    final JsonNode response;
    try {
      response = client.post().uri(url + "/storage/v1/object/sign/" + bucket + "/" + clave)
          .header("apikey", serviceRoleKey).header("Authorization", "Bearer " + serviceRoleKey)
          .contentType(MediaType.APPLICATION_JSON).body(Map.of("expiresIn", signedUrlSeconds))
          .retrieve().body(JsonNode.class);
    } catch (RestClientException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
          "No se pudo generar el enlace privado del sustento");
    }
    String path = response == null ? null : response.path("signedURL").asText(null);
    if (path == null || !path.startsWith("/object/sign/")) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
          "Supabase no devolvió un enlace temporal válido");
    }
    return url + "/storage/v1" + path;
  }

  @Override
  public void eliminar(String clave) {
    validarClave(clave);
    validarConfiguracion();
    try {
      client.method(org.springframework.http.HttpMethod.DELETE)
          .uri(url + "/storage/v1/object/" + bucket)
          .header("apikey", serviceRoleKey).header("Authorization", "Bearer " + serviceRoleKey)
          .contentType(MediaType.APPLICATION_JSON).body(Map.of("prefixes", List.of(clave)))
          .retrieve().toBodilessEntity();
    } catch (RestClientException ex) {
      throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
          "No se pudo limpiar el archivo privado de justificación");
    }
  }

  private void validarConfiguracion() {
    if (url.isBlank() || serviceRoleKey.isBlank() || !bucket.matches("[a-z0-9-]{3,63}")) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "El almacenamiento privado de justificaciones no está configurado");
    }
  }

  private void validarClave(String clave) {
    if (clave == null || !clave.matches("asistencia/justificaciones/[1-9][0-9]{0,18}/[0-9a-fA-F-]{36}\\.pdf")) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el sustento solicitado");
    }
  }

  private void asegurarBucketPrivado() {
    try {
      JsonNode info = client.get().uri(url + "/storage/v1/bucket/" + bucket)
          .header("apikey", serviceRoleKey).header("Authorization", "Bearer " + serviceRoleKey)
          .retrieve().body(JsonNode.class);
      if (info == null || !info.has("public") || info.path("public").asBoolean(true)) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "El bucket de justificaciones debe existir y ser privado");
      }
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode().value() == 404) {
        try {
          JsonNode creado = client.post().uri(url + "/storage/v1/bucket")
              .header("apikey", serviceRoleKey).header("Authorization", "Bearer " + serviceRoleKey)
              .contentType(MediaType.APPLICATION_JSON).body(Map.of(
                  "id", bucket, "name", bucket, "public", false,
                  "fileSizeLimit", 5L * 1024 * 1024, "allowedMimeTypes", List.of("application/pdf")))
              .retrieve().body(JsonNode.class);
          if (creado != null && creado.has("public") && !creado.path("public").asBoolean(true)) return;
        } catch (RestClientException createError) {
          throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
              "No se pudo crear el bucket privado de justificaciones");
        }
      }
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "No se pudo verificar el bucket privado de justificaciones");
    } catch (RestClientException ex) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "No se pudo verificar el bucket privado de justificaciones");
    }
  }
}
