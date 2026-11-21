package com.colegio.shuji.evaluacion.infrastructure.adapter;

import com.colegio.shuji.evaluacion.application.port.in.ConsultarLibretaNotasUseCase;
import com.colegio.shuji.evaluacion.application.port.in.GenerarLibretaPdfUseCase;
import com.colegio.shuji.matricula.application.dto.out.HijoPortalResponseDto;
import jakarta.persistence.EntityManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ReporteLibretaPdfAdapter implements GenerarLibretaPdfUseCase {

  private static final float MARGIN = 36f;
  private static final float PAGE_W = PDRectangle.A4.getWidth();
  private static final float PAGE_H = PDRectangle.A4.getHeight();
  private static final float CONTENT_W = PAGE_W - (MARGIN * 2f);
  private static final float BOTTOM_LIMIT = 54f;

  private static final float[] NAVY = {20 / 255f, 33 / 255f, 61 / 255f};
  private static final float[] GOLD = {196 / 255f, 154 / 255f, 88 / 255f};
  private static final float[] DARK_TEXT = {23 / 255f, 43 / 255f, 77 / 255f};
  private static final float[] BODY_TEXT = {51 / 255f, 65 / 255f, 85 / 255f};
  private static final float[] MUTED_TEXT = {100 / 255f, 116 / 255f, 139 / 255f};
  private static final float[] WHITE = {1f, 1f, 1f};
  private static final float[] SOFT_BG = {248 / 255f, 250 / 255f, 252 / 255f};
  private static final float[] AREA_BG = {235 / 255f, 241 / 255f, 248 / 255f};
  private static final float[] HEADER_BG = {241 / 255f, 245 / 255f, 249 / 255f};
  private static final float[] BORDER = {203 / 255f, 213 / 255f, 225 / 255f};

  private static final DateTimeFormatter FECHA_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

  public record CalificacionLibretaItem(
      String areaNombre,
      String competenciaNombre,
      int numeroPeriodo,
      String periodoNombre,
      String calificacion,
      String conclusionDescriptiva
  ) {}

  private enum TablaActiva {
    NINGUNA,
    MATRIZ,
    CONCLUSIONES
  }

  private static final class CompetenciaMatriz {
    final String nombre;
    final Map<Integer, String> notasPorPeriodo = new LinkedHashMap<>();

    CompetenciaMatriz(String nombre) {
      this.nombre = nombre;
    }
  }

  private static final class PdfCursor implements AutoCloseable {
    final PDDocument doc;
    final PDType1Font fontBold;
    final PDType1Font fontRegular;
    final PDType1Font fontItalic;
    final List<PDPage> pages = new ArrayList<>();
    PDPageContentStream cs;
    float y;

    PdfCursor(PDDocument doc) {
      this.doc = doc;
      this.fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
      this.fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
      this.fontItalic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);
    }

    void nuevaPagina() throws IOException {
      if (cs != null) cs.close();
      PDPage page = new PDPage(PDRectangle.A4);
      doc.addPage(page);
      pages.add(page);
      cs = new PDPageContentStream(doc, page);
      y = PAGE_H - MARGIN;
    }

    @Override
    public void close() throws IOException {
      if (cs != null) {
        cs.close();
        cs = null;
      }
    }
  }

  private final ConsultarLibretaNotasUseCase consulta;
  private final EntityManager em;

  @Transactional(readOnly = true)
  public byte[] generar(Long matriculaId) {
    var libreta = consulta.libreta(matriculaId); // Aplica custodia o asignación docente antes de leer datos personales.
    var alumno = em.createQuery("""
        select new com.colegio.shuji.matricula.application.dto.out.HijoPortalResponseDto(
          e.id,m.id,a.id,a.anio,s.id,e.nombres,e.apellidoPaterno,e.apellidoMaterno,e.numeroDocumento,n.nombre,g.nombre,s.letra)
        from MatriculaEntity m, EstudianteEntity e, SeccionEntity s, GradoEntity g, NivelEntity n, AnioLectivoEntity a
        where m.id=:id and m.estudianteId=e.id and m.seccionId=s.id and s.gradoId=g.id
          and g.nivelId=n.id and m.anioLectivoId=a.id
        """, HijoPortalResponseDto.class).setParameter("id", matriculaId).getSingleResult();

    var items = new ArrayList<CalificacionLibretaItem>();
    if (!libreta.calificaciones().isEmpty()) {
      var ids = libreta.calificaciones().stream().map(c -> c.id()).toList();
      var filas = em.createQuery("""
          select a.nombre, comp.nombre, p.numeroPeriodo, p.nombre, c.calificacionCualitativa, c.conclusionDescriptiva
          from CalificacionCnebEntity c, AreaCurricularEntity a, CompetenciaEntity comp, PeriodoAcademicoEntity p
          where c.id in :ids and c.areaCurricularId=a.id and c.competenciaId=comp.id and c.periodoAcademicoId=p.id
          order by a.nombre, comp.numeroOrden, p.numeroPeriodo
          """, Object[].class).setParameter("ids", ids).getResultList();
      for (var fila : filas) {
        items.add(new CalificacionLibretaItem(
            String.valueOf(fila[0]),
            String.valueOf(fila[1]),
            ((Number) fila[2]).intValue(),
            String.valueOf(fila[3]),
            String.valueOf(fila[4]),
            fila[5] == null ? "" : String.valueOf(fila[5])
        ));
      }
    }

    return renderizarLibretaOficial(
        alumno,
        items,
        LocalDate.now(ZoneId.of("America/Lima"))
    );
  }

  public static byte[] renderizarLibretaOficial(
      HijoPortalResponseDto alumno,
      List<CalificacionLibretaItem> items,
      LocalDate fechaEmision
  ) {
    LocalDate fecha = fechaEmision != null ? fechaEmision : LocalDate.now(ZoneId.of("America/Lima"));
    String nombreCompleto = (safe(alumno.apellidoPaterno()) + " " + safe(alumno.apellidoMaterno()) + ", " + safe(alumno.nombres())).trim();
    String dni = safe(alumno.numeroDocumento(), "-");
    String anio = alumno.anio() != null ? String.valueOf(alumno.anio()) : String.valueOf(fecha.getYear());
    String ubicacionEscolar = safe(alumno.nivel(), "-") + "  |  " + safe(alumno.grado(), "-") + "  |  Sección \"" + safe(alumno.seccion(), "-") + "\"";

    try (var doc = new PDDocument(); var salida = new ByteArrayOutputStream()) {
      try (var cursor = new PdfCursor(doc)) {
        cursor.nuevaPagina();
        dibujarEncabezadoPrincipal(cursor, anio);
        dibujarFichaEstudiante(cursor, nombreCompleto, dni, ubicacionEscolar, alumno.matriculaId(), fecha);
        dibujarLeyendaCneb(cursor, items);

        if (items.isEmpty()) {
          dibujarBannerAviso(cursor, "Sin calificaciones cualitativas CNEB registradas hasta la fecha de emisión.", SOFT_BG, BORDER, BODY_TEXT);
        } else {
          renderizarSeccionMatriz(cursor, items, nombreCompleto, dni, anio);
          renderizarSeccionConclusiones(cursor, items, nombreCompleto, dni, anio);
          dibujarBloqueFinalYSellos(cursor, items, nombreCompleto, dni, anio);
        }
      }

      estamparPiesDePagina(doc, nombreCompleto, fecha);
      doc.save(salida);
      return salida.toByteArray();
    } catch (IOException ex) {
      throw new IllegalStateException("No se pudo generar el informe PDF", ex);
    }
  }

  private static void renderizarSeccionMatriz(
      PdfCursor c,
      List<CalificacionLibretaItem> items,
      String nombreEstudiante,
      String dni,
      String anio
  ) throws IOException {
    asegurarEspacio(c, 58f, TablaActiva.NINGUNA, nombreEstudiante, dni, anio);
    dibujarTituloSeccion(c, "1. MATRIZ OFICIAL DE CALIFICACIONES CNEB POR ÁREA Y COMPETENCIA");
    dibujarCabeceraTablaMatriz(c);

    // Agrupar preservando el orden por Area -> Competencia -> Periodo (1..4)
    Map<String, Map<String, CompetenciaMatriz>> porArea = new LinkedHashMap<>();
    for (var item : items) {
      var compMap = porArea.computeIfAbsent(item.areaNombre(), k -> new LinkedHashMap<>());
      var comp = compMap.computeIfAbsent(item.competenciaNombre(), CompetenciaMatriz::new);
      comp.notasPorPeriodo.put(item.numeroPeriodo, safe(item.calificacion(), "-"));
    }

    float colDescW = 315f;
    float colBimW = 42f;
    float colFinW = CONTENT_W - colDescW - (colBimW * 4f);

    for (var areaEntry : porArea.entrySet()) {
      String areaNombre = areaEntry.getKey();
      List<CompetenciaMatriz> comps = new ArrayList<>(areaEntry.getValue().values());
      boolean areaConAlerta = comps.stream().anyMatch(cm -> cm.notasPorPeriodo.containsValue("C"));

      // Mantener cabecera de área + al menos la primera competencia juntas en la misma página
      float altoPrimeraComp = calcularAltoFilaMatriz(c, comps.get(0).nombre, colDescW - 12f);
      asegurarEspacio(c, 20f + altoPrimeraComp, TablaActiva.MATRIZ, nombreEstudiante, dni, anio);

      dibujarCaja(c.cs, MARGIN, c.y, CONTENT_W, 19f, AREA_BG, BORDER, 0.7f);
      escribirTexto(c.cs, c.fontBold, 8.5f, MARGIN + 6f, c.y - 13f, areaNombre.toUpperCase(), DARK_TEXT);
      if (areaConAlerta) {
        float[] alertaTxt = {153 / 255f, 27 / 255f, 27 / 255f};
        escribirTextoDerecha(c.cs, c.fontBold, 7.5f, MARGIN + CONTENT_W - 8f, c.y - 13f, "REQUIERE REFUERZO", alertaTxt);
      } else {
        escribirTextoDerecha(c.cs, c.fontRegular, 7.5f, MARGIN + CONTENT_W - 8f, c.y - 13f, comps.size() + " comp.", MUTED_TEXT);
      }
      c.y -= 19f;

      for (int idx = 0; idx < comps.size(); idx++) {
        CompetenciaMatriz comp = comps.get(idx);
        List<String> lineasNombre = ajustarLineas(c.fontRegular, 8f, colDescW - 12f, comp.nombre);
        float rowH = Math.max(18f, (lineasNombre.size() * 10.5f) + 6f);
        asegurarEspacio(c, rowH, TablaActiva.MATRIZ, nombreEstudiante, dni, anio);

        float[] rowBg = (idx % 2 == 1) ? SOFT_BG : WHITE;
        dibujarCaja(c.cs, MARGIN, c.y, colDescW, rowH, rowBg, BORDER, 0.5f);

        float textY = c.y - 11f;
        for (String linea : lineasNombre) {
          escribirTexto(c.cs, c.fontRegular, 8f, MARGIN + 8f, textY, linea, BODY_TEXT);
          textY -= 10.5f;
        }

        float x = MARGIN + colDescW;
        for (int p = 1; p <= 4; p++) {
          String nota = comp.notasPorPeriodo.getOrDefault(p, "-");
          dibujarCeldaNota(c, x, c.y, colBimW, rowH, nota);
          x += colBimW;
        }
        String cierre = resolverNotaCierre(comp.notasPorPeriodo);
        dibujarCeldaNota(c, x, c.y, colFinW, rowH, cierre);

        c.y -= rowH;
      }
    }
    c.y -= 12f;
  }

  private static void renderizarSeccionConclusiones(
      PdfCursor c,
      List<CalificacionLibretaItem> items,
      String nombreEstudiante,
      String dni,
      String anio
  ) throws IOException {
    List<CalificacionLibretaItem> ordenados = new ArrayList<>(items);
    ordenados.sort((a, b) -> {
      int pCmp = Integer.compare(a.numeroPeriodo, b.numeroPeriodo);
      if (pCmp != 0) return pCmp;
      return a.areaNombre.compareToIgnoreCase(b.areaNombre);
    });

    asegurarEspacio(c, 64f, TablaActiva.NINGUNA, nombreEstudiante, dni, anio);
    dibujarTituloSeccion(c, "2. CONCLUSIONES DESCRIPTIVAS Y OBSERVACIONES FORMATIVAS POR BIMESTRE");
    dibujarCabeceraTablaConclusiones(c);

    float colAreaW = 175f;
    float colNotaW = 42f;
    float colConcW = CONTENT_W - colAreaW - colNotaW;
    int periodoActual = -1;

    for (var item : ordenados) {
      if (item.numeroPeriodo != periodoActual) {
        periodoActual = item.numeroPeriodo;
        asegurarEspacio(c, 42f, TablaActiva.CONCLUSIONES, nombreEstudiante, dni, anio);
        dibujarCaja(c.cs, MARGIN, c.y, CONTENT_W, 17f, AREA_BG, BORDER, 0.7f);
        escribirTexto(c.cs, c.fontBold, 8.2f, MARGIN + 6f, c.y - 12f, item.periodoNombre.toUpperCase(), DARK_TEXT);
        c.y -= 17f;
      }

      String areaComp = item.areaNombre + " - " + item.competenciaNombre;
      String conclusion = item.conclusionDescriptiva == null || item.conclusionDescriptiva.isBlank()
          ? "Sin conclusión descriptiva registrada."
          : item.conclusionDescriptiva.replace('\n', ' ').trim();

      List<String> lineasArea = ajustarLineas(c.fontRegular, 7.6f, colAreaW - 10f, areaComp);
      List<String> lineasConc = ajustarLineas(c.fontRegular, 7.8f, colConcW - 10f, conclusion);
      int maxLineas = Math.max(lineasArea.size(), lineasConc.size());
      float rowH = Math.max(18f, (maxLineas * 10f) + 7f);

      asegurarEspacio(c, rowH, TablaActiva.CONCLUSIONES, nombreEstudiante, dni, anio);

      dibujarCaja(c.cs, MARGIN, c.y, colAreaW, rowH, WHITE, BORDER, 0.5f);
      float yArea = c.y - 10.5f;
      for (int i = 0; i < lineasArea.size(); i++) {
        escribirTexto(c.cs, i == 0 ? c.fontBold : c.fontRegular, 7.6f, MARGIN + 5f, yArea, lineasArea.get(i), i == 0 ? DARK_TEXT : BODY_TEXT);
        yArea -= 10f;
      }

      dibujarCeldaNota(c, MARGIN + colAreaW, c.y, colNotaW, rowH, safe(item.calificacion, "-"));

      dibujarCaja(c.cs, MARGIN + colAreaW + colNotaW, c.y, colConcW, rowH, WHITE, BORDER, 0.5f);
      float yConc = c.y - 10.5f;
      for (String l : lineasConc) {
        escribirTexto(c.cs, c.fontRegular, 7.8f, MARGIN + colAreaW + colNotaW + 5f, yConc, l, BODY_TEXT);
        yConc -= 10f;
      }

      c.y -= rowH;
    }
    c.y -= 12f;
  }

  private static void dibujarBloqueFinalYSellos(
      PdfCursor c,
      List<CalificacionLibretaItem> items,
      String nombreEstudiante,
      String dni,
      String anio
  ) throws IOException {
    var areasConC = new LinkedHashSet<String>();
    for (var it : items) {
      if ("C".equalsIgnoreCase(it.calificacion)) {
        areasConC.add(it.areaNombre);
      }
    }

    String resumenTexto = areasConC.isEmpty()
        ? "Situación Académica: Progreso satisfactorio sin áreas curriculares en nivel En Inicio (C)."
        : "Áreas con alerta de refuerzo pedagógico (Nota C): " + String.join(", ", areasConC) + ".";
    List<String> lineasResumen = ajustarLineas(c.fontRegular, 8.2f, CONTENT_W - 20f, resumenTexto);
    float boxH = 16f + (lineasResumen.size() * 11f);
    asegurarEspacio(c, boxH + 78f, TablaActiva.NINGUNA, nombreEstudiante, dni, anio);

    float[] bg = areasConC.isEmpty() ? new float[]{237 / 255f, 249 / 255f, 243 / 255f} : new float[]{255 / 255f, 241 / 255f, 239 / 255f};
    float[] brd = areasConC.isEmpty() ? new float[]{184 / 255f, 223 / 255f, 204 / 255f} : new float[]{242 / 255f, 183 / 255f, 176 / 255f};
    float[] txt = areasConC.isEmpty() ? new float[]{20 / 255f, 108 / 255f, 69 / 255f} : new float[]{153 / 255f, 27 / 255f, 27 / 255f};

    dibujarCaja(c.cs, MARGIN, c.y, CONTENT_W, boxH, bg, brd, 0.9f);
    float ty = c.y - 13f;
    for (int i = 0; i < lineasResumen.size(); i++) {
      escribirTexto(c.cs, i == 0 ? c.fontBold : c.fontRegular, 8.2f, MARGIN + 10f, ty, lineasResumen.get(i), txt);
      ty -= 11f;
    }
    c.y -= (boxH + 46f);

    // Firmas institucionales
    float firmaW = 190f;
    float leftX = MARGIN + 24f;
    float rightX = MARGIN + CONTENT_W - 24f - firmaW;
    c.cs.setStrokingColor(MUTED_TEXT[0], MUTED_TEXT[1], MUTED_TEXT[2]);
    c.cs.setLineWidth(0.8f);
    c.cs.moveTo(leftX, c.y);
    c.cs.lineTo(leftX + firmaW, c.y);
    c.cs.moveTo(rightX, c.y);
    c.cs.lineTo(rightX + firmaW, c.y);
    c.cs.stroke();

    escribirTextoCentrado(c.cs, c.fontBold, 8f, leftX, firmaW, c.y - 12f, "DIRECCIÓN ACADÉMICA", DARK_TEXT);
    escribirTextoCentrado(c.cs, c.fontRegular, 7.2f, leftX, firmaW, c.y - 22f, "I.E.P. Shuji Kitamura", MUTED_TEXT);
    escribirTextoCentrado(c.cs, c.fontBold, 8f, rightX, firmaW, c.y - 12f, "TUTOR(A) DE SECCIÓN", DARK_TEXT);
    escribirTextoCentrado(c.cs, c.fontRegular, 7.2f, rightX, firmaW, c.y - 22f, "Visto bueno pedagógico", MUTED_TEXT);
  }

  private static void dibujarEncabezadoPrincipal(PdfCursor c, String anio) throws IOException {
    float bannerH = 68f;
    dibujarCaja(c.cs, 0f, PAGE_H, PAGE_W, bannerH, NAVY, null, 0f);
    dibujarCaja(c.cs, 0f, PAGE_H - bannerH, PAGE_W, 3f, GOLD, null, 0f);

    escribirTexto(c.cs, c.fontBold, 15f, MARGIN, PAGE_H - 27f, "I.E.P. SHUJI KITAMURA", WHITE);
    escribirTexto(c.cs, c.fontRegular, 8.5f, MARGIN, PAGE_H - 41f, "Educación Básica Regular  |  Sistema de Gestión Escolar CNEB", new float[]{210 / 255f, 220 / 255f, 235 / 255f});
    escribirTexto(c.cs, c.fontRegular, 7.5f, MARGIN, PAGE_H - 54f, "Av. Principal S/N - Lima, Perú  |  Documento de Progreso Académico", GOLD);

    float boxW = 195f;
    float boxX = PAGE_W - MARGIN - boxW;
    dibujarCaja(c.cs, boxX, PAGE_H - 12f, boxW, 44f, new float[]{28 / 255f, 46 / 255f, 84 / 255f}, GOLD, 1f);
    escribirTextoCentrado(c.cs, c.fontBold, 8.5f, boxX, boxW, PAGE_H - 25f, "LIBRETA OFICIAL DE NOTAS", GOLD);
    escribirTextoCentrado(c.cs, c.fontBold, 8f, boxX, boxW, PAGE_H - 37f, "INFORME DE PROGRESO CNEB", WHITE);
    escribirTextoCentrado(c.cs, c.fontRegular, 7.8f, boxX, boxW, PAGE_H - 49f, "AÑO LECTIVO " + anio, new float[]{226 / 255f, 232 / 255f, 240 / 255f});

    c.y = PAGE_H - bannerH - 16f;
  }

  private static void dibujarEncabezadoContinuacion(PdfCursor c, String nombreEstudiante, String dni, String anio) throws IOException {
    float h = 28f;
    dibujarCaja(c.cs, MARGIN, PAGE_H - MARGIN + 10f, CONTENT_W, h, NAVY, null, 0f);
    dibujarCaja(c.cs, MARGIN, PAGE_H - MARGIN + 10f - h, CONTENT_W, 2f, GOLD, null, 0f);
    escribirTexto(c.cs, c.fontBold, 8.2f, MARGIN + 10f, PAGE_H - MARGIN - 7f, "I.E.P. SHUJI KITAMURA  |  LIBRETA CNEB " + anio, GOLD);
    escribirTextoDerecha(c.cs, c.fontRegular, 8f, MARGIN + CONTENT_W - 10f, PAGE_H - MARGIN - 7f, nombreEstudiante + "  (DNI: " + dni + ")", WHITE);
    c.y = PAGE_H - MARGIN - 28f;
  }

  private static void dibujarFichaEstudiante(
      PdfCursor c,
      String nombreCompleto,
      String dni,
      String ubicacionEscolar,
      Long matriculaId,
      LocalDate fecha
  ) throws IOException {
    float cardH = 48f;
    dibujarCaja(c.cs, MARGIN, c.y, CONTENT_W, cardH, SOFT_BG, BORDER, 0.9f);
    dibujarCaja(c.cs, MARGIN, c.y, 4f, cardH, NAVY, null, 0f);

    float row1Y = c.y - 16f;
    escribirTexto(c.cs, c.fontBold, 7.8f, MARGIN + 12f, row1Y, "ESTUDIANTE:", MUTED_TEXT);
    escribirTexto(c.cs, c.fontBold, 9.5f, MARGIN + 72f, row1Y, nombreCompleto, DARK_TEXT);
    escribirTexto(c.cs, c.fontBold, 7.8f, MARGIN + 375f, row1Y, "DNI:", MUTED_TEXT);
    escribirTexto(c.cs, c.fontBold, 9f, MARGIN + 400f, row1Y, dni, DARK_TEXT);

    float row2Y = c.y - 34f;
    escribirTexto(c.cs, c.fontBold, 7.8f, MARGIN + 12f, row2Y, "GRADO / SECCIÓN:", MUTED_TEXT);
    escribirTexto(c.cs, c.fontRegular, 8.5f, MARGIN + 92f, row2Y, ubicacionEscolar, BODY_TEXT);
    escribirTexto(c.cs, c.fontBold, 7.8f, MARGIN + 320f, row2Y, "MATRÍCULA:", MUTED_TEXT);
    escribirTexto(c.cs, c.fontBold, 8.2f, MARGIN + 375f, row2Y, "#" + matriculaId, DARK_TEXT);
    escribirTexto(c.cs, c.fontBold, 7.8f, MARGIN + 425f, row2Y, "EMISIÓN:", MUTED_TEXT);
    escribirTexto(c.cs, c.fontRegular, 8.2f, MARGIN + 468f, row2Y, fecha.format(FECHA_FMT), BODY_TEXT);

    c.y -= (cardH + 10f);
  }

  private static void dibujarLeyendaCneb(PdfCursor c, List<CalificacionLibretaItem> items) throws IOException {
    long ad = items.stream().filter(i -> "AD".equalsIgnoreCase(i.calificacion)).count();
    long a = items.stream().filter(i -> "A".equalsIgnoreCase(i.calificacion)).count();
    long b = items.stream().filter(i -> "B".equalsIgnoreCase(i.calificacion)).count();
    long cCount = items.stream().filter(i -> "C".equalsIgnoreCase(i.calificacion)).count();

    float h = 24f;
    dibujarCaja(c.cs, MARGIN, c.y, CONTENT_W, h, HEADER_BG, BORDER, 0.7f);
    escribirTexto(c.cs, c.fontBold, 7.5f, MARGIN + 8f, c.y - 15f, "ESCALA CNEB:", DARK_TEXT);
    escribirTexto(c.cs, c.fontRegular, 7.4f, MARGIN + 68f, c.y - 15f, "AD = Destacado   |   A = Esperado   |   B = En proceso   |   C = En inicio", BODY_TEXT);
    String resumen = String.format("Registros: %d  (AD:%d  A:%d  B:%d  C:%d)", items.size(), ad, a, b, cCount);
    escribirTextoDerecha(c.cs, c.fontBold, 7.5f, MARGIN + CONTENT_W - 8f, c.y - 15f, resumen, DARK_TEXT);

    c.y -= (h + 14f);
  }

  private static void dibujarTituloSeccion(PdfCursor c, String titulo) throws IOException {
    dibujarCaja(c.cs, MARGIN, c.y, CONTENT_W, 18f, NAVY, null, 0f);
    escribirTexto(c.cs, c.fontBold, 8.5f, MARGIN + 8f, c.y - 12.5f, titulo, WHITE);
    c.y -= 18f;
  }

  private static void dibujarCabeceraTablaMatriz(PdfCursor c) throws IOException {
    float colDescW = 315f;
    float colBimW = 42f;
    float colFinW = CONTENT_W - colDescW - (colBimW * 4f);
    float h = 18f;

    dibujarCaja(c.cs, MARGIN, c.y, CONTENT_W, h, HEADER_BG, BORDER, 0.7f);
    escribirTexto(c.cs, c.fontBold, 7.8f, MARGIN + 8f, c.y - 12f, "ÁREA CURRICULAR / COMPETENCIA EVALUADA", DARK_TEXT);

    float x = MARGIN + colDescW;
    String[] cols = {"I BIM", "II BIM", "III BIM", "IV BIM"};
    for (String col : cols) {
      dibujarLineaVertical(c.cs, x, c.y, h, BORDER);
      escribirTextoCentrado(c.cs, c.fontBold, 7.5f, x, colBimW, c.y - 12f, col, DARK_TEXT);
      x += colBimW;
    }
    dibujarLineaVertical(c.cs, x, c.y, h, BORDER);
    escribirTextoCentrado(c.cs, c.fontBold, 7.5f, x, colFinW, c.y - 12f, "FINAL", DARK_TEXT);

    c.y -= h;
  }

  private static void dibujarCabeceraTablaConclusiones(PdfCursor c) throws IOException {
    float colAreaW = 175f;
    float colNotaW = 42f;
    float colConcW = CONTENT_W - colAreaW - colNotaW;
    float h = 18f;

    dibujarCaja(c.cs, MARGIN, c.y, CONTENT_W, h, HEADER_BG, BORDER, 0.7f);
    escribirTexto(c.cs, c.fontBold, 7.8f, MARGIN + 6f, c.y - 12f, "ÁREA / COMPETENCIA", DARK_TEXT);
    dibujarLineaVertical(c.cs, MARGIN + colAreaW, c.y, h, BORDER);
    escribirTextoCentrado(c.cs, c.fontBold, 7.5f, MARGIN + colAreaW, colNotaW, c.y - 12f, "LOGRO", DARK_TEXT);
    dibujarLineaVertical(c.cs, MARGIN + colAreaW + colNotaW, c.y, h, BORDER);
    escribirTexto(c.cs, c.fontBold, 7.8f, MARGIN + colAreaW + colNotaW + 6f, c.y - 12f, "CONCLUSIÓN DESCRIPTIVA Y RECOMENDACIONES", DARK_TEXT);

    c.y -= h;
  }

  private static void dibujarCeldaNota(PdfCursor c, float x, float topY, float w, float h, String nota) throws IOException {
    float[] bg = colorFondoCalificacion(nota);
    float[] fg = colorTextoCalificacion(nota);
    dibujarCaja(c.cs, x, topY, w, h, bg, BORDER, 0.5f);
    float textY = topY - (h / 2f) - 3f;
    escribirTextoCentrado(c.cs, "-".equals(nota) ? c.fontRegular : c.fontBold, 8.2f, x, w, textY, nota, fg);
  }

  private static void dibujarBannerAviso(PdfCursor c, String msg, float[] bg, float[] brd, float[] fg) throws IOException {
    dibujarCaja(c.cs, MARGIN, c.y, CONTENT_W, 28f, bg, brd, 0.8f);
    escribirTexto(c.cs, c.fontRegular, 9f, MARGIN + 10f, c.y - 17f, msg, fg);
    c.y -= 38f;
  }

  private static void asegurarEspacio(
      PdfCursor c,
      float requerido,
      TablaActiva tablaActiva,
      String nombreEstudiante,
      String dni,
      String anio
  ) throws IOException {
    if (c.y - requerido >= BOTTOM_LIMIT) return;
    c.nuevaPagina();
    dibujarEncabezadoContinuacion(c, nombreEstudiante, dni, anio);
    if (tablaActiva == TablaActiva.MATRIZ) {
      dibujarCabeceraTablaMatriz(c);
    } else if (tablaActiva == TablaActiva.CONCLUSIONES) {
      dibujarCabeceraTablaConclusiones(c);
    }
  }

  private static void estamparPiesDePagina(PDDocument doc, String nombreEstudiante, LocalDate fecha) throws IOException {
    var font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    int total = doc.getNumberOfPages();
    for (int i = 0; i < total; i++) {
      try (var cs = new PDPageContentStream(doc, doc.getPage(i), AppendMode.APPEND, true, true)) {
        cs.setStrokingColor(BORDER[0], BORDER[1], BORDER[2]);
        cs.setLineWidth(0.7f);
        cs.moveTo(MARGIN, 40f);
        cs.lineTo(MARGIN + CONTENT_W, 40f);
        cs.stroke();
        escribirTexto(cs, font, 7.3f, MARGIN, 28f,
            "I.E.P. Shuji Kitamura  |  " + nombreEstudiante + "  |  Emitido el " + fecha.format(FECHA_FMT), MUTED_TEXT);
        escribirTextoDerecha(cs, font, 7.5f, MARGIN + CONTENT_W, 28f,
            "Página " + (i + 1) + " de " + total, DARK_TEXT);
      }
    }
  }

  private static String resolverNotaCierre(Map<Integer, String> notasPorPeriodo) {
    String ultima = "-";
    for (int p = 1; p <= 4; p++) {
      String nota = notasPorPeriodo.get(p);
      if (nota != null && !nota.isBlank() && !"-".equals(nota)) {
        ultima = nota;
      }
    }
    return ultima;
  }

  private static float calcularAltoFilaMatriz(PdfCursor c, String nombreComp, float maxW) throws IOException {
    int n = ajustarLineas(c.fontRegular, 8f, maxW, nombreComp).size();
    return Math.max(18f, (n * 10.5f) + 6f);
  }

  private static float[] colorFondoCalificacion(String nota) {
    if (nota == null) return WHITE;
    return switch (nota.toUpperCase()) {
      case "AD" -> new float[]{224 / 255f, 232 / 255f, 245 / 255f};
      case "A" -> new float[]{220 / 255f, 252 / 255f, 231 / 255f};
      case "B" -> new float[]{254 / 255f, 243 / 255f, 199 / 255f};
      case "C" -> new float[]{254 / 255f, 226 / 255f, 226 / 255f};
      default -> WHITE;
    };
  }

  private static float[] colorTextoCalificacion(String nota) {
    if (nota == null) return MUTED_TEXT;
    return switch (nota.toUpperCase()) {
      case "AD" -> NAVY;
      case "A" -> new float[]{20 / 255f, 83 / 255f, 45 / 255f};
      case "B" -> new float[]{146 / 255f, 64 / 255f, 14 / 255f};
      case "C" -> new float[]{153 / 255f, 27 / 255f, 27 / 255f};
      default -> MUTED_TEXT;
    };
  }

  private static void dibujarCaja(
      PDPageContentStream cs,
      float x,
      float topY,
      float w,
      float h,
      float[] fillRgb,
      float[] strokeRgb,
      float lineWidth
  ) throws IOException {
    float bottomY = topY - h;
    if (fillRgb != null) {
      cs.setNonStrokingColor(fillRgb[0], fillRgb[1], fillRgb[2]);
      cs.addRect(x, bottomY, w, h);
      cs.fill();
    }
    if (strokeRgb != null) {
      cs.setStrokingColor(strokeRgb[0], strokeRgb[1], strokeRgb[2]);
      cs.setLineWidth(lineWidth);
      cs.addRect(x, bottomY, w, h);
      cs.stroke();
    }
  }

  private static void dibujarLineaVertical(PDPageContentStream cs, float x, float topY, float h, float[] rgb) throws IOException {
    cs.setStrokingColor(rgb[0], rgb[1], rgb[2]);
    cs.setLineWidth(0.6f);
    cs.moveTo(x, topY);
    cs.lineTo(x, topY - h);
    cs.stroke();
  }

  private static void escribirTexto(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String raw, float[] rgb) throws IOException {
    String clean = limpiarTexto(font, raw);
    cs.beginText();
    cs.setFont(font, size);
    cs.setNonStrokingColor(rgb[0], rgb[1], rgb[2]);
    cs.newLineAtOffset(x, y);
    cs.showText(clean);
    cs.endText();
  }

  private static void escribirTextoDerecha(PDPageContentStream cs, PDType1Font font, float size, float rightX, float y, String raw, float[] rgb) throws IOException {
    String clean = limpiarTexto(font, raw);
    float width = font.getStringWidth(clean) * size / 1000f;
    escribirTexto(cs, font, size, rightX - width, y, clean, rgb);
  }

  private static void escribirTextoCentrado(PDPageContentStream cs, PDType1Font font, float size, float boxX, float boxW, float y, String raw, float[] rgb) throws IOException {
    String clean = limpiarTexto(font, raw);
    float width = font.getStringWidth(clean) * size / 1000f;
    float x = boxX + Math.max(0f, (boxW - width) / 2f);
    escribirTexto(cs, font, size, x, y, clean, rgb);
  }

  private static String limpiarTexto(PDType1Font font, String raw) throws IOException {
    if (raw == null || raw.isEmpty()) return "";
    StringBuilder sb = new StringBuilder(raw.length());
    for (int i = 0; i < raw.length(); i++) {
      char ch = raw.charAt(i);
      if (ch == '\r' || ch == '\n' || ch == '\t') {
        sb.append(' ');
        continue;
      }
      try {
        font.encode(String.valueOf(ch));
        sb.append(ch);
      } catch (IllegalArgumentException ex) {
        sb.append(' ');
      }
    }
    return sb.toString();
  }

  private static List<String> ajustarLineas(PDType1Font font, float size, float maxW, String raw) throws IOException {
    String clean = limpiarTexto(font, raw).trim();
    var result = new ArrayList<String>();
    if (clean.isEmpty()) {
      result.add("");
      return result;
    }
    var actual = new StringBuilder();
    for (String palabra : clean.split("\\s+")) {
      String candidato = actual.isEmpty() ? palabra : actual + " " + palabra;
      if (font.getStringWidth(candidato) * size / 1000f <= maxW) {
        actual.setLength(0);
        actual.append(candidato);
      } else {
        if (!actual.isEmpty()) {
          result.add(actual.toString());
          actual.setLength(0);
        }
        // Si una palabra suelta excede el ancho, dividir por caracteres
        if (font.getStringWidth(palabra) * size / 1000f > maxW) {
          StringBuilder chunk = new StringBuilder();
          for (int i = 0; i < palabra.length(); i++) {
            char c = palabra.charAt(i);
            if (font.getStringWidth(chunk.toString() + c) * size / 1000f > maxW && !chunk.isEmpty()) {
              result.add(chunk.toString());
              chunk.setLength(0);
            }
            chunk.append(c);
          }
          actual.append(chunk);
        } else {
          actual.append(palabra);
        }
      }
    }
    if (!actual.isEmpty()) result.add(actual.toString());
    return result;
  }

  private static String safe(String val) {
    return val == null ? "" : val.trim();
  }

  private static String safe(String val, String fallback) {
    return val == null || val.isBlank() ? fallback : val.trim();
  }

  public static byte[] renderizar(List<String> lineas) {
    var fuente = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    try (var doc = new PDDocument(); var salida = new ByteArrayOutputStream()) {
      PDPageContentStream stream = null; float y = 0; int numero = 0;
      try {
        for (String parrafo : lineas) {
          StringBuilder texto = new StringBuilder();
          for (char c : parrafo.toCharArray()) {
            if (c == '\n' || c == '\r') { texto.append('\n'); continue; }
            try { fuente.encode(String.valueOf(c)); texto.append(c); } catch (IllegalArgumentException ex) { texto.append('?'); }
          }
          StringBuilder linea = new StringBuilder();
          var ajustadas = new ArrayList<String>();
          for (char c : texto.toString().toCharArray()) {
            if (c == '\n') { ajustadas.add(linea.toString()); linea.setLength(0); continue; }
            if (fuente.getStringWidth(linea.toString() + c) * 10 / 1000 > 495) {
              int espacio = linea.lastIndexOf(" ");
              if (espacio > 0) {
                ajustadas.add(linea.substring(0, espacio)); String resto = linea.substring(espacio + 1);
                linea.setLength(0); linea.append(resto);
              } else { ajustadas.add(linea.toString()); linea.setLength(0); }
            }
            linea.append(c);
          }
          ajustadas.add(linea.toString());
          for (String contenido : ajustadas) {
            if (stream == null || y < 55) {
              if (stream != null) stream.close();
              var pagina = new PDPage(PDRectangle.A4); doc.addPage(pagina);
              stream = new PDPageContentStream(doc, pagina); y = 785; numero++;
              stream.beginText(); stream.setFont(fuente, 8); stream.newLineAtOffset(50, 30);
              stream.showText("Shuji Kitamura | Página " + numero); stream.endText();
            }
            stream.beginText(); stream.setFont(fuente, 10); stream.newLineAtOffset(50, y);
            stream.showText(contenido); stream.endText(); y -= 14;
          }
          y -= 3;
        }
      } finally { if (stream != null) stream.close(); }
      doc.save(Objects.requireNonNull(salida)); return salida.toByteArray();
    } catch (IOException ex) { throw new IllegalStateException("No se pudo generar el informe PDF", ex); }
  }
}
