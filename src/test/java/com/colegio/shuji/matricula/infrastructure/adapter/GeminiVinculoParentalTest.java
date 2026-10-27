package com.colegio.shuji.matricula.infrastructure.adapter;

import static org.junit.jupiter.api.Assertions.*;
import com.colegio.shuji.matricula.application.port.out.GeminiDocumentoMatriculaPort.ContextoParental;
import com.colegio.shuji.matricula.domain.enums.Parentesco;
import com.colegio.shuji.matricula.domain.enums.TipoDocumentoSolicitud;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class GeminiVinculoParentalTest {
  final ObjectMapper json = new ObjectMapper();
  final GeminiDocumentoMatriculaAdapter adapter = new GeminiDocumentoMatriculaAdapter("", "", "prueba", "", json);

  private com.fasterxml.jackson.databind.node.ObjectNode partida() {
    return json.createObjectNode().put("tipoCorrecto", true).put("legible", true).put("identidadCoincide", true)
        .put("nombreMadre", "Rosa Pérez López").put("dniMadre", "87654321")
        .put("nombrePadre", "Luis Pérez Ruiz").put("dniPadre", "01234567");
  }

  @Test void madreCoincideConNombreYDocumentoPresentes() {
    assertTrue(adapter.interpretar(partida(), TipoDocumentoSolicitud.PARTIDA_NACIMIENTO,
        new ContextoParental("ROSA PEREZ LOPEZ", "87654321", Parentesco.MADRE)).vinculoParentalCoincide());
  }

  @Test void noConfundeAlPadreConLaMadreAunqueAmbosFigurenEnLaPartida() {
    assertFalse(adapter.interpretar(partida(), TipoDocumentoSolicitud.PARTIDA_NACIMIENTO,
        new ContextoParental("Rosa Pérez López", "87654321", Parentesco.PADRE)).vinculoParentalCoincide());
  }

  @Test void dniVisibleDistintoImpideAprobarElVinculo() {
    assertFalse(adapter.interpretar(partida(), TipoDocumentoSolicitud.PARTIDA_NACIMIENTO,
        new ContextoParental("Rosa Pérez López", "87654322", Parentesco.MADRE)).vinculoParentalCoincide());
  }

  @Test void nombreNoLegibleNoSeInventaPorElContextoDeclarado() {
    var datos = partida().put("nombreMadre", "");
    assertFalse(adapter.interpretar(datos, TipoDocumentoSolicitud.PARTIDA_NACIMIENTO,
        new ContextoParental("Rosa Pérez López", "87654321", Parentesco.MADRE)).vinculoParentalCoincide());
  }

  @Test void partidaSinDniImpresaContrastaNombreMientrasLaApiConfirmaLaIdentidad() {
    var datos = partida().put("dniMadre", "");
    assertTrue(adapter.interpretar(datos, TipoDocumentoSolicitud.PARTIDA_NACIMIENTO,
        new ContextoParental("Rosa Pérez López", "87654321", Parentesco.MADRE)).vinculoParentalCoincide());
  }

  @Test void noDeduceParentescoParaUnTutor() {
    assertFalse(adapter.interpretar(partida(), TipoDocumentoSolicitud.PARTIDA_NACIMIENTO,
        new ContextoParental("Rosa Pérez López", "87654321", Parentesco.TUTOR_LEGAL)).vinculoParentalCoincide());
  }
}
