package com.colegio.shuji.matricula.infrastructure.service;

import com.colegio.shuji.academico.application.dto.out.AnioLectivoResponseDto;
import com.colegio.shuji.academico.infrastructure.repository.JpaAnioLectivoRepository;
import com.colegio.shuji.academico.infrastructure.repository.JpaSeccionRepository;
import com.colegio.shuji.matricula.application.dto.in.CrearSolicitudMatriculaPublicaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.ObservarSolicitudMatriculaPublicaRequestDto;
import com.colegio.shuji.matricula.application.dto.out.CatalogoMatriculaPublicaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.DocumentoSolicitudResponseDto;
import com.colegio.shuji.matricula.application.dto.out.PagoMatriculaPublicaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.SeccionMatriculaPublicaOpcionDto;
import com.colegio.shuji.matricula.application.dto.out.SolicitudMatriculaPublicaAdminResponseDto;
import com.colegio.shuji.matricula.application.dto.out.SolicitudMatriculaPublicaResponseDto;
import com.colegio.shuji.matricula.application.port.out.GeminiDocumentoMatriculaPort;
import com.colegio.shuji.matricula.domain.enums.EstadoSolicitudMatriculaPublica;
import com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento;
import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.OrigenRegistro;
import com.colegio.shuji.matricula.domain.enums.Parentesco;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
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
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class SolicitudMatriculaPublicaService {
  public static final String REFERENCIA_PAGO_PREFIX = "MATRICULA_PUBLICA:";

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final List<EstadoSolicitudMatriculaPublica> SOLICITUDES_ACTIVAS = List.of(
      EstadoSolicitudMatriculaPublica.DOCUMENTOS_PENDIENTES,
      EstadoSolicitudMatriculaPublica.DOCUMENTOS_OBSERVADOS,
      EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS,
      EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE);

  private final JpaSolicitudMatriculaPublicaRepository solicitudes;
  private final JpaSolicitudMatriculaDocumentoRepository documentos;
  private final JpaAnioLectivoRepository anios;
  private final JpaSeccionRepository secciones;
  private final JpaEstudianteRepository estudiantes;
  private final JpaApoderadoRepository apoderados;
  private final JpaEstudianteApoderadoRepository vinculos;
  private final JpaMatriculaRepository matriculas;
  private final GeminiDocumentoMatriculaPort gemini;
  private final MercadoPagoPort mercadoPago;

  @Value("${app.matricula-publica.monto-matricula:350.00}")
  private BigDecimal montoMatriculaPublica = new BigDecimal("350.00");

  @Value("${app.matricula-publica.duracion-reserva-minutos:30}")
  private long duracionReservaMinutos;

  @Transactional(readOnly = true)
  public CatalogoMatriculaPublicaResponseDto catalogo() {
    var aniosAbiertos = anios.findAll().stream()
        .filter(a -> Boolean.TRUE.equals(a.getAbierto()))
        .map(a -> new AnioLectivoResponseDto(a.getId(), a.getAnio(), a.getFechaInicio(), a.getFechaFin(), a.getAbierto()))
        .toList();
    var opciones = aniosAbiertos.stream()
        .flatMap(a -> secciones.buscarPorAnioLectivoId(a.id()).stream())
        .filter(s -> s.getCupoMaximo() > s.getVacantesOcupadas())
        .map(s -> {
          var grado = s.getRelacion1();
          var nivel = grado == null ? null : grado.getRelacion0();
          return new SeccionMatriculaPublicaOpcionDto(
              s.getId(), s.getAnioLectivoId(),
              nivel == null ? "Nivel" : nivel.getNombre(),
              grado == null ? "Grado" : grado.getNombre(),
              s.getLetra(), s.getCupoMaximo() - s.getVacantesOcupadas());
        })
        .toList();
    return new CatalogoMatriculaPublicaResponseDto(aniosAbiertos, opciones);
  }

  @Transactional
  public SolicitudMatriculaPublicaResponseDto crear(CrearSolicitudMatriculaPublicaRequestDto r) {
    var anio = anios.findById(r.anioLectivoId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Año lectivo inexistente"));
    if (!Boolean.TRUE.equals(anio.getAbierto())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El año lectivo no admite matrículas");
    }
    var seccion = secciones.findById(r.seccionId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sección inexistente"));
    if (!seccion.getAnioLectivoId().equals(r.anioLectivoId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La sección no corresponde al año lectivo");
    }
    if (seccion.getCupoMaximo() <= seccion.getVacantesOcupadas()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La sección seleccionada ya no tiene vacantes");
    }
    if (!estudiantes.buscarPorNumeroDocumento(r.numeroDocumentoEstudiante()).isEmpty()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El DNI del estudiante ya está registrado");
    }
    if (solicitudes.existsByNumeroDocumentoEstudianteAndEstadoIn(
        r.numeroDocumentoEstudiante(), SOLICITUDES_ACTIVAS)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una solicitud pendiente para ese DNI");
    }

    byte[] secreto = new byte[32];
    RANDOM.nextBytes(secreto);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(secreto);
    var s = new SolicitudMatriculaPublicaEntity();
    s.setId(UUID.randomUUID());
    s.setTokenHash(hashToken(token));
    s.setEstado(EstadoSolicitudMatriculaPublica.DOCUMENTOS_PENDIENTES);
    s.setAnioLectivoId(r.anioLectivoId());
    s.setSeccionId(r.seccionId());
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
    s.setUbigeoApoderado(r.ubigeoApoderado().trim());
    s.setParentesco(r.parentesco());
    s.setConsentimientoGemini(r.consentimientoGemini());
    s.setPagoMonto(obtenerMontoMatriculaPublica());
    var guardado = solicitudes.saveAndFlush(s);
    return respuesta(guardado, token);
  }

  @Transactional(readOnly = true)
  public String prepararValidacion(UUID id, String token, TipoDocumentoSolicitud tipo) {
    var s = solicitudConToken(id, token);
    if (!s.isConsentimientoGemini()) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No se autorizó enviar los documentos a Gemini");
    }
    if (s.getEstado() != EstadoSolicitudMatriculaPublica.DOCUMENTOS_PENDIENTES
        && s.getEstado() != EstadoSolicitudMatriculaPublica.DOCUMENTOS_OBSERVADOS) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La etapa documental ya está cerrada");
    }
    var estado = s.estadoDocumento(tipo);
    if (estado == EstadoValidacionDocumento.VALIDADO) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Este documento ya fue validado");
    }
    return s.getNombresEstudiante() + " " + s.getApellidoPaternoEstudiante() + " "
        + s.getApellidoMaternoEstudiante() + "|" + s.getNumeroDocumentoEstudiante();
  }

  @Transactional
  public SolicitudMatriculaPublicaResponseDto guardarResultadoDocumento(
      UUID id, String token, TipoDocumentoSolicitud tipo, GeminiDocumentoMatriculaPort.Resultado resultado,
      String bucket, String objectKey, String mimeType, long tamanoBytes, String sha256) {
    var s = solicitudConTokenBloqueada(id, token);
    if (s.getEstado() != EstadoSolicitudMatriculaPublica.DOCUMENTOS_PENDIENTES
        && s.getEstado() != EstadoSolicitudMatriculaPublica.DOCUMENTOS_OBSERVADOS) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud cambió mientras se validaba el archivo");
    }
    var archivo = new SolicitudMatriculaDocumentoEntity();
    archivo.setId(new SolicitudMatriculaDocumentoId(id, tipo));
    archivo.setBucket(bucket);
    archivo.setObjectKey(objectKey);
    archivo.setMimeType(mimeType);
    archivo.setTamanoBytes(tamanoBytes);
    archivo.setSha256(sha256);
    archivo.setActualizadoAt(OffsetDateTime.now(ZoneOffset.UTC));
    documentos.save(archivo);
    s.actualizarDocumento(tipo,
        resultado.valido() ? EstadoValidacionDocumento.VALIDADO : EstadoValidacionDocumento.OBSERVADO,
        resultado.observacion());
    s.setEstado(todosDocumentosValidados(s)
        ? EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS
        : hayDocumentoObservado(s)
            ? EstadoSolicitudMatriculaPublica.DOCUMENTOS_OBSERVADOS
            : EstadoSolicitudMatriculaPublica.DOCUMENTOS_PENDIENTES);
    return respuesta(solicitudes.saveAndFlush(s), null);
  }

  @Transactional(readOnly = true)
  public SolicitudMatriculaPublicaResponseDto consultar(UUID id, String token) {
    return respuesta(solicitudConToken(id, token), null);
  }

  @Transactional(readOnly = true)
  public DocumentoDescargaAdmin documentoDescargaAdmin(UUID id, TipoDocumentoSolicitud tipo) {
    var archivo = documentos.findById(new SolicitudMatriculaDocumentoId(id, tipo))
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no disponible"));
    return new DocumentoDescargaAdmin(
        archivo.getBucket(), archivo.getObjectKey(), archivo.getMimeType(), archivo.getTamanoBytes(),
        archivo.getActualizadoAt());
  }

  public record DocumentoDescargaAdmin(
      String bucket, String objectKey, String mimeType, long tamanoBytes, OffsetDateTime actualizadoAt) {}

  @Transactional(readOnly = true)
  public String generarComprobanteHtml(UUID id, String token) {
    var s = solicitudConToken(id, token);
    validarDocumentosEmitidos(s);
    String filas = fila("Estudiante", nombreEstudiante(s))
        + fila("DNI del estudiante", s.getNumeroDocumentoEstudiante())
        + fila("Código de matrícula", "FIC-MAT-" + s.getMatriculaId())
        + fila("Referencia de pago Mercado Pago", s.getPagoId())
        + fila("Importe pagado", "S/ " + s.getPagoMonto().toPlainString() + " PEN")
        + fila("Estado", "PAGO APROBADO Y VERIFICADO")
        + fila("Emitido", fechaDocumento(s.getDocumentosEmitidosAt()));
    String aviso = "Comprobante interno de pago. No es una boleta electrónica ni reemplaza un comprobante tributario emitido ante SUNAT.";
    return documentoHtml("Comprobante de pago de matrícula", s.getComprobantePagoCodigo(), filas, aviso);
  }

  @Transactional(readOnly = true)
  public String generarFichaMatriculaHtml(UUID id, String token) {
    var s = solicitudConToken(id, token);
    validarDocumentosEmitidos(s);
    var anio = anios.findById(s.getAnioLectivoId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Año lectivo de la matrícula no disponible"));
    var seccion = secciones.findById(s.getSeccionId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Sección de la matrícula no disponible"));
    var grado = seccion.getRelacion1();
    var nivel = grado == null ? null : grado.getRelacion0();
    String seccionLabel = (nivel == null ? "Nivel" : nivel.getNombre()) + " · "
        + (grado == null ? "Grado" : grado.getNombre()) + " " + seccion.getLetra();
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
        + fila("Estado", "MATRÍCULA CONFIRMADA")
        + fila("Emitida", fechaDocumento(s.getDocumentosEmitidosAt()));
    return documentoHtml("Ficha de matrícula", fichaCodigo, filas,
        "Documento informativo generado al confirmar el pago y registrar la matrícula.");
  }

  @Transactional
  public PagoMatriculaPublicaResponseDto crearPago(UUID id, String token) {
    var s = solicitudConTokenBloqueada(id, token);
    if (s.getEstado() == EstadoSolicitudMatriculaPublica.MATRICULADA) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta solicitud ya fue matriculada");
    }
    if (s.getEstado() != EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS
        && s.getEstado() != EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Gemini debe validar los tres documentos antes del pago");
    }
    var ahora = OffsetDateTime.now(ZoneOffset.UTC);
    if (s.isVacanteReservada() && s.getPagoExpiraAt() != null && s.getPagoExpiraAt().isAfter(ahora)
        && s.getPagoEnlace() != null && !s.getPagoEnlace().isBlank()) {
      return pagoResponse(s);
    }
    if (!s.isVacanteReservada() && secciones.reservarVacante(s.getSeccionId()) != 1) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La vacante fue ocupada; selecciona otra sección");
    }
    s.setVacanteReservada(true);
    s.setPagoExpiraAt(ahora.plusMinutes(Math.max(5, duracionReservaMinutos)));
    s.setPagoMonto(obtenerMontoMatriculaPublica());
    String referencia = REFERENCIA_PAGO_PREFIX + s.getId();
    var preferencia = mercadoPago.crearPreferenciaMatriculaPublica(
        referencia, obtenerMontoMatriculaPublica(), "Derecho de matrícula escolar");
    s.setPagoPreferenciaId(preferencia.preferenceId());
    if (preferencia.sandboxInitPoint() == null || preferencia.sandboxInitPoint().isBlank()) {
      throw new BusinessException("Mercado Pago no devolvió el enlace de sandbox para la matrícula");
    }
    s.setPagoEnlace(preferencia.sandboxInitPoint());
    s.setEstado(EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE);
    solicitudes.saveAndFlush(s);
    return pagoResponse(s);
  }

  @Transactional
  public SolicitudMatriculaPublicaResponseDto finalizarPagoVerificado(
      UUID id, String token, PagoVerificado pago) {
    var s = token == null ? solicitudes.bloquearPorId(id).orElseThrow(() -> noEncontrada())
        : solicitudConTokenBloqueada(id, token);
    validarReferenciaPago(s, pago);
    if (!pago.aprobado()) return respuesta(s, null);
    if (s.getEstado() == EstadoSolicitudMatriculaPublica.MATRICULADA) {
      if (!pago.transaccionId().equals(s.getPagoId())) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Esta matrícula ya está asociada a otro pago");
      }
      return respuesta(s, null);
    }
    if (s.getEstado() != EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud no está en etapa de pago");
    }
    if (pago.monto() == null || pago.monto().compareTo(obtenerMontoMatriculaPublica()) != 0
        || !"PEN".equalsIgnoreCase(pago.moneda())) {
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "El pago debe coincidir con S/ " + obtenerMontoMatriculaPublica().toPlainString() + " PEN");
    }
    if (!s.isVacanteReservada()) {
      if (secciones.reservarVacante(s.getSeccionId()) != 1) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Pago aprobado, pero la vacante requiere revisión administrativa");
      }
      s.setVacanteReservada(true);
    }

    var anio = anios.bloquearPorId(s.getAnioLectivoId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "El año lectivo ya no existe"));
    if (!Boolean.TRUE.equals(anio.getAbierto())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El año lectivo fue cerrado; requiere revisión administrativa");
    }
    var seccion = secciones.bloquearPorId(s.getSeccionId())
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "La sección ya no existe"));
    if (!seccion.getAnioLectivoId().equals(s.getAnioLectivoId())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La sección ya no corresponde al año lectivo");
    }

    var existentes = estudiantes.buscarPorNumeroDocumento(s.getNumeroDocumentoEstudiante());
    if (!existentes.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Pago confirmado; el DNI del estudiante requiere conciliación administrativa");
    }
    var estudiante = new EstudianteEntity();
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
      apoderado.setUbigeoInei(s.getUbigeoApoderado());
      apoderado.setValidadoReniec(false);
      apoderado.setOrigenRegistro(OrigenRegistro.MANUAL_CONTINGENCIA);
      apoderado = apoderados.saveAndFlush(apoderado);
    }

    var vinculo = new EstudianteApoderadoEntity();
    vinculo.setEstudianteId(estudiante.getId());
    vinculo.setApoderadoId(apoderado.getId());
    vinculo.setParentesco(s.getParentesco());
    vinculo.setEsResponsableEconomico(true);
    vinculo.setTieneCustodia(true);
    vinculo.setPermiteRecojo(true);
    vinculos.saveAndFlush(vinculo);

    // La solicitud reservó el cupo al generar el checkout; se transfiere al trigger de matrícula.
    secciones.liberarVacante(s.getSeccionId());
    s.setVacanteReservada(false);
    var matricula = new MatriculaEntity();
    matricula.setAnioLectivoId(s.getAnioLectivoId());
    matricula.setEstudianteId(estudiante.getId());
    matricula.setSeccionId(s.getSeccionId());
    matricula.setEstadoMatricula(com.colegio.shuji.matricula.domain.enums.EstadoMatricula.MATRICULADO);
    matricula.setObservaciones("Matrícula pública confirmada por pago verificado de Mercado Pago");
    matricula = matriculas.saveAndFlush(matricula);

    s.setPagoId(pago.transaccionId());
    s.setEstudianteId(estudiante.getId());
    s.setApoderadoId(apoderado.getId());
    s.setMatriculaId(matricula.getId());
    s.setComprobantePagoCodigo("REC-MAT-" + matricula.getId());
    s.setDocumentosEmitidosAt(OffsetDateTime.now(ZoneOffset.UTC));
    s.setEstado(EstadoSolicitudMatriculaPublica.MATRICULADA);
    s.setPagoExpiraAt(null);
    return respuesta(solicitudes.saveAndFlush(s), null);
  }

  @Transactional(readOnly = true)
  public List<SolicitudMatriculaPublicaAdminResponseDto> listarAdmin() {
    return solicitudes.findTop200ByOrderByCreatedAtDesc().stream().map(s ->
        new SolicitudMatriculaPublicaAdminResponseDto(
            s.getId(), s.getEstado(), nombreEstudiante(s), s.getNumeroDocumentoEstudiante(),
            nombreApoderado(s), s.getCelularApoderado(), s.getEmailApoderado(),
            s.getAnioLectivoId(), s.getSeccionId(), documentos(s), s.getCreatedAt(),
            s.getEstudianteId(), s.getMatriculaId())).toList();
  }

  @Transactional
  public SolicitudMatriculaPublicaAdminResponseDto observarAdmin(
      UUID id, ObservarSolicitudMatriculaPublicaRequestDto r) {
    var s = solicitudes.bloquearPorId(id).orElseThrow(() -> noEncontrada());
    if (s.getEstado() == EstadoSolicitudMatriculaPublica.MATRICULADA
        || s.getEstado() == EstadoSolicitudMatriculaPublica.RECHAZADA) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud ya terminó");
    }
    liberarReserva(s);
    s.actualizarDocumento(r.tipo(), EstadoValidacionDocumento.OBSERVADO, r.observacion().trim());
    s.setEstado(EstadoSolicitudMatriculaPublica.DOCUMENTOS_OBSERVADOS);
    return adminResponse(solicitudes.saveAndFlush(s));
  }

  @Transactional
  public void rechazarAdmin(UUID id) {
    var s = solicitudes.bloquearPorId(id).orElseThrow(() -> noEncontrada());
    if (s.getEstado() == EstadoSolicitudMatriculaPublica.MATRICULADA) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "No se puede rechazar una matrícula ya confirmada");
    }
    liberarReserva(s);
    s.setEstado(EstadoSolicitudMatriculaPublica.RECHAZADA);
    solicitudes.saveAndFlush(s);
  }

  @Scheduled(fixedDelayString = "${app.matricula-publica.reserva-limpieza-ms:60000}")
  @Transactional
  public void liberarReservasVencidas() {
    var ahora = OffsetDateTime.now(ZoneOffset.UTC);
    for (var snapshot : solicitudes.buscarReservasExpiradas(ahora)) {
      var actual = solicitudes.bloquearPorId(snapshot.getId()).orElse(null);
      if (actual != null && actual.isVacanteReservada() && actual.getPagoExpiraAt() != null
          && !actual.getPagoExpiraAt().isAfter(ahora)) {
        liberarReserva(actual);
        actual.setPagoEnlace(null);
        actual.setPagoPreferenciaId(null);
        actual.setPagoExpiraAt(null);
        if (actual.getEstado() == EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE) {
          actual.setEstado(EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS);
        }
        solicitudes.save(actual);
      }
    }
  }

  @Transactional(readOnly = true)
  public void validarToken(UUID id, String token) { solicitudConToken(id, token); }

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
    return s.getEstadoPartida() == EstadoValidacionDocumento.VALIDADO
        && s.getEstadoDniC4() == EstadoValidacionDocumento.VALIDADO
        && s.getEstadoRecibo() == EstadoValidacionDocumento.VALIDADO;
  }

  private boolean hayDocumentoObservado(SolicitudMatriculaPublicaEntity s) {
    return s.getEstadoPartida() == EstadoValidacionDocumento.OBSERVADO
        || s.getEstadoDniC4() == EstadoValidacionDocumento.OBSERVADO
        || s.getEstadoRecibo() == EstadoValidacionDocumento.OBSERVADO;
  }

  private List<DocumentoSolicitudResponseDto> documentos(SolicitudMatriculaPublicaEntity s) {
    return java.util.Arrays.stream(TipoDocumentoSolicitud.values())
        .map(tipo -> new DocumentoSolicitudResponseDto(tipo, s.estadoDocumento(tipo), s.observacionDocumento(tipo)))
        .toList();
  }

  private SolicitudMatriculaPublicaResponseDto respuesta(SolicitudMatriculaPublicaEntity s, String token) {
    return new SolicitudMatriculaPublicaResponseDto(
        s.getId(), token, s.getEstado(), nombreEstudiante(s), documentos(s), s.getPagoEnlace(),
        s.getPagoMonto(), s.getEstudianteId(), s.getMatriculaId());
  }

  private PagoMatriculaPublicaResponseDto pagoResponse(SolicitudMatriculaPublicaEntity s) {
    return new PagoMatriculaPublicaResponseDto(s.getPagoPreferenciaId(), s.getPagoEnlace(), s.getPagoMonto(), "PEN");
  }

  private SolicitudMatriculaPublicaAdminResponseDto adminResponse(SolicitudMatriculaPublicaEntity s) {
    return new SolicitudMatriculaPublicaAdminResponseDto(
        s.getId(), s.getEstado(), nombreEstudiante(s), s.getNumeroDocumentoEstudiante(),
        nombreApoderado(s), s.getCelularApoderado(), s.getEmailApoderado(), s.getAnioLectivoId(),
        s.getSeccionId(), documentos(s), s.getCreatedAt(), s.getEstudianteId(), s.getMatriculaId());
  }

  private String nombreEstudiante(SolicitudMatriculaPublicaEntity s) {
    return s.getNombresEstudiante() + " " + s.getApellidoPaternoEstudiante() + " " + s.getApellidoMaternoEstudiante();
  }

  private String nombreApoderado(SolicitudMatriculaPublicaEntity s) {
    return s.getNombresApoderado() + " " + s.getApellidoPaternoApoderado() + " " + s.getApellidoMaternoApoderado();
  }

  private void validarDocumentosEmitidos(SolicitudMatriculaPublicaEntity solicitud) {
    if (solicitud.getEstado() != EstadoSolicitudMatriculaPublica.MATRICULADA
        || solicitud.getMatriculaId() == null
        || solicitud.getPagoId() == null
        || solicitud.getComprobantePagoCodigo() == null
        || solicitud.getDocumentosEmitidosAt() == null) {
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
      s.setPagoExpiraAt(null);
    }
  }

  private ResponseStatusException noEncontrada() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Solicitud inexistente o token inválido");
  }

  private BigDecimal obtenerMontoMatriculaPublica() {
    if (montoMatriculaPublica == null || montoMatriculaPublica.signum() <= 0) {
      throw new IllegalStateException("El monto de matrícula debe ser mayor que cero");
    }
    try {
      return montoMatriculaPublica.setScale(2, RoundingMode.UNNECESSARY);
    } catch (ArithmeticException e) {
      throw new IllegalStateException("El monto de matrícula debe tener como máximo dos decimales", e);
    }
  }
}
