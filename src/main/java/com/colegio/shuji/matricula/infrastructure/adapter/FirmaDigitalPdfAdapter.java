package com.colegio.shuji.matricula.infrastructure.adapter;

import com.colegio.shuji.matricula.application.port.out.FirmaDigitalDocumentoPort;
import com.colegio.shuji.matricula.domain.enums.EstadoFirmaDigital;
import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.TrustAnchor;
import java.security.cert.CertificateException;
import java.security.cert.X509CertSelector;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.CertStore;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CRL;
import java.security.cert.X509CRL;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

/**
 * Política estricta para PDF/CMS: integridad completa, cadena configurada y CRL vigente.
 * Solo consulta repositorios públicos autorizados; no confía en fechas declaradas de firma.
 * No certifica autenticidad documental ni cumplimiento legal IOFE/PAdES-LTV.
 */
@Component
public class FirmaDigitalPdfAdapter implements FirmaDigitalDocumentoPort {
  private static final Set<String> DIGESTS = Set.of(
      "2.16.840.1.101.3.4.2.1", "2.16.840.1.101.3.4.2.2", "2.16.840.1.101.3.4.2.3");
  private final String certificados;
  private final String crls;
  private final ResourceLoader resources;
  private final ReniecPkiHttpSource pki;
  private final BouncyCastleProvider provider = new BouncyCastleProvider();

  public FirmaDigitalPdfAdapter(
      @Value("${app.matricula-publica.firmas.certificados-confiables:classpath:security/reniec/autoridades.pem}") String certificados,
      @Value("${app.matricula-publica.firmas.crls:}") String crls, ResourceLoader resources, ReniecPkiHttpSource pki) {
    this.certificados = certificados;
    this.crls = crls;
    this.resources = resources;
    this.pki = pki;
  }

