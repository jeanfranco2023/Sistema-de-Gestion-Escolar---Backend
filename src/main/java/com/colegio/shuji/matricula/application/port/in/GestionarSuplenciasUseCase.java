package com.colegio.shuji.matricula.application.port.in;
import com.colegio.shuji.matricula.application.dto.in.CrearSuplenciaRequestDto;
import com.colegio.shuji.matricula.application.dto.out.SuplenciaResponseDto;
import java.util.List;
public interface GestionarSuplenciasUseCase {
  SuplenciaResponseDto solicitar(CrearSuplenciaRequestDto request);
  List<SuplenciaResponseDto> misSolicitudes();
  List<SuplenciaResponseDto> bandeja();
  SuplenciaResponseDto aprobar(Long id);
  SuplenciaResponseDto rechazar(Long id,String motivo);
  SuplenciaResponseDto revocar(Long id,String motivo);
}
