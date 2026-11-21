package com.colegio.shuji.tesoreria.application.dto.in;

import com.colegio.shuji.tesoreria.domain.enums.MetodoPago;
import com.colegio.shuji.tesoreria.domain.enums.TipoConcepto;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class CobroExtraordinarioRequest {
  private CobroExtraordinarioRequest() {}

  public record Concepto(
      @NotBlank @Size(max = 50) String nombre,
      @NotNull TipoConcepto tipo,
      @NotNull @DecimalMin("0.01") @Digits(integer = 8, fraction = 2) BigDecimal monto,
      @NotNull Boolean activo,
      LocalDate vencimiento) {
    public Concepto(String nombre, TipoConcepto tipo, BigDecimal monto, Boolean activo) {
      this(nombre, tipo, monto, activo, null);
    }
  }

  public record Asignacion(
      @NotNull @Positive Short conceptoId,
      @NotEmpty @Size(max = 200) List<@NotNull @Positive Long> matriculaIds,
      @NotNull LocalDate vencimiento) {}

  public record Pension(@NotNull @Min(1) @Max(28) Short diaVencimiento) {}

  public record PreferenciaAdelanto(
      @NotEmpty @Size(max = 60) List<@NotNull @Positive Long> obligacionPagoIds,
      String backUrlSuccess,
      String backUrlFailure) {}

  public record SimularPagoAdelanto(
      @NotEmpty @Size(max = 60) List<@NotNull @Positive Long> obligacionPagoIds,
      @NotNull MetodoPago metodoPago,
      String referenciaOperacion) {}

  public record ConfirmarAdelanto(
      @NotEmpty @Size(max = 60) List<@NotNull @Positive Long> obligacionPagoIds) {}
}
