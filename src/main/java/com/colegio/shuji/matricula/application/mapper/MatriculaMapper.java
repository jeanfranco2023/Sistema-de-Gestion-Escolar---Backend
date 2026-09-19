package com.colegio.shuji.matricula.application.mapper;

import com.colegio.shuji.matricula.application.dto.in.RegistrarApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.in.RegistrarEstudianteRequestDto;
import com.colegio.shuji.matricula.application.dto.in.SolicitarMatriculaRequestDto;
import com.colegio.shuji.matricula.application.dto.in.VincularApoderadoRequestDto;
import com.colegio.shuji.matricula.application.dto.out.ApoderadoResponseDto;
import com.colegio.shuji.matricula.application.dto.out.EstudianteApoderadoResponseDto;
import com.colegio.shuji.matricula.application.dto.out.EstudianteResponseDto;
import com.colegio.shuji.matricula.application.dto.out.MatriculaResponseDto;
import com.colegio.shuji.matricula.domain.model.Apoderado;
import com.colegio.shuji.matricula.domain.model.Estudiante;
import com.colegio.shuji.matricula.domain.model.EstudianteApoderado;
import com.colegio.shuji.matricula.domain.model.Matricula;
import org.mapstruct.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface MatriculaMapper {
  Estudiante toDomain(RegistrarEstudianteRequestDto request);

  Apoderado toDomain(RegistrarApoderadoRequestDto request);

  EstudianteApoderado toDomain(VincularApoderadoRequestDto request);

  Matricula toDomain(SolicitarMatriculaRequestDto request);

  EstudianteResponseDto toResponse(Estudiante model);

  ApoderadoResponseDto toResponse(Apoderado model);

  EstudianteApoderadoResponseDto toResponse(EstudianteApoderado model);

  MatriculaResponseDto toResponse(Matricula model);
}
