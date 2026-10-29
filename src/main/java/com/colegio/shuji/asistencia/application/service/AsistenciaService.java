package com.colegio.shuji.asistencia.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.asistencia.application.dto.in.ImportarLoteBiometricoRequestDto;
import com.colegio.shuji.asistencia.application.dto.in.RegistrarAsistenciaAulaRequestDto;
import com.colegio.shuji.asistencia.application.dto.out.AsistenciaAulaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.DocenteAsistenciaDiariaResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.DocenteMatriculaAsistenciaDto;
import com.colegio.shuji.asistencia.application.dto.out.LoteBiometricoResponseDto;
import com.colegio.shuji.asistencia.application.dto.out.ReporteAsistenciaDiariaResponseDto;
import com.colegio.shuji.asistencia.application.mapper.AsistenciaMapper;
import com.colegio.shuji.asistencia.application.port.in.ProcesarBiometricoUseCase;
import com.colegio.shuji.asistencia.application.port.in.RegistrarListaAulaUseCase;
import com.colegio.shuji.asistencia.application.port.out.AsistenciaAulaRepositoryPort;
import com.colegio.shuji.asistencia.application.port.out.BiometricoRepositoryPort;
import com.colegio.shuji.asistencia.application.port.out.MarcaPorteriaRepositoryPort;
import com.colegio.shuji.asistencia.domain.enums.EstadoAsistenciaAula;
import com.colegio.shuji.asistencia.domain.enums.EstadoMarca;
import com.colegio.shuji.asistencia.domain.model.LoteBiometrico;
import com.colegio.shuji.asistencia.domain.model.MarcaPorteria;
import com.colegio.shuji.academico.application.port.out.AnioLectivoRepositoryPort;
import com.colegio.shuji.curriculo.application.port.out.AsignacionRepositoryPort;
import com.colegio.shuji.matricula.domain.model.Matricula;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.MatriculaRepositoryPort;
import com.colegio.shuji.matricula.domain.enums.EstadoMatricula;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import com.colegio.shuji.shared.application.port.out.ActorActualPort;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AsistenciaService implements ProcesarBiometricoUseCase, RegistrarListaAulaUseCase {
  private final AsistenciaMapper mapper;
  private final BiometricoRepositoryPort lotes;
  private final MarcaPorteriaRepositoryPort marcas;
  private final EstudianteRepositoryPort estudiantes;
  private final MatriculaRepositoryPort matriculas;
  private final AsistenciaAulaRepositoryPort asistencias;
  private final AsignacionRepositoryPort asignaciones;
  private final AnioLectivoRepositoryPort anios;
  private final ActorActualPort actor;

  public LoteBiometricoResponseDto importar(ImportarLoteBiometricoRequestDto r) {
    String hashContenido = calcularHash(r);
    var loteExistente = lotes.buscarPorHashContenido(hashContenido);
    if (loteExistente.isPresent()) return mapper.toResponse(loteExistente.get());
    var lote =
        LoteBiometrico.builder()
            .nombreArchivo(r.nombreArchivo())
            .hashContenido(hashContenido)
            .totalFilas(r.marcas().size())
            .marcasValidas(0)
            .marcasErroneas(0)
            .importadoPorUsuarioId(actor.usuarioId())
            .build();
    lote = lotes.guardar(lote);
    int validas = 0, errores = 0;
    var vistas = new HashSet<String>();
    var estudiantesPorDni =
        estudiantes
            .buscarPorNumerosDocumento(
                r.marcas().stream().map(f -> f.dniLeido()).distinct().toList())
            .stream()
            .filter(e -> e.getTipoDocumento() == TipoDocumento.DNI)
            .collect(Collectors.toMap(e -> e.getNumeroDocumento(), Function.identity(), (a, b) -> a));
    var estudiantesBloqueados =
        estudiantes.bloquearPorIds(
            estudiantesPorDni.values().stream().map(e -> e.getId()).distinct().toList());
    var estudiantesPorId =
        estudiantesBloqueados.stream()
            .collect(Collectors.toMap(e -> e.getId(), Function.identity()));
    estudiantesPorDni.replaceAll((dni, e) -> estudiantesPorId.getOrDefault(e.getId(), e));

    var inicio = r.marcas().stream().map(f -> f.fechaHora()).min(java.util.Comparator.naturalOrder());
    var fin = r.marcas().stream().map(f -> f.fechaHora()).max(java.util.Comparator.naturalOrder());
    var existentes = new HashSet<String>();
    if (inicio.isPresent() && fin.isPresent()) {
      marcas.buscarPorRangoFecha(inicio.get(), fin.get().plusNanos(1)).stream()
          .filter(m -> m.getEstudianteId() != null)
          .map(
              m ->
                  m.getEstudianteId()
                      + "/"
                      + m.getFechaHora().toInstant()
                      + "/"
                      + m.getDispositivoCodigo())
          .forEach(existentes::add);
    }
    List<MarcaPorteria> paraGuardar = new ArrayList<>(r.marcas().size());
    for (var fila : r.marcas()) {
      var marca =
          MarcaPorteria.builder()
              .loteId(lote.getId())
              .dniLeido(fila.dniLeido())
              .fechaHora(fila.fechaHora())
              .dispositivoCodigo(fila.dispositivoCodigo())
              .build();
      var est = java.util.Optional.ofNullable(estudiantesPorDni.get(fila.dniLeido()));
      String clave =
          fila.dniLeido() + "/" + fila.fechaHora().toInstant() + "/" + fila.dispositivoCodigo();
      boolean duplicada = !vistas.add(clave);
      if (est.isPresent()) {
        marca.identificar(est.get().getId());
        String claveExistente =
            est.get().getId()
                + "/"
                + fila.fechaHora().toInstant()
                + "/"
                + fila.dispositivoCodigo();
        duplicada = duplicada || existentes.contains(claveExistente);
      }
      if (duplicada) marca.marcarDuplicado();
      else if (est.isEmpty()) marca.marcarDniNoIdentificado();
      if (marca.getEstadoProcesamiento() == EstadoMarca.PENDIENTE) validas++;
      else errores++;
      paraGuardar.add(marca);
    }
    marcas.guardarTodos(paraGuardar);
    lote.registrarResultado(validas, errores);
    return mapper.toResponse(lotes.guardar(lote));
  }

  private String calcularHash(ImportarLoteBiometricoRequestDto request) {
    try {
      var digest = MessageDigest.getInstance("SHA-256");
      for (var marca : request.marcas()) {
        String fila =
            marca.dniLeido()
                + '|'
                + marca.fechaHora().toInstant()
                + '|'
                + marca.dispositivoCodigo()
                + '\n';
        digest.update(fila.getBytes(StandardCharsets.UTF_8));
      }
      return java.util.HexFormat.of().formatHex(digest.digest());
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 no disponible", ex);
    }
  }

  public AsistenciaAulaResponseDto registrar(RegistrarAsistenciaAulaRequestDto r) {
    var m = requerido(matriculas.bloquearPorId(r.matriculaId()));
    if (actor.tieneRol("DOCENTE") || actor.tieneRol("TUTOR")) exigirAccesoDocente(m);
    exigir(m.getEstadoMatricula() == EstadoMatricula.MATRICULADO, "Matrícula no activa");
    if (asistencias.buscarPorMatriculaId(r.matriculaId()).stream()
        .anyMatch(a -> a.getFechaSesion().equals(r.fechaSesion())))
      throw new com.colegio.shuji.asistencia.domain.exception.AsistenciaYaRegistradaException(
          "Ya existe asistencia para esa fecha");
    exigir(
        r.estado() != EstadoAsistenciaAula.FALTA_JUSTIFICADA,
        "Registre la falta y luego su justificación");
    var a = mapper.toDomain(r);
    a.registrarPor(actor.usuarioId());
    return mapper.toResponse(asistencias.guardar(a));
  }

  public AsistenciaAulaResponseDto justificar(Long asistenciaId, String motivo, String documentoSustentoKey) {
    var a = requerido(asistencias.bloquearPorId(asistenciaId));
    if (actor.tieneRol("DOCENTE") || actor.tieneRol("TUTOR")) {
      exigirAccesoDocente(requerido(matriculas.bloquearPorId(a.getMatriculaId())));
    }
    a.justificar(motivo, documentoSustentoKey);
    return mapper.toResponse(asistencias.guardar(a));
  }

  @Transactional(readOnly = true)
  public String documentoSustentoKey(Long asistenciaId) {
    var asistencia = requerido(asistencias.buscarPorId(asistenciaId));
    exigir(asistencia.getDocumentoSustentoKey() != null && !asistencia.getDocumentoSustentoKey().isBlank(),
        "La asistencia no tiene un documento de sustento");
    return asistencia.getDocumentoSustentoKey();
  }

  @Transactional(readOnly = true)
  public ReporteAsistenciaDiariaResponseDto reporte(LocalDate fecha) {
    var registros = asistencias.buscarPorFechaSesion(fecha);
    return new ReporteAsistenciaDiariaResponseDto(
        fecha, respuestasConNombres(registros));
  }

  private List<AsistenciaAulaResponseDto> respuestasConNombres(
      List<com.colegio.shuji.asistencia.domain.model.AsistenciaAula> registros) {
    if (registros.isEmpty()) return List.of();
    var matriculasPorId = matriculas.buscarPorIds(registros.stream()
        .map(com.colegio.shuji.asistencia.domain.model.AsistenciaAula::getMatriculaId).distinct().toList())
        .stream().collect(Collectors.toMap(Matricula::getId, Function.identity()));
    var estudiantesPorId = estudiantes.buscarPorIds(matriculasPorId.values().stream()
        .map(Matricula::getEstudianteId).distinct().toList()).stream()
        .collect(Collectors.toMap(com.colegio.shuji.matricula.domain.model.Estudiante::getId, Function.identity()));
    return registros.stream().map(registro -> {
      var dto = mapper.toResponse(registro);
      var matricula = matriculasPorId.get(registro.getMatriculaId());
      var estudiante = matricula == null ? null : estudiantesPorId.get(matricula.getEstudianteId());
      String nombre = estudiante == null ? "Estudiante sin datos" : estudiante.nombreCompleto();
      return new AsistenciaAulaResponseDto(dto.id(), dto.matriculaId(), nombre,
          dto.fechaSesion(), dto.horaRegistro(), dto.estado(), dto.auxiliarUsuarioId(),
          dto.justificada(), dto.motivoJustificacion(), dto.tieneDocumentoSustento());
    }).toList();
  }

  @Transactional(readOnly = true)
  public List<DocenteMatriculaAsistenciaDto> matriculasDelDocente() {
    exigirRolDocente();
    var matriculasAsignadas = cargarMatriculasAsignadas();
    var nombres = nombresPorEstudiante(matriculasAsignadas);
    return matriculasAsignadas.stream()
        .map(m -> new DocenteMatriculaAsistenciaDto(m.getId(), nombres.get(m.getId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<DocenteAsistenciaDiariaResponseDto> reporteDelDocente(LocalDate fecha) {
    exigirRolDocente();
    var asignadas = cargarMatriculasAsignadas();
    if (asignadas.isEmpty()) return List.of();
    var nombres = nombresPorEstudiante(asignadas);
    return asistencias.buscarPorFechaSesionYMatriculaIds(
            fecha, asignadas.stream().map(Matricula::getId).toList())
        .stream()
        .map(
            a ->
                new DocenteAsistenciaDiariaResponseDto(
                    a.getId(),
                    a.getMatriculaId(),
                    nombres.get(a.getMatriculaId()),
                    a.getFechaSesion(),
                    a.getHoraRegistro(),
                    a.getEstado()))
        .toList();
  }

  private void exigirAccesoDocente(Matricula matricula) {
    exigirRolDocente();
    var asignadas = asignacionesAbiertas();
    if (!asignadas.getOrDefault(matricula.getSeccionId(), Set.of())
        .contains(matricula.getAnioLectivoId())) {
      throw new AccessDeniedException("La matrícula no pertenece a una sección asignada al docente");
    }
  }

  private List<Matricula> cargarMatriculasAsignadas() {
    var asignadas = asignacionesAbiertas();
    if (asignadas.isEmpty()) return List.of();
    var candidatas = matriculas.buscarPorSeccionIds(asignadas.keySet()).stream()
        .filter(Matricula::estaMatriculado)
        .filter(m -> asignadas.getOrDefault(m.getSeccionId(), Set.of()).contains(m.getAnioLectivoId()))
        .toList();
    if (candidatas.isEmpty()) return List.of();
    var estudiantesActivos =
        estudiantes.buscarPorIds(candidatas.stream().map(Matricula::getEstudianteId).distinct().toList())
            .stream()
            .filter(e -> Boolean.TRUE.equals(e.getActivo()))
            .map(e -> e.getId())
            .collect(Collectors.toSet());
    return candidatas.stream().filter(m -> estudiantesActivos.contains(m.getEstudianteId())).toList();
  }

  private Map<Integer, Set<Short>> asignacionesAbiertas() {
    var aniosAbiertos =
        anios.listar().stream().filter(a -> a.estaAbierto()).map(a -> a.getId()).collect(Collectors.toSet());
    return asignaciones.buscarPorDocenteUsuarioId(actor.usuarioId()).stream()
        .filter(a -> a.getSeccionId() != null && aniosAbiertos.contains(a.getAnioLectivoId()))
        .collect(Collectors.groupingBy(
            a -> a.getSeccionId(), Collectors.mapping(a -> a.getAnioLectivoId(), Collectors.toSet())));
  }

  private Map<Long, String> nombresPorEstudiante(List<Matricula> matriculasAsignadas) {
    var nombresPorEstudiante =
        estudiantes.buscarPorIds(
                matriculasAsignadas.stream().map(Matricula::getEstudianteId).distinct().toList())
            .stream()
            .collect(Collectors.toMap(e -> e.getId(), e -> e.nombreCompleto()));
    return matriculasAsignadas.stream()
        .collect(Collectors.toMap(
            Matricula::getId,
            matricula -> nombresPorEstudiante.getOrDefault(matricula.getEstudianteId(), "Estudiante")));
  }

  private void exigirRolDocente() {
    if (!actor.tieneRol("DOCENTE") && !actor.tieneRol("TUTOR")) {
      throw new AccessDeniedException("El endpoint es exclusivo para docentes y tutores");
    }
  }
}
