package com.colegio.shuji.matricula.application.service;

import com.colegio.shuji.matricula.application.port.out.ReniecServicePort;
import java.text.Normalizer;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Contrasta el DNI y nombres declarados contra el proveedor configurado de consulta. */
@Service
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@RequiredArgsConstructor
public class VerificacionIdentidadMatriculaService {
  public enum Resultado { COINCIDE, NO_COINCIDE, NO_DISPONIBLE }
  private final ReniecServicePort consulta;

  public Resultado verificar(String nombresDeclarados, String dni) {
    if (dni == null || !dni.matches("[0-9]{8}")) return Resultado.NO_COINCIDE;
    var respuesta = consulta.consultar(dni);
    if (respuesta.isEmpty()) return Resultado.NO_DISPONIBLE;
    var identidad = respuesta.get();
    String nombre = identidad.nombres() + " " + identidad.apellidoPaterno() + " " + identidad.apellidoMaterno();
    return dni.equals(identidad.numeroDocumento()) && normalizar(nombre).equals(normalizar(nombresDeclarados))
        ? Resultado.COINCIDE : Resultado.NO_COINCIDE;
  }

  private String normalizar(String valor) {
    if (valor == null) return "";
    return Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
        .replaceAll("[^\\p{L}\\p{N}]", "").toUpperCase(Locale.ROOT);
  }
}
