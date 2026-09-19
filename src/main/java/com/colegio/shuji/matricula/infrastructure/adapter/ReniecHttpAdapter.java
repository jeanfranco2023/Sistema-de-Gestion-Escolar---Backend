package com.colegio.shuji.matricula.infrastructure.adapter;

import com.colegio.shuji.matricula.application.port.out.ReniecServicePort;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Adaptador para un proxy RENIEC con contrato Identidad; contingencia explícita si no está
 * disponible.
 */
@Component
public class ReniecHttpAdapter implements ReniecServicePort {
  private final RestClient client;
  private final String url;

  public ReniecHttpAdapter(
      @Value("${integraciones.reniec.url:}") String url,
      @Value("${integraciones.reniec.token:}") String token) {
    this.url = url;
    var factory =
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build());
    factory.setReadTimeout(Duration.ofSeconds(5));
    this.client =
        RestClient.builder()
            .requestFactory(factory)
            .defaultHeader("Authorization", "Bearer " + token)
            .build();
  }

  public Optional<Identidad> consultar(String dni) {
    if (url.isBlank()) return Optional.empty();
    try {
      var d = client.get().uri(url + "/{dni}", dni).retrieve().body(Identidad.class);
      if (d == null
          || d.nombres() == null
          || d.apellidoPaterno() == null
          || d.apellidoMaterno() == null) return Optional.empty();
      return Optional.of(d);
    } catch (RestClientException e) {
      return Optional.empty();
    }
  }
}