  @Override
  public Resultado verificar(byte[] contenido, String mimeType, boolean exigirEmisorReniec) {
    if (!"application/pdf".equals(mimeType)) {
      return resultado(EstadoFirmaDigital.NO_APLICA, "La imagen no contiene una firma criptográfica PDF.", null);
    }
    try (var pdf = Loader.loadPDF(contenido)) {
      if (pdf.isEncrypted()) return resultado(EstadoFirmaDigital.NO_VERIFICABLE,
          "El PDF cifrado no puede verificarse; adjunta el original sin contraseña.", null);
      var firmas = pdf.getSignatureDictionaries();
      if (firmas.isEmpty()) return resultado(EstadoFirmaDigital.NO_APLICA,
          "El PDF no contiene una firma digital.", null);
      if (firmas.size() > 16) return resultado(EstadoFirmaDigital.NO_VERIFICABLE,
          "El documento supera el límite de firmas admitido.", null);
      boolean cubreTodo = false;
      Resultado pendiente = null;
      String huella = null;
      for (var firma : firmas) {
        if (!"adbe.pkcs7.detached".equals(firma.getSubFilter())
            && !"ETSI.CAdES.detached".equals(firma.getSubFilter())) {
          pendiente = resultado(EstadoFirmaDigital.NO_VERIFICABLE,
              "Formato de firma no admitido; requiere un validador especializado.", huella);
          continue;
        }
        if (!rangoValido(firma, contenido)) {
          return resultado(EstadoFirmaDigital.INVALIDA, "El rango firmado no corresponde al contenido de firma.", null);
        }
        int[] rango = firma.getByteRange();
        cubreTodo |= (long) rango[2] + rango[3] == contenido.length;
        var cms = new CMSSignedData(new CMSProcessableByteArray(firma.getSignedContent(contenido)),
            new ByteArrayInputStream(firma.getContents(contenido)));
        if (cms.getCertificates().getMatches(null).size() > 32 || cms.getSignerInfos().size() > 4)
          return resultado(EstadoFirmaDigital.NO_VERIFICABLE, "La firma supera los límites de certificados o firmantes admitidos.", null);
        if (cms.getSignerInfos().size() == 0) {
          return resultado(EstadoFirmaDigital.INVALIDA, "La firma no identifica ningún firmante.", null);
        }
        for (SignerInformation signer : cms.getSignerInfos().getSigners()) {
          if (!DIGESTS.contains(signer.getDigestAlgOID())) {
            return resultado(EstadoFirmaDigital.INVALIDA, "Algoritmo de resumen no admitido (se requiere SHA-256 o superior).", null);
          }
          var matches = certificadosFirmante(cms, signer);
          if (matches.size() != 1) return resultado(EstadoFirmaDigital.NO_VERIFICABLE,
              "No se pudo identificar un certificado único para la firma.", null);
          var holder = matches.iterator().next();
          huella = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(holder.getEncoded()));
          if (!signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider(provider).build(holder))) {
            return resultado(EstadoFirmaDigital.INVALIDA, "La firma criptográfica no coincide con los bytes del PDF.", huella);
          }
          var confianza = validarCertificado(holder, cms, exigirEmisorReniec);
          if (confianza.estado() == EstadoFirmaDigital.INVALIDA) return confianza;
          if (confianza.estado() != EstadoFirmaDigital.VALIDA) pendiente = confianza;
        }
      }
      if (!cubreTodo) return resultado(EstadoFirmaDigital.NO_VERIFICABLE,
          "Existen bytes o revisiones posteriores a la última firma admitida.", huella);
      return pendiente != null ? pendiente : resultado(EstadoFirmaDigital.VALIDA,
          "Integridad, vigencia, cadena y revocación por CRL verificadas a la fecha actual.", huella);
    } catch (java.io.IOException | org.bouncycastle.cms.CMSException ex) {
      return resultado(EstadoFirmaDigital.INVALIDA, "El PDF o su firma CMS están dañados o no pueden interpretarse.", null);
    } catch (Exception ex) {
      return resultado(EstadoFirmaDigital.ERROR_SERVICIO,
          "No se pudo completar la verificación; vuelve a intentar más tarde.", null);
    }
  }

  private boolean rangoValido(PDSignature firma, byte[] bytes) {
    int[] r = firma.getByteRange();
    if (r == null || r.length != 4 || r[0] != 0 || r[1] < 0 || r[2] <= r[1]
        || r[3] < 0 || (long) r[2] + r[3] > bytes.length || r[2] > bytes.length) return false;
    if (bytes[r[1]] != '<' || bytes[r[2] - 1] != '>') return false;
    var contenido = firma.getCOSObject().getDictionaryObject(COSName.CONTENTS);
    if (!(contenido instanceof COSString cadena)) return false;
    try {
      String hex = new String(bytes, r[1] + 1, r[2] - r[1] - 2,
          java.nio.charset.StandardCharsets.US_ASCII).replaceAll("\\s", "");
      return Arrays.equals(HexFormat.of().parseHex(hex), cadena.getBytes());
    } catch (IllegalArgumentException ex) {
      return false;
    }
  }

  private Resultado validarCertificado(X509CertificateHolder holder, CMSSignedData cms, boolean exigirEmisorReniec) throws Exception {
    var factory = CertificateFactory.getInstance("X.509");
    var leaf = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(holder.getEncoded()));
    String huella = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(leaf.getEncoded()));
    Date ahora = new Date();
    if (exigirEmisorReniec) {
      var organizaciones = holder.getSubject().getRDNs(org.bouncycastle.asn1.x500.style.BCStyle.O);
      boolean emisorReniec = Arrays.stream(organizaciones).anyMatch(rdn -> {
        String valor = java.text.Normalizer.normalize(rdn.getFirst().getValue().toString(), java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        return valor.equals("RENIEC") || valor.equals("REGISTRONACIONALDEIDENTIFICACIONYESTADOCIVIL")
            || valor.equals("REGISTRONACIONALDEIDENTIFICACIONYESTADOCIVILRENIEC");
      });
      if (!emisorReniec) return resultado(EstadoFirmaDigital.INVALIDA,
          "La partida no está firmada por un certificado institucional de RENIEC.", huella);
    }
    try {
      leaf.checkValidity(ahora);
    } catch (CertificateExpiredException | CertificateNotYetValidException ex) {
      return resultado(EstadoFirmaDigital.INVALIDA, "El certificado no está vigente a la fecha de revisión.", huella);
    }
    boolean[] uso = leaf.getKeyUsage();
    if (uso != null && !uso[0] && (uso.length < 2 || !uso[1])) {
      return resultado(EstadoFirmaDigital.INVALIDA, "El certificado no permite firma digital.", huella);
    }
    if (certificados == null || certificados.isBlank()) return resultado(EstadoFirmaDigital.NO_VERIFICABLE,
        "Integridad correcta; faltan autoridades de confianza institucionales.", huella);
    Set<TrustAnchor> anchors = new HashSet<>();
    try (var stream = recursoLocal(certificados)) {
      for (var cert : factory.generateCertificates(stream)) {
        var ca = (X509Certificate) cert;
        if (ca.getBasicConstraints() < 0) throw new CertificateException("Ancla no CA");
        ca.checkValidity(ahora);
        anchors.add(new TrustAnchor(ca, null));
      }
    }
    if (anchors.isEmpty()) throw new CertificateException("Sin autoridades de confianza");
    List<X509Certificate> cadena = new ArrayList<>();
    for (var cert : cms.getCertificates().getMatches(null)) {
      cadena.add((X509Certificate) factory.generateCertificate(new ByteArrayInputStream(cert.getEncoded())));
    }
    anchors.forEach(a -> cadena.add(a.getTrustedCert()));
    pki.completarCadena(cadena, factory);
    var selector = new X509CertSelector();
    selector.setCertificate(leaf);
    var params = new PKIXBuilderParameters(anchors, selector);
    params.setDate(ahora);
    params.setRevocationEnabled(false); // Se verifica exclusivamente con CRL local abajo.
    params.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(cadena)));
    final PKIXCertPathBuilderResult path;
    try {
      path = (PKIXCertPathBuilderResult) CertPathBuilder.getInstance("PKIX").build(params);
    } catch (CertPathBuilderException ex) {
      return resultado(EstadoFirmaDigital.NO_VERIFICABLE, "No se pudo establecer una cadena de confianza admitida.", huella);
    }
    List<CRL> listas = new ArrayList<>();
    if (crls != null && !crls.isBlank()) {
      try (var stream = recursoLocal(crls)) {
        listas.addAll(factory.generateCRLs(stream));
      }
    }
    var certificadosCadena = path.getCertPath().getCertificates();
    for (int i = 0; i < certificadosCadena.size(); i++) {
      var cert = (X509Certificate) certificadosCadena.get(i);
      listas.addAll(pki.buscarCrl(cert));
      var issuer = i + 1 < certificadosCadena.size() ? (X509Certificate) certificadosCadena.get(i + 1)
          : path.getTrustAnchor().getTrustedCert();
      boolean comprobado = false;
      for (var item : listas) {
        var crl = (X509CRL) item;
        if (!crl.getIssuerX500Principal().equals(issuer.getSubjectX500Principal())
            || crl.getThisUpdate().after(ahora) || crl.getNextUpdate() == null || crl.getNextUpdate().before(ahora)
            || crl.getExtensionValue("2.5.29.28") != null || crl.getExtensionValue("2.5.29.27") != null
            || crl.hasUnsupportedCriticalExtension()) continue;
        boolean[] issuerUso = issuer.getKeyUsage();
        if (issuerUso != null && (issuerUso.length < 7 || !issuerUso[6])) continue;
        try { crl.verify(issuer.getPublicKey()); } catch (java.security.GeneralSecurityException ex) { continue; }
        if (crl.isRevoked(cert)) return resultado(EstadoFirmaDigital.INVALIDA, "El certificado fue revocado.", huella);
        comprobado = true;
      }
      if (!comprobado) return resultado(EstadoFirmaDigital.NO_VERIFICABLE,
          "Falta una CRL completa, vigente y verificable para la cadena.", huella);
    }
    return resultado(EstadoFirmaDigital.VALIDA, "Certificado y revocación verificados.", huella);
  }

  private java.io.InputStream recursoLocal(String ruta) throws java.io.IOException {
    if (!(ruta.startsWith("file:") || ruta.startsWith("classpath:"))) {
      throw new java.io.IOException("Solo se admiten certificados y CRL locales");
    }
    return resources.getResource(ruta).getInputStream();
  }

  private Resultado resultado(EstadoFirmaDigital estado, String detalle, String huella) {
    return new Resultado(estado, detalle, huella);
  }

  @SuppressWarnings("unchecked") // SignerId de Bouncy Castle conserva una interfaz Selector sin genéricos.
  private Collection<X509CertificateHolder> certificadosFirmante(CMSSignedData cms, SignerInformation signer) {
    return cms.getCertificates().getMatches(signer.getSID());
  }
}
