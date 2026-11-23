package com.colegio.shuji.matricula.infrastructure.service;

import com.colegio.shuji.matricula.domain.service.PoliticaAdmision;
import com.colegio.shuji.academico.application.dto.out.AnioLectivoResponseDto;
import com.colegio.shuji.academico.infrastructure.repository.JpaAnioLectivoRepository;
import com.colegio.shuji.academico.infrastructure.repository.JpaSeccionRepository;
import com.colegio.shuji.matricula.application.dto.in.CrearSolicitudMatriculaPublicaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.ObservarSolicitudMatriculaPublicaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.ReenviarTokenSolicitudRequestDto;
import com.colegio.shuji.matricula.application.dto.out.ReenviarTokenSolicitudResponseDto;
import com.colegio.shuji.matricula.application.dto.out.CatalogoMatriculaPublicaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.DocumentoSolicitudResponseDto;
import com.colegio.shuji.matricula.application.dto.out.PagoMatriculaPublicaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.SeccionMatriculaPublicaOpcionDto;
import com.colegio.shuji.matricula.application.dto.out.SolicitudMatriculaPublicaAdminResponseDto;
import com.colegio.shuji.matricula.application.dto.out.SolicitudMatriculaPublicaResponseDto;
import com.colegio.shuji.matricula.application.port.out.GeminiDocumentoMatriculaPort;
import com.colegio.shuji.matricula.application.port.out.FirmaDigitalDocumentoPort;
import com.colegio.shuji.matricula.application.port.out.CuentaApoderadoPort;
import com.colegio.shuji.matricula.application.port.out.SeguimientoSolicitudCorreoPort;
import com.colegio.shuji.matricula.application.dto.out.VersionDocumentoMatriculaResponseDto;
import com.colegio.shuji.matricula.domain.enums.EstadoFirmaDigital;
import com.colegio.shuji.matricula.infrastructure.entity.DocumentoMatriculaVersionEntity;
import com.colegio.shuji.matricula.infrastructure.repository.JpaDocumentoMatriculaVersionRepository;
import com.colegio.shuji.matricula.domain.enums.EstadoSolicitudMatriculaPublica;
import com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento;
import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.ModalidadIngreso;
import com.colegio.shuji.matricula.domain.enums.OrigenRegistro;
import com.colegio.shuji.matricula.domain.enums.Parentesco;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
import com.colegio.shuji.matricula.domain.service.ReglaEdadMatricula;
import com.colegio.shuji.matricula.infrastructure.entity.ApoderadoEntity;
import com.colegio.shuji.matricula.infrastructure.entity.EstudianteApoderadoEntity;
import com.colegio.shuji.matricula.infrastructure.entity.EstudianteEntity;
import com.colegio.shuji.matricula.infrastructure.entity.MatriculaEntity;
import com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaPublicaEntity;
import com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaDocumentoEntity;
import com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaDocumentoId;
import com.colegio.shuji.matricula.infrastructure.repository.JpaApoderadoRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaEstudianteApoderadoRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaEstudianteRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaMatriculaRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaSolicitudMatriculaPublicaRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaSolicitudMatriculaDocumentoRepository;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.application.port.out.MercadoPagoPort;
import com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort.PagoVerificado;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SolicitudMatriculaPublicaService {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SolicitudMatriculaPublicaService.class);
  public static final String REFERENCIA_PAGO_PREFIX = "MATRICULA_PUBLICA:";
  private static final String MENSAJE_REENVIO_SEGURO =
      "Si existe una solicitud activa con esos datos, enviaremos un enlace al correo registrado.";

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final List<EstadoSolicitudMatriculaPublica> SOLICITUDES_ACTIVAS = List.of(
      EstadoSolicitudMatriculaPublica.DOCUMENTOS_PENDIENTES,
      EstadoSolicitudMatriculaPublica.DOCUMENTOS_EN_VALIDACION,
      EstadoSolicitudMatriculaPublica.DOCUMENTOS_OBSERVADOS,
      EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS,
      EstadoSolicitudMatriculaPublica.AUTORIZACION_EDAD_PENDIENTE,
      EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE,
      EstadoSolicitudMatriculaPublica.PAGO_CONFIRMADO_PENDIENTE_ASIGNACION);

  private final JpaSolicitudMatriculaPublicaRepository solicitudes;
  private final JpaSolicitudMatriculaDocumentoRepository documentos;
  private final JpaDocumentoMatriculaVersionRepository versiones;
  private final JpaAnioLectivoRepository anios;
  private final JpaSeccionRepository secciones;
  private final JpaEstudianteRepository estudiantes;
  private final JpaApoderadoRepository apoderados;
  private final JpaEstudianteApoderadoRepository vinculos;
  private final JpaMatriculaRepository matriculas;
  private final GeminiDocumentoMatriculaPort gemini;
  private final MercadoPagoPort mercadoPago;
  private final com.colegio.shuji.tesoreria.application.port.in.RegistrarCobroMatriculaUseCase cobrosMatricula;

  @Autowired(required = false)
  private SeguimientoSolicitudCorreoPort correoSeguimiento;

  @Autowired(required = false)
  private CuentaApoderadoPort cuentaApoderadoPort;

  @Autowired(required = false)
  private com.colegio.shuji.tesoreria.infrastructure.adapter.ComprobanteMatriculaPdfAdapter comprobantePdfAdapter;

  public void setComprobantePdfAdapter(com.colegio.shuji.tesoreria.infrastructure.adapter.ComprobanteMatriculaPdfAdapter comprobantePdfAdapter) {
    this.comprobantePdfAdapter = comprobantePdfAdapter;
  }

  public void setCuentaApoderadoPort(CuentaApoderadoPort cuentaApoderadoPort) {
    this.cuentaApoderadoPort = cuentaApoderadoPort;
  }

  public void setCorreoSeguimiento(SeguimientoSolicitudCorreoPort correoSeguimiento) {
    this.correoSeguimiento = correoSeguimiento;
  }

  @Value("${app.matricula-publica.monto-matricula:350.00}")
  private BigDecimal montoMatriculaPublica = new BigDecimal("350.00");

  @Value("${app.matricula-publica.monto-cobro:350.00}")
  private BigDecimal montoCobroMatriculaPublica = new BigDecimal("350.00");

  @Value("${app.matricula-publica.duracion-reserva-minutos:30}")
  private long duracionReservaMinutos;

  @Value("${app.matricula-publica.validar-edad:false}")
  private boolean validarEdad;

  @Value("${integraciones.pagos.mercadopago.sandbox:false}")
  private boolean mercadoPagoSandbox;

  @Transactional(readOnly = true)
  public CatalogoMatriculaPublicaResponseDto catalogo() {
    var aniosAbiertos = anios.findAll().stream()
        .filter(a -> Boolean.TRUE.equals(a.getAbierto()))
        .filter(a -> PoliticaAdmision.admiteNuevasMatriculas(a.getAnio()))
        .map(a -> new AnioLectivoResponseDto(a.getId(), a.getAnio(), a.getFechaInicio(), a.getFechaFin(), a.getAbierto()))
        .toList();
    var comparadorSeccion = java.util.Comparator
        .<com.colegio.shuji.academico.infrastructure.entity.SeccionEntity, Integer>comparing(s -> {
          var g = s.getRelacion1();
          var n = g == null ? null : g.getRelacion0();
          if (n != null) {
            if (n.getCodigo() != null) {
              return switch (n.getCodigo()) {
                case INICIAL -> 1;
                case PRIMARIA -> 2;
                case SECUNDARIA -> 3;
              };
            }
            if (n.getNombre() != null) {
              String nombre = n.getNombre().toUpperCase(java.util.Locale.ROOT);
              if (nombre.contains("INICIAL")) return 1;
              if (nombre.contains("PRIMARIA")) return 2;
              if (nombre.contains("SECUNDARIA")) return 3;
            }
          }
          return 999;
        })
        .thenComparingInt(s -> {
          var g = s.getRelacion1();
          if (g != null && g.getNumeroGrado() != null) {
            return g.getNumeroGrado();
          }
          if (g != null && g.getNombre() != null) {
            var m = java.util.regex.Pattern.compile("\\d+").matcher(g.getNombre());
            if (m.find()) {
              return Integer.parseInt(m.group());
            }
          }
          return 999;
        })
        .thenComparing(com.colegio.shuji.academico.infrastructure.entity.SeccionEntity::getLetra, java.util.Comparator.nullsLast(String::compareToIgnoreCase))
        .thenComparing(s -> s.getId() == null ? 0 : s.getId());

    var opciones = aniosAbiertos.stream()
        .flatMap(a -> secciones.buscarPorAnioLectivoId(a.id()).stream())
        .filter(s -> s.getCupoMaximo() > s.getVacantesOcupadas())
        .sorted(comparadorSeccion)
        .map(s -> {
          var grado = s.getRelacion1();
          var nivel = grado == null ? null : grado.getRelacion0();
          var edades = limitesEdad(s);
          return new SeccionMatriculaPublicaOpcionDto(
              s.getId(), s.getAnioLectivoId(),
              s.getGradoId(),
              nivel == null ? "Nivel" : nivel.getNombre(),
              grado == null ? "Grado" : grado.getNombre(),
              s.getLetra(), s.getCupoMaximo() - s.getVacantesOcupadas(), edades.minima(), edades.maxima());
        })
        .toList();
    return new CatalogoMatriculaPublicaResponseDto(aniosAbiertos, opciones);
  }

  @Transactional
  public SolicitudMatriculaPublicaResponseDto crear(CrearSolicitudMatriculaPublicaRequestDto r) {
    exigirProgenitor(r.parentesco());
    var anio = anios.findById(r.anioLectivoId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Año lectivo inexistente"));
    if (!Boolean.TRUE.equals(anio.getAbierto())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El año lectivo no admite matrículas");
    }
    if (!PoliticaAdmision.admiteNuevasMatriculas(anio.getAnio())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          PoliticaAdmision.mensajeAnioNoDisponible());
    }
    var seccionesGrado = secciones.buscarPorAnioLectivoId(r.anioLectivoId());
    var seccion = seccionesGrado.stream()
        .filter(opcion -> opcion.getGradoId().equals(r.gradoId()))
        .findFirst()
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El grado no corresponde al año lectivo"));
    boolean hayVacantesEnGrado = seccionesGrado.stream()
        .filter(opcion -> opcion.getGradoId().equals(r.gradoId()))
        .anyMatch(opcion -> opcion.getCupoMaximo() > opcion.getVacantesOcupadas());
    if (!hayVacantesEnGrado) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "No hay vacantes disponibles para el grado seleccionado");
    }
    var edades = limitesEdad(seccion);
    boolean requiereAutorizacionEdad = validarEdad
        && !edades.admite(r.fechaNacimientoEstudiante(), anio.getAnio());
    var estudianteExistente = estudiantes.buscarPorNumeroDocumento(r.numeroDocumentoEstudiante().trim());
    if (!estudianteExistente.isEmpty()) {
      Long estudianteId = estudianteExistente.getFirst().getId();
      if (matriculas.existsByEstudianteIdAndAnioLectivoId(estudianteId, r.anioLectivoId())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "El estudiante ya cuenta con vacante confirmada para este año lectivo.");
      }
    }
    if (solicitudes.existsByNumeroDocumentoEstudianteAndEstadoIn(
        r.numeroDocumentoEstudiante().trim(), SOLICITUDES_ACTIVAS)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una solicitud en trámite para este DNI. Puedes solicitar el reenvío de tu enlace de seguimiento.");
    }

    ModalidadIngreso modalidad = r.modalidadIngreso() != null ? r.modalidadIngreso() : ModalidadIngreso.REGULAR;
    String colegioProcedencia = r.colegioProcedencia() == null ? null : r.colegioProcedencia().trim();
    if (colegioProcedencia != null && colegioProcedencia.isEmpty()) colegioProcedencia = null;
    if (modalidad == ModalidadIngreso.TRASLADO && (colegioProcedencia == null || colegioProcedencia.length() < 3)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "En solicitudes por traslado debes indicar el colegio de procedencia");
    }

    byte[] secreto = new byte[32];
    RANDOM.nextBytes(secreto);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(secreto);
    var s = new SolicitudMatriculaPublicaEntity();
    s.setId(UUID.randomUUID());
    s.setTokenHash(hashToken(token));
    s.setEstado(requiereAutorizacionEdad
        ? EstadoSolicitudMatriculaPublica.AUTORIZACION_EDAD_PENDIENTE
        : EstadoSolicitudMatriculaPublica.DOCUMENTOS_PENDIENTES);
    s.setRequiereAutorizacionEdad(requiereAutorizacionEdad);
    s.setAnioLectivoId(r.anioLectivoId());
    s.setGradoId(r.gradoId());
    s.setSeccionId(null);
    s.setModalidadIngreso(modalidad);
    s.setColegioProcedencia(colegioProcedencia);
    s.setNumeroDocumentoEstudiante(r.numeroDocumentoEstudiante().trim());
    s.setNombresEstudiante(r.nombresEstudiante().trim());
    s.setApellidoPaternoEstudiante(r.apellidoPaternoEstudiante().trim());
    s.setApellidoMaternoEstudiante(r.apellidoMaternoEstudiante().trim());
    s.setFechaNacimientoEstudiante(r.fechaNacimientoEstudiante());
    s.setGeneroEstudiante(r.generoEstudiante());
    s.setNumeroDocumentoApoderado(r.numeroDocumentoApoderado().trim());
    s.setNombresApoderado(r.nombresApoderado().trim());
    s.setApellidoPaternoApoderado(r.apellidoPaternoApoderado().trim());
    s.setApellidoMaternoApoderado(r.apellidoMaternoApoderado().trim());
    s.setCelularApoderado(r.celularApoderado().trim());
    s.setEmailApoderado(r.emailApoderado() == null ? null : r.emailApoderado().trim());
    s.setDireccionApoderado(r.direccionApoderado().trim());
    s.setUbigeoApoderado(
        r.ubigeoApoderado() != null && !r.ubigeoApoderado().isBlank()
            ? r.ubigeoApoderado().trim()
            : null);
    s.setParentesco(r.parentesco());
    s.setConsentimientoGemini(r.consentimientoGemini());
    s.setPagoMonto(obtenerMontoMatriculaPublica());
    SolicitudMatriculaPublicaEntity guardada;
    try {
      guardada = solicitudes.saveAndFlush(s);
    } catch (DataIntegrityViolationException ex) {
      // Una solicitud simultánea en otra sección puede ganar el índice único del DNI.
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "La solicitud entra en conflicto con un registro existente; consulta tu expediente antes de reintentar", ex);
    }
    String email = guardada.getEmailApoderado();
    if (email != null && !email.isBlank()) {
      if (correoSeguimiento == null) {
        log.warn("Solicitud {} creada; adaptador de correo no disponible, se devuelve enlace de seguimiento",
            guardada.getId());
      } else {
        try {
          correoSeguimiento.enviarEnlaceSeguimiento(email.trim(), guardada.getId(), token);
        } catch (RuntimeException ex) {
          log.warn("Solicitud {} creada; no se pudo enviar el enlace SMTP ({})",
              guardada.getId(), ex.getClass().getSimpleName());
        }
      }
    }
    return respuesta(guardada, token);
  }

  private ReglaEdadMatricula.Rango limitesEdad(
      com.colegio.shuji.academico.infrastructure.entity.SeccionEntity seccion) {
    var grado = seccion.getRelacion1();
    var nivel = grado == null ? null : grado.getRelacion0();
    try {
      return ReglaEdadMatricula.paraGrado(nivel == null || nivel.getCodigo() == null ? null : nivel.getCodigo().name(),
          grado == null ? null : grado.getNumeroGrado());
    } catch (IllegalArgumentException ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage());
    }
  }

  @Transactional(readOnly = true)
  public PreparacionDocumento prepararValidacion(UUID id, String token, TipoDocumentoSolicitud tipo) {
    if (!TipoDocumentoSolicitud.REQUERIDOS_TRASLADO.contains(tipo)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de documento no admitido para esta operación");
    var s = solicitudConToken(id, token);
    if (!s.tiposRequeridos().contains(tipo)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de documento no requerido para esta modalidad de ingreso");
    }
    exigirProgenitor(s.getParentesco());
    if (!s.isConsentimientoGemini()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No se autorizó enviar los documentos a Gemini");
    }
    if (s.getEstado() != EstadoSolicitudMatriculaPublica.DOCUMENTOS_PENDIENTES
        && s.getEstado() != EstadoSolicitudMatriculaPublica.DOCUMENTOS_EN_VALIDACION
        && s.getEstado() != EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS
        && s.getEstado() != EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE
        && s.getEstado() != EstadoSolicitudMatriculaPublica.DOCUMENTOS_OBSERVADOS
        && s.getEstado() != EstadoSolicitudMatriculaPublica.AUTORIZACION_EDAD_PENDIENTE) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La etapa documental ya está cerrada");
    }
    var estado = s.estadoDocumento(tipo);
    if (estado == EstadoValidacionDocumento.VALIDADO && !requiereNuevaEvidencia(id, tipo)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Este documento ya fue validado");
    }
    var actual = documentos.findById(new SolicitudMatriculaDocumentoId(id, tipo));
    return new PreparacionDocumento(nombreEstudiante(s), s.getNumeroDocumentoEstudiante(),
        actual.map(SolicitudMatriculaDocumentoEntity::getVersionId).orElse(null),
        new GeminiDocumentoMatriculaPort.ContextoParental(nombreApoderado(s), s.getNumeroDocumentoApoderado(), s.getParentesco()));
  }

  public record PreparacionDocumento(String nombres, String dni, UUID versionActual,
      GeminiDocumentoMatriculaPort.ContextoParental progenitor) {}

  @Transactional
  public SolicitudMatriculaPublicaResponseDto guardarResultadoDocumento(
      UUID id, String token, TipoDocumentoSolicitud tipo, GeminiDocumentoMatriculaPort.Resultado resultado,
      FirmaDigitalDocumentoPort.Resultado firma, Boolean dniApiCoincide, UUID versionEsperada,
      String bucket, String objectKey, String mimeType, long tamanoBytes, String sha256) {
    return guardarResultadoDocumento(id, token, tipo, resultado, firma, dniApiCoincide, versionEsperada,
        bucket, objectKey, mimeType, tamanoBytes, sha256, false);
  }

  @Transactional
  public SolicitudMatriculaPublicaResponseDto guardarResultadoDocumentoPresentacion(
      UUID id, String token, TipoDocumentoSolicitud tipo, GeminiDocumentoMatriculaPort.Resultado resultado,
      FirmaDigitalDocumentoPort.Resultado firma, UUID versionEsperada,
      String bucket, String objectKey, String mimeType, long tamanoBytes, String sha256) {
    return guardarResultadoDocumento(id, token, tipo, resultado, firma, null, versionEsperada,
        bucket, objectKey, mimeType, tamanoBytes, sha256, true);
  }

  private SolicitudMatriculaPublicaResponseDto guardarResultadoDocumento(
      UUID id, String token, TipoDocumentoSolicitud tipo, GeminiDocumentoMatriculaPort.Resultado resultado,
      FirmaDigitalDocumentoPort.Resultado firma, Boolean dniApiCoincide, UUID versionEsperada,
      String bucket, String objectKey, String mimeType, long tamanoBytes, String sha256,
      boolean modoPresentacion) {
    if (!"application/pdf".equals(mimeType)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
        "Los tres documentos deben ser PDF");

    var s = solicitudConTokenBloqueada(id, token);
    exigirProgenitor(s.getParentesco());
    if (s.getEstado() != EstadoSolicitudMatriculaPublica.DOCUMENTOS_PENDIENTES
        && s.getEstado() != EstadoSolicitudMatriculaPublica.DOCUMENTOS_EN_VALIDACION
        && s.getEstado() != EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS
        && s.getEstado() != EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE
        && s.getEstado() != EstadoSolicitudMatriculaPublica.DOCUMENTOS_OBSERVADOS
        && s.getEstado() != EstadoSolicitudMatriculaPublica.AUTORIZACION_EDAD_PENDIENTE) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud cambió mientras se validaba el archivo");
    }
    var actual = documentos.findById(new SolicitudMatriculaDocumentoId(id, tipo));
    if ((s.estadoDocumento(tipo) == EstadoValidacionDocumento.VALIDADO && !requiereNuevaEvidencia(id, tipo))
        || !java.util.Objects.equals(versionEsperada, actual.map(SolicitudMatriculaDocumentoEntity::getVersionId).orElse(null))) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El documento fue actualizado o revisado; consulta el trámite nuevamente");
    }
    boolean comprobarIdentidad = tipo == TipoDocumentoSolicitud.DNI_C4
        || tipo == TipoDocumentoSolicitud.PARTIDA_NACIMIENTO;
    boolean esProgenitor = s.getParentesco() == Parentesco.PADRE || s.getParentesco() == Parentesco.MADRE;
    boolean vinculoAceptado = tipo != TipoDocumentoSolicitud.PARTIDA_NACIMIENTO
        || !esProgenitor
        || Boolean.TRUE.equals(resultado.vinculoParentalCoincide());
    boolean identidadAceptada = !modoPresentacion || !comprobarIdentidad
        || (resultado.valido() && (tipo != TipoDocumentoSolicitud.PARTIDA_NACIMIENTO || vinculoAceptado));
    String observacion = modoPresentacion
        ? !comprobarIdentidad
            ? "Modo presentación: archivo recibido; su contenido no se evaluó."
            : identidadAceptada
                ? tipo == TipoDocumentoSolicitud.DNI_C4
                    ? "Modo presentación: identidad coincide; se compararon el DNI y nombre del estudiante."
                    : "Modo presentación: identidad coincide; se compararon los nombres del estudiante y apoderado, y el DNI del apoderado si figura en el acta."
                : "Modo presentación: identidad no coincide; revisa el DNI o los nombres declarados."
        : resultado.observacion();
    var version = new DocumentoMatriculaVersionEntity();
    version.setId(UUID.randomUUID());
    version.setSolicitudId(id);
    version.setTipo(tipo);
    version.setBucket(bucket);
    version.setObjectKey(objectKey);
    version.setMimeType(mimeType);
    version.setTamanoBytes(tamanoBytes);
    version.setSha256(sha256);
    version.setIaValida(modoPresentacion && !comprobarIdentidad
        ? null : resultado.disponible() ? resultado.valido() : null);
    version.setDniApiCoincide(modoPresentacion ? null : dniApiCoincide);
    version.setVinculoParentalCoincide(resultado.vinculoParentalCoincide());
    version.setObservacionIa(observacion);
    version.setEstadoFirma(modoPresentacion ? EstadoFirmaDigital.NO_APLICA : firma.estado());
    version.setDetalleFirma(modoPresentacion
        ? "No se verifica firma digital en el modo de presentación." : firma.detalle());
    version.setCertificadoSha256(modoPresentacion ? null : firma.certificadoSha256());
    version.setFechaCarga(OffsetDateTime.now(ZoneOffset.UTC));
    versiones.saveAndFlush(version);
    var archivo = new SolicitudMatriculaDocumentoEntity();
    archivo.setId(new SolicitudMatriculaDocumentoId(id, tipo));
    archivo.setVersionId(version.getId());
    archivo.setBucket(bucket);
    archivo.setObjectKey(objectKey);
    archivo.setMimeType(mimeType);
    archivo.setTamanoBytes(tamanoBytes);
    archivo.setSha256(sha256);
    archivo.setFechaActualizacion(OffsetDateTime.now(ZoneOffset.UTC));
    documentos.save(archivo);
    boolean firmaAceptada = modoPresentacion || firma.estado() == EstadoFirmaDigital.VALIDA
        || firma.estado() == EstadoFirmaDigital.NO_APLICA;
    boolean dniAceptado = modoPresentacion || tipo == TipoDocumentoSolicitud.RECIBO_SERVICIO
        || Boolean.TRUE.equals(dniApiCoincide);
    boolean rechazo = modoPresentacion ? !identidadAceptada
        : Boolean.FALSE.equals(dniApiCoincide) || firma.estado() == EstadoFirmaDigital.INVALIDA
            || (esProgenitor && Boolean.FALSE.equals(resultado.vinculoParentalCoincide()))
            || (resultado.disponible() && !resultado.valido());
    boolean pendiente = !modoPresentacion && !rechazo
        && (!dniAceptado || !vinculoAceptado || !resultado.disponible()
            || firma.estado() == EstadoFirmaDigital.NO_VERIFICABLE
            || firma.estado() == EstadoFirmaDigital.ERROR_SERVICIO);
    boolean aprobado = modoPresentacion ? identidadAceptada
        : resultado.valido() && firmaAceptada && dniAceptado && vinculoAceptado;
    s.actualizarDocumento(tipo, pendiente ? EstadoValidacionDocumento.EN_VALIDACION
        : aprobado ? EstadoValidacionDocumento.VALIDADO : EstadoValidacionDocumento.OBSERVADO,
        modoPresentacion ? observacion
            : Boolean.FALSE.equals(dniApiCoincide) ? "El DNI y los nombres declarados no coinciden con la API de identidad."
            : !firmaAceptada ? firma.detalle() : resultado.observacion());


    actualizarEtapaDocumental(s);
    return respuesta(solicitudes.saveAndFlush(s), null);
  }

  @Transactional(readOnly = true)
  public SolicitudMatriculaPublicaResponseDto consultar(UUID id, String token) {
    return respuesta(solicitudConToken(id, token), null);
  }

  @Transactional
  public SolicitudMatriculaPublicaResponseDto consultarConVistoBuenoPresentacion(UUID id, String token) {
    var s = solicitudConTokenBloqueada(id, token);
    if (s.getEstado() == EstadoSolicitudMatriculaPublica.DOCUMENTOS_PENDIENTES
        || s.getEstado() == EstadoSolicitudMatriculaPublica.DOCUMENTOS_EN_VALIDACION
        || s.getEstado() == EstadoSolicitudMatriculaPublica.DOCUMENTOS_OBSERVADOS) {
      boolean huboCambios = false;
      for (TipoDocumentoSolicitud tipo : s.tiposRequeridos()) {
        var estadoDoc = s.estadoDocumento(tipo);
        if (estadoDoc == EstadoValidacionDocumento.EN_VALIDACION
            || estadoDoc == EstadoValidacionDocumento.OBSERVADO) {
          var docOpt = documentos.findById(new SolicitudMatriculaDocumentoId(s.getId(), tipo));
          if (docOpt.isPresent() && docOpt.get().getVersionId() != null) {
            versiones.findById(docOpt.get().getVersionId()).ifPresent(v -> {
              v.setIaValida(true);
              v.setDniApiCoincide(tipo == TipoDocumentoSolicitud.RECIBO_SERVICIO ? null : true);
              if (tipo == TipoDocumentoSolicitud.PARTIDA_NACIMIENTO) {
                v.setVinculoParentalCoincide(true);
              }
              v.setEstadoFirma(EstadoFirmaDigital.NO_APLICA);
              v.setObservacionIa("Aprobado automáticamente con visto bueno institucional.");
              versiones.save(v);
            });
          }
          s.actualizarDocumento(tipo, EstadoValidacionDocumento.VALIDADO,
              "Aprobado automáticamente con visto bueno institucional.");
          huboCambios = true;
        }
      }
      if (huboCambios) {
        actualizarEtapaDocumental(s);
        solicitudes.saveAndFlush(s);
      }
    }
    return respuesta(s, null);
  }

  @Transactional(readOnly = true)
  public SolicitudMatriculaPublicaResponseDto consultarPorToken(String token) {
    if (token == null || token.isBlank()) throw noEncontrada();
    String hashBuscado = hashToken(token.trim());
    var s = solicitudes.findByTokenHash(hashBuscado).orElseThrow(this::noEncontrada);
    if (!MessageDigest.isEqual(
        hashBuscado.getBytes(StandardCharsets.US_ASCII),
        s.getTokenHash().getBytes(StandardCharsets.US_ASCII))) {
      throw noEncontrada();
    }
    return respuesta(s, token.trim());
  }

  @Transactional
  public ReenviarTokenSolicitudResponseDto reenviarToken(ReenviarTokenSolicitudRequestDto r) {
    if (correoSeguimiento == null) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "El reenvío por correo no está disponible temporalmente");
    }
    var lista = solicitudes.buscarActivasPorDniYAnio(r.numeroDocumentoEstudiante().trim(), r.anioLectivoId(), SOLICITUDES_ACTIVAS);
    if (lista.isEmpty()) return new ReenviarTokenSolicitudResponseDto(MENSAJE_REENVIO_SEGURO);
    var s = lista.getFirst();
    String email = s.getEmailApoderado();
    if (email == null || email.isBlank()) {
      return new ReenviarTokenSolicitudResponseDto(MENSAJE_REENVIO_SEGURO);
    }
    byte[] secreto = new byte[32];
    RANDOM.nextBytes(secreto);
    String nuevoToken = Base64.getUrlEncoder().withoutPadding().encodeToString(secreto);
    try {
      correoSeguimiento.enviarEnlaceSeguimiento(email, s.getId(), nuevoToken);
    } catch (RuntimeException ex) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "No fue posible enviar el enlace temporalmente", ex);
    }
    s.setTokenHash(hashToken(nuevoToken));
    solicitudes.saveAndFlush(s);
    return new ReenviarTokenSolicitudResponseDto(MENSAJE_REENVIO_SEGURO);
  }

  public static String enmascararEmail(String email) {
    if (email == null || email.isBlank()) return "***@***.***";
    String trimmed = email.trim();
    int atIndex = trimmed.indexOf('@');
    if (atIndex <= 0) return "***@***.***";
    String local = trimmed.substring(0, atIndex);
    String domain = trimmed.substring(atIndex);
    if (local.length() == 1) {
      return local + "***" + domain;
    } else if (local.length() == 2) {
      return local.charAt(0) + "***" + local.charAt(1) + domain;
    } else {
      return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
    }
  }

  @Transactional(readOnly = true)
  public DocumentoDescargaAdmin documentoDescargaAdmin(UUID id, TipoDocumentoSolicitud tipo) {
    var archivo = documentos.findById(new SolicitudMatriculaDocumentoId(id, tipo))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no disponible"));
    return new DocumentoDescargaAdmin(
        archivo.getBucket(), archivo.getObjectKey(), archivo.getMimeType(), archivo.getTamanoBytes(),
        archivo.getFechaActualizacion(), archivo.getVersionId());
  }

  public record DocumentoDescargaAdmin(
      String bucket, String objectKey, String mimeType, long tamanoBytes, OffsetDateTime fechaActualizacion, UUID versionId) {}

  @Transactional(readOnly = true)
  public String generarComprobanteHtml(UUID id, String token) {
    var s = solicitudConToken(id, token);
    validarDocumentosEmitidos(s);
    boolean efectivo = s.getPagoId() != null && s.getPagoId().startsWith("CAJA");
    String filas = fila("Estudiante", nombreEstudiante(s))
        + fila("DNI del estudiante", s.getNumeroDocumentoEstudiante())
        + fila("Código de matrícula", "FIC-MAT-" + s.getMatriculaId())
        + fila("Medio de pago", efectivo ? "Efectivo" : "Mercado Pago")
        + fila("Referencia de pago", s.getPagoId())
        + fila("Importe pagado", "S/ " + (s.isPresencial() || efectivo
            ? s.getPagoMonto() : obtenerMontoCobroMatriculaPublica()).toPlainString() + " PEN")
        + fila("Monto nominal de matrícula", "S/ " + s.getPagoMonto().toPlainString() + " PEN")
        + fila("Estado", "PAGO APROBADO Y VERIFICADO")
        + fila("Emitido", fechaDocumento(s.getFechaEmisionDocumentos()));
    String aviso = "Comprobante interno de pago. No es una boleta electrónica ni reemplaza un comprobante tributario emitido ante SUNAT.";
    return documentoHtml("Comprobante de pago de matrícula", s.getComprobantePagoCodigo(), filas, aviso);
  }

  @Transactional(readOnly = true)
  public byte[] generarComprobantePdf(UUID id, String token) {
    var s = solicitudConToken(id, token);
    validarDocumentosEmitidos(s);
    if (comprobantePdfAdapter == null) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Generador institucional de PDF no disponible");
    }
    String detalleGrado = resolverDetalleGrado(s);
    var pdfDto = new com.colegio.shuji.tesoreria.application.dto.out.ComprobanteMatriculaPdfDto(
        s.getComprobantePagoCodigo(),
        "CONSTANCIA DE PAGO",
        s.getFechaEmisionDocumentos(),
        s.getPagoId() != null && s.getPagoId().startsWith("CAJA") ? "EFECTIVO - CAJA" : "MERCADO PAGO",
        s.getPagoId(),
        nombreApoderado(s),
        s.getNumeroDocumentoApoderado(),
        s.getEmailApoderado(),
        s.getCelularApoderado(),
        s.getDireccionApoderado(),
        nombreEstudiante(s),
        s.getNumeroDocumentoEstudiante(),
        detalleGrado,
        "FIC-MAT-" + s.getMatriculaId(),
        s.getPagoMonto(),
        "Derecho de Matricula Escolar"
    );
    return comprobantePdfAdapter.generarPdf(pdfDto);
  }

  @Transactional(readOnly = true)
  public String generarFichaMatriculaHtml(UUID id, String token) {
    var s = solicitudConToken(id, token);
    validarDocumentosEmitidos(s);
    var anio = anios.findById(s.getAnioLectivoId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Año lectivo de la matrícula no disponible"));
    var seccion = s.getSeccionId() == null ? null : secciones.findById(s.getSeccionId()).orElse(null);
    var seccionGrado = seccion == null
        ? secciones.buscarPorAnioLectivoId(s.getAnioLectivoId()).stream()
            .filter(opcion -> opcion.getGradoId().equals(s.getGradoId())).findFirst().orElse(null)
        : seccion;
    var grado = seccionGrado == null ? null : seccionGrado.getRelacion1();
    var nivel = grado == null ? null : grado.getRelacion0();
    String seccionLabel = (nivel == null ? "Nivel" : nivel.getNombre()) + " · "
        + (grado == null ? "Grado" : grado.getNombre())
        + (seccion == null ? " · sección y aula pendientes" : " · Sección " + seccion.getLetra());
    String fichaCodigo = "FIC-MAT-" + s.getMatriculaId();
    String filas = fila("Ficha", fichaCodigo)
        + fila("Matrícula", "#" + s.getMatriculaId())
        + fila("Año lectivo", String.valueOf(anio.getAnio()))
        + fila("Nivel, grado y sección", seccionLabel)
        + fila("Estudiante", nombreEstudiante(s))
        + fila("DNI", s.getNumeroDocumentoEstudiante())
        + fila("Fecha de nacimiento", s.getFechaNacimientoEstudiante().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
        + fila("Género", s.getGeneroEstudiante() == Genero.M ? "Masculino" : "Femenino")
        + fila("Apoderado", nombreApoderado(s))
        + fila("DNI del apoderado", s.getNumeroDocumentoApoderado())
        + fila("Parentesco", s.getParentesco().name().replace('_', ' '))
        + fila("Celular", s.getCelularApoderado())
        + fila("Correo", s.getEmailApoderado())
        + fila("Dirección", s.getDireccionApoderado())
        + fila("Estado", s.getEstado() == EstadoSolicitudMatriculaPublica.MATRICULADA
            ? "PAGO CONFIRMADO · AULA ASIGNADA" : "PAGO CONFIRMADO · AULA PENDIENTE")
        + fila("Emitida", fechaDocumento(s.getFechaEmisionDocumentos()));
    return documentoHtml("Ficha de matrícula", fichaCodigo, filas,
        "Documento informativo generado al confirmar el pago y registrar la matrícula.");
  }

  @Transactional
  public PagoMatriculaPublicaResponseDto crearPago(UUID id, String token) {
    var s = solicitudConTokenBloqueada(id, token);
    if (s.getEstado() == EstadoSolicitudMatriculaPublica.PAGO_CONFIRMADO_PENDIENTE_ASIGNACION
        || s.getEstado() == EstadoSolicitudMatriculaPublica.MATRICULADA) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta solicitud ya fue matriculada");
    }
    if (s.getEstado() != EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS
        && s.getEstado() != EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Completa la verificación automática de los documentos antes del pago");
    }
    if (!todosDocumentosValidados(s)) throw new ResponseStatusException(HttpStatus.CONFLICT,
        "Los documentos todavía no completaron la verificación automática");
    exigirEvidenciaDocumental(s);
    var ahora = OffsetDateTime.now(ZoneOffset.UTC);
    if (s.getFechaExpiracionPago() != null && s.getFechaExpiracionPago().isAfter(ahora)
        && s.getPagoEnlace() != null && !s.getPagoEnlace().isBlank()) {
      return pagoResponse(s);
    }
    // La sección se asigna después del pago por Secretaría; el checkout no consume vacantes.
    if (s.isVacanteReservada()) liberarReserva(s);
    s.setFechaExpiracionPago(ahora.plusHours(24));
    BigDecimal montoCheckout = obtenerMontoCobroMatriculaPublica();
    s.setPagoMonto(s.isPresencial() ? montoCheckout : obtenerMontoMatriculaPublica());
    String referencia = REFERENCIA_PAGO_PREFIX + s.getId();
    com.colegio.shuji.tesoreria.application.dto.out.PreferenciaMercadoPagoResponseDto preferencia;
    try {
      preferencia = mercadoPago.crearPreferenciaMatriculaPublica(
          referencia, montoCheckout, "Derecho de matrícula escolar");
    } catch (BusinessException ex) {
      if (!mercadoPagoSandbox) {
        throw ex;
      }
      log.warn("Usando preferencia sandbox simulada para solicitud {} ({})", s.getId(), ex.getMessage());
      String enlaceSim = "https://sandbox.mercadopago.com.pe/checkout/v1/redirect?pref_id=SIM-" + s.getId();
      preferencia = new com.colegio.shuji.tesoreria.application.dto.out.PreferenciaMercadoPagoResponseDto(
          "SIM-PREF-" + s.getId(), enlaceSim, enlaceSim, null);
    }
    s.setPagoPreferenciaId(preferencia.preferenceId());
    if (preferencia.initPoint() == null || preferencia.initPoint().isBlank()) {
      throw new BusinessException("Mercado Pago no devolvió el enlace de pago para la matrícula");
    }
    s.setPagoEnlace(preferencia.initPoint());
    s.setEstado(EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE);
    solicitudes.saveAndFlush(s);
    return pagoResponse(s);
  }

  @Transactional
  public SolicitudMatriculaPublicaResponseDto simularPago(
      UUID id,
      String token,
      com.colegio.shuji.tesoreria.domain.enums.MetodoPago metodo,
      String referenciaOperacion) {
    if (!mercadoPagoSandbox) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN, "La simulación de pagos solo está habilitada en entorno sandbox");
    }
    if (metodo == null || metodo == com.colegio.shuji.tesoreria.domain.enums.MetodoPago.EFECTIVO) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Selecciona un medio de pago en línea válido");
    }
    var s = solicitudConTokenBloqueada(id, token);
    if (s.getEstado() == EstadoSolicitudMatriculaPublica.PAGO_CONFIRMADO_PENDIENTE_ASIGNACION
        || s.getEstado() == EstadoSolicitudMatriculaPublica.MATRICULADA) {
      return respuesta(s, null);
    }
    if (s.getEstado() != EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS
        && s.getEstado() != EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Completa la verificación automática de los documentos antes del pago");
    }
    if (!todosDocumentosValidados(s)) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Los documentos todavía no completaron la verificación automática");
    }
    exigirEvidenciaDocumental(s);
    BigDecimal montoCheckout = s.isPresencial() && s.getPagoMonto() != null
        ? s.getPagoMonto()
        : obtenerMontoCobroMatriculaPublica();
    if (s.getPagoMonto() == null) {
      s.setPagoMonto(s.isPresencial() ? montoCheckout : obtenerMontoMatriculaPublica());
    }
    if (s.getEstado() == EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS) {
      s.setEstado(EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE);
      solicitudes.saveAndFlush(s);
    }
    String sufijo = referenciaOperacion != null && !referenciaOperacion.isBlank()
        ? referenciaOperacion.trim().replaceAll("[^A-Za-z0-9_-]", "")
        : UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    String txId = "SIM-" + metodo.name() + "-" + sufijo;
    var pago = new PagoVerificado(
        txId.length() > 64 ? txId.substring(0, 64) : txId,
        null,
        montoCheckout,
        "PEN",
        metodo,
        true,
        REFERENCIA_PAGO_PREFIX + s.getId());
    return finalizarPagoVerificado(id, token, pago);
  }

  @Transactional
  public SolicitudMatriculaPublicaResponseDto finalizarPagoVerificado(
      UUID id, String token, PagoVerificado pago) {
    var s = token == null ? solicitudes.bloquearPorId(id).orElseThrow(() -> noEncontrada())
        : solicitudConTokenBloqueada(id, token);
    validarReferenciaPago(s, pago);
    if (!pago.aprobado()) return respuesta(s, null);
    if (s.getEstado() == EstadoSolicitudMatriculaPublica.PAGO_CONFIRMADO_PENDIENTE_ASIGNACION
        || s.getEstado() == EstadoSolicitudMatriculaPublica.MATRICULADA) {
      if (!pago.transaccionId().equals(s.getPagoId())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta matrícula ya está asociada a otro pago");
      }
      return respuesta(s, null);
    }
    if (s.getEstado() != EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE
        && s.getEstado() != EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud no está en etapa de pago");
    }
    if (!todosDocumentosValidados(s)) throw new ResponseStatusException(HttpStatus.CONFLICT,
        "Pago recibido; la documentación requiere conciliación administrativa");
    exigirEvidenciaDocumental(s);
    BigDecimal montoEsperado = s.isPresencial() ? s.getPagoMonto() : obtenerMontoCobroMatriculaPublica();
    if (pago.monto() == null || pago.monto().compareTo(montoEsperado) != 0
        || !"PEN".equalsIgnoreCase(pago.moneda())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "El pago debe coincidir con S/ " + montoEsperado.toPlainString() + " PEN");
    }
    if (s.isVacanteReservada()) liberarReserva(s);

    var anio = anios.bloquearPorId(s.getAnioLectivoId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "El año lectivo ya no existe"));
    if (!Boolean.TRUE.equals(anio.getAbierto())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El año lectivo fue cerrado; requiere revisión administrativa");
    }
    boolean gradoVigente = secciones.buscarPorAnioLectivoId(s.getAnioLectivoId()).stream()
        .anyMatch(opcion -> opcion.getGradoId().equals(s.getGradoId()));
    if (!gradoVigente) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El grado ya no corresponde al año lectivo");
    }

    var existentes = estudiantes.buscarPorNumeroDocumento(s.getNumeroDocumentoEstudiante());
    var estudiante = existentes.isEmpty() ? new EstudianteEntity() : existentes.getFirst();
    if (!existentes.isEmpty() && (!s.isPresencial()
        || matriculas.existsByEstudianteIdAndAnioLectivoId(estudiante.getId(), s.getAnioLectivoId()))) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Pago confirmado; el DNI del estudiante requiere conciliación administrativa");
    }
    if (existentes.isEmpty()) {
      estudiante.setTipoDocumento(TipoDocumento.DNI);
      estudiante.setNumeroDocumento(s.getNumeroDocumentoEstudiante());
      estudiante.setNombres(s.getNombresEstudiante());
      estudiante.setApellidoPaterno(s.getApellidoPaternoEstudiante());
      estudiante.setApellidoMaterno(s.getApellidoMaternoEstudiante());
      estudiante.setFechaNacimiento(s.getFechaNacimientoEstudiante());
      estudiante.setGenero(s.getGeneroEstudiante());
      estudiante.setValidadoReniec(false);
      estudiante.setOrigenRegistro(OrigenRegistro.MANUAL_CONTINGENCIA);
      estudiante.setActivo(true);
      estudiante = estudiantes.saveAndFlush(estudiante);
    }

    var apoderado = apoderados.buscarPorNumeroDocumento(s.getNumeroDocumentoApoderado()).stream()
        .findFirst().orElse(null);
    if (apoderado == null) {
      apoderado = new ApoderadoEntity();
      apoderado.setTipoDocumento(TipoDocumento.DNI);
      apoderado.setNumeroDocumento(s.getNumeroDocumentoApoderado());
      apoderado.setNombres(s.getNombresApoderado());
      apoderado.setApellidoPaterno(s.getApellidoPaternoApoderado());
      apoderado.setApellidoMaterno(s.getApellidoMaternoApoderado());
      apoderado.setCelular(s.getCelularApoderado());
      apoderado.setEmail(s.getEmailApoderado());
      apoderado.setDireccion(s.getDireccionApoderado());
      apoderado.setUbigeoInei(
          s.getUbigeoApoderado() != null && !s.getUbigeoApoderado().isBlank()
              ? s.getUbigeoApoderado()
              : "150101");
      apoderado.setValidadoReniec(false);
      apoderado.setOrigenRegistro(OrigenRegistro.MANUAL_CONTINGENCIA);
      apoderado = apoderados.saveAndFlush(apoderado);
    }

    String passwordTemporal = null;
    String usernameApoderado = null;
    if (cuentaApoderadoPort != null && s.getEmailApoderado() != null && !s.getEmailApoderado().isBlank()) {
      try {
        var creds = cuentaApoderadoPort.asegurarCuentaApoderado(
            s.getNumeroDocumentoApoderado(),
            s.getEmailApoderado(),
            s.getNombresApoderado(),
            s.getApellidoPaternoApoderado(),
            s.getApellidoMaternoApoderado(),
            s.getCelularApoderado(),
            s.getDireccionApoderado());
        if (creds != null && creds.usuarioId() != null) {
          apoderado.setUsuarioId(creds.usuarioId());
          apoderado = apoderados.saveAndFlush(apoderado);
          passwordTemporal = creds.passwordTemporal();
          usernameApoderado = creds.username();
        }
      } catch (Exception ex) {
        log.warn("No se pudo aprovisionar usuario institucional para apoderado {}: {}",
            s.getNumeroDocumentoApoderado(), ex.getMessage());
      }
    }

    Long apoderadoId = apoderado.getId();
    boolean vinculado = vinculos.buscarPorEstudianteId(estudiante.getId()).stream()
        .anyMatch(v -> apoderadoId.equals(v.getApoderadoId()));
    if (!vinculado) {
      var vinculo = new EstudianteApoderadoEntity();
      vinculo.setEstudianteId(estudiante.getId());
      vinculo.setApoderadoId(apoderado.getId());
      vinculo.setParentesco(s.getParentesco());
      vinculo.setEsResponsableEconomico(true);
      vinculo.setTieneCustodia(true);
      vinculo.setPermiteRecojo(true);
      vinculos.saveAndFlush(vinculo);
    }

    var seccion = s.isPresencial() && s.getSeccionId() != null
        ? secciones.bloquearPorId(s.getSeccionId())
            .filter(aula -> aula.getAnioLectivoId().equals(s.getAnioLectivoId())
                && aula.getGradoId().equals(s.getGradoId())
                && aula.getVacantesOcupadas() < aula.getCupoMaximo()).orElse(null)
        : null;
    var matricula = new MatriculaEntity();
    matricula.setAnioLectivoId(s.getAnioLectivoId());
    matricula.setEstudianteId(estudiante.getId());
    matricula.setSeccionId(seccion == null ? null : seccion.getId());
    matricula.setModalidadIngreso(s.getModalidadIngreso() != null ? s.getModalidadIngreso() : ModalidadIngreso.REGULAR);
    matricula.setColegioProcedencia(s.getColegioProcedencia());
    matricula.setEstadoMatricula(seccion == null
        ? com.colegio.shuji.matricula.domain.enums.EstadoMatricula.PENDIENTE_ASIGNACION
        : com.colegio.shuji.matricula.domain.enums.EstadoMatricula.MATRICULADO);
    matricula.setObservaciones(seccion == null ? "Pago confirmado; pendiente asignar aula por Secretaría"
        : "Matrícula presencial con pago confirmado por Mercado Pago");
    matricula = matriculas.saveAndFlush(matricula);

    s.setPagoId(pago.transaccionId());
    s.setEstudianteId(estudiante.getId());
    s.setApoderadoId(apoderado.getId());
    s.setMatriculaId(matricula.getId());
    s.setComprobantePagoCodigo(cobrosMatricula.registrar(matricula.getId(), s.getPagoMonto(), pago));
    s.setFechaEmisionDocumentos(OffsetDateTime.now(ZoneOffset.UTC));
    s.setSeccionId(matricula.getSeccionId());
    s.setEstado(seccion == null ? EstadoSolicitudMatriculaPublica.PAGO_CONFIRMADO_PENDIENTE_ASIGNACION
        : EstadoSolicitudMatriculaPublica.MATRICULADA);
    s.setFechaExpiracionPago(null);
    String tokenSeguimiento = token;
    if (tokenSeguimiento == null || tokenSeguimiento.isBlank()) {
      byte[] secreto = new byte[32];
      RANDOM.nextBytes(secreto);
      tokenSeguimiento = Base64.getUrlEncoder().withoutPadding().encodeToString(secreto);
      s.setTokenHash(hashToken(tokenSeguimiento));
    }

    var guardada = solicitudes.saveAndFlush(s);

    if (correoSeguimiento != null && guardada.getEmailApoderado() != null
        && !guardada.getEmailApoderado().isBlank()) {
      try {
        String detalleGrado = resolverDetalleGrado(guardada);
        detalleGrado += seccion == null ? " · Sección pendiente" : " · Sección " + seccion.getLetra();
        byte[] pdfBytes = null;
        String nombrePdf = "Constancia_Matricula_" + guardada.getComprobantePagoCodigo() + ".pdf";
        if (comprobantePdfAdapter != null) {
          try {
            var pdfDto = new com.colegio.shuji.tesoreria.application.dto.out.ComprobanteMatriculaPdfDto(
                guardada.getComprobantePagoCodigo(),
                "BOLETA DE VENTA ELECTRONICA",
                guardada.getFechaEmisionDocumentos(),
                "MERCADO PAGO",
                guardada.getPagoId(),
                nombreApoderado(guardada),
                guardada.getNumeroDocumentoApoderado(),
                guardada.getEmailApoderado(),
                guardada.getCelularApoderado(),
                guardada.getDireccionApoderado(),
                nombreEstudiante(guardada),
                guardada.getNumeroDocumentoEstudiante(),
                detalleGrado,
                "FIC-MAT-" + guardada.getMatriculaId(),
                guardada.getPagoMonto(),
                "Derecho de Matricula Escolar"
            );
            pdfBytes = comprobantePdfAdapter.generarPdf(pdfDto, usernameApoderado, passwordTemporal);
          } catch (Exception exPdf) {
            log.warn("No fue posible compilar PDF del voucher para la solicitud {}: {}", guardada.getId(), exPdf.getMessage());
          }
        }
        if (pdfBytes == null) throw new IllegalStateException("No se generó la constancia de pago");
        correoSeguimiento.enviarCredencialesMatricula(
            guardada.getEmailApoderado().trim(),
            nombreApoderado(guardada),
            nombreEstudiante(guardada),
            passwordTemporal,
            detalleGrado,
            guardada.getComprobantePagoCodigo(),
            guardada.getId(),
            tokenSeguimiento,
            pdfBytes,
            nombrePdf);
      } catch (Exception ex) {
        log.warn("No fue posible enviar correo de confirmación de matrícula para solicitud {}: {}",
            guardada.getId(), ex.getMessage());
      }
    }

    return respuesta(guardada, null);
  }

  @Transactional(readOnly = true)
  public List<SolicitudMatriculaPublicaAdminResponseDto> listarAdmin() {
    var lista = solicitudes.findTop200ByOrderByFechaCreacionDesc();
    var archivos = cargarVersiones(lista.stream().map(SolicitudMatriculaPublicaEntity::getId).toList());
    return lista.stream().map(s ->
        new SolicitudMatriculaPublicaAdminResponseDto(
            s.getId(), s.getEstado(), nombreEstudiante(s), s.getNumeroDocumentoEstudiante(),
            nombreApoderado(s), s.getCelularApoderado(), s.getEmailApoderado(),
            s.getAnioLectivoId(), s.getGradoId(), s.getSeccionId(), documentos(s, archivos), s.getFechaCreacion(),
            s.getEstudianteId(), s.getMatriculaId(), s.isRequiereAutorizacionEdad(),
            s.getMotivoAutorizacionEdad(), s.getAutorizadoPorUsuarioId(), s.getFechaAutorizacionEdad(),
            s.getModalidadIngreso(), s.getColegioProcedencia())).toList();
  }

  @Transactional
  public SolicitudMatriculaPublicaAdminResponseDto asignarSeccion(UUID id, Integer seccionId) {
    var solicitud = solicitudes.bloquearPorId(id).orElseThrow(() -> noEncontrada());
    if (solicitud.getEstado() != EstadoSolicitudMatriculaPublica.PAGO_CONFIRMADO_PENDIENTE_ASIGNACION
        || solicitud.getMatriculaId() == null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Solo se puede asignar aula a una matrícula pagada pendiente de asignación");
    }
    var matricula = matriculas.bloquearPorId(solicitud.getMatriculaId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No se encontró la matrícula pagada"));
    if (matricula.getEstudianteId() == null
        || solicitud.getEstudianteId() == null
        || !java.util.Objects.equals(matricula.getEstudianteId(), solicitud.getEstudianteId())
        || !java.util.Objects.equals(matricula.getAnioLectivoId(), solicitud.getAnioLectivoId())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "La matrícula vinculada no corresponde al estudiante y año de la solicitud");
    }
    if (matricula.getEstadoMatricula() != com.colegio.shuji.matricula.domain.enums.EstadoMatricula.PENDIENTE_ASIGNACION
        || matricula.getSeccionId() != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La matrícula ya tiene una asignación o cambió de estado");
    }
    var seccion = secciones.bloquearPorId(seccionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sección no existe"));
    if (!seccion.getAnioLectivoId().equals(solicitud.getAnioLectivoId())
        || !seccion.getGradoId().equals(solicitud.getGradoId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "La sección debe pertenecer al grado solicitado y al año lectivo correspondiente");
    }
    if (seccion.getVacantesOcupadas() >= seccion.getCupoMaximo()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La sección ya alcanzó su capacidad máxima");
    }
    matricula.setSeccionId(seccionId);
    matricula.setEstadoMatricula(com.colegio.shuji.matricula.domain.enums.EstadoMatricula.MATRICULADO);
    matricula.setObservaciones("Aula asignada por Secretaría después de confirmar el pago de matrícula");
    matriculas.saveAndFlush(matricula);
    solicitud.setSeccionId(seccionId);
    solicitud.setEstado(EstadoSolicitudMatriculaPublica.MATRICULADA);
    rotarTokenYNotificar(solicitud, "¡Felicitaciones! Se ha asignado aula a tu matrícula para el presente año lectivo.");
    return adminResponse(solicitudes.saveAndFlush(solicitud));
  }

  @Transactional
  public SolicitudMatriculaPublicaAdminResponseDto autorizarExcepcionEdad(
      UUID id, String motivo, Long usuarioId) {
    var s = solicitudes.bloquearPorId(id).orElseThrow(() -> noEncontrada());
    if (s.getEstado() != EstadoSolicitudMatriculaPublica.AUTORIZACION_EDAD_PENDIENTE
        || !s.isRequiereAutorizacionEdad()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "La solicitud no está pendiente de autorización por edad");
    }
    if (!todosDocumentosValidados(s)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Autoriza la excepción solo después de validar los tres documentos automáticamente");
    }
    if (usuarioId == null || motivo == null || motivo.isBlank() || motivo.trim().length() < 10
        || motivo.trim().length() > 500) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Registra una justificación institucional de 10 a 500 caracteres");
    }
    s.setMotivoAutorizacionEdad(motivo.trim());
    s.setAutorizadoPorUsuarioId(usuarioId);
    s.setFechaAutorizacionEdad(OffsetDateTime.now(ZoneOffset.UTC));
    s.setEstado(EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS);
    rotarTokenYNotificar(s, "Tu solicitud de excepci\u00F3n de edad ha sido autorizada institucionalmente. Puedes continuar con el tr\u00E1mite.");
    return adminResponse(solicitudes.saveAndFlush(s));
  }

  @Transactional
  public SolicitudMatriculaPublicaAdminResponseDto observarAdmin(
      UUID id, ObservarSolicitudMatriculaPublicaRequestDto r) {
    var s = solicitudes.bloquearPorId(id).orElseThrow(() -> noEncontrada());
    if (!s.tiposRequeridos().contains(r.tipo())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de documento no admitido para esta modalidad de solicitud");
    if (s.getEstado() == EstadoSolicitudMatriculaPublica.PAGO_CONFIRMADO_PENDIENTE_ASIGNACION
        || s.getEstado() == EstadoSolicitudMatriculaPublica.MATRICULADA
        || s.getEstado() == EstadoSolicitudMatriculaPublica.RECHAZADA) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud ya terminó");
    }
    liberarReserva(s);
    s.actualizarDocumento(r.tipo(), EstadoValidacionDocumento.OBSERVADO, r.observacion().trim());
    s.setEstado(EstadoSolicitudMatriculaPublica.DOCUMENTOS_OBSERVADOS);
    rotarTokenYNotificar(s, "Se ha observado la documentaci\u00F3n presentada: " + r.observacion().trim() + ". Por favor ingresa al enlace para corregir los documentos.");
    return adminResponse(solicitudes.saveAndFlush(s));
  }

  @Transactional
  public void rechazarAdmin(UUID id) {
    var s = solicitudes.bloquearPorId(id).orElseThrow(() -> noEncontrada());
    if (s.getEstado() == EstadoSolicitudMatriculaPublica.PAGO_CONFIRMADO_PENDIENTE_ASIGNACION
        || s.getEstado() == EstadoSolicitudMatriculaPublica.MATRICULADA) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede rechazar una matrícula ya confirmada");
    }
    liberarReserva(s);
    s.setEstado(EstadoSolicitudMatriculaPublica.RECHAZADA);
    rotarTokenYNotificar(s, "Tu solicitud de matr\u00EDcula ha sido desestimada institucionalmente.");
    solicitudes.saveAndFlush(s);
  }


  @Transactional
  public SolicitudMatriculaPublicaAdminResponseDto darVistoBuenoAdmin(UUID id, String motivo, Long usuarioId) {
    var s = solicitudes.bloquearPorId(id).orElseThrow(() -> noEncontrada());
    if (s.getEstado() == EstadoSolicitudMatriculaPublica.MATRICULADA
        || s.getEstado() == EstadoSolicitudMatriculaPublica.RECHAZADA
        || s.getEstado() == EstadoSolicitudMatriculaPublica.PAGO_CONFIRMADO_PENDIENTE_ASIGNACION) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud ya se encuentra tramitada o cerrada");
    }
    for (TipoDocumentoSolicitud tipo : s.tiposRequeridos()) {
      s.actualizarDocumento(tipo, EstadoValidacionDocumento.VALIDADO,
          "Validado con visto bueno institucional.");
      var docOpt = documentos.findById(new SolicitudMatriculaDocumentoId(s.getId(), tipo));
      if (docOpt.isPresent() && docOpt.get().getVersionId() != null) {
        versiones.findById(docOpt.get().getVersionId()).ifPresent(v -> {
          v.setIaValida(true);
          v.setDniApiCoincide(true);
          if (tipo == TipoDocumentoSolicitud.PARTIDA_NACIMIENTO) {
            v.setVinculoParentalCoincide(true);
          }
          if (v.getEstadoFirma() == null || v.getEstadoFirma() == EstadoFirmaDigital.INVALIDA) {
            v.setEstadoFirma(EstadoFirmaDigital.NO_APLICA);
          }
          v.setObservacionIa("Aprobado con visto bueno institucional.");
          versiones.save(v);
        });
      }
    }
    if (s.isRequiereAutorizacionEdad() && s.getFechaAutorizacionEdad() == null) {
      s.setMotivoAutorizacionEdad(motivo != null && !motivo.isBlank() ? motivo.trim() : "Excepci\u00F3n de edad aprobada con visto bueno institucional");
      s.setAutorizadoPorUsuarioId(usuarioId);
      s.setFechaAutorizacionEdad(OffsetDateTime.now(ZoneOffset.UTC));
    }
    s.setEstado(EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS);
    rotarTokenYNotificar(s, "\u00A1Tu solicitud ha recibido el visto bueno institucional! Tus documentos han sido aceptados y puedes proceder con el siguiente paso.");
    return adminResponse(solicitudes.saveAndFlush(s));
  }

  private String rotarTokenYNotificar(SolicitudMatriculaPublicaEntity s, String mensajeDetalle) {
    String email = s.getEmailApoderado();
    if (email == null || email.isBlank()) {
      log.warn("No se rotó el token de la solicitud {} porque no hay correo de seguimiento disponible", s.getId());
      return null;
    }
    if (correoSeguimiento == null) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "El servicio de correo no está disponible; intenta nuevamente");
    }
    byte[] secreto = new byte[32];
    RANDOM.nextBytes(secreto);
    String nuevoToken = Base64.getUrlEncoder().withoutPadding().encodeToString(secreto);
    try {
      correoSeguimiento.enviarActualizacionEstado(email.trim(), s.getId(), nuevoToken, s.getEstado().name(), mensajeDetalle);
      s.setTokenHash(hashToken(nuevoToken));
      return nuevoToken;
    } catch (RuntimeException ex) {
      log.warn("No fue posible enviar notificaci\u00F3n de correo para la solicitud {} ({})",
          s.getId(), ex.getClass().getSimpleName());
      return null;
    }
  }

  @Scheduled(fixedDelayString = "${app.matricula-publica.reserva-limpieza-ms:60000}")
  @Transactional
  public void liberarReservasVencidas() {
    var ahora = OffsetDateTime.now(ZoneOffset.UTC);
    for (var snapshot : solicitudes.buscarReservasExpiradas(ahora)) {
      var actual = solicitudes.bloquearPorId(snapshot.getId()).orElse(null);
      if (actual != null && actual.isVacanteReservada() && actual.getFechaExpiracionPago() != null
          && !actual.getFechaExpiracionPago().isAfter(ahora)) {
        liberarReserva(actual);
        actual.setPagoEnlace(null);
        actual.setPagoPreferenciaId(null);
        actual.setFechaExpiracionPago(null);
        if (actual.getEstado() == EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE) {
          actual.setEstado(EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS);
        }
        solicitudes.save(actual);
      }
    }
  }

  @Transactional(readOnly = true)
  public void validarToken(UUID id, String token) { solicitudConToken(id, token); }

  @Transactional
  public void procesarPagoWebhook(PagoVerificado pago) {
    if (pago.referenciaExterna() == null || !pago.referenciaExterna().startsWith(REFERENCIA_PAGO_PREFIX)) return;
    if (!pago.aprobado()) return;
    UUID id;
    try {
      id = UUID.fromString(pago.referenciaExterna().substring(REFERENCIA_PAGO_PREFIX.length()));
    } catch (IllegalArgumentException ex) {
      throw new BusinessException("Referencia de solicitud inválida en el pago");
    }
    finalizarPagoVerificado(id, null, pago);
  }

  private void validarReferenciaPago(SolicitudMatriculaPublicaEntity s, PagoVerificado pago) {
    if (pago == null || pago.referenciaExterna() == null
        || !pago.referenciaExterna().equals(REFERENCIA_PAGO_PREFIX + s.getId())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El pago no pertenece a esta solicitud");
    }
  }

  private SolicitudMatriculaPublicaEntity solicitudConToken(UUID id, String token) {
    var s = solicitudes.findById(id).orElseThrow(() -> noEncontrada());
    validarToken(s, token);
    return s;
  }

  private SolicitudMatriculaPublicaEntity solicitudConTokenBloqueada(UUID id, String token) {
    var s = solicitudes.bloquearPorId(id).orElseThrow(() -> noEncontrada());
    validarToken(s, token);
    return s;
  }

  private void validarToken(SolicitudMatriculaPublicaEntity s, String token) {
    if (token == null || token.isBlank()
        || !MessageDigest.isEqual(hashToken(token).getBytes(StandardCharsets.US_ASCII),
            s.getTokenHash().getBytes(StandardCharsets.US_ASCII))) {
      throw noEncontrada();
    }
  }

  private String hashToken(String token) {
    try {
      var digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
      return java.util.HexFormat.of().formatHex(digest);
    } catch (java.security.NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 no está disponible", ex);
    }
  }

  private boolean todosDocumentosValidados(SolicitudMatriculaPublicaEntity s) {
    return s.tiposRequeridos().stream()
        .allMatch(tipo -> s.estadoDocumento(tipo) == EstadoValidacionDocumento.VALIDADO);
  }

  private void exigirProgenitor(Parentesco parentesco) {
    if (parentesco != Parentesco.PADRE && parentesco != Parentesco.MADRE) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "La solicitud pública solo admite al padre o a la madre");
    }
  }

  private void exigirEvidenciaDocumental(SolicitudMatriculaPublicaEntity s) {
    if (!s.isPresencial()) exigirProgenitor(s.getParentesco());
    var actuales = cargarVersiones(List.of(s.getId()));
    for (var tipo : s.tiposRequeridos()) {
      var version = actuales.get(new SolicitudMatriculaDocumentoId(s.getId(), tipo));
      if (!evidenciaCompleta(tipo, version)) throw new ResponseStatusException(HttpStatus.CONFLICT,
          s.getModalidadIngreso() == ModalidadIngreso.TRASLADO
              ? "Revalida los cuatro PDF (incluido el certificado de estudios o libreta) y el vínculo del padre o madre antes de continuar"
              : "Revalida los tres PDF y el vínculo del padre o madre antes de continuar");
    }
  }

  private boolean requiereNuevaEvidencia(UUID id, TipoDocumentoSolicitud tipo) {
    var actual = documentos.findById(new SolicitudMatriculaDocumentoId(id, tipo));
    var version = actual.flatMap(a -> versiones.findById(a.getVersionId())).orElse(null);
    return !evidenciaCompleta(tipo, version);
  }

  private static boolean esMimeAdmitido(String mimeType) {
    return "application/pdf".equals(mimeType)
        || "image/jpeg".equals(mimeType)
        || "image/png".equals(mimeType)
        || "image/webp".equals(mimeType);
  }

  private boolean evidenciaCompleta(TipoDocumentoSolicitud tipo, DocumentoMatriculaVersionEntity version) {
    if (version == null || !esMimeAdmitido(version.getMimeType())) return false;
    String observacion = version.getObservacionIa();
    boolean evidenciaPresentacion = "No se verifica firma digital en el modo de presentación."
        .equals(version.getDetalleFirma());
    if (evidenciaPresentacion && observacion != null
        && (observacion.startsWith("Modo presentación: identidad coincide")
            || observacion.startsWith("Modo presentación: archivo recibido"))) return true;
    if (!Boolean.TRUE.equals(version.getIaValida())) return false;
    if (tipo == TipoDocumentoSolicitud.RECIBO_SERVICIO) return true;
    if (!Boolean.TRUE.equals(version.getDniApiCoincide())) return false;
    return (version.getEstadoFirma() == EstadoFirmaDigital.VALIDA || version.getEstadoFirma() == EstadoFirmaDigital.NO_APLICA)
        && (tipo != TipoDocumentoSolicitud.PARTIDA_NACIMIENTO || Boolean.TRUE.equals(version.getVinculoParentalCoincide()));


  }

  private boolean hayDocumentoObservado(SolicitudMatriculaPublicaEntity s) {
    return s.tiposRequeridos().stream()
        .anyMatch(tipo -> s.estadoDocumento(tipo) == EstadoValidacionDocumento.OBSERVADO);
  }

  private List<DocumentoSolicitudResponseDto> documentos(SolicitudMatriculaPublicaEntity s) {
    return documentos(s, cargarVersiones(List.of(s.getId())));
  }

  private java.util.Map<SolicitudMatriculaDocumentoId, DocumentoMatriculaVersionEntity> cargarVersiones(List<UUID> ids) {
    if (ids.isEmpty()) return java.util.Map.of();
    var actuales = documentos.findByIdSolicitudIdIn(ids);
    var versionesIds = actuales.stream().map(SolicitudMatriculaDocumentoEntity::getVersionId).toList();
    if (versionesIds.isEmpty()) return java.util.Map.of();
    var porId = new java.util.HashMap<UUID, DocumentoMatriculaVersionEntity>();
    versiones.findAllById(versionesIds).forEach(v -> porId.put(v.getId(), v));
    var mapa = new java.util.HashMap<SolicitudMatriculaDocumentoId, DocumentoMatriculaVersionEntity>();
    actuales.forEach(a -> { var v = porId.get(a.getVersionId()); if (v != null) mapa.put(a.getId(), v); });
    return mapa;
  }

  private List<DocumentoSolicitudResponseDto> documentos(SolicitudMatriculaPublicaEntity s,
      java.util.Map<SolicitudMatriculaDocumentoId, DocumentoMatriculaVersionEntity> archivos) {
    return s.tiposRequeridos().stream().map(tipo -> {
      var v = archivos.get(new SolicitudMatriculaDocumentoId(s.getId(), tipo));
      boolean revalidar = s.getEstado() != EstadoSolicitudMatriculaPublica.PAGO_CONFIRMADO_PENDIENTE_ASIGNACION
          && s.getEstado() != EstadoSolicitudMatriculaPublica.MATRICULADA
          && s.getEstado() != EstadoSolicitudMatriculaPublica.RECHAZADA
          && s.estadoDocumento(tipo) == EstadoValidacionDocumento.VALIDADO && !evidenciaCompleta(tipo, v);
      return new DocumentoSolicitudResponseDto(tipo, revalidar ? EstadoValidacionDocumento.EN_VALIDACION : s.estadoDocumento(tipo),
          revalidar ? "Actualiza la evidencia: se requieren los PDF solicitados y el vínculo del padre o madre con la partida." : s.observacionDocumento(tipo),
          v == null ? null : v.getId(), v == null ? null : v.getIaValida(),
          v == null ? null : v.getDniApiCoincide(),
          v == null ? null : v.getEstadoFirma(), v == null ? null : v.getDetalleFirma(),
          v == null ? null : v.getVinculoParentalCoincide());
    }).toList();
  }

  private void actualizarEtapaDocumental(SolicitudMatriculaPublicaEntity s) {
    if (s.getEstado() == EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE) return;
    if (s.isRequiereAutorizacionEdad() && s.getFechaAutorizacionEdad() == null
        && todosDocumentosValidados(s)) {
      s.setEstado(EstadoSolicitudMatriculaPublica.AUTORIZACION_EDAD_PENDIENTE);
      return;
    }
    boolean cargados = s.tiposRequeridos().stream()
        .allMatch(tipo -> s.estadoDocumento(tipo) != EstadoValidacionDocumento.PENDIENTE);
    s.setEstado(todosDocumentosValidados(s) ? EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS
        : hayDocumentoObservado(s) ? EstadoSolicitudMatriculaPublica.DOCUMENTOS_OBSERVADOS
        : cargados ? EstadoSolicitudMatriculaPublica.DOCUMENTOS_EN_VALIDACION
        : EstadoSolicitudMatriculaPublica.DOCUMENTOS_PENDIENTES);
  }

  @Transactional(readOnly = true)
  public List<VersionDocumentoMatriculaResponseDto> historialDocumento(UUID id, TipoDocumentoSolicitud tipo) {
    if (!solicitudes.existsById(id)) throw noEncontrada();
    var lista = versiones.findBySolicitudIdAndTipoOrderByFechaCargaDesc(id, tipo);
    return lista.stream().map(v -> new VersionDocumentoMatriculaResponseDto(v.getId(), v.getSha256(), v.getMimeType(),
        v.getTamanoBytes(), v.getFechaCarga(), v.getIaValida(), v.getDniApiCoincide(), v.getObservacionIa(), v.getEstadoFirma(), v.getDetalleFirma(),
        v.getCertificadoSha256(), v.getVinculoParentalCoincide())).toList();
  }
  private SolicitudMatriculaPublicaResponseDto respuesta(SolicitudMatriculaPublicaEntity s, String token) {
    return new SolicitudMatriculaPublicaResponseDto(
        s.getId(), token, s.getEstado(), nombreEstudiante(s), documentos(s), s.getPagoEnlace(),
        s.getPagoMonto(), s.getEstudianteId(), s.getMatriculaId(), s.getModalidadIngreso(), s.getColegioProcedencia());
  }

  private PagoMatriculaPublicaResponseDto pagoResponse(SolicitudMatriculaPublicaEntity s) {
    return new PagoMatriculaPublicaResponseDto(s.getPagoPreferenciaId(), s.getPagoEnlace(), s.getPagoMonto(), "PEN");
  }

  private SolicitudMatriculaPublicaAdminResponseDto adminResponse(SolicitudMatriculaPublicaEntity s) {
    return new SolicitudMatriculaPublicaAdminResponseDto(
        s.getId(), s.getEstado(), nombreEstudiante(s), s.getNumeroDocumentoEstudiante(),
        nombreApoderado(s), s.getCelularApoderado(), s.getEmailApoderado(), s.getAnioLectivoId(),
        s.getGradoId(), s.getSeccionId(), documentos(s), s.getFechaCreacion(), s.getEstudianteId(), s.getMatriculaId(),
        s.isRequiereAutorizacionEdad(), s.getMotivoAutorizacionEdad(), s.getAutorizadoPorUsuarioId(),
        s.getFechaAutorizacionEdad(), s.getModalidadIngreso(), s.getColegioProcedencia());
  }

  private String nombreEstudiante(SolicitudMatriculaPublicaEntity s) {
    return s.getNombresEstudiante() + " " + s.getApellidoPaternoEstudiante() + " " + s.getApellidoMaternoEstudiante();
  }

  private String nombreApoderado(SolicitudMatriculaPublicaEntity s) {
    return s.getNombresApoderado() + " " + s.getApellidoPaternoApoderado() + " " + s.getApellidoMaternoApoderado();
  }

  private void validarDocumentosEmitidos(SolicitudMatriculaPublicaEntity solicitud) {
    if ((solicitud.getEstado() != EstadoSolicitudMatriculaPublica.PAGO_CONFIRMADO_PENDIENTE_ASIGNACION
            && solicitud.getEstado() != EstadoSolicitudMatriculaPublica.MATRICULADA)
        || solicitud.getMatriculaId() == null
        || solicitud.getPagoId() == null
        || solicitud.getComprobantePagoCodigo() == null
        || solicitud.getFechaEmisionDocumentos() == null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT,
          "Los documentos estarán disponibles al confirmar el pago y la matrícula");
    }
  }

  private String documentoHtml(String titulo, String codigo, String filas, String aviso) {
    String template = """
        <!doctype html>
        <html lang="es">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1">
          <title>__TITULO__</title>
          <style>
            @page { size: A4; margin: 18mm; }
            * { box-sizing: border-box; }
            body { margin: 0; background: #f1f5f9; color: #172b4d; font: 15px Arial, sans-serif; }
            main { max-width: 800px; margin: 36px auto; padding: 42px; background: #fff; border: 1px solid #dbe3ee; border-radius: 16px; }
            header { display: flex; justify-content: space-between; gap: 20px; padding-bottom: 20px; border-bottom: 2px solid #d9b96f; color: #52647e; font-size: 12px; }
            header strong { color: #172b4d; font-size: 16px; }
            h1 { margin: 30px 0 8px; font-size: 27px; }
            .code { margin-bottom: 22px; color: #245dcc; font-weight: 700; }
            table { width: 100%; border-collapse: collapse; }
            th, td { padding: 12px 10px; border-bottom: 1px solid #e6ebf2; text-align: left; vertical-align: top; }
            th { width: 38%; color: #687991; font-size: 12px; font-weight: 600; }
            td { color: #1b304c; overflow-wrap: anywhere; }
            .notice { margin-top: 24px; padding: 14px; border: 1px solid #e5eaf1; border-radius: 10px; color: #64748b; font-size: 12px; line-height: 1.6; }
            .print { margin-top: 24px; padding: 11px 16px; border: 0; border-radius: 9px; background: #245dcc; color: white; font-weight: 700; cursor: pointer; }
            @media print { body { background: white; } main { max-width: none; margin: 0; padding: 0; border: 0; border-radius: 0; } .print { display: none; } }
            @media (max-width: 560px) { main { margin: 12px; padding: 22px 16px; } h1 { font-size: 22px; } th, td { padding: 10px 6px; } }
          </style>
        </head>
        <body><main>
          <header><strong>Shuji Kitamura · Gestión Escolar</strong><span>Documento generado desde el portal</span></header>
          <h1>__TITULO__</h1>
          <div class="code">__CODIGO__</div>
          <table><tbody>__FILAS__</tbody></table>
          <p class="notice">__AVISO__</p>
          <button class="print" onclick="window.print()">Imprimir / Guardar como PDF</button>
        </main></body></html>
        """;
    return template.replace("__TITULO__", escaparHtml(titulo))
        .replace("__CODIGO__", escaparHtml(codigo))
        .replace("__FILAS__", filas)
        .replace("__AVISO__", escaparHtml(aviso));
  }

  private String fila(String etiqueta, String valor) {
    return "<tr><th>" + escaparHtml(etiqueta) + "</th><td>" + escaparHtml(valor) + "</td></tr>";
  }

  private String escaparHtml(String valor) {
    if (valor == null || valor.isBlank()) return "—";
    return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        .replace("\"", "&quot;").replace("'", "&#39;");
  }

  private String fechaDocumento(OffsetDateTime fecha) {
    return fecha.atZoneSameInstant(ZoneId.of("America/Lima"))
        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + " (hora de Perú)";
  }

  private void liberarReserva(SolicitudMatriculaPublicaEntity s) {
    if (s.isVacanteReservada()) {
      secciones.liberarVacante(s.getSeccionId());
      s.setVacanteReservada(false);
      s.setFechaExpiracionPago(null);
    }
  }

  private ResponseStatusException noEncontrada() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud inexistente o token inválido");
  }

  BigDecimal obtenerMontoMatriculaPublica() {
    if (montoMatriculaPublica == null || montoMatriculaPublica.signum() <= 0) {
      throw new IllegalStateException("El monto de matrícula debe ser mayor que cero");
    }
    try {
      return montoMatriculaPublica.setScale(2, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException e) {
      throw new IllegalStateException("El monto de matrícula debe tener como máximo dos decimales", e);
    }
  }

  private BigDecimal obtenerMontoCobroMatriculaPublica() {
    if (montoCobroMatriculaPublica == null || montoCobroMatriculaPublica.signum() <= 0) {
      throw new IllegalStateException("El monto de cobro debe ser mayor que cero");
    }
    try {
      return montoCobroMatriculaPublica.setScale(2, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException e) {
      throw new IllegalStateException("El monto de cobro debe tener como máximo dos decimales", e);
    }
  }


  private String resolverDetalleGrado(SolicitudMatriculaPublicaEntity s) {
    try {
      var seccionGrado = secciones.buscarPorAnioLectivoId(s.getAnioLectivoId()).stream()
          .filter(opcion -> opcion.getGradoId().equals(s.getGradoId())).findFirst().orElse(null);
      if (seccionGrado != null && seccionGrado.getRelacion1() != null) {
        var grado = seccionGrado.getRelacion1();
        var nivel = grado.getRelacion0();
        String nivelNombre = nivel != null ? nivel.getNombre() : "";
        return (nivelNombre.isBlank() ? "" : nivelNombre + " · ") + grado.getNombre();
      }
    } catch (Exception ignored) {
    }
    return "Grado " + s.getGradoId();
  }
}
