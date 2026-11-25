package com.colegio.shuji.matricula.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.matricula.application.dto.in.CambiarEstadoApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.in.CambiarEstadoEstudianteRequestDto;
import com.colegio.shuji.matricula.application.dto.in.ApoderadoMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.RegistrarApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.in.RegistrarEstudianteRequestDto;
import com.colegio.shuji.matricula.application.dto.in.VincularApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.out.ApoderadoFichaResponseDto;
import com.colegio.shuji.matricula.application.dto.out.ApoderadoResponseDto;
import com.colegio.shuji.matricula.application.dto.out.EstudianteApoderadoResponseDto;
import com.colegio.shuji.matricula.application.dto.out.EstudianteResponseDto;
import com.colegio.shuji.matricula.application.mapper.MatriculaMapper;
import com.colegio.shuji.matricula.application.port.in.RegistrarFichaFamiliarUseCase;
import com.colegio.shuji.matricula.application.port.out.ApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteApoderadoRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.EstudianteRepositoryPort;
import com.colegio.shuji.matricula.application.port.out.ReniecServicePort;
import com.colegio.shuji.matricula.domain.enums.TipoDocumento;
import com.colegio.shuji.matricula.domain.model.Apoderado;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import com.colegio.shuji.shared.application.port.out.AuditContextPort;
import com.colegio.shuji.shared.domain.exception.BusinessException;
import com.colegio.shuji.usuario.application.dto.in.CorregirDniRequestDto;
import com.colegio.shuji.usuario.domain.exception.ReglaNegocioException;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
public class EstudianteService implements RegistrarFichaFamiliarUseCase {
  private final MatriculaMapper mapper;
  private final EstudianteRepositoryPort estudiantes;
  private final ApoderadoRepositoryPort apoderados;
  private final EstudianteApoderadoRepositoryPort vinculos;
  private final ReniecServicePort reniec;
  private final AuditContextPort auditoria;

  @Autowired
  public EstudianteService(
      MatriculaMapper mapper,
      EstudianteRepositoryPort estudiantes,
      ApoderadoRepositoryPort apoderados,
      EstudianteApoderadoRepositoryPort vinculos,
      ReniecServicePort reniec,
      @Autowired(required = false) AuditContextPort auditoria) {
    this.mapper = mapper;
    this.estudiantes = estudiantes;
    this.apoderados = apoderados;
    this.vinculos = vinculos;
    this.reniec = reniec;
    this.auditoria = auditoria;
  }

  public EstudianteService(
      MatriculaMapper mapper,
      EstudianteRepositoryPort estudiantes,
      ApoderadoRepositoryPort apoderados,
      EstudianteApoderadoRepositoryPort vinculos,
      ReniecServicePort reniec) {
    this(mapper, estudiantes, apoderados, vinculos, reniec, null);
  }

  private void validarDocumento(TipoDocumento tipo, String numero) {
    if (tipo != TipoDocumento.DNI || numero == null || !numero.matches("[0-9]{8}"))
      throw new com.colegio.shuji.matricula.domain.exception.DniInvalidoException(
          "Solo se admite DNI peruano de ocho dígitos");
  }

  private void validarFechaNacimientoEscolar(LocalDate fechaNacimiento) {
    exigir(fechaNacimiento != null, "La fecha de nacimiento es obligatoria");
    LocalDate hoy = LocalDate.now(ZoneId.of("America/Lima"));
    exigir(fechaNacimiento.isBefore(hoy), "La fecha de nacimiento debe ser anterior a hoy");
    int edad = Period.between(fechaNacimiento, hoy).getYears();
    exigir(
        edad >= 2 && edad <= 22,
        "La edad del estudiante (" + edad + " años) no corresponde a educación escolar (2 a 22 años)");
  }

