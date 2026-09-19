package com.colegio.shuji.tesoreria.application.dto.out;

import com.colegio.shuji.tesoreria.domain.enums.EstadoObligacion;
import com.colegio.shuji.tesoreria.domain.enums.TipoConcepto;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ObligacionResponseDto(
    Long id,
    Long matriculaId,
    Short conceptoId,
    TipoConcepto tipoConcepto,
    Short numeroCuota,
    String descripcion,
    LocalDate fechaVencimiento,
    BigDecimal montoBase,
    BigDecimal montoMora,
    BigDecimal montoDescuento,
    BigDecimal totalPagado,
    BigDecimal saldoPendiente,
    EstadoObligacion estado) {}
