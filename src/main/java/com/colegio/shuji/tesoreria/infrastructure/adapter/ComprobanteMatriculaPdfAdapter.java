package com.colegio.shuji.tesoreria.infrastructure.adapter;

import com.colegio.shuji.tesoreria.application.dto.out.ComprobanteMatriculaPdfDto;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Generador de voucher institucional y boleta electrónica de matrícula escolar en formato PDF
 * utilizando Apache PDFBox 3.x.
 */
@Component
public class ComprobanteMatriculaPdfAdapter {

  private static final Logger log = LoggerFactory.getLogger(ComprobanteMatriculaPdfAdapter.class);
  private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("es-PE"));

  public byte[] generarPdf(ComprobanteMatriculaPdfDto dto) {
    return generarPdf(dto, null, null);
  }

  public byte[] generarPdf(ComprobanteMatriculaPdfDto dto, String usuario, String claveTemporal) {
    return generarPdf(dto, usuario, claveTemporal, "PAGO DE MATRÍCULA ESCOLAR");
  }

  public byte[] generarConstanciaExtraordinaria(ComprobanteMatriculaPdfDto dto) {
    return generarPdf(dto, null, null, "PAGO EXTRAORDINARIO ESCOLAR");
  }

  private byte[] generarPdf(ComprobanteMatriculaPdfDto dto, String usuario, String claveTemporal, String titulo) {
    try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      PDPage page = new PDPage(PDRectangle.A4);
      doc.addPage(page);

      PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
      PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
      PDType1Font fontOblique = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

      try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
        float pageWidth = page.getMediaBox().getWidth();
        float pageHeight = page.getMediaBox().getHeight();

        // 1. Barra superior institucional (Azul Navy #14213D)
        cs.setNonStrokingColor(20 / 255f, 33 / 255f, 61 / 255f);
        cs.addRect(0, pageHeight - 85, pageWidth, 85);
        cs.fill();

        // Acento Dorado #C49A58
        cs.setNonStrokingColor(196 / 255f, 154 / 255f, 88 / 255f);
        cs.addRect(0, pageHeight - 90, pageWidth, 5);
        cs.fill();

        // Título del Colegio en la barra
        dibujarTexto(cs, fontBold, 18, 40, pageHeight - 40, "I.E.P. SHUJI KITAMURA", 1f, 1f, 1f);
        dibujarTexto(cs, fontRegular, 9, 40, pageHeight - 55, "GESTIÓN ESCOLAR  /  TESORERÍA", 0.85f, 0.85f, 0.85f);

        // Folio de la constancia
        float folioX = pageWidth - 220;
        float folioY = pageHeight - 164;
        float folioW = 180;
        float folioH = 59;

        cs.setNonStrokingColor(248 / 255f, 250 / 255f, 252 / 255f);
        cs.addRect(folioX, folioY, folioW, folioH);
        cs.fill();

        cs.setStrokingColor(20 / 255f, 33 / 255f, 61 / 255f);
        cs.setLineWidth(1.5f);
        cs.addRect(folioX, folioY, folioW, folioH);
        cs.stroke();

        dibujarTextoCentrado(cs, fontBold, 10, folioX + (folioW / 2), folioY + 38, "CONSTANCIA DE PAGO", 0.47f, 0.31f, 0.12f);
        dibujarTextoCentrado(cs, fontBold, 12, folioX + (folioW / 2), folioY + 18, dto.numeroComprobante(), 20 / 255f, 33 / 255f, 61 / 255f);

        // Subtítulo del comprobante
        float currentY = pageHeight - 120;
        dibujarTexto(cs, fontBold, 13, 40, currentY, titulo, 20 / 255f, 33 / 255f, 61 / 255f);
        currentY -= 16;
        String fechaStr = dto.fechaEmision() != null ? dto.fechaEmision().atZoneSameInstant(java.time.ZoneId.of("America/Lima")).format(FECHA_FMT) : "Fecha no registrada";
        dibujarTexto(cs, fontRegular, 9, 40, currentY, "Fecha y hora de emisión (Lima): " + fechaStr, 100 / 255f, 116 / 255f, 139 / 255f);
        currentY -= 54;

        // 2. Tarjeta: DATOS DEL APODERADO / RESPONSABLE
        dibujarSeccionHeader(cs, fontBold, 40, currentY, pageWidth - 80, 20, "1. DATOS DEL APODERADO / CLIENTE");
        currentY -= 24;

        float boxApoderadoY = currentY - 55;
        cs.setNonStrokingColor(250 / 255f, 250 / 255f, 250 / 255f);
        cs.addRect(40, boxApoderadoY, pageWidth - 80, 55);
        cs.fill();
        cs.setStrokingColor(226 / 255f, 232 / 255f, 240 / 255f);
        cs.setLineWidth(1f);
        cs.addRect(40, boxApoderadoY, pageWidth - 80, 55);
        cs.stroke();

        float textY = currentY - 14;
        dibujarDato(cs, fontBold, fontRegular, 50, textY, "Apoderado(a):", dto.nombreApoderado());
        dibujarDato(cs, fontBold, fontRegular, 360, textY, "DNI", dto.dniApoderado());

        textY -= 16;
        dibujarDato(cs, fontBold, fontRegular, 50, textY, "Correo", dto.emailApoderado());
        dibujarDato(cs, fontBold, fontRegular, 360, textY, "Celular", dto.celularApoderado());

        textY -= 16;
        dibujarDato(cs, fontBold, fontRegular, 50, textY, "Domicilio", dto.direccionApoderado());

        currentY = boxApoderadoY - 18;

        // 3. Tarjeta: DATOS DEL ESTUDIANTE Y MATRÍCULA
        dibujarSeccionHeader(cs, fontBold, 40, currentY, pageWidth - 80, 20, "2. DATOS DEL ESTUDIANTE Y MATRÍCULA");
        currentY -= 24;

        float boxEstudianteY = currentY - 45;
        cs.setNonStrokingColor(250 / 255f, 250 / 255f, 250 / 255f);
        cs.addRect(40, boxEstudianteY, pageWidth - 80, 45);
        cs.fill();
        cs.setStrokingColor(226 / 255f, 232 / 255f, 240 / 255f);
        cs.addRect(40, boxEstudianteY, pageWidth - 80, 45);
        cs.stroke();

        textY = currentY - 14;
        dibujarDato(cs, fontBold, fontRegular, 50, textY, "Estudiante:", dto.nombreEstudiante());
        dibujarDato(cs, fontBold, fontRegular, 360, textY, "DNI:", dto.dniEstudiante());

        textY -= 16;
        dibujarDato(cs, fontBold, fontRegular, 50, textY, "Grado y sección", dto.nivelGradoSeccion());
        dibujarDato(cs, fontBold, fontRegular, 360, textY, "Matrícula", dto.codigoMatricula());

        currentY = boxEstudianteY - 22;

        // 4. TABLA DE DETALLE DE PAGO
        dibujarSeccionHeader(cs, fontBold, 40, currentY, pageWidth - 80, 20, "3. DETALLE DE COBRO");
        currentY -= 22;

        // Encabezados de tabla
        cs.setNonStrokingColor(241 / 255f, 245 / 255f, 249 / 255f);
        cs.addRect(40, currentY - 18, pageWidth - 80, 18);
        cs.fill();
        cs.setStrokingColor(203 / 255f, 213 / 255f, 225 / 255f);
        cs.addRect(40, currentY - 18, pageWidth - 80, 18);
        cs.stroke();

        dibujarTexto(cs, fontBold, 8.5f, 50, currentY - 13, "ITEM", 30 / 255f, 41 / 255f, 59 / 255f);
        dibujarTexto(cs, fontBold, 8.5f, 90, currentY - 13, "DESCRIPCIÓN", 30 / 255f, 41 / 255f, 59 / 255f);
        dibujarTexto(cs, fontBold, 8.5f, 360, currentY - 13, "CANTIDAD", 30 / 255f, 41 / 255f, 59 / 255f);
        dibujarTexto(cs, fontBold, 8.5f, 440, currentY - 13, "P. UNITARIO", 30 / 255f, 41 / 255f, 59 / 255f);
        dibujarTexto(cs, fontBold, 8.5f, 510, currentY - 13, "TOTAL", 30 / 255f, 41 / 255f, 59 / 255f);

        currentY -= 18;

        // Fila 1
        cs.setStrokingColor(226 / 255f, 232 / 255f, 240 / 255f);
        cs.addRect(40, currentY - 24, pageWidth - 80, 24);
        cs.stroke();

        BigDecimal monto = dto.montoTotal() != null ? dto.montoTotal() : BigDecimal.ZERO;
        String montoFmt = "S/ " + String.format(Locale.ROOT, "%.2f", monto);

        dibujarTexto(cs, fontRegular, 8.5f, 55, currentY - 16, "01", 51 / 255f, 65 / 255f, 85 / 255f);
        String concepto = dto.concepto() != null && !dto.concepto().isBlank() ? dto.concepto() : "Derecho de Matricula Escolar";
        dibujarTexto(cs, fontRegular, 8.5f, 90, currentY - 16, concepto, 51 / 255f, 65 / 255f, 85 / 255f);
        dibujarTexto(cs, fontRegular, 8.5f, 380, currentY - 16, "1", 51 / 255f, 65 / 255f, 85 / 255f);
        dibujarTexto(cs, fontRegular, 8.5f, 440, currentY - 16, montoFmt, 51 / 255f, 65 / 255f, 85 / 255f);
        dibujarTexto(cs, fontBold, 8.5f, 510, currentY - 16, montoFmt, 20 / 255f, 33 / 255f, 61 / 255f);

        currentY -= 24;

        // 5. RESUMEN DE TOTALES Y FORMA DE PAGO
        float boxTotalesW = 200;
        float boxTotalesX = pageWidth - 40 - boxTotalesW;
        float boxTotalesY = currentY - 65;

        cs.setNonStrokingColor(248 / 255f, 250 / 255f, 252 / 255f);
        cs.addRect(boxTotalesX, boxTotalesY, boxTotalesW, 60);
        cs.fill();
        cs.setStrokingColor(203 / 255f, 213 / 255f, 225 / 255f);
        cs.addRect(boxTotalesX, boxTotalesY, boxTotalesW, 60);
        cs.stroke();

        dibujarTexto(cs, fontRegular, 8.5f, boxTotalesX + 10, boxTotalesY + 44, "Moneda", 100 / 255f, 116 / 255f, 139 / 255f);
        dibujarTexto(cs, fontBold, 8.5f, boxTotalesX + 140, boxTotalesY + 44, "PEN", 30 / 255f, 41 / 255f, 59 / 255f);


        // Barra total
        cs.setNonStrokingColor(20 / 255f, 33 / 255f, 61 / 255f);
        cs.addRect(boxTotalesX, boxTotalesY, boxTotalesW, 20);
        cs.fill();
        dibujarTexto(cs, fontBold, 9.5f, boxTotalesX + 10, boxTotalesY + 6, "TOTAL PAGADO", 1f, 1f, 1f);
        dibujarTexto(cs, fontBold, 10f, boxTotalesX + 135, boxTotalesY + 6, montoFmt, 196 / 255f, 154 / 255f, 88 / 255f);

        // Bloque forma de pago (a la izquierda de totales)
        float boxPagoW = boxTotalesX - 40 - 15;
        float boxPagoX = 40;
        float boxPagoY = boxTotalesY;

        cs.setNonStrokingColor(240 / 255f, 253 / 255f, 244 / 255f);
        cs.addRect(boxPagoX, boxPagoY, boxPagoW, 60);
        cs.fill();
        cs.setStrokingColor(187 / 255f, 247 / 255f, 208 / 255f);
        cs.addRect(boxPagoX, boxPagoY, boxPagoW, 60);
        cs.stroke();

        dibujarTexto(cs, fontBold, 9f, boxPagoX + 12, boxPagoY + 44, "ESTADO: PAGO VERIFICADO Y CONFORME", 22 / 255f, 101 / 255f, 52 / 255f);
        dibujarTexto(cs, fontRegular, 8.5f, boxPagoX + 12, boxPagoY + 28, "Medio de pago: " + (dto.metodoPago() != null ? dto.metodoPago() : "EFECTIVO - CAJA"), 30 / 255f, 41 / 255f, 59 / 255f);
        if (dto.transaccionId() != null && !dto.transaccionId().isBlank()) {
          dibujarTexto(cs, fontRegular, 8f, boxPagoX + 12, boxPagoY + 12, "Referencia / Op: " + dto.transaccionId(), 100 / 255f, 116 / 255f, 139 / 255f);
        }

        currentY = boxTotalesY - 26;

        if (claveTemporal != null && !claveTemporal.isBlank()) {
          dibujarSeccionHeader(cs, fontBold, 40, currentY, pageWidth - 80, 20, "4. ACCESO PRIVADO DEL APODERADO");
          cs.setNonStrokingColor(0.96f, 0.97f, 0.99f);
          cs.addRect(40, currentY - 82, pageWidth - 80, 82);
          cs.fill();
          dibujarDato(cs, fontBold, fontRegular, 52, currentY - 19, "Usuario:", usuario);
          dibujarDato(cs, fontBold, fontRegular, 52, currentY - 36, "Contraseña temporal:", claveTemporal);
          dibujarTexto(cs, fontRegular, 9, 52, currentY - 56,
              "Al ingresar por primera vez al portal, deberás cambiar esta contraseña.", 0.12f, 0.18f, 0.29f);
          dibujarTexto(cs, fontRegular, 8, 52, currentY - 70,
              "Conserva esta copia para tu uso personal.", 0.28f, 0.33f, 0.42f);
        }

        // 7. Pie de página institucional
        cs.setStrokingColor(226 / 255f, 232 / 255f, 240 / 255f);
        cs.setLineWidth(1f);
        cs.moveTo(40, 50);
        cs.lineTo(pageWidth - 40, 50);
        cs.stroke();

        dibujarTextoCentrado(cs, fontOblique, 7.5f, pageWidth / 2, 38,
            "Emitido por el Sistema Institucional de Gestión Escolar - I.E.P. Shuji Kitamura", 148 / 255f, 163 / 255f, 184 / 255f);
      }

      doc.save(out);
      return out.toByteArray();
    } catch (IOException e) {
      log.error("Error generando PDF de comprobante de matrícula", e);
      throw new IllegalStateException("No se pudo generar el comprobante PDF de matrícula", e);
    }
  }

  private void dibujarSeccionHeader(PDPageContentStream cs, PDType1Font font, float x, float y, float w, float h, String titulo) throws IOException {
    cs.setNonStrokingColor(20 / 255f, 33 / 255f, 61 / 255f);
    cs.addRect(x, y, w, h);
    cs.fill();
    dibujarTexto(cs, font, 9, x + 8, y + 6, titulo, 1f, 1f, 1f);
  }

  private void dibujarDato(PDPageContentStream cs, PDType1Font fontBold, PDType1Font fontRegular, float x, float y, String label, String valor) throws IOException {
    dibujarTexto(cs, fontBold, 8.5f, x, y, label, 100 / 255f, 116 / 255f, 139 / 255f);
    float labelWidth = calcularAncho(fontBold, 8.5f, label) + 6;
    dibujarTexto(cs, fontRegular, 8.5f, x + labelWidth, y, valor != null ? valor : "-", 30 / 255f, 41 / 255f, 59 / 255f);
  }

  private void dibujarTexto(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String text, float r, float g, float b) throws IOException {
    String safe = sanitizar(text);
    cs.beginText();
    cs.setFont(font, size);
    cs.setNonStrokingColor(r, g, b);
    cs.newLineAtOffset(x, y);
    cs.showText(safe);
    cs.endText();
  }

  private void dibujarTextoCentrado(PDPageContentStream cs, PDType1Font font, float size, float cx, float y, String text, float r, float g, float b) throws IOException {
    String safe = sanitizar(text);
    float width = calcularAncho(font, size, safe);
    dibujarTexto(cs, font, size, cx - (width / 2), y, safe, r, g, b);
  }

  private float calcularAncho(PDType1Font font, float size, String text) {
    try {
      return font.getStringWidth(sanitizar(text)) * size / 1000f;
    } catch (Exception e) {
      return text.length() * size * 0.5f;
    }
  }

  private String sanitizar(String text) {
    if (text == null) return "";
    return text.replaceAll("[^\\x20-\\x7E\\xA0-\\xFF]", " ").trim();
  }
}
