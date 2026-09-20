package com.colegio.shuji;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.colegio.shuji.academico.domain.enums.NivelCodigo;
import com.colegio.shuji.academico.domain.exception.AnioLectivoCerradoException;
import com.colegio.shuji.academico.domain.exception.CupoAgotadoException;
import com.colegio.shuji.academico.domain.model.AnioLectivo;
import com.colegio.shuji.academico.domain.model.Aula;
import com.colegio.shuji.academico.domain.model.Grado;
import com.colegio.shuji.academico.domain.model.Nivel;
import com.colegio.shuji.academico.domain.model.PeriodoAcademico;
import com.colegio.shuji.academico.domain.model.Seccion;
import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import com.colegio.shuji.asistencia.domain.enums.EstadoMarca;
import com.colegio.shuji.asistencia.domain.enums.TipoDiscrepancia;
import com.colegio.shuji.asistencia.domain.exception.LoteCorruptoException;
import com.colegio.shuji.asistencia.domain.model.AsistenciaAula;
import com.colegio.shuji.asistencia.domain.model.ConciliacionAsistencia;
import com.colegio.shuji.asistencia.domain.model.LoteBiometrico;
import com.colegio.shuji.asistencia.domain.model.MarcaPorteria;
import com.colegio.shuji.comunicado.domain.model.ComunicadoDestinatario;
import com.colegio.shuji.comunicado.domain.model.ComunicadoOficial;
import com.colegio.shuji.convivencia.domain.enums.EstadoIncidencia;
import com.colegio.shuji.convivencia.domain.enums.TipoFalta;
import com.colegio.shuji.convivencia.domain.model.IncidenciaConductual;
import com.colegio.shuji.curriculo.domain.enums.DiaSemana;
import com.colegio.shuji.curriculo.domain.model.AreaCurricular;
import com.colegio.shuji.curriculo.domain.model.AsignacionDocente;
import com.colegio.shuji.curriculo.domain.model.BloqueHorario;
import com.colegio.shuji.curriculo.domain.model.Competencia;
import com.colegio.shuji.curriculo.domain.model.HorarioSeccion;
import com.colegio.shuji.evaluacion.domain.enums.CalificacionCualitativa;
import com.colegio.shuji.evaluacion.domain.enums.EstadoAsistenciaRefuerzo;
import com.colegio.shuji.evaluacion.domain.exception.CalificacionInvalidaException;
import com.colegio.shuji.evaluacion.domain.model.CalificacionCneb;
import com.colegio.shuji.evaluacion.domain.model.InscripcionRefuerzo;
import com.colegio.shuji.evaluacion.domain.model.SesionRefuerzo;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.OrigenRegistro;
import com.colegio.shuji.matricula.domain.enums.Parentesco;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import com.colegio.shuji.matricula.domain.model.Apoderado;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import com.colegio.shuji.matricula.domain.model.EstudianteApoderado;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.shared.domain.model.Reglas;
import com.colegio.shuji.tesoreria.domain.enums.EstadoComprobante;
import com.colegio.shuji.tesoreria.domain.enums.EstadoObligacion;
import com.colegio.shuji.tesoreria.domain.enums.EstadoPago;
import com.colegio.shuji.tesoreria.domain.enums.MetodoPago;
import com.colegio.shuji.tesoreria.domain.enums.PasarelaProveedor;
import com.colegio.shuji.tesoreria.domain.enums.TipoComprobante;
import com.colegio.shuji.tesoreria.domain.enums.TipoConcepto;
import com.colegio.shuji.tesoreria.domain.exception.SobrepagoException;
import com.colegio.shuji.tesoreria.domain.model.ComprobantePago;
import com.colegio.shuji.tesoreria.domain.model.ConceptoCobro;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import com.colegio.shuji.tesoreria.domain.model.PagoTransaccion;
import com.colegio.shuji.tesoreria.domain.model.SerieComprobante;
import com.colegio.shuji.usuario.domain.model.Rol;
import com.colegio.shuji.usuario.domain.model.Sesion;
import com.colegio.shuji.usuario.domain.model.Usuario;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DominioCoberturaTest {

  @Nested
  @DisplayName("Academico Domain Tests")
  class AcademicoDomainTests {

    @Test
    void testAnioLectivoCicloDeVidaYReglas() {
      var anio =
          AnioLectivo.builder()
              .id((short) 1)
              .anio((short) 2026)
              .fechaInicio(LocalDate.of(2026, 3, 1))
              .fechaFin(LocalDate.of(2026, 12, 15))
              .abierto(false)
              .build();

      assertFalse(anio.estaAbierto());
      assertThrows(AnioLectivoCerradoException.class, anio::verificarAbierto);

      anio.abrir();
      assertTrue(anio.estaAbierto());
      assertDoesNotThrow(anio::verificarAbierto);

      anio.actualizarFechas(LocalDate.of(2026, 3, 15), LocalDate.of(2026, 12, 20));
      assertEquals(LocalDate.of(2026, 3, 15), anio.getFechaInicio());
      assertEquals(LocalDate.of(2026, 12, 20), anio.getFechaFin());

      assertThrows(
          BusinessException.class,
          () -> anio.actualizarFechas(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 1, 1)));

      anio.cerrar();
      assertFalse(anio.estaAbierto());
      assertThrows(AnioLectivoCerradoException.class, anio::verificarAbierto);

      var anioVacio = new AnioLectivo();
      anioVacio.setId((short) 2);
      anioVacio.setAnio((short) 2027);
      anioVacio.setAbierto(true);
      assertEquals((short) 2, anioVacio.getId());
      assertEquals((short) 2027, anioVacio.getAnio());
    }

    @Test
    void testAulaPrepararRegistro() {
      var aula =
          Aula.builder()
              .id(10)
              .codigo(" a-101 ")
              .nombre(" Aula Magna ")
              .ubicacion(" Pabellon Norte ")
              .capacidad((short) 35)
              .activa(false)
              .build();

      aula.prepararRegistro();
      assertEquals("A-101", aula.getCodigo());
      assertEquals("Aula Magna", aula.getNombre());
      assertEquals("Pabellon Norte", aula.getUbicacion());
      assertTrue(aula.getActiva());

      var aulaSinUbicacion =
          Aula.builder()
              .codigo("b-102")
              .nombre("Laboratorio")
              .ubicacion("   ")
              .capacidad((short) 20)
              .build();
      aulaSinUbicacion.prepararRegistro();
      assertNull(aulaSinUbicacion.getUbicacion());

      var aulaInvalida =
          Aula.builder().codigo("C-1").nombre("Aula C").capacidad((short) 0).build();
      assertThrows(BusinessException.class, aulaInvalida::prepararRegistro);

      var aulaCapacidadNula =
          Aula.builder().codigo("C-2").nombre("Aula C2").capacidad(null).build();
      assertThrows(BusinessException.class, aulaCapacidadNula::prepararRegistro);
    }

    @Test
    void testGradoYNivel() {
      var grado =
          Grado.builder().id((short) 1).nivelId((short) 2).numeroGrado((short) 1).nombre("Primero").build();
      assertEquals("Primero", grado.getNombre());
      grado.actualizar("Primer Grado", (short) 1);
      assertEquals("Primer Grado", grado.getNombre());

      var nivel =
          Nivel.builder()
              .id((short) 2)
              .codigo(NivelCodigo.SECUNDARIA)
              .nombre("Secundaria")
              .descripcion("Educación Secundaria")
              .build();
      assertEquals(NivelCodigo.SECUNDARIA, nivel.getCodigo());
      nivel.actualizar("Secundaria Regular", "Nueva descripcion");
      assertEquals("Secundaria Regular", nivel.getNombre());
      assertEquals("Nueva descripcion", nivel.getDescripcion());
    }

    @Test
    void testPeriodoAcademico() {
      var periodo =
          PeriodoAcademico.builder()
              .id((short) 1)
              .anioLectivoId((short) 1)
              .numeroPeriodo((short) 1)
              .nombre("Bimestre I")
              .fechaInicio(LocalDate.of(2026, 3, 1))
              .fechaFin(LocalDate.of(2026, 5, 15))
              .cerrado(false)
              .build();

      assertFalse(periodo.estaCerrado());
      assertDoesNotThrow(periodo::verificarAbierto);

      periodo.actualizar("Bimestre 1 Modificado", LocalDate.of(2026, 3, 5), LocalDate.of(2026, 5, 20));
      assertEquals("Bimestre 1 Modificado", periodo.getNombre());

      periodo.cerrar();
      assertTrue(periodo.estaCerrado());
      assertThrows(BusinessException.class, periodo::verificarAbierto);

      periodo.abrir();
      assertFalse(periodo.estaCerrado());

      assertThrows(
          BusinessException.class,
          () -> periodo.actualizar("Invalido", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1)));
    }

    @Test
    void testSeccionCuposYAsignacionAula() {
      var seccion =
          Seccion.builder()
              .id(100)
              .anioLectivoId((short) 1)
              .gradoId((short) 1)
              .nivelId((short) 1)
              .letra("A")
              .cupoMaximo((short) 2)
              .vacantesOcupadas((short) 0)
              .aulaFisica("  A-101  ")
              .build();

      assertEquals(2, seccion.vacantesDisponibles());
      assertTrue(seccion.tieneDisponibilidad());

      seccion.normalizarAula();
      assertEquals("A-101", seccion.getAulaFisica());

      seccion.ocuparVacante();
      assertEquals(1, seccion.vacantesDisponibles());

      seccion.ocuparVacante();
      assertEquals(0, seccion.vacantesDisponibles());
      assertFalse(seccion.tieneDisponibilidad());
      assertThrows(CupoAgotadoException.class, seccion::ocuparVacante);

      seccion.liberarVacante();
      assertEquals(1, seccion.vacantesDisponibles());
      seccion.liberarVacante();
      assertEquals(2, seccion.vacantesDisponibles());
      seccion.liberarVacante(); // no baja de 0
      assertEquals(2, seccion.vacantesDisponibles());

      seccion.actualizar("B", (short) 30, "A-102");
      assertEquals("B", seccion.getLetra());
      assertEquals((short) 30, seccion.getCupoMaximo());

      // Asignar aula
      var aulaValida = Aula.builder().id(5).codigo("A-102").capacidad((short) 35).activa(true).build();
      seccion.asignarAula(aulaValida);
      assertEquals(5, seccion.getAulaId());
      assertEquals("A-102", seccion.getAulaFisica());

      var aulaChica = Aula.builder().id(6).codigo("A-CHICA").capacidad((short) 25).activa(true).build();
      assertThrows(BusinessException.class, () -> seccion.asignarAula(aulaChica));

      var aulaInactiva = Aula.builder().id(7).codigo("A-INACTIVA").capacidad((short) 40).activa(false).build();
      assertThrows(BusinessException.class, () -> seccion.asignarAula(aulaInactiva));

      assertThrows(BusinessException.class, () -> seccion.asignarAula(null));
    }
  }

  @Nested
  @DisplayName("Asistencia Domain Tests")
  class AsistenciaDomainTests {

    @Test
    void testAsistenciaAulaOperacionesYEstados() {
      var asistencia =
          AsistenciaAula.builder()
              .id(1L)
              .matriculaId(10L)
              .fechaSesion(LocalDate.of(2026, 4, 10))
              .horaRegistro(LocalTime.of(8, 5))
              .estado(EstadoAsistenciaAula.FALTA_INJUSTIFICADA)
              .justificada(false)
              .build();

      assertTrue(asistencia.esFalta());
      assertFalse(asistencia.esPresente());

      asistencia.justificar("Cita médica en Essalud", "https://sustentos.com/doc1.pdf");
      assertTrue(asistencia.getJustificada());
      assertEquals(EstadoAsistenciaAula.FALTA_JUSTIFICADA, asistencia.getEstado());
      assertEquals("Cita médica en Essalud", asistencia.getMotivoJustificacion());
      assertEquals("https://sustentos.com/doc1.pdf", asistencia.getDocumentoSustentoUrl());

      assertThrows(BusinessException.class, () -> asistencia.justificar("Otro", null));

      asistencia.registrarEstado(EstadoAsistenciaAula.PRESENTE);
      assertTrue(asistencia.esPresente());
      assertFalse(asistencia.esFalta());
      assertFalse(asistencia.getJustificada());
      assertNull(asistencia.getMotivoJustificacion());

      asistencia.setEstado(EstadoAsistenciaAula.TARDANZA);
      assertTrue(asistencia.esPresente());

      assertThrows(
          BusinessException.class,
          () -> asistencia.registrarEstado(EstadoAsistenciaAula.FALTA_JUSTIFICADA));
      assertThrows(BusinessException.class, () -> asistencia.registrarEstado(null));

      asistencia.registrarPor(99L);
      assertEquals(99L, asistencia.getAuxiliarUsuarioId());
      assertThrows(BusinessException.class, () -> asistencia.registrarPor(null));
    }

    @Test
    void testConciliacionAsistenciaRamas() {
      var conc =
          ConciliacionAsistencia.builder()
              .id(1L)
              .fecha(LocalDate.of(2026, 4, 10))
              .estudianteId(50L)
              .build();

      assertFalse(conc.estaNotificada());
      conc.resolver();
      assertTrue(conc.estaNotificada());

      // Turno no cerrado
      conc.conciliar(true, false, false);
      assertEquals(TipoDiscrepancia.PENDIENTE_CIERRE_TURNO, conc.getTipoDiscrepancia());
      assertFalse(conc.estaNotificada());

      // Turno cerrado: portería sí, aula no => FUGA
      conc.conciliar(true, false, true);
      assertEquals(TipoDiscrepancia.DISCREPANCIA_FUGA, conc.getTipoDiscrepancia());

      // Turno cerrado: portería no, aula sí => OMISIÓN PORTERÍA
      conc.conciliar(false, true, true);
      assertEquals(TipoDiscrepancia.DISCREPANCIA_OMISION_PORTERIA, conc.getTipoDiscrepancia());

      // Turno cerrado: ambos sí => ASISTENCIA CONCILIADA
      conc.conciliar(true, true, true);
      assertEquals(TipoDiscrepancia.ASISTENCIA_CONCILIADA, conc.getTipoDiscrepancia());

      // Turno no cerrado cuando ya no es PENDIENTE_CIERRE_TURNO se ignora
      conc.conciliar(true, true, false);
      assertEquals(TipoDiscrepancia.ASISTENCIA_CONCILIADA, conc.getTipoDiscrepancia());
    }

    @Test
    void testLoteBiometricoValidaciones() {
      var lote =
          LoteBiometrico.builder()
              .id(1L)
              .nombreArchivo("bio_20260410.csv")
              .hashContenido("sha256abc")
              .totalFilas(100)
              .build();

      lote.registrarResultado(95, 5);
      assertEquals(95, lote.getMarcasValidas());
      assertEquals(5, lote.getMarcasErroneas());

      assertThrows(LoteCorruptoException.class, () -> lote.registrarResultado(90, 5));
      assertThrows(LoteCorruptoException.class, () -> lote.registrarResultado(-1, 101));
      assertThrows(LoteCorruptoException.class, () -> lote.registrarResultado(101, -1));

      var loteSinFilas = new LoteBiometrico();
      assertThrows(LoteCorruptoException.class, () -> loteSinFilas.registrarResultado(0, 0));
    }

    @Test
    void testMarcaPorteriaEstados() {
      var marca =
          MarcaPorteria.builder()
              .id(1L)
              .loteId(1L)
              .dniLeido("12345678")
              .fechaHora(OffsetDateTime.now(ZoneOffset.UTC))
              .dispositivoCodigo("BIO-01")
              .estadoProcesamiento(EstadoMarca.PENDIENTE)
              .build();

      marca.procesarValida(200L);
      assertEquals(200L, marca.getEstudianteId());
      assertEquals(EstadoMarca.CONCILIADO, marca.getEstadoProcesamiento());

      marca.marcarDniNoIdentificado();
      assertEquals(EstadoMarca.DNI_NO_IDENTIFICADO, marca.getEstadoProcesamiento());

      marca.marcarDuplicado();
      assertEquals(EstadoMarca.DUPLICADO, marca.getEstadoProcesamiento());
      assertThrows(BusinessException.class, marca::conciliar);

      marca.identificar(300L);
      assertEquals(300L, marca.getEstudianteId());
      assertEquals(EstadoMarca.PENDIENTE, marca.getEstadoProcesamiento());

      marca.conciliar();
      assertEquals(EstadoMarca.CONCILIADO, marca.getEstadoProcesamiento());

      marca.identificar(null);
      assertNull(marca.getEstudianteId());
      assertEquals(EstadoMarca.DNI_NO_IDENTIFICADO, marca.getEstadoProcesamiento());
    }
  }

  @Nested
  @DisplayName("Comunicado Domain Tests")
  class ComunicadoDomainTests {

    @Test
    void testComunicadoOficialFiltrosYPublicacion() {
      var general =
          ComunicadoOficial.builder()
              .id(null)
              .titulo("Bienvenida")
              .contenido("Inicio de clases")
              .build();

      assertTrue(general.esGeneral());
      assertFalse(general.esPorNivel());
      assertFalse(general.esPorSeccion());

      general.publicar(10L);
      assertEquals(10L, general.getRemitenteUsuarioId());
      assertNotNull(general.getFechaPublicacion());

      // No se puede publicar si ya tiene ID
      general.setId(1L);
      assertThrows(BusinessException.class, () -> general.publicar(10L));

      var porNivel =
          ComunicadoOficial.builder().nivelId((short) 1).titulo("T").contenido("C").build();
      assertFalse(porNivel.esGeneral());
      assertTrue(porNivel.esPorNivel());
      assertFalse(porNivel.esPorSeccion());

      var porSeccion =
          ComunicadoOficial.builder().seccionId(5).titulo("T").contenido("C").build();
      assertFalse(porSeccion.esGeneral());
      assertFalse(porSeccion.esPorNivel());
      assertTrue(porSeccion.esPorSeccion());

      porSeccion.actualizar("Nuevo Titulo", "Nuevo Contenido");
      assertEquals("Nuevo Titulo", porSeccion.getTitulo());
      assertEquals("Nuevo Contenido", porSeccion.getContenido());

      var invalido = ComunicadoOficial.builder().titulo("").contenido("C").build();
      assertThrows(BusinessException.class, () -> invalido.publicar(1L));

      var invalidoContenido = ComunicadoOficial.builder().titulo("T").contenido(null).build();
      assertThrows(BusinessException.class, () -> invalidoContenido.publicar(1L));

      var invalidoRemitente = ComunicadoOficial.builder().titulo("T").contenido("C").build();
      assertThrows(BusinessException.class, () -> invalidoRemitente.publicar(null));
    }

    @Test
    void testComunicadoDestinatarioLecturaYAcuse() {
      var dest =
          ComunicadoDestinatario.builder()
              .id(1L)
              .comunicadoId(10L)
              .apoderadoId(20L)
              .leido(false)
              .acuseConfirmado(false)
              .build();

      assertFalse(dest.estaLeido());
      assertFalse(dest.estaConfirmado());
      assertFalse(dest.getAcuseRecibo());

      var ahora = OffsetDateTime.now(ZoneOffset.UTC);
      dest.confirmarLectura(ahora, false);
      assertTrue(dest.estaLeido());
      assertFalse(dest.estaConfirmado());
      assertEquals(ahora, dest.getFechaLectura());
      assertNull(dest.getFechaAcuse());

      var luego = ahora.plusHours(1);
      dest.confirmarLectura(luego, true);
      assertTrue(dest.estaConfirmado());
      assertEquals(ahora, dest.getFechaLectura()); // Conserva la primera lectura
      assertEquals(luego, dest.getFechaAcuse());

      dest.setAcuseRecibo(false);
      assertFalse(dest.estaConfirmado());
    }
  }

  @Nested
  @DisplayName("Convivencia Domain Tests")
  class ConvivenciaDomainTests {

    @Test
    void testIncidenciaConductualCicloDeVida() {
      var inc =
          IncidenciaConductual.builder()
              .id(null)
              .matriculaId(10L)
              .fechaIncidencia(LocalDate.of(2026, 5, 2))
              .tipoFalta(TipoFalta.LEVE)
              .descripcion("Llegada tardía recurrente")
              .requiereCitacion(false)
              .build();

      inc.abrir(5L);
      assertEquals(5L, inc.getReportadoPorUsuarioId());
      assertEquals(EstadoIncidencia.ABIERTA, inc.getEstado());

      // No se puede abrir si ya tiene id o reportante nulo
      inc.setId(1L);
      assertThrows(BusinessException.class, () -> inc.abrir(5L));

      var incSinReportante = IncidenciaConductual.builder().build();
      assertThrows(BusinessException.class, () -> incSinReportante.abrir(null));

      // De abierta a cerrada directo no permitido
      assertThrows(BusinessException.class, inc::cerrar);

      inc.actualizarDetalles("Detalles actualizados", true);
      assertEquals("Detalles actualizados", inc.getDescripcion());
      assertTrue(inc.getRequiereCitacion());

      inc.atender();
      assertEquals(EstadoIncidencia.ATENDIDA, inc.getEstado());

      inc.cerrar();
      assertEquals(EstadoIncidencia.CERRADA, inc.getEstado());

      // Incidencia cerrada no se modifica ni se cambia de estado
      assertThrows(BusinessException.class, () -> inc.cambiarEstado(EstadoIncidencia.ABIERTA));
      assertThrows(BusinessException.class, () -> inc.actualizarDetalles("Mod", false));
    }
  }

  @Nested
  @DisplayName("Curriculo Domain Tests")
  class CurriculoDomainTests {

    @Test
    void testCurriculoEntidadesBasicas() {
      var area =
          AreaCurricular.builder()
              .id((short) 1)
              .nivelId((short) 2)
              .codigo("MAT")
              .nombre("Matemáticas")
              .build();
      area.actualizar("MAT-SEC", "Matemática Secundaria");
      assertEquals("MAT-SEC", area.getCodigo());
      assertEquals("Matemática Secundaria", area.getNombre());

      var asig =
          AsignacionDocente.builder()
              .id(10L)
              .docenteUsuarioId(50L)
              .seccionId(100)
              .anioLectivoId((short) 1)
              .nivelId((short) 2)
              .areaCurricularId((short) 1)
              .build();
      asig.reasignarDocente(51L);
      assertEquals(51L, asig.getDocenteUsuarioId());

      var bloque =
          BloqueHorario.builder()
              .id((short) 1)
              .numeroBloque((short) 1)
              .horaInicio(LocalTime.of(8, 0))
              .horaFin(LocalTime.of(8, 45))
              .esRecreo(false)
              .build();
      bloque.actualizar((short) 2, LocalTime.of(8, 45), LocalTime.of(9, 30), true);
      assertEquals((short) 2, bloque.getNumeroBloque());
      assertTrue(bloque.getEsRecreo());

      var comp =
          Competencia.builder()
              .id((short) 1)
              .areaId((short) 1)
              .numeroOrden((short) 1)
              .nombre("Resuelve problemas de cantidad")
              .descripcion("Desc")
              .build();
      comp.actualizar((short) 2, "Nuevo nombre", "Nueva desc");
      assertEquals((short) 2, comp.getNumeroOrden());
      assertEquals("Nuevo nombre", comp.getNombre());
      assertEquals("Nueva desc", comp.getDescripcion());

      var horario =
          HorarioSeccion.builder()
              .id(1L)
              .anioLectivoId((short) 1)
              .seccionId(10)
              .docenteUsuarioId(50L)
              .asignacionDocenteId(10L)
              .diaSemana((short) 1)
              .bloqueHorarioId((short) 1)
              .build();
      horario.reprogramar((short) 2, (short) 3);
      assertEquals((short) 2, horario.getDiaSemana());
      assertEquals((short) 3, horario.getBloqueHorarioId());

      horario.reprogramar(DiaSemana.MIERCOLES, (short) 4);
      assertEquals(DiaSemana.MIERCOLES.getCodigo(), horario.getDiaSemana());
      assertEquals((short) 4, horario.getBloqueHorarioId());

      horario.reprogramar((DiaSemana) null, (short) 5);
      assertNull(horario.getDiaSemana());
    }
  }

  @Nested
  @DisplayName("Evaluacion Domain Tests")
  class EvaluacionDomainTests {

    @Test
    void testCalificacionCneb() {
      var calif =
          CalificacionCneb.builder()
              .id(1L)
              .matriculaId(10L)
              .calificacionCualitativa(CalificacionCualitativa.C)
              .requiereRefuerzo(true)
              .build();

      assertTrue(calif.necesitaRefuerzo());

      calif.actualizar(CalificacionCualitativa.AD, "Excelente desempeño", false);
      assertEquals(CalificacionCualitativa.AD, calif.getCalificacionCualitativa());
      assertEquals("Excelente desempeño", calif.getConclusionDescriptiva());
      assertFalse(calif.getSugerenciaIaUtilizada());
      assertFalse(calif.getRequiereRefuerzo());
      assertFalse(calif.necesitaRefuerzo());

      calif.actualizar(CalificacionCualitativa.C, "Requiere apoyo", true);
      assertTrue(calif.getRequiereRefuerzo());
      assertTrue(calif.necesitaRefuerzo());

      assertThrows(
          CalificacionInvalidaException.class,
          () -> calif.actualizar(null, "Obs", true));
      assertThrows(
          CalificacionInvalidaException.class,
          () -> calif.actualizar(CalificacionCualitativa.A, "Obs", null));
    }

    @Test
    void testInscripcionYSesionRefuerzo() {
      var inscripcion =
          InscripcionRefuerzo.builder()
              .id(1L)
              .sesionRefuerzoId(2L)
              .matriculaId(10L)
              .estudianteId(15L)
              .calificacionOrigenId(1L)
              .estadoAsistencia(EstadoAsistenciaRefuerzo.PENDIENTE)
              .build();

      assertThrows(
          BusinessException.class,
          () -> inscripcion.registrarAsistencia(EstadoAsistenciaRefuerzo.PENDIENTE, ""));
      assertThrows(
          BusinessException.class,
          () -> inscripcion.registrarAsistencia(EstadoAsistenciaRefuerzo.JUSTIFICADO, "  "));

      inscripcion.registrarAsistencia(EstadoAsistenciaRefuerzo.ASISTIO, "Participó activamente");
      assertEquals(EstadoAsistenciaRefuerzo.ASISTIO, inscripcion.getEstadoAsistencia());
      assertEquals("Participó activamente", inscripcion.getObservaciones());

      inscripcion.justificar("Salud");
      assertEquals(EstadoAsistenciaRefuerzo.JUSTIFICADO, inscripcion.getEstadoAsistencia());
      assertEquals("Salud", inscripcion.getObservaciones());

      var sesion =
          SesionRefuerzo.builder()
              .id(1L)
              .anioLectivoId((short) 1)
              .tema("Álgebra lineal")
              .fechaProgramada(LocalDate.of(2026, 6, 1))
              .horaInicio(LocalTime.of(15, 0))
              .horaFin(LocalTime.of(16, 30))
              .aulaAsignada("A-101")
              .build();

      sesion.reprogramar(
          LocalDate.of(2026, 6, 5), LocalTime.of(16, 0), LocalTime.of(17, 30), "A-105");
      assertEquals(LocalDate.of(2026, 6, 5), sesion.getFechaProgramada());
      assertEquals(LocalTime.of(16, 0), sesion.getHoraInicio());
      assertEquals("A-105", sesion.getAulaAsignada());

      sesion.actualizarTema("Álgebra y Ecuaciones");
      assertEquals("Álgebra y Ecuaciones", sesion.getTema());
    }
  }

  @Nested
  @DisplayName("Matricula Domain Tests")
  class MatriculaDomainTests {

    @Test
    void testApoderadoYEstudianteIdentidadYMetodos() {
      var apoderado =
          Apoderado.builder()
              .id(null)
              .tipoDocumento(TipoDocumento.DNI)
              .numeroDocumento("44556677")
              .nombres("Carlos")
              .apellidoPaterno("Perez")
              .apellidoMaterno("Gomez")
              .build();

      assertEquals("Carlos Perez Gomez", apoderado.nombreCompleto());

      apoderado.actualizarContacto(
          "987654321", "carlos@test.com", "Av. Grau 123", "150101");
      assertEquals("987654321", apoderado.getCelular());
      assertEquals("carlos@test.com", apoderado.getEmail());
      assertEquals("Av. Grau 123", apoderado.getDireccion());
      assertEquals("150101", apoderado.getUbigeoInei());

      apoderado.vincularUsuario(77L);
      assertEquals(77L, apoderado.getUsuarioId());

      apoderado.iniciarRegistroManual();
      assertFalse(apoderado.getValidadoReniec());
      assertEquals(OrigenRegistro.MANUAL_CONTINGENCIA, apoderado.getOrigenRegistro());

      // No se puede iniciar manual si ya tiene ID
      apoderado.setId(10L);
      assertThrows(BusinessException.class, apoderado::iniciarRegistroManual);

      // Verificación de identidad
      apoderado.verificarIdentidad("44556677", "Carlos Alberto", "Perez", "Gomez");
      assertTrue(apoderado.getValidadoReniec());
      assertEquals("Carlos Alberto", apoderado.getNombres());

      assertThrows(
          BusinessException.class,
          () -> apoderado.verificarIdentidad("00000000", "A", "B", "C"));
      assertThrows(
          BusinessException.class,
          () -> apoderado.verificarIdentidad("44556677", "", "B", "C"));

      // Estudiante
      var estudiante =
          Estudiante.builder()
              .id(null)
              .tipoDocumento(TipoDocumento.DNI)
              .numeroDocumento("77889900")
              .nombres("Lucia")
              .apellidoPaterno("Perez")
              .apellidoMaterno(null)
              .activo(true)
              .build();

      assertEquals("Lucia Perez ", estudiante.nombreCompleto());

      estudiante.actualizarDatos(
          "Lucia Elena", "Perez", "Gomez", LocalDate.of(2015, 8, 20), Genero.F);
      assertEquals("Lucia Elena", estudiante.getNombres());
      assertEquals(Genero.F, estudiante.getGenero());

      estudiante.actualizarInformacionMedica("O+", "Ninguna");
      assertEquals("O+", estudiante.getGrupoSanguineo());
      assertEquals("Ninguna", estudiante.getAlergiasCondiciones());

      estudiante.actualizarCodigoSiagie("SIAGIE-12345");
      assertEquals("SIAGIE-12345", estudiante.getCodigoEstudianteSiagie());

      estudiante.desactivar();
      assertFalse(estudiante.getActivo());
      estudiante.activar();
      assertTrue(estudiante.getActivo());

      estudiante.iniciarRegistroManual();
      assertEquals(OrigenRegistro.MANUAL_CONTINGENCIA, estudiante.getOrigenRegistro());

      estudiante.verificarIdentidad("77889900", "Lucia Elena", "Perez", "Gomez");
      assertTrue(estudiante.getValidadoReniec());

      // EstudianteApoderado
      var estApo =
          EstudianteApoderado.builder()
              .estudianteId(1L)
              .apoderadoId(2L)
              .parentesco(Parentesco.PADRE)
              .esResponsableEconomico(true)
              .tieneCustodia(true)
              .permiteRecojo(true)
              .build();

      estApo.actualizarPermisos(Parentesco.TUTOR_LEGAL, false, false, false);
      assertEquals(Parentesco.TUTOR_LEGAL, estApo.getParentesco());
      assertFalse(estApo.getEsResponsableEconomico());
      assertFalse(estApo.getTieneCustodia());
      assertFalse(estApo.getPermiteRecojo());
    }

    @Test
    void testMatriculaCicloDeVida() {
      var mat = Matricula.builder().id(null).estadoMatricula(null).build();
      mat.iniciarSolicitud();
      assertEquals(EstadoMatricula.SOLICITADA, mat.getEstadoMatricula());

      // No se puede iniciar solicitud si ya tiene estado
      assertThrows(BusinessException.class, mat::iniciarSolicitud);

      var ahora = OffsetDateTime.now(ZoneOffset.UTC);
      assertThrows(BusinessException.class, () -> mat.reservar(ahora.minusMinutes(1)));

      mat.reservar(ahora.plusDays(2));
      assertEquals(EstadoMatricula.RESERVADA_TEMPORAL, mat.getEstadoMatricula());

      mat.confirmar(ahora);
      assertTrue(mat.estaMatriculado());
      assertNull(mat.getReservaExpiraAt());

      // Confirmar de nuevo es idempotente
      mat.confirmar(ahora);
      assertTrue(mat.estaMatriculado());

      // Cambiar sección
      mat.cambiarSeccion(20);
      assertEquals(20, mat.getSeccionId());

      // Trasladar
      mat.trasladar("Cambio de domicilio");
      assertEquals(EstadoMatricula.TRASLADADO, mat.getEstadoMatricula());
      assertEquals("Cambio de domicilio", mat.getObservaciones());

      // Retirar
      mat.retirar("Motivos familiares");
      assertEquals(EstadoMatricula.RETIRADO, mat.getEstadoMatricula());

      // Cancelar
      mat.cancelar("Anulación administrativa");
      assertEquals(EstadoMatricula.CANCELADA, mat.getEstadoMatricula());
    }
  }

  @Nested
  @DisplayName("Shared Domain Tests")
  class SharedDomainTests {

    @Test
    void testReglasValidaciones() {
      assertDoesNotThrow(() -> Reglas.exigir(true, "Error"));
      var ex = assertThrows(BusinessException.class, () -> Reglas.exigir(false, "Mensaje error"));
      assertEquals("Mensaje error", ex.getMessage());

      assertEquals("Valor", Reglas.requerido(Optional.of("Valor")));
      assertThrows(BusinessException.class, () -> Reglas.requerido(Optional.empty()));

      assertDoesNotThrow(
          () -> Reglas.fechas(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 2)));
      assertThrows(
          BusinessException.class,
          () -> Reglas.fechas(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 1)));
      assertThrows(
          BusinessException.class,
          () -> Reglas.fechas(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 1)));
      assertThrows(BusinessException.class, () -> Reglas.fechas(null, LocalDate.now()));

      assertDoesNotThrow(
          () -> Reglas.horas(LocalTime.of(8, 0), LocalTime.of(9, 0)));
      assertThrows(
          BusinessException.class,
          () -> Reglas.horas(LocalTime.of(9, 0), LocalTime.of(8, 0)));
      assertThrows(
          BusinessException.class,
          () -> Reglas.horas(LocalTime.of(8, 0), LocalTime.of(8, 0)));
      assertThrows(BusinessException.class, () -> Reglas.horas(null, LocalTime.now()));
    }
  }

  @Nested
  @DisplayName("Tesoreria Domain Tests")
  class TesoreriaDomainTests {

    @Test
    void testObligacionPagoProgramarYAbono() {
      var conceptoMatricula =
          ConceptoCobro.builder()
              .id((short) 1)
              .codigo("MAT-2026")
              .nombre("Matrícula 2026")
              .tipoConcepto(TipoConcepto.MATRICULA)
              .montoSugerido(new BigDecimal("450.00"))
              .build();

      conceptoMatricula.actualizar("Matrícula Actualizada", new BigDecimal("500.00"));
      assertEquals("Matrícula Actualizada", conceptoMatricula.getDescripcion());
      assertEquals(new BigDecimal("500.00"), conceptoMatricula.getMontoSugerido());

      var obligacion =
          ObligacionPago.programar(
              100L, conceptoMatricula, (short) 0, LocalDate.of(2026, 2, 28));

      assertEquals(100L, obligacion.getMatriculaId());
      assertEquals(EstadoObligacion.PENDIENTE, obligacion.getEstado());
      assertTrue(obligacion.estaPendiente());
      assertFalse(obligacion.estaPagadoTotal());
      assertEquals(new BigDecimal("500.00"), obligacion.saldo());

      // Incompatible cuota
      assertThrows(
          BusinessException.class,
          () -> ObligacionPago.programar(100L, conceptoMatricula, (short) 1, LocalDate.now()));

      var conceptoPension =
          ConceptoCobro.builder()
              .id((short) 2)
              .tipoConcepto(TipoConcepto.PENSION)
              .nombre("Pensión")
              .montoSugerido(new BigDecimal("400.00"))
              .build();
      assertThrows(
          BusinessException.class,
          () -> ObligacionPago.programar(100L, conceptoPension, (short) 0, LocalDate.now()));

      // Aplicar mora y descuento
      obligacion.aplicarMora(new BigDecimal("20.00"));
      obligacion.aplicarDescuento(new BigDecimal("50.00"));
      assertEquals(new BigDecimal("20.00"), obligacion.getMontoMora());
      assertEquals(new BigDecimal("50.00"), obligacion.getMontoDescuento());
      assertEquals(new BigDecimal("470.00"), obligacion.saldo());

      // Registrar abono parcial
      obligacion.registrarAbono(new BigDecimal("200.00"));
      assertEquals(new BigDecimal("200.00"), obligacion.getTotalPagado());
      assertEquals(EstadoObligacion.PAGADO_PARCIAL, obligacion.getEstado());
      assertEquals(new BigDecimal("270.00"), obligacion.saldo());

      // Sobrepago rechazado
      assertThrows(
          SobrepagoException.class,
          () -> obligacion.registrarAbono(new BigDecimal("270.01")));

      // Completar pago
      obligacion.registrarAbono(new BigDecimal("270.00"));
      assertEquals(EstadoObligacion.PAGADO_TOTAL, obligacion.getEstado());
      assertTrue(obligacion.estaPagadoTotal());
      assertEquals(0, obligacion.saldo().compareTo(BigDecimal.ZERO));

      // Anular por traslado
      obligacion.anularPorTraslado();
      assertEquals(EstadoObligacion.CANCELADO_POR_TRASLADO, obligacion.getEstado());
      assertThrows(BusinessException.class, () -> obligacion.validarPago(BigDecimal.ONE));
    }

    @Test
    void testPagoTransaccionMetodosCajaYAprobacion() {
      var pago =
          PagoTransaccion.builder()
              .id(1L)
              .obligacionPagoId(10L)
              .metodoPago(MetodoPago.EFECTIVO)
              .montoPagado(new BigDecimal("150.00"))
              .estadoPago(null)
              .payloadWebhook(new HashMap<>())
              .build();

      pago.registrarOrigenCaja();
      assertEquals(PasarelaProveedor.CAJA_EFECTIVO, pago.getPasarelaProveedor());

      pago.setMetodoPago(MetodoPago.TRANSFERENCIA);
      pago.registrarOrigenCaja();
      assertEquals(PasarelaProveedor.TRANSFERENCIA, pago.getPasarelaProveedor());

      pago.setMetodoPago(MetodoPago.TARJETA_CREDITO);
      assertThrows(BusinessException.class, pago::registrarOrigenCaja);

      pago.aprobar();
      assertTrue(pago.estaAprobado());

      assertThrows(BusinessException.class, pago::rechazar);

      pago.revertir("Error en conciliación bancaria");
      assertEquals(EstadoPago.REVERTIDO, pago.getEstadoPago());
      assertEquals("Error en conciliación bancaria", pago.getPayloadWebhook().get("motivoReversion"));

      assertThrows(BusinessException.class, pago::aprobar);
      assertThrows(BusinessException.class, pago::rechazar);
    }

    @Test
    void testComprobantePagoYSerie() {
      var pago =
          PagoTransaccion.builder()
              .id(10L)
              .estadoPago(EstadoPago.APROBADO)
              .montoPagado(new BigDecimal("350.00"))
              .build();

      var comp = ComprobantePago.emitir(pago, TipoComprobante.BOLETA, "B001", 42);
      assertEquals("B001-00000042", comp.numeroCompleto());
      assertEquals(EstadoComprobante.EMITIDO, comp.getEstadoComprobante());
      assertFalse(comp.estaAnulado());

      var ahora = OffsetDateTime.now(ZoneOffset.UTC);
      comp.anular("Duplicado", ahora);
      assertTrue(comp.estaAnulado());
      assertEquals("Duplicado", comp.getMotivoAnulacion());
      assertEquals(ahora, comp.getFechaAnulacion());

      // Validaciones emitir
      assertThrows(
          BusinessException.class,
          () -> ComprobantePago.emitir(null, TipoComprobante.BOLETA, "B001", 1));
      assertThrows(
          BusinessException.class,
          () -> ComprobantePago.emitir(pago, null, "B001", 1));
      assertThrows(
          BusinessException.class,
          () -> ComprobantePago.emitir(pago, TipoComprobante.BOLETA, "INVALID", 1));
      assertThrows(
          BusinessException.class,
          () -> ComprobantePago.emitir(pago, TipoComprobante.BOLETA, "B001", null));
      assertThrows(
          BusinessException.class,
          () -> ComprobantePago.emitir(pago, TipoComprobante.BOLETA, "B001", -1));

      // SerieComprobante
      var serie = SerieComprobante.builder().serie("B001").ultimoCorrelativo(10).build();
      assertEquals(11, serie.siguienteCorrelativo());
      assertEquals(12, serie.siguienteCorrelativo());
      assertNotNull(serie.getUpdatedAt());

      var serieCorrupta = SerieComprobante.builder().serie("B001").ultimoCorrelativo(-1).build();
      assertThrows(BusinessException.class, serieCorrupta::siguienteCorrelativo);
    }
  }

  @Nested
  @DisplayName("Usuario Domain Tests")
  class UsuarioDomainTests {

    @Test
    void testRolYSesion() {
      var rol1 = Rol.builder().id((short) 1).codigo("ADMIN").nombre("Administrador").descripcion("Admin").build();
      var rol2 = Rol.builder().id((short) 1).codigo("ADMIN").nombre("Administrador").descripcion("Admin").build();
      var rol3 = Rol.builder().id((short) 2).codigo("DOCENTE").nombre("Docente").descripcion("Doc").build();

      assertEquals(rol1, rol2);
      assertNotEquals(rol1, rol3);
      assertEquals(rol1.hashCode(), rol2.hashCode());
      assertNotNull(rol1.toString());

      var sesion =
          Sesion.builder()
              .id(1L)
              .usuarioId(10L)
              .tokenHash("hash123")
              .ipAddress("127.0.0.1")
              .userAgent("Mozilla/5.0")
              .expiraAt(LocalDateTime.of(2026, 6, 1, 12, 0))
              .createdAt(LocalDateTime.of(2026, 6, 1, 10, 0))
              .build();

      assertFalse(sesion.estaExpirada(LocalDateTime.of(2026, 6, 1, 11, 0)));
      assertTrue(sesion.estaExpirada(LocalDateTime.of(2026, 6, 1, 12, 0)));
      assertTrue(sesion.estaExpirada(LocalDateTime.of(2026, 6, 1, 12, 1)));
      assertTrue(sesion.estaExpirada(null));

      sesion.renovar(
          "hashNuevo",
          "192.168.1.1",
          "PostmanRuntime",
          LocalDateTime.of(2026, 6, 1, 11, 0),
          LocalDateTime.of(2026, 6, 1, 14, 0));
      assertEquals("hashNuevo", sesion.getTokenHash());
      assertEquals("192.168.1.1", sesion.getIpAddress());
      assertEquals("PostmanRuntime", sesion.getUserAgent());
      assertEquals(LocalDateTime.of(2026, 6, 1, 14, 0), sesion.getExpiraAt());

      assertThrows(
          BusinessException.class,
          () ->
              sesion.renovar(
                  "",
                  "ip",
                  "agente",
                  LocalDateTime.of(2026, 6, 1, 11, 0),
                  LocalDateTime.of(2026, 6, 1, 15, 0)));
    }

    @Test
    void testUsuarioRolesYPerfil() {
      var rolAdmin = Rol.builder().id((short) 1).codigo("ADMIN").nombre("Admin").build();
      var rolDocente = Rol.builder().id((short) 2).codigo("DOCENTE").nombre("Docente").build();

      var usuario =
          Usuario.builder()
              .id(null)
              .username("juan.perez")
              .email("juan@colegio.edu.pe")
              .activo(false)
              .build();

      assertFalse(usuario.estaActivo());
      assertFalse(usuario.tieneRol("ADMIN"));

      var ahora = LocalDateTime.now();
      usuario.registrar("bcryptHash$123", Set.of(rolAdmin), ahora);
      assertTrue(usuario.estaActivo());
      assertTrue(usuario.tieneRol("ADMIN"));
      assertFalse(usuario.tieneRol("DOCENTE"));
      assertNotNull(usuario.getUuid());

      usuario.agregarRol(rolDocente);
      assertTrue(usuario.tieneRol("DOCENTE"));

      usuario.removerRol(rolAdmin);
      assertFalse(usuario.tieneRol("ADMIN"));
      assertTrue(usuario.tieneRol("DOCENTE"));

      usuario.desactivar();
      assertFalse(usuario.estaActivo());
      usuario.activar();
      assertTrue(usuario.estaActivo());

      usuario.cambiarPassword("nuevoHash$456");
      assertEquals("nuevoHash$456", usuario.getPasswordHash());

      usuario.actualizarPerfil("juan.nuevo@colegio.edu.pe");
      assertEquals("juan.nuevo@colegio.edu.pe", usuario.getEmail());

      usuario.actualizarPerfil("   "); // no sobreescribe si blanco
      assertEquals("juan.nuevo@colegio.edu.pe", usuario.getEmail());

      // Validaciones registrar
      usuario.setId(10L);
      assertThrows(
          BusinessException.class,
          () -> usuario.registrar("hash", Set.of(rolAdmin), LocalDateTime.now()));

      var nuevoUsuario = new Usuario();
      assertThrows(
          BusinessException.class,
          () -> nuevoUsuario.registrar(null, Set.of(rolAdmin), LocalDateTime.now()));
      assertThrows(
          BusinessException.class,
          () -> nuevoUsuario.registrar("hash", Collections.emptySet(), LocalDateTime.now()));
    }
  }
}
