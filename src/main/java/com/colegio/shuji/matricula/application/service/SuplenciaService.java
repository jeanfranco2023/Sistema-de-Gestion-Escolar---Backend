package com.colegio.shuji.matricula.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;
import com.colegio.shuji.matricula.application.dto.in.CrearSuplenciaRequestDto;
import com.colegio.shuji.matricula.application.dto.out.SuplenciaResponseDto;
import com.colegio.shuji.matricula.application.port.in.GestionarSuplenciasUseCase;
import com.colegio.shuji.matricula.application.port.out.ApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.SuplenciaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoSuplencia;
import com.colegio.shuji.matricula.domain.model.Suplencia;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor @Transactional
public class SuplenciaService implements GestionarSuplenciasUseCase {
  private final SuplenciaRepositoryPort suplencias;
  private final ApoderadoRepositoryPort apoderados;
  private final EstudianteApoderadoRepositoryPort vinculos;
  private final EstudianteRepositoryPort estudiantes;
  private final ActorActualPort actor;
  private LocalDate hoy() { return LocalDate.now(ZoneId.of("America/Lima")); }
  private void gestor() { if (!actor.tieneRol("DIRECCION") && !actor.tieneRol("SECRETARIA")) throw new AccessDeniedException("Solo Dirección o Secretaría puede resolver suplencias"); }
  private com.colegio.shuji.matricula.domain.model.Apoderado titular() { return apoderados.buscarPorUsuarioId(actor.usuarioId()).stream().findFirst()
      .orElseThrow(() -> new AccessDeniedException("No tienes una ficha de apoderado vinculada")); }

  public SuplenciaResponseDto solicitar(CrearSuplenciaRequestDto r) {
    if (!actor.tieneRol("APODERADO")) throw new AccessDeniedException("La solicitud debe registrarla el apoderado titular");
    var padre = titular(); Long titular = padre.getId();
    var estudiante = requerido(estudiantes.bloquearPorIds(List.of(r.estudianteId())).stream().findFirst());
    boolean autorizado = vinculos.buscarPorApoderadoId(titular).stream().anyMatch(v -> v.getEstudianteId().equals(r.estudianteId())
        && Boolean.TRUE.equals(v.getTieneCustodia()) && Boolean.TRUE.equals(v.getPermiteRecojo()));
    if (!autorizado) throw new AccessDeniedException("No tienes custodia y permiso de recojo para este estudiante");
    exigir(Boolean.TRUE.equals(estudiante.getActivo()), "El estudiante no está activo");
    exigir(r.fechaInicio() != null && r.fechaFin() != null && r.isPeriodoValido(), "El periodo solicitado no es válido");
    exigir(!r.numeroDocumento().equals(padre.getNumeroDocumento()) && !r.numeroDocumento().equals(estudiante.getNumeroDocumento()),
        "El suplente debe ser una persona distinta del titular y del estudiante");
    var solicitud = Suplencia.builder().estudianteId(r.estudianteId()).apoderadoTitularId(titular)
        .numeroDocumento(r.numeroDocumento()).nombres(r.nombres().strip()).apellidoPaterno(r.apellidoPaterno().strip())
        .apellidoMaterno(r.apellidoMaterno().strip()).parentesco(r.parentesco().strip()).celular(r.celular())
        .fechaInicio(r.fechaInicio()).fechaFin(r.fechaFin()).estado(EstadoSuplencia.PENDIENTE_APROBACION)
        .fechaSolicitud(OffsetDateTime.now(ZoneOffset.UTC)).build();
    return respuesta(suplencias.guardar(solicitud));
  }
  @Transactional(readOnly=true)
  public List<SuplenciaResponseDto> misSolicitudes() { return suplencias.porTitular(titular().getId()).stream().map(this::respuesta).toList(); }
  @Transactional(readOnly=true)
  public List<SuplenciaResponseDto> bandeja() { gestor(); return suplencias.listar().stream().map(this::respuesta).toList(); }
  public SuplenciaResponseDto aprobar(Long id) {
    gestor(); var s = requerido(suplencias.bloquearPorId(id));
    var estudiante = requerido(estudiantes.bloquearPorIds(List.of(s.getEstudianteId())).stream().findFirst());
    exigir(Boolean.TRUE.equals(estudiante.getActivo()), "El estudiante no está activo");
    exigir(vinculos.buscarPorApoderadoId(s.getApoderadoTitularId()).stream().anyMatch(v ->
        v.getEstudianteId().equals(s.getEstudianteId()) && Boolean.TRUE.equals(v.getTieneCustodia()) && Boolean.TRUE.equals(v.getPermiteRecojo())),
        "El titular ya no tiene custodia o permiso de recojo");
    exigir(!suplencias.hayConflicto(s.getEstudianteId(),s.getFechaInicio(),s.getFechaFin(),s.getId()), "Existe una suplencia aprobada para ese periodo");
    s.aprobar(actor.usuarioId(),hoy()); return respuesta(suplencias.guardar(s));
  }
  public SuplenciaResponseDto rechazar(Long id,String motivo) { gestor(); var s=requerido(suplencias.bloquearPorId(id));s.rechazar(actor.usuarioId(),motivo);return respuesta(suplencias.guardar(s)); }
  public SuplenciaResponseDto revocar(Long id,String motivo) { gestor(); var s=requerido(suplencias.bloquearPorId(id));s.revocar(actor.usuarioId(),motivo);return respuesta(suplencias.guardar(s)); }
  private SuplenciaResponseDto respuesta(Suplencia s) { return new SuplenciaResponseDto(s.getId(),s.getEstudianteId(),s.getApoderadoTitularId(),s.getNumeroDocumento(),s.getNombres(),s.getApellidoPaterno(),s.getApellidoMaterno(),s.getParentesco(),s.getCelular(),s.getFechaInicio(),s.getFechaFin(),s.estadoVisible(hoy()),s.getMotivoResolucion(),s.getAutorizadoPorUsuarioId(),s.getFechaSolicitud(),s.getFechaResolucion()); }
}
