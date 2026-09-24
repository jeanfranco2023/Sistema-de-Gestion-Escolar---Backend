package com.colegio.shuji.matricula.application.service;

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
import com.colegio.shuji.matricula.infrastructure.repository.JpaApoderadoRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaEstudianteApoderadoRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaEstudianteRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaMatriculaRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaSolicitudMatriculaPublicaRepository;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.application.port.out.MercadoPagoPort;
import com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort.PagoVerificado;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
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
  private static final BigDecimal MONTO_PRUEBA = new BigDecimal("1.00");
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final List<EstadoSolicitudMatriculaPublica> SOLICITUDES_ACTIVAS = List.of(
      EstadoSolicitudMatriculaPublica.DOCUMENTOS_PENDIENTES,
      EstadoSolicitudMatriculaPublica.DOCUMENTOS_OBSERVADOS,
      EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS,
      EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE);

  private final JpaSolicitudMatriculaPublicaRepository solicitudes;
  private final JpaAnioLectivoRepository anios;
  private final JpaSeccionRepository secciones;
  private final JpaEstudianteRepository estudiantes;
  private final JpaApoderadoRepository apoderados;
  private final JpaEstudianteApoderadoRepository vinculos;
  private final JpaMatriculaRepository matriculas;
  private final GeminiDocumentoMatriculaPort gemini;
  private final MercadoPagoPort mercadoPago;

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
    s.setPagoMonto(MONTO_PRUEBA);
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
      UUID id, String token, TipoDocumentoSolicitud tipo, GeminiDocumentoMatriculaPort.Resultado resultado) {
    var s = solicitudConTokenBloqueada(id, token);
    if (s.getEstado() != EstadoSolicitudMatriculaPublica.DOCUMENTOS_PENDIENTES
        && s.getEstado() != EstadoSolicitudMatriculaPublica.DOCUMENTOS_OBSERVADOS) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "La solicitud cambió mientras se validaba el archivo");
    }
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
    s.setPagoMonto(MONTO_PRUEBA);
    String referencia = REFERENCIA_PAGO_PREFIX + s.getId();
    var preferencia = mercadoPago.crearPreferenciaMatriculaPublica(
        referencia, MONTO_PRUEBA, "Derecho de matrícula escolar (prueba) ");
    s.setPagoPreferenciaId(preferencia.preferenceId());
    s.setPagoEnlace(preferencia.sandboxInitPoint() == null || preferencia.sandboxInitPoint().isBlank()
        ? preferencia.initPoint() : preferencia.sandboxInitPoint());
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
    if (pago.monto() == null || pago.monto().compareTo(MONTO_PRUEBA) != 0
        || !"PEN".equalsIgnoreCase(pago.moneda())) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El pago no coincide con S/ 1.00 PEN");
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
}
