package com.colegio.shuji.matricula.application.service;

import static com.colegio.shuji.shared.domain.model.Reglas.exigir;
import static com.colegio.shuji.shared.domain.model.Reglas.requerido;

import com.colegio.shuji.matricula.application.dto.in.RegistrarApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.in.RegistrarEstudianteRequestDto;
import com.colegio.shuji.matricula.application.dto.in.VincularApoderadoRequestDto;
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
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class EstudianteService implements RegistrarFichaFamiliarUseCase {
  private final MatriculaMapper mapper;
  private final EstudianteRepositoryPort estudiantes;
  private final ApoderadoRepositoryPort apoderados;
  private final EstudianteApoderadoRepositoryPort vinculos;
  private final ReniecServicePort reniec;

  private void validarDocumento(TipoDocumento tipo, String numero) {
    if (tipo == TipoDocumento.DNI && (numero == null || !numero.matches("[0-9]{8}")))
      throw new com.colegio.shuji.matricula.domain.exception.DniInvalidoException(
          "El DNI debe tener ocho dígitos");
    exigir(numero != null && !numero.isBlank() && numero.length() <= 15, "Documento inválido");
  }

  public EstudianteResponseDto registrarEstudiante(RegistrarEstudianteRequestDto r) {
    validarDocumento(r.tipoDocumento(), r.numeroDocumento());
    exigir(
        estudiantes.buscarPorNumeroDocumento(r.numeroDocumento()).stream()
            .noneMatch(e -> e.getTipoDocumento() == r.tipoDocumento()),
        "Documento ya registrado");
    var e = mapper.toDomain(r);
    e.actualizarCodigoSiagie(
        r.codigoEstudianteSiagie() == null || r.codigoEstudianteSiagie().isBlank()
            ? null
            : r.codigoEstudianteSiagie().trim());
    e.iniciarRegistroManual();
    e.activar();
    return mapper.toResponse(estudiantes.guardar(e));
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
    return estudiantes.listar().stream().map(mapper::toResponse).toList();
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
}
