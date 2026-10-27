package com.colegio.shuji.matricula.application.port.out;
import com.colegio.shuji.matricula.domain.model.Suplencia;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface SuplenciaRepositoryPort {
  Suplencia guardar(Suplencia valor);
  Optional<Suplencia> bloquearPorId(Long id);
  List<Suplencia> porTitular(Long id);
  List<Suplencia> listar();
  boolean hayConflicto(Long estudianteId,LocalDate inicio,LocalDate fin,Long excluirId);
}
