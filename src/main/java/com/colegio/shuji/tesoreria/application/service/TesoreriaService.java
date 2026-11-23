package com.colegio.shuji.tesoreria.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.ApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.in.GestionarPagoMatriculaPublicaUseCase;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import com.colegio.shuji.tesoreria.application.dto.in.CrearPreferenciaMercadoPagoDto;
import com.colegio.shuji.tesoreria.application.dto.in.GenerarObligacionesAnualesRequestDto;
import com.colegio.shuji.tesoreria.application.dto.in.GenerarObligacionMatriculaRequestDto;
import com.colegio.shuji.tesoreria.application.dto.in.ProcesarPagoWebhookRequestDto;
import com.colegio.shuji.tesoreria.application.dto.in.RegistrarPagoCajaRequestDto;
import com.colegio.shuji.tesoreria.application.dto.in.RevertirPagoRequestDto;
import com.colegio.shuji.tesoreria.application.dto.out.EstadoCuentaEstudianteResponseDto;
import com.colegio.shuji.tesoreria.application.dto.out.ConceptoCobroResponseDto;
import com.colegio.shuji.tesoreria.application.dto.out.ObligacionResponseDto;
import com.colegio.shuji.tesoreria.application.dto.out.PreferenciaMercadoPagoResponseDto;
import com.colegio.shuji.tesoreria.application.dto.out.TransaccionResponseDto;
import com.colegio.shuji.tesoreria.application.mapper.TesoreriaMapper;
import com.colegio.shuji.tesoreria.application.port.in.EmitirComprobanteUseCase;
import com.colegio.shuji.tesoreria.application.port.in.GenerarCronogramaPensionesUseCase;
import com.colegio.shuji.tesoreria.application.port.in.ProcesarPagoPasarelaUseCase;
import com.colegio.shuji.tesoreria.application.port.in.RevertirPagoUseCase;
import com.colegio.shuji.tesoreria.application.port.out.ConceptoCobroRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.MercadoPagoPort;
import com.colegio.shuji.tesoreria.application.port.out.ObligacionRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.PagoRepositoryPort;
import com.colegio.shuji.tesoreria.application.port.out.VerificarPagoPort;
import com.colegio.shuji.tesoreria.domain.enums.EstadoObligacion;
import com.colegio.shuji.tesoreria.domain.enums.EstadoPago;
import com.colegio.shuji.tesoreria.domain.enums.MetodoPago;
import com.colegio.shuji.tesoreria.domain.enums.PasarelaProveedor;
import com.colegio.shuji.tesoreria.domain.enums.TipoComprobante;
import com.colegio.shuji.tesoreria.domain.enums.TipoConcepto;
import com.colegio.shuji.tesoreria.domain.model.ObligacionPago;
import com.colegio.shuji.tesoreria.domain.model.PagoTransaccion;
import com.colegio.shuji.tesoreria.application.dto.in.CobroExtraordinarioRequest;
import com.colegio.shuji.tesoreria.application.dto.out.ComprobanteMatriculaPdfDto;
import com.colegio.shuji.tesoreria.domain.model.ConceptoCobro;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class TesoreriaService
    implements GenerarCronogramaPensionesUseCase, ProcesarPagoPasarelaUseCase, RevertirPagoUseCase {
  private final TesoreriaMapper mapper;
  private final ObligacionRepositoryPort obligaciones;
  private final PagoRepositoryPort pagos;
  private final ConceptoCobroRepositoryPort conceptos;
  private final MatriculaRepositoryPort matriculas;
  private final EstudianteRepositoryPort estudiantes;
  private final AnioLectivoRepositoryPort anios;
  private final VerificarPagoPort verificador;
  private final MercadoPagoPort mercadoPagoPort;
  private final EmitirComprobanteUseCase comprobantes;
  private final ActorActualPort actor;
  private final ApoderadoRepositoryPort apoderados;
  private final EstudianteApoderadoRepositoryPort estudianteApoderados;
  private final GestionarPagoMatriculaPublicaUseCase matriculasPublicas;

  @org.springframework.beans.factory.annotation.Value("${integraciones.pagos.mercadopago.sandbox:false}")
  private boolean mercadoPagoSandbox;

  @Transactional(readOnly = true)
  public List<ConceptoCobroResponseDto> listarConceptos() {
    return conceptos.listar().stream().map(mapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<ConceptoCobroResponseDto> conceptosExtraordinarios() {
    return conceptos.listar().stream()
        .filter(c -> esExtraordinario(c.getTipoConcepto()))
        .filter(c -> c.isActivo() || puedeAdministrarCatalogo())
        .map(mapper::toResponse).toList();
  }

  public ConceptoCobroResponseDto guardarConceptoExtraordinario(
      Short id, CobroExtraordinarioRequest.Concepto r) {
    exigir(esExtraordinario(r.tipo()), "Tipo de concepto extraordinario incorrecto");
    var concepto = id == null ? ConceptoCobro.builder()
        .codigo("EXTRA_" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 24))
        .tipoConcepto(r.tipo()).build() : requerido(conceptos.bloquearPorId(id));
    exigir(concepto.getTipoConcepto() == r.tipo(), "El tipo del concepto no se puede cambiar");
    concepto.actualizar(r.nombre().strip(), r.monto());
    concepto.setActivo(r.activo());
    var guardado = conceptos.guardar(concepto);
    if (guardado.isActivo() && (id == null || r.vencimiento() != null)) {
      generarCargosAutomaticosParaConcepto(guardado, r.vencimiento());
    }
    return mapper.toResponse(guardado);
  }

  private void generarCargosAutomaticosParaConcepto(
      ConceptoCobro concepto, LocalDate vencimientoSolicitado) {
    LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
    var abiertos = anios.listar().stream()
        .filter(a -> Boolean.TRUE.equals(a.getAbierto()))
        .collect(java.util.stream.Collectors.toMap(
            com.colegio.shuji.academico.domain.model.AnioLectivo::getId,
            java.util.function.Function.identity()));
    if (abiertos.isEmpty()) return;

    var matriculasActivas = matriculas.listar().stream()
        .filter(m -> m.estaMatriculado() && m.getSeccionId() != null && abiertos.containsKey(m.getAnioLectivoId()))
        .toList();
    if (matriculasActivas.isEmpty()) return;

    var yaCargadas = obligaciones.buscarPorMatriculaIds(
            matriculasActivas.stream().map(com.colegio.shuji.matricula.domain.model.Matricula::getId).toList())
        .stream()
        .filter(o -> java.util.Objects.equals(o.getConceptoId(), concepto.getId()))
        .map(ObligacionPago::getMatriculaId)
        .collect(java.util.stream.Collectors.toSet());

    for (var matricula : matriculasActivas) {
      if (yaCargadas.contains(matricula.getId())) continue;
      int anioLectivo = abiertos.get(matricula.getAnioLectivoId()).getAnio();
      LocalDate fechaBase = (vencimientoSolicitado != null && !vencimientoSolicitado.isBefore(hoy))
          ? vencimientoSolicitado
          : hoy.plusDays(15);
      LocalDate fechaVencimiento;
      if (fechaBase.getYear() == anioLectivo) {
        fechaVencimiento = fechaBase;
      } else if (anioLectivo == hoy.getYear()) {
        LocalDate finAnio = LocalDate.of(anioLectivo, 12, 28);
        fechaVencimiento = finAnio.isBefore(hoy) ? hoy : finAnio;
      } else {
        fechaVencimiento = LocalDate.of(anioLectivo, fechaBase.getMonth(), Math.min(fechaBase.getDayOfMonth(), 28));
      }
      if (fechaVencimiento.isBefore(hoy) && anioLectivo == hoy.getYear()) {
        fechaVencimiento = hoy;
      }
      obligaciones.guardar(ObligacionPago.programar(matricula.getId(), concepto, (short) 0, fechaVencimiento));
    }
  }

  public ConceptoCobroResponseDto configurarPension(CobroExtraordinarioRequest.Pension r) {
    var pension = requerido(conceptos.buscarPorCodigo("PENSION_MENSUAL").stream().findFirst());
    pension = requerido(conceptos.bloquearPorId(pension.getId()));
    pension.setDiaVencimiento(r.diaVencimiento());
    return mapper.toResponse(conceptos.guardar(pension));
  }

  public record MatriculaParaCobro(Long id, String estudiante, Integer anio) {}

  @Transactional(readOnly = true)
  public List<MatriculaParaCobro> matriculasParaCobro() {
    var abiertos = anios.listar().stream().filter(a -> Boolean.TRUE.equals(a.getAbierto()))
        .collect(java.util.stream.Collectors.toMap(a -> a.getId(), a -> a.getAnio().intValue()));
    var vigentes = matriculas.listar().stream()
        .filter(m -> m.estaMatriculado() && abiertos.containsKey(m.getAnioLectivoId())).toList();
    var nombres = estudiantes.buscarPorIds(vigentes.stream().map(m -> m.getEstudianteId()).distinct().toList())
        .stream().collect(java.util.stream.Collectors.toMap(e -> e.getId(), e -> e.nombreCompleto()));
    return vigentes.stream().filter(m -> nombres.containsKey(m.getEstudianteId()))
        .map(m -> new MatriculaParaCobro(m.getId(), nombres.get(m.getEstudianteId()), abiertos.get(m.getAnioLectivoId())))
        .sorted(java.util.Comparator.comparing(MatriculaParaCobro::estudiante)).toList();
  }

  public List<ObligacionResponseDto> generarCobrosExtraordinarios(CobroExtraordinarioRequest.Asignacion r) {
    var concepto = requerido(conceptos.bloquearPorId(r.conceptoId()));
    exigir(esExtraordinario(concepto.getTipoConcepto()) && concepto.isActivo(), "El concepto no está habilitado");
    var resultado = new ArrayList<ObligacionResponseDto>();
    for (Long id : r.matriculaIds().stream().distinct().sorted().toList()) {
      var matricula = requerido(matriculas.bloquearPorId(id));
      var anio = requerido(anios.buscarPorId(matricula.getAnioLectivoId()));
      anio.verificarAbierto();
      exigir(matricula.estaMatriculado() && matricula.getSeccionId() != null, "La matrícula debe estar confirmada");
      var existente = obligaciones.buscarPorMatriculaId(id).stream()
          .filter(o -> o.getConceptoId().equals(concepto.getId())).findFirst();
      if (existente.isPresent()) {
        resultado.add(mapper.toResponse(existente.get()));
        continue;
      }
      exigir(r.vencimiento().getYear() == anio.getAnio()
          && !r.vencimiento().isBefore(LocalDate.now(ZoneId.of("America/Lima"))),
          "El vencimiento debe ser vigente y corresponder al año lectivo");
      resultado.add(mapper.toResponse(obligaciones.guardar(
          ObligacionPago.programar(id, concepto, (short) 0, r.vencimiento()))));
    }
    return conNombres(resultado);
  }

  @Transactional(readOnly = true)
  public List<ObligacionResponseDto> obligacionesExtraordinarias(Long matriculaId) {
    if (matriculaId == null || matriculaId <= 0) {
      return conNombres(obligaciones.listar().stream()
          .filter(o -> esExtraordinario(o.getTipoConcepto())).map(mapper::toResponse).toList());
    }
    var matricula = requerido(matriculas.buscarPorId(matriculaId));
    requerido(anios.buscarPorId(matricula.getAnioLectivoId())).verificarAbierto();
    return conNombres(obligaciones.buscarPorMatriculaId(matriculaId).stream()
        .filter(o -> esExtraordinario(o.getTipoConcepto())).map(mapper::toResponse).toList());
  }

  private boolean puedeAdministrarCatalogo() {
    return actor.tieneRol("DIRECCION") || actor.tieneRol("SECRETARIA") || actor.tieneRol("ADMINISTRADOR");
  }

  private static boolean esExtraordinario(TipoConcepto tipo) {
    return tipo == TipoConcepto.OTRO || tipo == TipoConcepto.CERTIFICADO;
  }

  public ComprobanteMatriculaPdfDto constanciaExtraordinaria(Long obligacionId) {
    var obligacion = requerido(obligaciones.buscarPorId(obligacionId));
    var matricula = requerido(matriculas.buscarPorId(obligacion.getMatriculaId()));
    if (!puedeAdministrarCatalogo() && actor.tieneRol("AUXILIAR")) {
      exigir(esExtraordinario(obligacion.getTipoConcepto()), "El cobro no es extraordinario");
      requerido(anios.buscarPorId(matricula.getAnioLectivoId())).verificarAbierto();
    } else {
      autorizarPago(obligacion);
    }
    var pago = requerido(pagos.buscarPorObligacionPagoId(obligacionId).stream()
        .filter(p -> p.getEstadoPago() == EstadoPago.APROBADO)
        .max(java.util.Comparator.comparing(PagoTransaccion::getId)));
    var comprobante = comprobantes.emitirAutomaticoParaPago(pago.getId(), TipoComprobante.BOLETA, "B001");
    exigir(comprobante.estadoComprobante() == com.colegio.shuji.tesoreria.domain.enums.EstadoComprobante.EMITIDO,
        "La constancia fue anulada");
    var estudiante = requerido(estudiantes.buscarPorId(matricula.getEstudianteId()));
    String folio = comprobante.serie() + "-" + String.format(java.util.Locale.ROOT, "%08d", comprobante.correlativo());
    return new ComprobanteMatriculaPdfDto(folio, comprobante.tipoComprobante().name(),
        comprobante.fechaEmision(), pago.getMetodoPago().name(), pago.getPasarelaTransaccionId(),
        null, null, null, null, null, estudiante.nombreCompleto(), null, null,
        matricula.getId().toString(), comprobante.montoTotal(), obligacion.getDescripcion());
  }

  public List<ObligacionResponseDto> generarCronograma(GenerarObligacionesAnualesRequestDto r) {
    var m = requerido(matriculas.bloquearPorId(r.matriculaId()));
    var anio = requerido(anios.buscarPorId(m.getAnioLectivoId()));
    anio.verificarAbierto();
    var matricula = requerido(conceptos.buscarPorId(r.conceptoMatriculaId()));
    var pension = requerido(conceptos.buscarPorId(r.conceptoPensionId()));
    exigir(
        matricula.getTipoConcepto() == TipoConcepto.MATRICULA
            && pension.getTipoConcepto() == TipoConcepto.PENSION,
        "Conceptos incorrectos");
    exigir(
        r.primerVencimiento().getYear() == anio.getAnio()
            && r.primerVencimiento().plusMonths(9).getYear() == anio.getAnio(),
        "Las diez cuotas deben corresponder al año lectivo");
    exigir(
        m.getEstadoMatricula() != EstadoMatricula.CANCELADA
            && m.getEstadoMatricula() != EstadoMatricula.RETIRADO
            && m.getEstadoMatricula() != EstadoMatricula.TRASLADADO,
        "Matrícula inactiva");
    var existentes = obligaciones.buscarPorMatriculaId(m.getId());
    var result = new ArrayList<ObligacionResponseDto>();
    for (short i = 0; i <= 10; i++) {
      var concepto = i == 0 ? matricula : pension;
      short cuota = i;
      var existente =
          existentes.stream()
              .filter(
                  o -> o.getConceptoId().equals(concepto.getId()) && o.getNumeroCuota() == cuota)
              .findFirst();
      if (existente.isPresent()) {
        result.add(mapper.toResponse(existente.get()));
        continue;
      }
      var o =
          ObligacionPago.programar(
              m.getId(),
              concepto,
              i,
              i == 0 ? r.primerVencimiento() : r.primerVencimiento().plusMonths(i - 1));
      result.add(mapper.toResponse(obligaciones.guardar(o)));
    }
    return conNombres(result);
  }

  public ObligacionResponseDto generarObligacionMatricula(GenerarObligacionMatriculaRequestDto r) {
    var m = requerido(matriculas.bloquearPorId(r.matriculaId()));
    exigir(
        m.getEstadoMatricula() != EstadoMatricula.CANCELADA
            && m.getEstadoMatricula() != EstadoMatricula.RETIRADO
            && m.getEstadoMatricula() != EstadoMatricula.TRASLADADO,
        "Matrícula inactiva");
    requerido(anios.buscarPorId(m.getAnioLectivoId())).verificarAbierto();
    boolean responsableEconomico = estudianteApoderados.buscarPorEstudianteId(m.getEstudianteId())
        .stream().anyMatch(v -> Boolean.TRUE.equals(v.getEsResponsableEconomico()));
    exigir(responsableEconomico, "La matrícula requiere un responsable económico");

    var conceptosMatricula = conceptos.listar().stream()
        .filter(c -> c.getTipoConcepto() == TipoConcepto.MATRICULA)
        .toList();
    exigir(conceptosMatricula.size() == 1, "Debe existir un único concepto de matrícula");
    var concepto = conceptosMatricula.getFirst();
    exigir(concepto.getMontoSugerido() != null && concepto.getMontoSugerido().signum() > 0,
        "El monto del concepto de matrícula debe ser positivo");

    var cuotasMatricula = obligaciones.buscarPorMatriculaId(m.getId()).stream()
        .filter(o -> o.getTipoConcepto() == TipoConcepto.MATRICULA && o.getNumeroCuota() == 0)
        .toList();
    if (!cuotasMatricula.isEmpty()) {
      exigir(cuotasMatricula.size() == 1
          && java.util.Objects.equals(cuotasMatricula.getFirst().getConceptoId(), concepto.getId()),
          "Ya existe una cuota 0 con un concepto distinto");
      return conNombres(List.of(mapper.toResponse(cuotasMatricula.getFirst()))).getFirst();
    }

    var obligacion = ObligacionPago.programar(
        m.getId(), concepto, (short) 0, r.fechaVencimiento());
    return conNombres(List.of(mapper.toResponse(obligaciones.guardar(obligacion)))).getFirst();
  }

  @Transactional(readOnly = true)
  public List<ObligacionResponseDto> obligacionesMatricula(Long id) {
    var lista = (id == null || id <= 0)
        ? obligaciones.listar()
        : obligaciones.buscarPorMatriculaId(id);
    return conNombres(lista.stream().map(mapper::toResponse).toList());
  }

  @Transactional(readOnly = true)
  public List<ObligacionResponseDto> misObligaciones() {
    var apoderado = apoderados.buscarPorUsuarioId(actor.usuarioId()).stream().findFirst();
    if (apoderado.isEmpty()) return List.of();
    var matriculaIds = estudianteApoderados.buscarPorApoderadoId(apoderado.get().getId()).stream()
        .filter(v -> Boolean.TRUE.equals(v.getEsResponsableEconomico()))
        .flatMap(v -> matriculas.buscarPorEstudianteId(v.getEstudianteId()).stream())
        .map(m -> m.getId())
        .distinct()
        .toList();
    if (matriculaIds.isEmpty()) return List.of();
    return conNombres(obligaciones.buscarPorMatriculaIds(matriculaIds).stream()
        .map(mapper::toResponse).toList());
  }

  @Transactional(readOnly = true)
  public EstadoCuentaEstudianteResponseDto estadoCuenta(Long estudianteId) {
    var matriculaIds =
        matriculas.buscarPorEstudianteId(estudianteId).stream().map(m -> m.getId()).toList();
    var deudas = obligaciones.buscarPorMatriculaIds(matriculaIds);
    return new EstadoCuentaEstudianteResponseDto(
        estudianteId,
        conNombres(deudas.stream().map(mapper::toResponse).toList()),
        deudas.stream()
            .filter(o -> o.getEstado() != EstadoObligacion.CANCELADO_POR_TRASLADO)
            .map(ObligacionPago::saldo)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
  }

  private List<ObligacionResponseDto> conNombres(List<ObligacionResponseDto> filas) {
    if (filas.isEmpty()) return List.of();
    var matriculasPorId = matriculas.buscarPorIds(filas.stream().map(ObligacionResponseDto::matriculaId).distinct().toList())
        .stream().collect(java.util.stream.Collectors.toMap(
            com.colegio.shuji.matricula.domain.model.Matricula::getId, java.util.function.Function.identity()));
    var estudiantesPorId = estudiantes.buscarPorIds(matriculasPorId.values().stream()
        .map(com.colegio.shuji.matricula.domain.model.Matricula::getEstudianteId).distinct().toList())
        .stream().collect(java.util.stream.Collectors.toMap(
            com.colegio.shuji.matricula.domain.model.Estudiante::getId,
            com.colegio.shuji.matricula.domain.model.Estudiante::nombreCompleto));
    var hoy = LocalDate.now(ZoneId.of("America/Lima"));
    return filas.stream().map(fila -> {
      var matricula = matriculasPorId.get(fila.matriculaId());
      String nombre = matricula == null ? null : estudiantesPorId.get(matricula.getEstudianteId());
      return new ObligacionResponseDto(fila.id(), fila.matriculaId(), fila.conceptoId(), fila.tipoConcepto(),
          fila.numeroCuota(), fila.descripcion(), fila.fechaVencimiento(), fila.montoBase(), fila.montoMora(),
          fila.montoDescuento(), fila.totalPagado(), fila.saldoPendiente(), estadoParaConsulta(fila, hoy),
          nombre == null ? "Estudiante sin datos" : nombre);
    }).toList();
  }

  static EstadoObligacion estadoParaConsulta(ObligacionResponseDto fila, LocalDate hoy) {
    if (fila.tipoConcepto() == TipoConcepto.PENSION
        && (fila.estado() == EstadoObligacion.PENDIENTE || fila.estado() == EstadoObligacion.PAGADO_PARCIAL)
        && fila.fechaVencimiento().isBefore(hoy) && fila.saldoPendiente().signum() > 0) {
      return EstadoObligacion.VENCIDO;
    }
    return fila.estado();
  }

  private TransaccionResponseDto registrar(PagoTransaccion p) {
    var o = requerido(obligaciones.bloquearPorId(p.getObligacionPagoId()));
    var anteriores = pagos.buscarPorPasarelaTransaccionId(p.getPasarelaTransaccionId());
    if (!anteriores.isEmpty()) {
      var previo = anteriores.getFirst();
      if (!previo.getObligacionPagoId().equals(p.getObligacionPagoId())
          || previo.getMontoPagado().compareTo(p.getMontoPagado()) != 0
          || previo.getPasarelaProveedor() != p.getPasarelaProveedor()
          || previo.getMetodoPago() != p.getMetodoPago())
        throw new com.colegio.shuji.tesoreria.domain.exception.TransaccionDuplicadaException(
            "La clave idempotente pertenece a otro pago");
      return mapper.toResponse(previo);
    }
    o.validarPago(p.getMontoPagado());
    p.aprobar();
    return mapper.toResponse(pagos.guardar(p));
  }

  public TransaccionResponseDto registrarCaja(RegistrarPagoCajaRequestDto r) {
    exigir(
        r.metodoPago() == MetodoPago.EFECTIVO || r.metodoPago() == MetodoPago.TRANSFERENCIA,
        "Caja admite efectivo o transferencia verificada");
    var p = mapper.toDomain(r);
    p.registrarOrigenCaja();
    var obligacion = requerido(obligaciones.bloquearPorId(r.obligacionPagoId()));
    boolean extraordinario = esExtraordinario(obligacion.getTipoConcepto());
    if (actor.tieneRol("AUXILIAR") && !puedeAdministrarCatalogo()) {
      if (!extraordinario) {
        throw new org.springframework.security.access.AccessDeniedException("Cobro no autorizado");
      }
      var matricula = requerido(matriculas.buscarPorId(obligacion.getMatriculaId()));
      requerido(anios.buscarPorId(matricula.getAnioLectivoId())).verificarAbierto();
    }
    if ((extraordinario || obligacion.getTipoConcepto() == TipoConcepto.MATRICULA)
        && pagos.buscarPorPasarelaTransaccionId(p.getPasarelaTransaccionId()).isEmpty()) {
      exigir(r.montoPagado().compareTo(obligacion.saldo()) == 0, "Debe registrar el saldo total");
    }
    if (extraordinario && actor.usuarioId() != null) {
      p.setPayloadWebhook(java.util.Map.of("usuarioCajaId", actor.usuarioId()));
    }
    var respuesta = registrar(p);
    if (extraordinario) {
      comprobantes.emitirAutomaticoParaPago(respuesta.id(), TipoComprobante.BOLETA, "B001");
    }
    return respuesta;
  }

  public PreferenciaMercadoPagoResponseDto crearPreferenciaMercadoPago(CrearPreferenciaMercadoPagoDto r) {
    var o = requerido(obligaciones.buscarPorId(r.obligacionPagoId()));
    autorizarPago(o);
    exigir(
        o.getEstado() == EstadoObligacion.PENDIENTE
            || o.getEstado() == EstadoObligacion.PAGADO_PARCIAL
            || o.getEstado() == EstadoObligacion.VENCIDO,
        "La obligación no está pendiente de pago");
    exigir(o.saldo().compareTo(BigDecimal.ZERO) > 0, "No hay saldo pendiente");
    var concepto = requerido(conceptos.buscarPorId(o.getConceptoId()));
    var descripcion = concepto.getDescripcion() + " - Cuota " + o.getNumeroCuota();
    try {
      return mercadoPagoPort.crearPreferencia(o, descripcion, r.backUrlSuccess(), r.backUrlFailure());
    } catch (com.colegio.shuji.shared.domain.exception.BusinessException ex) {
      if (!mercadoPagoSandbox) {
        throw ex;
      }
      String enlaceSim = "https://sandbox.mercadopago.com.pe/checkout/v1/redirect?pref_id=SIM-OBL-" + o.getId();
      return new PreferenciaMercadoPagoResponseDto("SIM-PREF-OBL-" + o.getId(), enlaceSim, enlaceSim, null);
    }
  }

  public PreferenciaMercadoPagoResponseDto crearPreferenciaAdelantoMercadoPago(
      CobroExtraordinarioRequest.PreferenciaAdelanto r) {
    var ids = r.obligacionPagoIds().stream().distinct().toList();
    exigir(!ids.isEmpty(), "Selecciona al menos una obligación para pagar");
    if (ids.size() == 1) {
      return crearPreferenciaMercadoPago(
          new CrearPreferenciaMercadoPagoDto(ids.getFirst(), r.backUrlSuccess(), r.backUrlFailure()));
    }
    var lista = cargarObligacionesPagables(ids);
    BigDecimal total = lista.stream().map(ObligacionPago::saldo).reduce(BigDecimal.ZERO, BigDecimal::add);
    String referencia = "ADELANTO-" + lista.stream()
        .map(o -> o.getId().toString())
        .collect(java.util.stream.Collectors.joining("-"));
    exigir(referencia.length() <= 250, "Demasiadas cuotas seleccionadas para una sola preferencia");
    String descripcion = "Pago adelantado (" + lista.size() + " cuotas)";
    try {
      return mercadoPagoPort.crearPreferenciaResumen(
          referencia, total, descripcion, r.backUrlSuccess(), r.backUrlFailure());
    } catch (com.colegio.shuji.shared.domain.exception.BusinessException ex) {
      if (!mercadoPagoSandbox) {
        throw ex;
      }
      String enlaceSim = "https://sandbox.mercadopago.com.pe/checkout/v1/redirect?pref_id=" + referencia;
      return new PreferenciaMercadoPagoResponseDto("SIM-PREF-" + referencia, enlaceSim, enlaceSim, null);
    }
  }

  public List<TransaccionResponseDto> simularPagoAdelantoMercadoPago(
      CobroExtraordinarioRequest.SimularPagoAdelanto r) {
    if (!mercadoPagoSandbox) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN,
          "La simulación de pagos solo está habilitada en entorno sandbox");
    }
    exigir(r.metodoPago() != null && r.metodoPago() != MetodoPago.EFECTIVO,
        "Selecciona un medio de pago en línea válido");
    var ids = r.obligacionPagoIds().stream().distinct().toList();
    exigir(!ids.isEmpty(), "Selecciona al menos una obligación para pagar");
    String prefijo = r.referenciaOperacion() != null && !r.referenciaOperacion().isBlank()
        ? r.referenciaOperacion().trim().replaceAll("[^A-Za-z0-9_-]", "")
        : java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    var transacciones = new ArrayList<TransaccionResponseDto>();
    for (Long obligacionId : ids) {
      transacciones.add(simularPagoMercadoPago(obligacionId, r.metodoPago(), prefijo + "-" + obligacionId));
    }
    return transacciones;
  }

  public List<TransaccionResponseDto> confirmarPagoAdelantoMercadoPago(List<Long> obligacionPagoIds) {
    var ids = obligacionPagoIds == null ? List.<Long>of() : obligacionPagoIds.stream().distinct().toList();
    if (ids.isEmpty()) return List.of();
    if (ids.size() == 1) {
      var unico = confirmarPagoMercadoPago(ids.getFirst());
      return unico == null ? List.of() : List.of(unico);
    }
    for (Long id : ids) {
      autorizarPago(requerido(obligaciones.buscarPorId(id)));
    }
    String referencia = "ADELANTO-" + ids.stream()
        .map(String::valueOf)
        .collect(java.util.stream.Collectors.joining("-"));
    return mercadoPagoPort.buscarPagoAprobado(referencia)
        .map(pago -> registrarAdelantoVerificado(PasarelaProveedor.MERCADO_PAGO, pago))
        .orElse(List.of());
  }

  private List<ObligacionPago> cargarObligacionesPagables(List<Long> ids) {
    var lista = new ArrayList<ObligacionPago>();
    for (Long id : ids) {
      var o = requerido(obligaciones.buscarPorId(id));
      autorizarPago(o);
      exigir(
          o.getEstado() == EstadoObligacion.PENDIENTE
              || o.getEstado() == EstadoObligacion.PAGADO_PARCIAL
              || o.getEstado() == EstadoObligacion.VENCIDO,
          "La obligación no está pendiente de pago");
      exigir(o.saldo().compareTo(BigDecimal.ZERO) > 0, "No hay saldo pendiente");
      lista.add(o);
    }
    return lista;
  }

  public TransaccionResponseDto simularPagoMercadoPago(
      Long obligacionId, MetodoPago metodo, String referenciaOperacion) {
    if (!mercadoPagoSandbox) {
      throw new org.springframework.web.server.ResponseStatusException(
          org.springframework.http.HttpStatus.FORBIDDEN,
          "La simulación de pagos solo está habilitada en entorno sandbox");
    }
    exigir(metodo != null && metodo != MetodoPago.EFECTIVO, "Selecciona un medio de pago en línea válido");
    var o = requerido(obligaciones.buscarPorId(obligacionId));
    autorizarPago(o);
    exigir(
        o.getEstado() == EstadoObligacion.PENDIENTE
            || o.getEstado() == EstadoObligacion.PAGADO_PARCIAL
            || o.getEstado() == EstadoObligacion.VENCIDO,
        "La obligación no está pendiente de pago");
    BigDecimal saldo = o.saldo();
    exigir(saldo.compareTo(BigDecimal.ZERO) > 0, "No hay saldo pendiente");
    String sufijo = referenciaOperacion != null && !referenciaOperacion.isBlank()
        ? referenciaOperacion.trim().replaceAll("[^A-Za-z0-9_-]", "")
        : java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    String txId = "SIM-" + metodo.name() + "-" + sufijo;
    var pago = new VerificarPagoPort.PagoVerificado(
        txId.length() > 64 ? txId.substring(0, 64) : txId,
        o.getId(),
        saldo,
        "PEN",
        metodo,
        true,
        o.getId().toString());
    return registrarPagoVerificado(PasarelaProveedor.MERCADO_PAGO, pago);
  }

  public TransaccionResponseDto confirmarPagoMercadoPago(Long obligacionId) {
    var o = requerido(obligaciones.buscarPorId(obligacionId));
    autorizarPago(o);
    return mercadoPagoPort.buscarPagoAprobado(obligacionId.toString())
        .map(pago -> {
          exigir(obligacionId.equals(pago.obligacionPagoId()), "El pago no pertenece a esta obligación");
          if (o.saldo().signum() > 0 && !pagos.buscarPorPasarelaTransaccionId(pago.transaccionId()).isEmpty())
            return null;
          return registrarPagoVerificado(PasarelaProveedor.MERCADO_PAGO, pago);
        }).orElse(null);
  }

  private void autorizarPago(ObligacionPago o) {
    boolean soloApoderado =
        actor.tieneRol("APODERADO")
            && !puedeAdministrarCatalogo();
    if (soloApoderado) {
      var apoderadosLista = apoderados.buscarPorUsuarioId(actor.usuarioId());
      if (apoderadosLista.isEmpty()) {
        throw new org.springframework.security.access.AccessDeniedException(
            "Ficha de apoderado no encontrada");
      }
      var apoderado = apoderadosLista.getFirst();
      var m = requerido(matriculas.buscarPorId(o.getMatriculaId()));
      var vinculos = estudianteApoderados.buscarPorApoderadoId(apoderado.getId());
      boolean autorizado =
          vinculos.stream()
              .anyMatch(
                  v ->
                      v.getEstudianteId().equals(m.getEstudianteId())
                          && Boolean.TRUE.equals(v.getEsResponsableEconomico()));
      if (!autorizado) {
        throw new org.springframework.security.access.AccessDeniedException(
            "Solo el responsable económico puede gestionar el pago");
      }
    }
  }

  public TransaccionResponseDto procesarWebhook(
      PasarelaProveedor proveedor, ProcesarPagoWebhookRequestDto r, String credencial) {
    VerificarPagoPort.PagoVerificado confirmado;
    if (proveedor == PasarelaProveedor.MERCADO_PAGO) {
      confirmado = mercadoPagoPort.consultarPago(r.transaccionId());
      if (matriculasPublicas.esPagoDeSolicitud(confirmado)) {
        matriculasPublicas.procesarWebhook(confirmado);
        return null;
      }
    } else {
      confirmado = verificador.verificar(proveedor, r.transaccionId(), credencial);
    }
    if (!confirmado.aprobado()) return null;
    exigir(confirmado.transaccionId().equals(r.transaccionId()), "Referencia de pago incorrecta");
    if (r.obligacionPagoId() == null
        && confirmado.obligacionPagoId() == null
        && confirmado.referenciaExterna() != null
        && confirmado.referenciaExterna().startsWith("ADELANTO-")) {
      var aplicadas = registrarAdelantoVerificado(proveedor, confirmado);
      return aplicadas.isEmpty() ? null : aplicadas.getLast();
    }
    if (r.obligacionPagoId() != null) {
      exigir(
          confirmado.obligacionPagoId().equals(r.obligacionPagoId()),
          "Referencia de pago incorrecta");
    }
    return registrarPagoVerificado(proveedor, confirmado);
  }

  private List<TransaccionResponseDto> registrarAdelantoVerificado(
      PasarelaProveedor proveedor, VerificarPagoPort.PagoVerificado confirmado) {
    exigir(confirmado.aprobado() && "PEN".equals(confirmado.moneda()), "Pago no aprobado en soles");
    String[] partes = confirmado.referenciaExterna().substring("ADELANTO-".length()).split("-");
    var ids = new ArrayList<Long>();
    for (String parte : partes) {
      if (!parte.isBlank()) {
        try {
          ids.add(Long.valueOf(parte));
        } catch (NumberFormatException ex) {
          throw new com.colegio.shuji.shared.domain.exception.BusinessException("Referencia de adelanto inválida");
        }
      }
    }
    exigir(!ids.isEmpty(), "Referencia de adelanto vacía");
    var respuestas = new ArrayList<TransaccionResponseDto>();
    var porPagar = new ArrayList<ObligacionPago>();
    BigDecimal sumaPendiente = BigDecimal.ZERO;
    for (Long id : ids.stream().distinct().toList()) {
      String subTx = (confirmado.transaccionId() + "-" + id);
      if (subTx.length() > 64) subTx = subTx.substring(0, 64);
      var previo = pagos.buscarPorPasarelaTransaccionId(subTx);
      if (!previo.isEmpty()) {
        respuestas.add(mapper.toResponse(previo.getFirst()));
        continue;
      }
      var obligacion = requerido(obligaciones.bloquearPorId(id));
      porPagar.add(obligacion);
      sumaPendiente = sumaPendiente.add(obligacion.saldo());
    }
    if (!porPagar.isEmpty() && respuestas.isEmpty()) {
      exigir(sumaPendiente.compareTo(confirmado.monto()) == 0,
          "El importe pagado no coincide con el total de las cuotas seleccionadas");
    }
    for (var obligacion : porPagar) {
      String subTx = (confirmado.transaccionId() + "-" + obligacion.getId());
      if (subTx.length() > 64) subTx = subTx.substring(0, 64);
      var subPago = new VerificarPagoPort.PagoVerificado(
          subTx,
          obligacion.getId(),
          obligacion.saldo(),
          confirmado.moneda(),
          confirmado.metodo(),
          true,
          obligacion.getId().toString());
      respuestas.add(registrarPagoVerificado(proveedor, subPago));
    }
    return respuestas;
  }

  private TransaccionResponseDto registrarPagoVerificado(PasarelaProveedor proveedor, VerificarPagoPort.PagoVerificado confirmado) {
    exigir(confirmado.obligacionPagoId() != null, "Referencia de obligación inválida");
    exigir(confirmado.aprobado() && "PEN".equals(confirmado.moneda()), "Pago no aprobado en soles");
    var p =
        PagoTransaccion.builder()
            .obligacionPagoId(confirmado.obligacionPagoId())
            .pasarelaTransaccionId(confirmado.transaccionId())
            .pasarelaProveedor(proveedor)
            .montoPagado(confirmado.monto())
            .metodoPago(confirmado.metodo())
            .build();
    var respuesta = registrar(p);
    comprobantes.emitirAutomaticoParaPago(respuesta.id(), TipoComprobante.BOLETA, "B001");
    return respuesta;
  }

  public TransaccionResponseDto revertir(RevertirPagoRequestDto r) {
    var snapshot = requerido(pagos.buscarPorId(r.pagoId()));
    requerido(obligaciones.bloquearPorId(snapshot.getObligacionPagoId()));
    var p = requerido(pagos.bloquearPorId(r.pagoId()));
    if (p.getEstadoPago() == EstadoPago.REVERTIDO) return mapper.toResponse(p);
    p.revertir(r.motivo());
    return mapper.toResponse(pagos.guardar(p));
  }
}