  public EstudianteResponseDto registrarEstudiante(RegistrarEstudianteRequestDto r) {
    validarDocumento(r.tipoDocumento(), r.numeroDocumento());
    validarFechaNacimientoEscolar(r.fechaNacimiento());
    exigir(
        estudiantes.buscarPorNumeroDocumento(r.numeroDocumento()).stream()
            .noneMatch(e -> e.getTipoDocumento() == r.tipoDocumento()),
        "Documento ya registrado");
    var e = mapper.toDomain(r);
    e.iniciarRegistroManual();
    e.activar();
    var guardado = estudiantes.guardar(e);
    boolean tieneApoderado = r.apoderado() != null;
    if (tieneApoderado) {
      var datos = r.apoderado();
      Long apoderadoId = apoderados.buscarPorNumeroDocumento(datos.numeroDocumento()).stream()
          .filter(a -> a.getTipoDocumento() == datos.tipoDocumento())
          .map(Apoderado::getId)
          .findFirst()
          .orElseGet(() -> crearApoderado(datos));
      vincular(new VincularApoderadoRequestDto(
          guardado.getId(), apoderadoId, datos.parentesco(), true, true, true));
    }
    return mapper.toResponse(guardado).conTieneApoderado(tieneApoderado);
  }

  private Long crearApoderado(ApoderadoMatriculaRequestDto datos) {
    var request = new RegistrarApoderadoRequestDto(datos.tipoDocumento(), datos.numeroDocumento(),
        datos.nombres(), datos.apellidoPaterno(), datos.apellidoMaterno(), datos.celular(),
        datos.email(), datos.direccion(), datos.ubigeoInei(), null);
    var apoderado = mapper.toDomain(request);
    apoderado.iniciarRegistroManual();
    return apoderados.guardar(apoderado).getId();
  }

  public ApoderadoResponseDto registrarApoderado(RegistrarApoderadoRequestDto r) {
    validarDocumento(r.tipoDocumento(), r.numeroDocumento());
    exigir(
        apoderados.buscarPorNumeroDocumento(r.numeroDocumento()).stream()
            .noneMatch(a -> a.getTipoDocumento() == r.tipoDocumento()),
        "Documento ya registrado");
    var a = mapper.toDomain(r);
    a.iniciarRegistroManual();
    if (r.tipoDocumento() == TipoDocumento.DNI)
      reniec
          .consultar(r.numeroDocumento())
          .ifPresent(
              d ->
                  a.completarDesdeConsultaDni(
                      d.nombres(), d.apellidoPaterno(), d.apellidoMaterno()));
    return mapper.toResponse(apoderados.guardar(a));
  }

