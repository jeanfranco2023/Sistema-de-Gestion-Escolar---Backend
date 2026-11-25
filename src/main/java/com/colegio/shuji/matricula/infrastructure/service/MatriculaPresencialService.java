package com.colegio.shuji.matricula.infrastructure.service;

import com.colegio.shuji.matricula.domain.service.PoliticaAdmision;
import com.colegio.shuji.matricula.domain.service.ReglaEdadMatricula;
import static com.colegio.shuji.shared.domain.model.Reglas.exigir;

import com.colegio.shuji.academico.infrastructure.repository.JpaAnioLectivoRepository;
import com.colegio.shuji.academico.infrastructure.repository.JpaGradoRepository;
import com.colegio.shuji.academico.infrastructure.repository.JpaSeccionRepository;
import com.colegio.shuji.matricula.application.dto.in.RegistrarMatriculaPresencialRequestDto;
import com.colegio.shuji.matricula.application.dto.out.MatriculaPresencialResponseDto;
import com.colegio.shuji.matricula.application.port.out.AlmacenDocumentosMatriculaPort;
import com.colegio.shuji.matricula.application.port.out.CuentaApoderadoPort;
import com.colegio.shuji.matricula.application.port.out.SeguimientoSolicitudCorreoPort;
import com.colegio.shuji.matricula.domain.enums.EstadoFirmaDigital;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.enums.EstadoSolicitudMatriculaPublica;
import com.colegio.shuji.matricula.domain.enums.EstadoValidacionDocumento;
import com.colegio.shuji.matricula.domain.enums.ModalidadIngreso;
import com.colegio.shuji.matricula.domain.enums.OrigenRegistro;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
import com.colegio.shuji.matricula.infrastructure.entity.ApoderadoEntity;
import com.colegio.shuji.matricula.infrastructure.entity.DocumentoMatriculaVersionEntity;
import com.colegio.shuji.matricula.infrastructure.entity.EstudianteApoderadoEntity;
import com.colegio.shuji.matricula.infrastructure.entity.EstudianteEntity;
import com.colegio.shuji.matricula.infrastructure.entity.MatriculaEntity;
import com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaDocumentoEntity;
import com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaDocumentoId;
import com.colegio.shuji.matricula.infrastructure.entity.SolicitudMatriculaPublicaEntity;
import com.colegio.shuji.matricula.infrastructure.repository.JpaApoderadoRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaDocumentoMatriculaVersionRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaEstudianteApoderadoRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaEstudianteRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaMatriculaRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaSolicitudMatriculaDocumentoRepository;
import com.colegio.shuji.matricula.infrastructure.repository.JpaSolicitudMatriculaPublicaRepository;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.tesoreria.application.dto.out.ComprobanteMatriculaPdfDto;
import com.colegio.shuji.tesoreria.application.port.in.EmitirComprobanteUseCase;
import com.colegio.shuji.tesoreria.application.port.out.ConceptoCobroRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.ObligacionRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.PagoRepositoryPort;
import com.colegio.shuji.tesoreria.domain.enums.MetodoPago;
import com.colegio.shuji.tesoreria.domain.enums.PasarelaProveedor;
import com.colegio.shuji.tesoreria.domain.enums.TipoComprobante;
import com.colegio.shuji.tesoreria.domain.enums.TipoConcepto;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import com.colegio.shuji.tesoreria.domain.model.PagoTransaccion;
import com.colegio.shuji.tesoreria.infrastructure.adapter.ComprobanteMatriculaPdfAdapter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * Servicio para procesar la matrícula presencial realizada por Secretaría o Dirección,
 * registrando evidencias documentales de archivo (sin bloqueo por IA), cobro en efectivo,
 * emisión de comprobante oficial, asignación de aula y envío de credenciales con PDF adjunto.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class MatriculaPresencialService {

  private static final Logger log = LoggerFactory.getLogger(MatriculaPresencialService.class);
  private static final SecureRandom RANDOM = new SecureRandom();

  private final JpaAnioLectivoRepository anios;
  private final JpaGradoRepository grados;
  private final JpaSeccionRepository secciones;
  private final JpaEstudianteRepository estudiantes;
  private final JpaApoderadoRepository apoderados;
  private final JpaEstudianteApoderadoRepository vinculos;
  private final JpaMatriculaRepository matriculas;
  private final JpaSolicitudMatriculaPublicaRepository solicitudes;
  private final JpaSolicitudMatriculaDocumentoRepository documentos;
  private final JpaDocumentoMatriculaVersionRepository versiones;

  private final ConceptoCobroRepositoryPort conceptos;
  private final ObligacionRepositoryPort obligaciones;
  private final PagoRepositoryPort pagos;
  private final EmitirComprobanteUseCase comprobantes;
  private final AlmacenDocumentosMatriculaPort almacenDocumentos;
  private final ComprobanteMatriculaPdfAdapter pdfAdapter;
  private final SolicitudMatriculaPublicaService solicitudesPublicas;
  private final com.colegio.shuji.tesoreria.application.port.out.MercadoPagoPort mercadoPago;

  @org.springframework.beans.factory.annotation.Value("${integraciones.pagos.mercadopago.sandbox:false}")
  private boolean mercadoPagoSandbox;

  @Autowired(required = false)
  private CuentaApoderadoPort cuentaApoderadoPort;

  @Autowired(required = false)
  private SeguimientoSolicitudCorreoPort correoSeguimiento;

  @Transactional(readOnly = true)
  public BigDecimal importeEfectivo() {
    return solicitudesPublicas.obtenerMontoMatriculaPublica();
  }

  public MatriculaPresencialResponseDto registrar(RegistrarMatriculaPresencialRequestDto r) {
    validarArchivos(r);
    var importe = importeEfectivo();
    exigir(r.getMontoEfectivo() != null && r.getMontoEfectivo().compareTo(importe) == 0,
        "El pago en efectivo debe cubrir el total de la matrícula: S/ " + importe.toPlainString());
    var constancia = r.getConstanciaPago();
    exigir(constancia == null || constancia.isEmpty() || constancia.getSize() <= 5 * 1024 * 1024,
        "La imagen de constancia de pago no debe superar los 5 MB");
    var anio = validarAnio(r);
    var grado = grados.bloquearPorId(r.getGradoId())
        .orElseThrow(() -> new BusinessException("El grado seleccionado no existe"));
    validarEdadPresencial(r.getFechaNacimientoEstudiante(), grado, anio.getAnio());
    var seccion = validarSeccion(r);
    validarSinTramiteActivo(r);

    var concepto = conceptos.listar().stream()
        .filter(c -> c.getTipoConcepto() == TipoConcepto.MATRICULA).findFirst()
        .orElseThrow(() -> new BusinessException("Falta configurar el concepto de matrícula en Tesorería"));

    // 4. Registrar o actualizar Estudiante
    var estudiante = registrarOActualizarEstudiante(r);

    if (matriculas.existsByEstudianteIdAndAnioLectivoId(estudiante.getId(), r.getAnioLectivoId())) {
      throw new BusinessException("El estudiante ya cuenta con una matrícula registrada para este año lectivo");
    }

    // 5. Registrar o actualizar Apoderado
    var apoderado = registrarOActualizarApoderado(r);

    // 6. Vincular tutoría/custodia
    vincularEstudianteApoderado(estudiante.getId(), apoderado.getId(), r.getParentesco());

    // 7. Aprovisionar credenciales institucionales para el apoderado
    String passwordTemporal = null;
    boolean esCuentaNueva = false;
    String usernameApoderado = null;
    if (cuentaApoderadoPort != null && apoderado.getEmail() != null && !apoderado.getEmail().isBlank()) {
      try {
        var creds = cuentaApoderadoPort.asegurarCuentaApoderado(
            apoderado.getNumeroDocumento(),
            apoderado.getEmail(),
            apoderado.getNombres(),
            apoderado.getApellidoPaterno(),
            apoderado.getApellidoMaterno(),
            apoderado.getCelular(),
            apoderado.getDireccion());
        if (creds != null) {
          apoderado.setUsuarioId(creds.usuarioId());
          apoderados.saveAndFlush(apoderado);
          passwordTemporal = creds.passwordTemporal();
          esCuentaNueva = creds.esNuevaCuenta();
          usernameApoderado = creds.username();
        }
      } catch (Exception ex) {
        log.warn("No se pudo aprovisionar usuario institucional para apoderado {}: {}",
            apoderado.getNumeroDocumento(), ex.getMessage());
      }
    }

    // 8. Crear Matrícula
    var matricula = new MatriculaEntity();
    matricula.setAnioLectivoId(r.getAnioLectivoId());
    matricula.setEstudianteId(estudiante.getId());
    matricula.setSeccionId(r.getSeccionId());
    matricula.setEstadoMatricula(EstadoMatricula.MATRICULADO);
    ModalidadIngreso modalidad = r.getModalidadIngreso() != null ? r.getModalidadIngreso() : ModalidadIngreso.REGULAR;
    String colegioProcedencia = r.getColegioProcedencia() == null ? null : r.getColegioProcedencia().trim();
    if (colegioProcedencia != null && colegioProcedencia.isEmpty()) colegioProcedencia = null;
    matricula.setModalidadIngreso(modalidad);
    matricula.setColegioProcedencia(colegioProcedencia);
    matricula.setObservaciones(r.getObservaciones() != null && !r.getObservaciones().isBlank()
        ? r.getObservaciones().trim()
        : "Matrícula presencial registrada en ventanilla por Secretaría / Pago en Efectivo");
    matricula = matriculas.saveAndFlush(matricula);

    // 9. Registrar Pago en Efectivo en Tesorería y Emitir Comprobante B001
    var obligacion = ObligacionPago.programar(matricula.getId(), concepto, (short) 0,
        LocalDate.now(ZoneId.of("America/Lima")), importe);
    obligacion = obligaciones.guardar(obligacion);

    var pago = PagoTransaccion.builder()
        .obligacionPagoId(obligacion.getId())
        .pasarelaProveedor(PasarelaProveedor.CAJA_EFECTIVO)
        .pasarelaTransaccionId("CAJA-" + UUID.randomUUID())
        .montoPagado(importe)
        .metodoPago(MetodoPago.EFECTIVO)
        .build();
    pago.aprobar();
    pago = pagos.guardar(pago);

    var comprobanteEmitido = comprobantes.emitirAutomaticoParaPago(pago.getId(), TipoComprobante.BOLETA, "B001");
    String comprobanteCodigo = comprobanteEmitido.serie() + "-" + String.format(Locale.ROOT, "%08d", comprobanteEmitido.correlativo());

    String tokenSeguimiento = generarToken();
    var solicitud = expediente(r, tokenSeguimiento, EstadoSolicitudMatriculaPublica.MATRICULADA);
    UUID solicitudId = solicitud.getId();
    solicitud.setPagoId(pago.getPasarelaTransaccionId());
    solicitud.setPagoMonto(importe);
    solicitud.setComprobantePagoCodigo(comprobanteCodigo);
    solicitud.setFechaEmisionDocumentos(OffsetDateTime.now(ZoneOffset.UTC));
    solicitud.setEstudianteId(estudiante.getId());
    solicitud.setApoderadoId(apoderado.getId());
    solicitud.setMatriculaId(matricula.getId());
    guardarExpediente(solicitud, r);
    if (constancia != null && !constancia.isEmpty()) {
      guardarEvidenciaArchivo(solicitudId, TipoDocumentoSolicitud.CONSTANCIA_PAGO, constancia);
    }

    // 12. Generar PDF oficial del Boucher de Matrícula
    String detalleAcademico = grado.getNombre() + " - Sección " + seccion.getLetra();
    String nombreApoderado = apoderado.getNombres() + " " + apoderado.getApellidoPaterno() + " " + apoderado.getApellidoMaterno();
    String nombreEstudiante = estudiante.getNombres() + " " + estudiante.getApellidoPaterno() + " " + estudiante.getApellidoMaterno();

    var pdfDto = new ComprobanteMatriculaPdfDto(
        comprobanteCodigo,
        "CONSTANCIA DE PAGO",
        OffsetDateTime.now(ZoneOffset.UTC),
        "EFECTIVO - CAJA",
        pago.getPasarelaTransaccionId(),
        nombreApoderado,
        apoderado.getNumeroDocumento(),
        apoderado.getEmail(),
        apoderado.getCelular(),
        apoderado.getDireccion(),
        nombreEstudiante,
        estudiante.getNumeroDocumento(),
        detalleAcademico,
        "FIC-MAT-" + matricula.getId(),
        importe,
        "Derecho de Matricula Escolar - Ano " + anio.getAnio()
    );

    byte[] pdf = pdfAdapter.generarPdf(pdfDto);
    if (correoSeguimiento != null) {
      byte[] privado = passwordTemporal == null ? pdf : pdfAdapter.generarPdf(pdfDto, usernameApoderado, passwordTemporal);
      correoSeguimiento.enviarCredencialesMatricula(
          r.getEmailApoderado().trim(), nombreApoderado, nombreEstudiante, passwordTemporal,
          detalleAcademico, comprobanteCodigo, solicitudId, tokenSeguimiento, privado,
          "Constancia_Matricula_" + comprobanteCodigo + ".pdf");
    }

    return new MatriculaPresencialResponseDto(
        matricula.getId(),
        estudiante.getId(),
        apoderado.getId(),
        solicitudId,
        comprobanteCodigo,
        importe,
        "EFECTIVO - CAJA",
        OffsetDateTime.now(ZoneOffset.UTC),
        usernameApoderado != null ? usernameApoderado : apoderado.getEmail(),
        r.getEmailApoderado().trim(),
        esCuentaNueva,
        Base64.getEncoder().encodeToString(pdf),
        "Matrícula presencial registrada con éxito. Se emitió el comprobante " + comprobanteCodigo
    );
  }

  public PagoEnLinea iniciarPagoEnLinea(RegistrarMatriculaPresencialRequestDto r) {
    validarArchivos(r);
    var anio = validarAnio(r);
    var seccion = validarSeccion(r);
    var grado = seccion.getRelacion1() != null
        ? seccion.getRelacion1()
        : grados.findById(r.getGradoId()).orElse(null);
    validarEdadPresencial(r.getFechaNacimientoEstudiante(), grado, anio.getAnio());
    validarSinTramiteActivo(r);
    var existente = estudiantes.buscarPorNumeroDocumento(r.getNumeroDocumentoEstudiante()).stream().findFirst();
    exigir(existente.isEmpty() || !matriculas.existsByEstudianteIdAndAnioLectivoId(
        existente.get().getId(), r.getAnioLectivoId()), "El estudiante ya cuenta con una matrícula para este año");

    String token = generarToken();
    var solicitud = expediente(r, token, EstadoSolicitudMatriculaPublica.DOCUMENTOS_VALIDADOS);
    guardarExpediente(solicitud, r);
    var pago = solicitudesPublicas.crearPago(solicitud.getId(), token);
    return new PagoEnLinea(solicitud.getId(), pago.enlacePago(), pago.monto());
  }

  public record PagoEnLinea(UUID solicitudId, String enlacePago, java.math.BigDecimal monto) {}

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  public EstadoPagoPresencial estadoPago(UUID id) {
    var solicitud = solicitudes.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
        org.springframework.http.HttpStatus.NOT_FOUND, "No se encontró la solicitud"));
    exigir(solicitud.isPresencial(), "La solicitud no corresponde a una matrícula presencial");
    if (solicitud.getEstado() == EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE) {
      var pago = mercadoPago.buscarPagoAprobado(SolicitudMatriculaPublicaService.REFERENCIA_PAGO_PREFIX + id);
      if (pago.isPresent()) {
        solicitudesPublicas.finalizarPagoVerificado(id, null, pago.get());
        solicitud = solicitudes.findById(id).orElseThrow();
      }
    }
    return new EstadoPagoPresencial(solicitud.getEstado(), solicitud.getComprobantePagoCodigo());
  }

  public record EstadoPagoPresencial(EstadoSolicitudMatriculaPublica estado, String comprobanteCodigo) {}

  @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  public EstadoPagoPresencial simularPagoEnLinea(
      UUID id,
      com.colegio.shuji.tesoreria.domain.enums.MetodoPago metodo,
      String referenciaOperacion) {
    if (!mercadoPagoSandbox) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN,
          "La simulación de pagos solo está habilitada en entorno sandbox");
    }
    exigir(
        metodo != null && metodo != com.colegio.shuji.tesoreria.domain.enums.MetodoPago.EFECTIVO,
        "Selecciona un medio de pago en línea válido");
    var solicitud = solicitudes.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
        org.springframework.http.HttpStatus.NOT_FOUND, "No se encontró la solicitud"));
    exigir(solicitud.isPresencial(), "La solicitud no corresponde a una matrícula presencial");
    if (solicitud.getEstado() == EstadoSolicitudMatriculaPublica.PAGO_PENDIENTE) {
      java.math.BigDecimal monto = solicitud.getPagoMonto() != null ? solicitud.getPagoMonto() : importeEfectivo();
      String sufijo = referenciaOperacion != null && !referenciaOperacion.isBlank()
          ? referenciaOperacion.trim().replaceAll("[^A-Za-z0-9_-]", "")
          : UUID.randomUUID().toString().replace("-", "").substring(0, 10);
      String txId = "SIM-" + metodo.name() + "-" + sufijo;
      var pago = new com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort.PagoVerificado(
          txId.length() > 64 ? txId.substring(0, 64) : txId,
          null,
          monto,
          "PEN",
          metodo,
          true,
          SolicitudMatriculaPublicaService.REFERENCIA_PAGO_PREFIX + id);
      solicitudesPublicas.finalizarPagoVerificado(id, null, pago);
      solicitud = solicitudes.findById(id).orElseThrow();
    }
    return new EstadoPagoPresencial(solicitud.getEstado(), solicitud.getComprobantePagoCodigo());
  }

  private com.colegio.shuji.academico.infrastructure.entity.AnioLectivoEntity validarAnio(
      RegistrarMatriculaPresencialRequestDto r) {
    var anio = anios.bloquearPorId(r.getAnioLectivoId())
        .orElseThrow(() -> new BusinessException("El año lectivo seleccionado no existe"));
    exigir(Boolean.TRUE.equals(anio.getAbierto()), "El año lectivo seleccionado se encuentra cerrado");
    exigir(PoliticaAdmision.admiteNuevasMatriculas(anio.getAnio()),
        PoliticaAdmision.mensajeAnioNoDisponible());
    return anio;
  }

  private com.colegio.shuji.academico.infrastructure.entity.SeccionEntity validarSeccion(
      RegistrarMatriculaPresencialRequestDto r) {
    var seccion = secciones.bloquearPorId(r.getSeccionId())
        .orElseThrow(() -> new BusinessException("La sección seleccionada no existe"));
    exigir(seccion.getAnioLectivoId().equals(r.getAnioLectivoId()), "La sección no corresponde al año lectivo");
    exigir(seccion.getGradoId().equals(r.getGradoId()), "La sección no corresponde al grado seleccionado");
    exigir(seccion.getVacantesOcupadas() < seccion.getCupoMaximo(),
        "No quedan vacantes disponibles en la sección " + seccion.getLetra());
    return seccion;
  }

  private void validarEdadPresencial(
      LocalDate fechaNacimiento,
      com.colegio.shuji.academico.infrastructure.entity.GradoEntity grado,
      Short anioLectivo) {
    LocalDate hoyLima = LocalDate.now(ZoneId.of("America/Lima"));
    if (fechaNacimiento == null || !fechaNacimiento.isBefore(hoyLima)) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST,
          "La fecha de nacimiento del estudiante debe ser anterior al día de hoy.");
    }
    if (grado == null || anioLectivo == null || grado.getNumeroGrado() == null) return;
    String codigoNivel = grado.getRelacion0() != null && grado.getRelacion0().getCodigo() != null
        ? grado.getRelacion0().getCodigo().name()
        : grado.getNivelId() == null ? null
        : switch (grado.getNivelId()) {
          case 1 -> "INICIAL";
          case 2 -> "PRIMARIA";
          case 3 -> "SECUNDARIA";
          default -> null;
        };
    if (codigoNivel == null) return;
    var rango = ReglaEdadMatricula.paraGrado(codigoNivel, grado.getNumeroGrado());
    if (!rango.admiteConExcepcion(fechaNacimiento, anioLectivo)) {
      int edad = rango.edadAlCorte(fechaNacimiento, anioLectivo);
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.BAD_REQUEST,
          "La edad del estudiante" + (edad >= 0 ? " (" + edad + " años al 31 de marzo)" : "")
              + " no corresponde al grado seleccionado (rango EBR: "
              + rango.minima() + " a " + rango.maxima() + " años).");
    }
  }

  private void validarSinTramiteActivo(RegistrarMatriculaPresencialRequestDto r) {
    var activos = java.util.EnumSet.allOf(EstadoSolicitudMatriculaPublica.class);
    activos.remove(EstadoSolicitudMatriculaPublica.MATRICULADA);
    activos.remove(EstadoSolicitudMatriculaPublica.RECHAZADA);
    exigir(!solicitudes.existsByNumeroDocumentoEstudianteAndEstadoIn(r.getNumeroDocumentoEstudiante(), java.util.List.copyOf(activos)),
        "Ya existe una solicitud en trámite para el DNI del estudiante. Revísela en Solicitudes de matrícula");
  }

  private String generarToken() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private SolicitudMatriculaPublicaEntity expediente(RegistrarMatriculaPresencialRequestDto r,
      String tokenSeguimiento, EstadoSolicitudMatriculaPublica estado) {
    UUID solicitudId = UUID.randomUUID();
    var solicitud = new SolicitudMatriculaPublicaEntity();
    solicitud.setId(solicitudId);
    solicitud.setPresencial(true);
    solicitud.setTokenHash(hashToken(tokenSeguimiento));
    solicitud.setEstado(estado);
    solicitud.setAnioLectivoId(r.getAnioLectivoId());
    solicitud.setGradoId(r.getGradoId());
    solicitud.setSeccionId(r.getSeccionId());
    solicitud.setNumeroDocumentoEstudiante(r.getNumeroDocumentoEstudiante());
    solicitud.setNombresEstudiante(r.getNombresEstudiante().trim());
    solicitud.setApellidoPaternoEstudiante(r.getApellidoPaternoEstudiante().trim());
    solicitud.setApellidoMaternoEstudiante(r.getApellidoMaternoEstudiante().trim());
    solicitud.setFechaNacimientoEstudiante(r.getFechaNacimientoEstudiante());
    solicitud.setGeneroEstudiante(r.getGeneroEstudiante());
    solicitud.setNumeroDocumentoApoderado(r.getNumeroDocumentoApoderado());
    solicitud.setNombresApoderado(r.getNombresApoderado().trim());
    solicitud.setApellidoPaternoApoderado(r.getApellidoPaternoApoderado().trim());
    solicitud.setApellidoMaternoApoderado(r.getApellidoMaternoApoderado().trim());
    solicitud.setCelularApoderado(r.getCelularApoderado().trim());
    solicitud.setEmailApoderado(r.getEmailApoderado().trim().toLowerCase(Locale.ROOT));
    solicitud.setDireccionApoderado(r.getDireccionApoderado().trim());
    solicitud.setUbigeoApoderado("150101");
    ModalidadIngreso modalidad = r.getModalidadIngreso() != null ? r.getModalidadIngreso() : ModalidadIngreso.REGULAR;
    String colegioProcedencia = r.getColegioProcedencia() == null ? null : r.getColegioProcedencia().trim();
    if (colegioProcedencia != null && colegioProcedencia.isEmpty()) colegioProcedencia = null;
    solicitud.setModalidadIngreso(modalidad);
    solicitud.setColegioProcedencia(colegioProcedencia);
    solicitud.setParentesco(r.getParentesco());
    solicitud.setConsentimientoGemini(false);
    solicitud.setEstadoPartida(EstadoValidacionDocumento.VALIDADO);
    solicitud.setObservacionPartida("Evidencia formal registrada en ventanilla por Secretaría");
    solicitud.setEstadoDniC4(EstadoValidacionDocumento.VALIDADO);
    solicitud.setObservacionDniC4("Evidencia formal registrada en ventanilla por Secretaría");
    solicitud.setEstadoRecibo(EstadoValidacionDocumento.VALIDADO);
    solicitud.setObservacionRecibo("Evidencia formal registrada en ventanilla por Secretaría");
    if (modalidad == ModalidadIngreso.TRASLADO) {
      solicitud.setEstadoCertificado(EstadoValidacionDocumento.VALIDADO);
      solicitud.setObservacionCertificado("Evidencia académica de traslado registrada en ventanilla por Secretaría");
    }
    return solicitud;
  }

  private void guardarExpediente(SolicitudMatriculaPublicaEntity solicitud, RegistrarMatriculaPresencialRequestDto r) {
    solicitudes.saveAndFlush(solicitud);
    UUID id = solicitud.getId();
    guardarEvidenciaArchivo(id, TipoDocumentoSolicitud.PARTIDA_NACIMIENTO, r.getPartida());
    guardarEvidenciaArchivo(id, TipoDocumentoSolicitud.DNI_C4, r.getDni());
    guardarEvidenciaArchivo(id, TipoDocumentoSolicitud.RECIBO_SERVICIO, r.getRecibo());
    if (solicitud.getModalidadIngreso() == ModalidadIngreso.TRASLADO
        && r.getCertificadoEstudios() != null && !r.getCertificadoEstudios().isEmpty()) {
      guardarEvidenciaArchivo(id, TipoDocumentoSolicitud.CERTIFICADO_ESTUDIOS, r.getCertificadoEstudios());
    }
  }

  private void validarArchivos(RegistrarMatriculaPresencialRequestDto r) {
    if (r.getPartida() == null || r.getPartida().isEmpty()) {
      throw new BusinessException("Debe adjuntar la imagen o documento de la Partida de Nacimiento");
    }
    if (r.getDni() == null || r.getDni().isEmpty()) {
      throw new BusinessException("Debe adjuntar la imagen o documento del DNI del estudiante");
    }
    if (r.getRecibo() == null || r.getRecibo().isEmpty()) {
      throw new BusinessException("Debe adjuntar la imagen o documento del Recibo de luz o agua");
    }
    if (r.getModalidadIngreso() == ModalidadIngreso.TRASLADO) {
      if (r.getColegioProcedencia() == null || r.getColegioProcedencia().trim().length() < 3) {
        throw new BusinessException("Debe registrar el colegio de procedencia para matrículas por traslado");
      }
      if (r.getCertificadoEstudios() == null || r.getCertificadoEstudios().isEmpty()) {
        throw new BusinessException("Debe adjuntar el certificado de estudios o libreta de notas para traslado");
      }
    }
  }

  private EstudianteEntity registrarOActualizarEstudiante(RegistrarMatriculaPresencialRequestDto r) {
    var existentes = estudiantes.buscarPorNumeroDocumento(r.getNumeroDocumentoEstudiante());
    EstudianteEntity e;
    if (!existentes.isEmpty()) {
      e = existentes.getFirst();
      e.setNombres(r.getNombresEstudiante().trim());
      e.setApellidoPaterno(r.getApellidoPaternoEstudiante().trim());
      e.setApellidoMaterno(r.getApellidoMaternoEstudiante().trim());
      e.setFechaNacimiento(r.getFechaNacimientoEstudiante());
      e.setGenero(r.getGeneroEstudiante());
    } else {
      e = new EstudianteEntity();
      e.setTipoDocumento(TipoDocumento.DNI);
      e.setNumeroDocumento(r.getNumeroDocumentoEstudiante().trim());
      e.setNombres(r.getNombresEstudiante().trim());
      e.setApellidoPaterno(r.getApellidoPaternoEstudiante().trim());
      e.setApellidoMaterno(r.getApellidoMaternoEstudiante().trim());
      e.setFechaNacimiento(r.getFechaNacimientoEstudiante());
      e.setGenero(r.getGeneroEstudiante());
      e.setValidadoReniec(false);
      e.setOrigenRegistro(OrigenRegistro.MANUAL_CONTINGENCIA);
      e.setActivo(true);
    }
    return estudiantes.saveAndFlush(e);
  }

  private ApoderadoEntity registrarOActualizarApoderado(RegistrarMatriculaPresencialRequestDto r) {
    var existentes = apoderados.buscarPorNumeroDocumento(r.getNumeroDocumentoApoderado());
    ApoderadoEntity a;
    if (!existentes.isEmpty()) {
      a = existentes.getFirst();
      a.setNombres(r.getNombresApoderado().trim());
      a.setApellidoPaterno(r.getApellidoPaternoApoderado().trim());
      a.setApellidoMaterno(r.getApellidoMaternoApoderado().trim());
      a.setCelular(r.getCelularApoderado().trim());
      a.setEmail(r.getEmailApoderado().trim().toLowerCase(Locale.ROOT));
      a.setDireccion(r.getDireccionApoderado().trim());
    } else {
      a = new ApoderadoEntity();
      a.setTipoDocumento(TipoDocumento.DNI);
      a.setNumeroDocumento(r.getNumeroDocumentoApoderado().trim());
      a.setNombres(r.getNombresApoderado().trim());
      a.setApellidoPaterno(r.getApellidoPaternoApoderado().trim());
      a.setApellidoMaterno(r.getApellidoMaternoApoderado().trim());
      a.setCelular(r.getCelularApoderado().trim());
      a.setEmail(r.getEmailApoderado().trim().toLowerCase(Locale.ROOT));
      a.setDireccion(r.getDireccionApoderado().trim());
      a.setUbigeoInei("150101");
      a.setValidadoReniec(false);
      a.setOrigenRegistro(OrigenRegistro.MANUAL_CONTINGENCIA);
    }
    return apoderados.saveAndFlush(a);
  }

  private void vincularEstudianteApoderado(Long estudianteId, Long apoderadoId, com.colegio.shuji.matricula.domain.enums.Parentesco parentesco) {
    var vinculoOpt = vinculos.buscarPorEstudianteId(estudianteId).stream()
        .filter(v -> v.getApoderadoId().equals(apoderadoId))
        .findFirst();
    if (vinculoOpt.isEmpty()) {
      var v = new EstudianteApoderadoEntity();
      v.setEstudianteId(estudianteId);
      v.setApoderadoId(apoderadoId);
      v.setParentesco(parentesco);
      v.setEsResponsableEconomico(true);
      v.setTieneCustodia(true);
      v.setPermiteRecojo(true);
      vinculos.saveAndFlush(v);
    }
  }

  private void guardarEvidenciaArchivo(UUID solicitudId, TipoDocumentoSolicitud tipo, MultipartFile file) {
    try {
      byte[] bytes = file.getBytes();
      String mime = detectarMime(bytes);
      exigir(mime != null, "El archivo no es un PDF ni una imagen JPG, PNG o WebP válida");
      exigir(tipo != TipoDocumentoSolicitud.CONSTANCIA_PAGO || mime.startsWith("image/"),
          "La constancia de pago debe ser una imagen JPG, PNG o WebP");
      var guardado = almacenDocumentos.guardar(solicitudId, tipo, bytes, mime);
      org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
          new org.springframework.transaction.support.TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
              if (status == STATUS_ROLLED_BACK) {
                try {
                  almacenDocumentos.eliminar(guardado);
                } catch (RuntimeException ex) {
                  log.warn("No se pudo limpiar un archivo de la solicitud {}", solicitudId);
                }
              }
            }
          });
      String sha256 = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));

      var version = new DocumentoMatriculaVersionEntity();
      version.setId(UUID.randomUUID());
      version.setSolicitudId(solicitudId);
      version.setTipo(tipo);
      version.setBucket(guardado.bucket());
      version.setObjectKey(guardado.objectKey());
      version.setMimeType(mime);
      version.setTamanoBytes(bytes.length);
      version.setSha256(sha256);
      if (tipo != TipoDocumentoSolicitud.CONSTANCIA_PAGO) {
        version.setIaValida(true);
      }
      if (tipo == TipoDocumentoSolicitud.PARTIDA_NACIMIENTO
          || tipo == TipoDocumentoSolicitud.DNI_C4
          || tipo == TipoDocumentoSolicitud.CERTIFICADO_ESTUDIOS) {
        version.setDniApiCoincide(true);
      }
      if (tipo == TipoDocumentoSolicitud.PARTIDA_NACIMIENTO) {
        version.setVinculoParentalCoincide(true);
      }
      version.setObservacionIa("Evidencia documental formal archivada por Secretaría.");
      version.setEstadoFirma(EstadoFirmaDigital.NO_APLICA);
      version.setDetalleFirma("Registro presencial en ventanilla");
      version.setFechaCarga(OffsetDateTime.now(ZoneOffset.UTC));
      versiones.saveAndFlush(version);

      var doc = new SolicitudMatriculaDocumentoEntity();
      doc.setId(new SolicitudMatriculaDocumentoId(solicitudId, tipo));
      doc.setVersionId(version.getId());
      doc.setBucket(guardado.bucket());
      doc.setObjectKey(guardado.objectKey());
      doc.setMimeType(mime);
      doc.setTamanoBytes(bytes.length);
      doc.setSha256(sha256);
      doc.setFechaActualizacion(OffsetDateTime.now(ZoneOffset.UTC));
      documentos.saveAndFlush(doc);
    } catch (IOException | NoSuchAlgorithmException ex) {
      log.error("Error al guardar archivo de evidencia {}", tipo, ex);
      throw new BusinessException("No se pudo procesar el archivo de evidencia " + tipo);
    }
  }

  private String detectarMime(byte[] bytes) {
    if (bytes.length >= 5 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D'
        && bytes[3] == 'F' && bytes[4] == '-') return "application/pdf";
    if (bytes.length >= 3 && bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8
        && bytes[2] == (byte) 0xff) return "image/jpeg";
    if (bytes.length >= 8 && HexFormat.of().formatHex(bytes, 0, 8).equals("89504e470d0a1a0a")) return "image/png";
    if (bytes.length >= 12 && new String(bytes, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
        && new String(bytes, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) return "image/webp";
    return null;
  }

  private String hashToken(String token) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 no está disponible", ex);
    }
  }
}
