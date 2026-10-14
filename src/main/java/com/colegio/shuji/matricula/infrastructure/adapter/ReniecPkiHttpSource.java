package com.colegio.shuji.matricula.infrastructure.adapter;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.cert.X509CRL;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Consulta solo certificados/CRL públicos; nunca transmite el PDF ni datos del alumno. */
@Component
public class ReniecPkiHttpSource {
  private static final Set<String> HOSTS = Set.of("www.reniec.gob.pe", "crl.reniec.gob.pe", "pki.reniec.gob.pe", "cdn.www.gob.pe");
  private static final int MAX_BYTES = 8 * 1024 * 1024;
  private final boolean habilitado;
  private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3))
      .followRedirects(HttpClient.Redirect.NEVER).build();
  private final Map<String, Cache> cache = new ConcurrentHashMap<>();
  private record Cache(byte[] bytes, Instant expira) {}

  public ReniecPkiHttpSource(@Value("${app.matricula-publica.firmas.consulta-publica-habilitada:true}") boolean habilitado) {
    this.habilitado = habilitado;
  }

  public void completarCadena(List<X509Certificate> certificados, CertificateFactory factory) {
    for (int i = 0; i < certificados.size() && i < 8; i++) {
      var cert = certificados.get(i);
      if (certificados.stream().anyMatch(c -> c.getSubjectX500Principal().equals(cert.getIssuerX500Principal()))) continue;
      try {
        byte[] ext = cert.getExtensionValue(Extension.authorityInfoAccess.getId());
        if (ext == null) continue;
        var aia = AuthorityInformationAccess.getInstance(JcaX509ExtensionUtils.parseExtensionValue(ext));
        for (var info : aia.getAccessDescriptions()) {
          if (!AccessDescription.id_ad_caIssuers.equals(info.getAccessMethod())
              || info.getAccessLocation().getTagNo() != GeneralName.uniformResourceIdentifier) continue;
          var bytes = descargar(info.getAccessLocation().getName().toString());
          if (bytes.isEmpty()) continue;
          for (var fetched : factory.generateCertificates(new ByteArrayInputStream(bytes.get()))) {
            var candidato = (X509Certificate) fetched;
            if (certificados.size() < 8 && !certificados.contains(candidato)) certificados.add(candidato);
          }
        }
      } catch (Exception ex) {
        // Ausencia de cadena queda como NO_VERIFICABLE, nunca como VALIDA.
      }
    }
  }

  public List<X509CRL> buscarCrl(X509Certificate cert) {
    List<X509CRL> result = new ArrayList<>();
    try {
      byte[] ext = cert.getExtensionValue(Extension.cRLDistributionPoints.getId());
      if (ext == null) return result;
      var distribution = CRLDistPoint.getInstance(JcaX509ExtensionUtils.parseExtensionValue(ext));
      for (var point : distribution.getDistributionPoints()) {
        if (point.getDistributionPoint() == null || point.getDistributionPoint().getType() != DistributionPointName.FULL_NAME) continue;
        var names = GeneralNames.getInstance(point.getDistributionPoint().getName()).getNames();
        for (var name : names) {
          if (name.getTagNo() != GeneralName.uniformResourceIdentifier) continue;
          var bytes = descargar(name.getName().toString());
          if (bytes.isPresent()) result.add((X509CRL) CertificateFactory.getInstance("X.509")
              .generateCRL(new ByteArrayInputStream(bytes.get())));
          if (result.size() >= 3) return result;
        }
      }
    } catch (Exception ex) {
      // Respuesta no interpretable deja la revocación sin confirmar.
    }
    return result;
  }

  Optional<byte[]> descargar(String url) {
    if (!habilitado) return Optional.empty();
    try {
      URI uri = URI.create(url);
      if (!Set.of("http", "https").contains(uri.getScheme()) || uri.getUserInfo() != null
          || uri.getHost() == null || !HOSTS.contains(uri.getHost().toLowerCase(Locale.ROOT))
          || (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443)
          || ("cdn.www.gob.pe".equals(uri.getHost()) && !uri.getPath().startsWith("/ecernep/"))) return Optional.empty();
      var item = cache.get(url);
      if (item != null && item.expira().isAfter(Instant.now())) return Optional.of(item.bytes());
      for (var address : InetAddress.getAllByName(uri.getHost())) {
        byte[] ip = address.getAddress();
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isSiteLocalAddress()
            || address.isLinkLocalAddress() || address.isMulticastAddress()
            || (ip.length == 16 && (ip[0] & 0xfe) == 0xfc)) return Optional.empty();
      }
      var response = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(5)).GET().build(),
          HttpResponse.BodyHandlers.ofInputStream());
      try (var body = response.body()) {
        if (response.statusCode() != 200) return Optional.empty();
        var bytes = body.readNBytes(MAX_BYTES + 1);
        if (bytes.length == 0 || bytes.length > MAX_BYTES) return Optional.empty();
        if (cache.size() >= 64) cache.clear();
        cache.put(url, new Cache(bytes, Instant.now().plusSeconds(300)));
        return Optional.of(bytes);
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return Optional.empty();
    } catch (Exception ex) {
      return Optional.empty();
    }
  }
}
