package com.colegio.shuji.matricula.application.dto.in;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.colegio.shuji.matricula.domain.enums.Genero;
import com.colegio.shuji.matricula.domain.enums.ModalidadIngreso;
import com.colegio.shuji.matricula.domain.enums.Parentesco;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class CrearSolicitudMatriculaPublicaRequestDtoTest {
  private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void aceptaFechaDeNacimientoAnteriorAlDiaActualEnPeru() {
    var request = request(LocalDate.now(ZoneId.of("America/Lima")).minusDays(1));

    assertTrue(VALIDATOR.validate(request).isEmpty());
  }

  @Test
  void rechazaFechaDeNacimientoDeHoyOFuturaEnPeru() {
    var hoy = LocalDate.now(ZoneId.of("America/Lima"));

    assertFalse(VALIDATOR.validate(request(hoy)).isEmpty());
    assertFalse(VALIDATOR.validate(request(hoy.plusDays(1))).isEmpty());
  }

  private CrearSolicitudMatriculaPublicaRequestDto request(LocalDate nacimiento) {
    return request(nacimiento, Parentesco.MADRE);
  }

  @Test
  void soloAdmitePadreOMadreEnLaSolicitudPublica() {
    var nacimiento = LocalDate.of(2018, 1, 1);
    assertTrue(VALIDATOR.validate(request(nacimiento, Parentesco.PADRE)).isEmpty());
    for (var parentesco : new Parentesco[]{Parentesco.TUTOR_LEGAL, Parentesco.ABUELO_A, Parentesco.OTRO}) {
      assertFalse(VALIDATOR.validate(request(nacimiento, parentesco)).isEmpty());
    }
  }

  @Test
  void requiereElCorreoDelApoderadoParaEnviarElSeguimiento() {
    var nacimiento = LocalDate.of(2018, 1, 1);
    assertFalse(VALIDATOR.validate(request(nacimiento, Parentesco.MADRE, null)).isEmpty());
    assertFalse(VALIDATOR.validate(request(nacimiento, Parentesco.MADRE, " ")).isEmpty());
  }

  @Test
  void exigeColegioProcedenciaCuandoEsIngresoPorTraslado() {
    var nacimiento = LocalDate.of(2018, 1, 1);
    var sinColegio = request(nacimiento, Parentesco.MADRE, "rosa@example.test", ModalidadIngreso.TRASLADO, "  ");
    var conColegio = request(nacimiento, Parentesco.MADRE, "rosa@example.test", ModalidadIngreso.TRASLADO, "I.E. Santa Rosa");

    assertFalse(VALIDATOR.validate(sinColegio).isEmpty());
    assertTrue(VALIDATOR.validate(conColegio).isEmpty());
  }

  private CrearSolicitudMatriculaPublicaRequestDto request(LocalDate nacimiento, Parentesco parentesco) {
    return request(nacimiento, parentesco, "rosa@example.test");
  }

  private CrearSolicitudMatriculaPublicaRequestDto request(
      LocalDate nacimiento, Parentesco parentesco, String email) {
    return request(nacimiento, parentesco, email, ModalidadIngreso.REGULAR, null);
  }

  private CrearSolicitudMatriculaPublicaRequestDto request(
      LocalDate nacimiento,
      Parentesco parentesco,
      String email,
      ModalidadIngreso modalidad,
      String colegioProcedencia) {
    return new CrearSolicitudMatriculaPublicaRequestDto(
        (short) 2026,
        (short) 1,
        "12345678",
        "Ana María",
        "Pérez",
        "López",
        nacimiento,
        Genero.F,
        "87654321",
        "Rosa",
        "Pérez",
        "López",
        "912345678",
        email,
        "Av. Ejemplo 123",
        "150101",
        parentesco,
        true,
        modalidad,
        colegioProcedencia);
  }
}
