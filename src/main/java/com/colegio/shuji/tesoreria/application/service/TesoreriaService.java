package com.colegio.shuji.tesoreria.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.ApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import com.colegio.shuji.tesoreria.application.dto.in.CrearPreferenciaMercadoPagoDto;
import com.colegio.shuji.tesoreria.application.dto.in.GenerarObligacionesAnualesRequestDto;
import com.colegio.shuji.tesoreria.application.dto.in.ProcesarPagoWebhookRequestDto;
import com.colegio.shuji.tesoreria.application.dto.in.RegistrarPagoCajaRequestDto;
import com.colegio.shuji.tesoreria.application.dto.in.RevertirPagoRequestDto;
import com.colegio.shuji.tesoreria.application.dto.out.EstadoCuentaEstudianteResponseDto;
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
import java.math.BigDecimal;
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
  private final AnioLectivoRepositoryPort anios;
  private final VerificarPagoPort verificador;
  private final MercadoPagoPort mercadoPagoPort;
  private final EmitirComprobanteUseCase comprobantes;
  private final ActorActualPort actor;
  private final ApoderadoRepositoryPort apoderados;
  private final EstudianteApoderadoRepositoryPort estudianteApoderados;

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
    return result;
  }

  @Transactional(readOnly = true)
  public List<ObligacionResponseDto> obligacionesMatricula(Long id) {
    return obligaciones.buscarPorMatriculaId(id).stream().map(mapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public EstadoCuentaEstudianteResponseDto estadoCuenta(Long estudianteId) {
    var matriculaIds =
        matriculas.buscarPorEstudianteId(estudianteId).stream().map(m -> m.getId()).toList();
    var deudas = obligaciones.buscarPorMatriculaIds(matriculaIds);
    return new EstadoCuentaEstudianteResponseDto(
        estudianteId,
        deudas.stream().map(mapper::toResponse).toList(),
        deudas.stream()
            .filter(o -> o.getEstado() != EstadoObligacion.CANCELADO_POR_TRASLADO)
            .map(ObligacionPago::saldo)
            .reduce(BigDecimal.ZERO, BigDecimal::add));
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
    return registrar(p);
  }

  public PreferenciaMercadoPagoResponseDto crearPreferenciaMercadoPago(CrearPreferenciaMercadoPagoDto r) {
    var o = requerido(obligaciones.buscarPorId(r.obligacionPagoId()));
    boolean soloApoderado =
        actor.tieneRol("APODERADO")
            && !actor.tieneRol("DIRECCION")
            && !actor.tieneRol("SECRETARIA");
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
    exigir(
        o.getEstado() == EstadoObligacion.PENDIENTE || o.getEstado() == EstadoObligacion.VENCIDO,
        "La obligación no está pendiente de pago");
    exigir(o.saldo().compareTo(BigDecimal.ZERO) > 0, "No hay saldo pendiente");
    var concepto = requerido(conceptos.buscarPorId(o.getConceptoId()));
    var descripcion = concepto.getDescripcion() + " - Cuota " + o.getNumeroCuota();
    return mercadoPagoPort.crearPreferencia(o, descripcion, r.backUrlSuccess(), r.backUrlFailure());
  }

  public TransaccionResponseDto procesarWebhook(
      PasarelaProveedor proveedor, ProcesarPagoWebhookRequestDto r, String credencial) {
    VerificarPagoPort.PagoVerificado confirmado;
    if (proveedor == PasarelaProveedor.MERCADO_PAGO) {
      confirmado = mercadoPagoPort.consultarPago(r.transaccionId());
    } else {
      confirmado = verificador.verificar(proveedor, r.transaccionId(), credencial);
    }
    if (r.obligacionPagoId() != null) {
      exigir(
          confirmado.obligacionPagoId().equals(r.obligacionPagoId()),
          "Referencia de pago incorrecta");
    }
    exigir(confirmado.transaccionId().equals(r.transaccionId()), "Referencia de pago incorrecta");
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
