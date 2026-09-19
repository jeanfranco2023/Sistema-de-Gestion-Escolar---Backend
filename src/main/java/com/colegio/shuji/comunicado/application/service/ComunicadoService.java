package com.colegio.shuji.comunicado.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.academico.application.port.out.SeccionRepositoryPort;
import com.colegio.shuji.comunicado.application.dto.in.ConfirmarAcuseReciboRequestDto;
import com.colegio.shuji.comunicado.application.dto.in.EmitirComunicadoRequestDto;
import com.colegio.shuji.comunicado.application.dto.out.BandejaApoderadoResponseDto;
import com.colegio.shuji.comunicado.application.dto.out.ComunicadoDestinatarioResponseDto;
import com.colegio.shuji.comunicado.application.dto.out.ComunicadoResponseDto;
import com.colegio.shuji.comunicado.application.dto.out.MetricasLecturaResponseDto;
import com.colegio.shuji.comunicado.application.mapper.ComunicadoMapper;
import com.colegio.shuji.comunicado.application.port.in.ConfirmarLecturaUseCase;
import com.colegio.shuji.comunicado.application.port.in.ConsultarBandejaUseCase;
import com.colegio.shuji.comunicado.application.port.in.PublicarComunicadoUseCase;
import com.colegio.shuji.comunicado.application.port.out.ComunicadoRepositoryPort;
import com.colegio.shuji.comunicado.application.port.out.DestinatarioRepositoryPort;
import com.colegio.shuji.comunicado.domain.model.ComunicadoDestinatario;
import com.colegio.shuji.matricula.application.port.out.ApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.academico.domain.model.Seccion;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.model.Apoderado;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ComunicadoService
    implements PublicarComunicadoUseCase, ConfirmarLecturaUseCase, ConsultarBandejaUseCase {
  private final ComunicadoMapper mapper;
  private final ComunicadoRepositoryPort comunicados;
  private final DestinatarioRepositoryPort destinatarios;
  private final ApoderadoRepositoryPort apoderados;
  private final EstudianteApoderadoRepositoryPort vinculos;
  private final MatriculaRepositoryPort matriculas;
  private final SeccionRepositoryPort secciones;
  private final AnioLectivoRepositoryPort anios;
  private final ActorActualPort actor;

  public ComunicadoResponseDto publicar(EmitirComunicadoRequestDto r) {
    requerido(anios.buscarPorId(r.anioLectivoId()));
    if (r.seccionId() != null) {
      var s = requerido(secciones.buscarPorId(r.seccionId()));
      exigir(
          s.getAnioLectivoId().equals(r.anioLectivoId())
              && (r.nivelId() == null || s.getNivelId().equals(r.nivelId())),
          "Filtros de destinatarios incompatibles");
    }
    var c = mapper.toDomain(r);
    c.publicar(actor.usuarioId());
    c = comunicados.guardar(c);
    var ids = new TreeSet<Long>();
    var seccionesCache = new HashMap<Integer, Seccion>();
    for (var m : matriculas.buscarPorAnioLectivoId(r.anioLectivoId())) {
      if (m.getEstadoMatricula() != EstadoMatricula.MATRICULADO
          || (r.seccionId() != null && !m.getSeccionId().equals(r.seccionId()))) continue;
      var s =
          seccionesCache.computeIfAbsent(
              m.getSeccionId(), id -> requerido(secciones.buscarPorId(id)));
      if (r.nivelId() != null && !s.getNivelId().equals(r.nivelId())) continue;
      vinculos.buscarPorEstudianteId(m.getEstudianteId()).stream()
          .filter(v -> Boolean.TRUE.equals(v.getTieneCustodia()))
          .forEach(v -> ids.add(v.getApoderadoId()));
    }
    exigir(!ids.isEmpty(), "No hay destinatarios para el filtro seleccionado");
    for (Long id : ids) {
      var d =
          ComunicadoDestinatario.builder()
              .comunicadoId(c.getId())
              .apoderadoId(id)
              .leido(false)
              .acuseConfirmado(false)
              .build();
      destinatarios.guardar(d);
    }
    return mapper.toResponse(c);
  }

  private Apoderado apoderadoActual() {
    return apoderados.buscarPorUsuarioId(actor.usuarioId()).stream()
        .findFirst()
        .orElseThrow(
            () ->
                new com.colegio.shuji.shared.domain.exception.BusinessException(
                    "Usuario sin ficha de apoderado"));
  }

  @Transactional(readOnly = true)
  public BandejaApoderadoResponseDto bandeja() {
    var a = apoderadoActual();
    return new BandejaApoderadoResponseDto(
        a.getId(),
        destinatarios.buscarPorApoderadoId(a.getId()).stream()
            .map(d -> mapper.toResponse(requerido(comunicados.buscarPorId(d.getComunicadoId()))))
            .toList());
  }

  public ComunicadoDestinatarioResponseDto confirmar(
      Long comunicadoId, ConfirmarAcuseReciboRequestDto r) {
    var a = apoderadoActual();
    var snapshot =
        destinatarios.buscarPorApoderadoId(a.getId()).stream()
            .filter(d -> d.getComunicadoId().equals(comunicadoId))
            .findFirst()
            .orElseThrow(
                () ->
                    new com.colegio.shuji.comunicado.domain.exception
                        .ComunicadoNoEncontradoException(
                        "Comunicado no disponible para este apoderado"));
    var d = requerido(destinatarios.bloquearPorId(snapshot.getId()));
    var c = requerido(comunicados.buscarPorId(comunicadoId));
    exigir(
        !r.confirmarAcuse() || Boolean.TRUE.equals(c.getRequiereAcuse()),
        "El comunicado no requiere acuse");
    d.confirmarLectura(OffsetDateTime.now(), r.confirmarAcuse());
    return mapper.toResponse(destinatarios.guardar(d));
  }

  @Transactional(readOnly = true)
  public MetricasLecturaResponseDto metricas(Long id) {
    requerido(comunicados.buscarPorId(id));
    var lista = destinatarios.buscarPorComunicadoId(id);
    return new MetricasLecturaResponseDto(
        id,
        lista.size(),
        lista.stream().filter(d -> Boolean.TRUE.equals(d.getLeido())).count(),
        lista.stream().filter(d -> Boolean.TRUE.equals(d.getAcuseConfirmado())).count());
  }
}
