package com.colegio.shuji.matricula.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import com.colegio.shuji.matricula.domain.enums.EstadoFirmaDigital;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.bouncycastle.util.CollectionStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

class FirmaDigitalPdfAdapterTest {
  static final BouncyCastleProvider BC = new BouncyCastleProvider();
  static KeyPair rootKey, leafKey;
  static X509CertificateHolder root, leaf;
  static final X500Name CA = new X500Name("CN=CA Prueba,O=Pruebas,C=PE");
  @TempDir Path temp;

  @BeforeAll static void certificados() throws Exception {
    var keys = KeyPairGenerator.getInstance("RSA");
    keys.initialize(2048);
    rootKey = keys.generateKeyPair();
    leafKey = keys.generateKeyPair();
    root = certificado(CA, CA, rootKey, rootKey, BigInteger.ONE, true);
    leaf = certificado(CA, new X500Name("CN=Emisor Prueba,O=RENIEC,C=PE"),
        leafKey, rootKey, BigInteger.TWO, false);
  }

  static X509CertificateHolder certificado(X500Name issuer, X500Name subject, KeyPair key,
      KeyPair signer, BigInteger serial, boolean ca) throws Exception {
    var builder = new JcaX509v3CertificateBuilder(issuer, serial, Date.from(Instant.now().minusSeconds(3600)),
        Date.from(Instant.now().plusSeconds(86400)), subject, key.getPublic());
    builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(ca));
    builder.addExtension(Extension.keyUsage, true, new KeyUsage(ca ? KeyUsage.keyCertSign | KeyUsage.cRLSign : KeyUsage.digitalSignature));
    return builder.build(new JcaContentSignerBuilder("SHA256withRSA").setProvider(BC).build(signer.getPrivate()));
  }

  FirmaDigitalPdfAdapter adapter(boolean configurarCrl, boolean revocado, boolean vencida) throws Exception {
    Path ca = temp.resolve("ca.cer");
    Files.write(ca, root.getEncoded());
    String crlPath = "";
    if (configurarCrl) {
      var crl = new X509v2CRLBuilder(CA, Date.from(Instant.now().minusSeconds(3600)));
      crl.setNextUpdate(Date.from(Instant.now().plusSeconds(vencida ? -60 : 3600)));
      if (revocado) crl.addCRLEntry(leaf.getSerialNumber(), Date.from(Instant.now().minusSeconds(100)), 1);
      Path lista = temp.resolve("ca.crl");
      Files.write(lista, crl.build(new JcaContentSignerBuilder("SHA256withRSA").setProvider(BC).build(rootKey.getPrivate())).getEncoded());
      crlPath = lista.toUri().toString();
    }
    return new FirmaDigitalPdfAdapter(ca.toUri().toString(), crlPath, new DefaultResourceLoader(), new ReniecPkiHttpSource(false));
  }

  byte[] pdfSinFirma() throws Exception {
    try (var pdf = new PDDocument(); var out = new ByteArrayOutputStream()) {
      pdf.addPage(new PDPage());
      pdf.getDocumentCatalog().getCOSObject().setString(COSName.getPDFName("DatoPrueba"), "DOCUMENTO-ORIGINAL");
      pdf.save(out);
      return out.toByteArray();
    }
  }

  byte[] pdfFirmado(X509CertificateHolder certificado, String algoritmo) throws Exception {
    try (var pdf = Loader.loadPDF(pdfSinFirma()); var out = new ByteArrayOutputStream()) {
      var firma = new PDSignature();
      firma.setFilter(PDSignature.FILTER_ADOBE_PPKLITE);
      firma.setSubFilter(PDSignature.SUBFILTER_ADBE_PKCS7_DETACHED);
      pdf.addSignature(firma, contenido -> {
        try {
          var generator = new CMSSignedDataGenerator();
          var signer = new JcaContentSignerBuilder(algoritmo).setProvider(BC).build(leafKey.getPrivate());
          generator.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
              new JcaDigestCalculatorProviderBuilder().setProvider(BC).build()).build(signer, certificado));
          generator.addCertificates(new CollectionStore<>(List.of(certificado, root)));
          return generator.generate(new CMSProcessableByteArray(contenido.readAllBytes()), false).getEncoded();
        } catch (Exception ex) { throw new java.io.IOException(ex); }
      });
      pdf.saveIncremental(out);
      return out.toByteArray();
    }
  }

  @Test void verificaPdfCmsConCadenaYRevocacion() throws Exception {
    assertEquals(EstadoFirmaDigital.VALIDA, adapter(true, false, false).verificar(pdfFirmado(leaf, "SHA256withRSA"), "application/pdf", true).estado());
  }
  @Test void detectaAlteracionDeBytesFirmados() throws Exception {
    var bytes = pdfFirmado(leaf, "SHA256withRSA");
    String texto = new String(bytes, StandardCharsets.ISO_8859_1);
    int indice = texto.indexOf("DOCUMENTO-ORIGINAL");
    bytes[indice] = 'X';
    assertEquals(EstadoFirmaDigital.INVALIDA, adapter(true, false, false).verificar(bytes, "application/pdf").estado());
  }
  @Test void noApruebaRevisionPosteriorSinFirma() throws Exception {
    var bytes = pdfFirmado(leaf, "SHA256withRSA");
    var out = new ByteArrayOutputStream();
    out.write(bytes); out.write("\n% cambio posterior\n".getBytes(StandardCharsets.US_ASCII));
    assertEquals(EstadoFirmaDigital.NO_VERIFICABLE, adapter(true, false, false).verificar(out.toByteArray(), "application/pdf").estado());
  }
  @Test void rechazaCertificadoRevocado() throws Exception {
    assertEquals(EstadoFirmaDigital.INVALIDA, adapter(true, true, false).verificar(pdfFirmado(leaf, "SHA256withRSA"), "application/pdf").estado());
  }
  @Test void noApruebaSinCrlVigente() throws Exception {
    assertEquals(EstadoFirmaDigital.NO_VERIFICABLE, adapter(false, false, false).verificar(pdfFirmado(leaf, "SHA256withRSA"), "application/pdf").estado());
    assertEquals(EstadoFirmaDigital.NO_VERIFICABLE, adapter(true, false, true).verificar(pdfFirmado(leaf, "SHA256withRSA"), "application/pdf").estado());
  }
  @Test void noConfiaEnCertificadoSoloPorEstarIncluidoEnPdf() throws Exception {
    var sinTrust = new FirmaDigitalPdfAdapter("", "", new DefaultResourceLoader(), new ReniecPkiHttpSource(false));
    assertEquals(EstadoFirmaDigital.NO_VERIFICABLE, sinTrust.verificar(pdfFirmado(leaf, "SHA256withRSA"), "application/pdf").estado());
  }
  @Test void firmaPersonalNoPuedeHacersePasarPorReniec() throws Exception {
    var personal = certificado(CA, new X500Name("CN=Persona Prueba,O=Persona,C=PE"), leafKey, rootKey, BigInteger.valueOf(3), false);
    assertEquals(EstadoFirmaDigital.INVALIDA, adapter(true, false, false).verificar(pdfFirmado(personal, "SHA256withRSA"), "application/pdf", true).estado());
  }
  @Test void rechazaAlgoritmoDebil() throws Exception {
    assertEquals(EstadoFirmaDigital.INVALIDA, adapter(true, false, false).verificar(pdfFirmado(leaf, "SHA1withRSA"), "application/pdf").estado());
  }
  @Test void noInventaFirmaParaImagenNiPdfSinFirma() throws Exception {
    var validator = adapter(true, false, false);
    assertEquals(EstadoFirmaDigital.NO_APLICA, validator.verificar(new byte[]{1}, "image/jpeg").estado());
    assertEquals(EstadoFirmaDigital.NO_APLICA, validator.verificar(pdfSinFirma(), "application/pdf").estado());
    assertEquals(EstadoFirmaDigital.INVALIDA, validator.verificar("%PDF-invalido".getBytes(), "application/pdf").estado());
  }
  @Test void bloqueaUrlsAjenasALosRepositoriosOficiales() {
    var source = new ReniecPkiHttpSource(true);
    assertFalse(source.descargar("http://127.0.0.1/admin").isPresent());
    assertFalse(source.descargar("http://169.254.169.254/latest/meta-data").isPresent());
    assertFalse(source.descargar("https://crl.reniec.gob.pe.ejemplo.com/falsa.crl").isPresent());
    assertFalse(source.descargar("https://cdn.www.gob.pe/no-admitido/crl").isPresent());
  }
}