  @Transactional(readOnly = true)
  public List<ApoderadoResponseDto> listarApoderados() {
    return apoderados.listar().stream().map(mapper::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<ApoderadoFichaResponseDto> listarApoderadosPorEstudiante(Long estudianteId) {
    var relaciones = vinculos.buscarPorEstudianteId(estudianteId);
    if (relaciones.isEmpty()) {
      return List.of();
    }
    var apoderadosMap =
        apoderados.buscarPorIds(relaciones.stream().map(v -> v.getApoderadoId()).toList()).stream()
            .collect(java.util.stream.Collectors.toMap(
                Apoderado::getId,
                java.util.function.Function.identity()));
    return relaciones.stream()
        .map(v -> new ApoderadoFichaResponseDto(
            mapper.toResponse(requerido(java.util.Optional.ofNullable(apoderadosMap.get(v.getApoderadoId())))),
            v.getParentesco(),
            v.getEsResponsableEconomico()))
        .toList();
  }

  public EstudianteApoderadoResponseDto vincular(VincularApoderadoRequestDto r) {
    requerido(estudiantes.bloquearPorId(r.estudianteId()));
    requerido(apoderados.buscarPorId(r.apoderadoId()));
    exigir(
        vinculos.buscarPorEstudianteId(r.estudianteId()).stream()
            .noneMatch(v -> v.getApoderadoId().equals(r.apoderadoId())),
        "Vínculo ya existente");
    if (r.esResponsableEconomico())
      exigir(
          vinculos.buscarPorEstudianteId(r.estudianteId()).stream()
              .noneMatch(v -> Boolean.TRUE.equals(v.getEsResponsableEconomico())),
          "Ya existe responsable económico");
    return mapper.toResponse(vinculos.guardar(mapper.toDomain(r)));
  }

  @Transactional(readOnly = true)
  public EstudianteResponseDto consultarEstudiante(Long id) {
    return mapper.toResponse(requerido(estudiantes.buscarPorId(id)));
  }

  @Transactional(readOnly = true)
  public List<EstudianteResponseDto> listarEstudiantes() {
    var lista = estudiantes.listar();
    if (lista.isEmpty()) return List.of();
    var estudiantesConApoderado = vinculos.buscarPorEstudianteIds(
        lista.stream().map(Estudiante::getId).toList()).stream()
        .map(com.colegio.shuji.matricula.domain.model.EstudianteApoderado::getEstudianteId)
        .collect(java.util.stream.Collectors.toSet());
    return lista.stream()
        .map(e -> mapper.toResponse(e).conTieneApoderado(estudiantesConApoderado.contains(e.getId())))
        .toList();
  }

  @Transactional(readOnly = true)
  public ApoderadoResponseDto consultarApoderado(Long id) {
    return mapper.toResponse(requerido(apoderados.buscarPorId(id)));
  }

  @Transactional(readOnly = true)
  public Optional<ReniecServicePort.Identidad> consultarDni(String dni) {
    validarDocumento(TipoDocumento.DNI, dni);
    return reniec.consultar(dni);
  }

  @Override
  public EstudianteResponseDto corregirDniEstudiante(Long id, CorregirDniRequestDto request) {
    log.info("Acción transaccional: Corrigiendo DNI para estudiante ID: {} con motivo: {}", id, request.motivo());
    if (auditoria != null) {
      auditoria.syncCurrentUserFromSecurityContext();
    }
    var e = requerido(estudiantes.buscarPorId(id));
    if (request.nuevoDni().equals(e.getNumeroDocumento())) {
      throw new BusinessException("El nuevo DNI es idéntico al DNI actualmente registrado", "DNI_IDENTICO");
    }
    if (estudiantes.buscarPorNumeroDocumento(request.nuevoDni()).stream()
        .anyMatch(otro -> !otro.getId().equals(id))) {
      throw new ReglaNegocioException("El DNI ingresado ya se encuentra asignado a otro estudiante", "DNI_DUPLICADO");
    }
    e.corregirNumeroDocumento(request.nuevoDni());
    var guardado = estudiantes.guardar(e);
    log.info("DNI del estudiante ID: {} corregido exitosamente a {}", id, request.nuevoDni());
    return mapper.toResponse(guardado);
  }

  @Override
  public EstudianteResponseDto cambiarEstadoEstudiante(Long id, CambiarEstadoEstudianteRequestDto request) {
    log.info("Cambiando estado de estudiante ID: {} a: {} con motivo: {}", id, request.estado(), request.motivo());
    if (auditoria != null) {
      auditoria.syncCurrentUserFromSecurityContext();
    }
    var e = requerido(estudiantes.buscarPorId(id));
    e.cambiarEstado(request.estado());
    var guardado = estudiantes.guardar(e);
    log.info("Estado de estudiante ID: {} actualizado a: {}", id, request.estado());
    return mapper.toResponse(guardado);
  }

  @Override
  public ApoderadoResponseDto corregirDniApoderado(Long id, CorregirDniRequestDto request) {
    log.info("Acción transaccional: Corrigiendo DNI para apoderado ID: {} con motivo: {}", id, request.motivo());
    if (auditoria != null) {
      auditoria.syncCurrentUserFromSecurityContext();
    }
    var a = requerido(apoderados.buscarPorId(id));
    if (request.nuevoDni().equals(a.getNumeroDocumento())) {
      throw new BusinessException("El nuevo DNI es idéntico al DNI actualmente registrado", "DNI_IDENTICO");
    }
    if (apoderados.buscarPorNumeroDocumento(request.nuevoDni()).stream()
        .anyMatch(otro -> !otro.getId().equals(id))) {
      throw new ReglaNegocioException("El DNI ingresado ya se encuentra asignado a otro apoderado", "DNI_DUPLICADO");
    }
    a.corregirNumeroDocumento(request.nuevoDni());
    var guardado = apoderados.guardar(a);
    log.info("DNI del apoderado ID: {} corregido exitosamente a {}", id, request.nuevoDni());
    return mapper.toResponse(guardado);
  }

  @Override
  public ApoderadoResponseDto cambiarEstadoApoderado(Long id, CambiarEstadoApoderadoRequestDto request) {
    log.info("Cambiando estado de apoderado ID: {} a: {} con motivo: {}", id, request.estado(), request.motivo());
    if (auditoria != null) {
      auditoria.syncCurrentUserFromSecurityContext();
    }
    var a = requerido(apoderados.buscarPorId(id));
    a.cambiarEstado(request.estado());
    var guardado = apoderados.guardar(a);
    log.info("Estado de apoderado ID: {} actualizado a: {}", id, request.estado());
    return mapper.toResponse(guardado);
  }
}
