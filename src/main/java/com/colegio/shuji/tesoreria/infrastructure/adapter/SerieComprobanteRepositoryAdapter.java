package com.colegio.shuji.tesoreria.infrastructure.adapter;

import com.colegio.shuji.tesoreria.application.port.out.SerieComprobanteRepositoryPort;
import com.colegio.shuji.tesoreria.domain.model.SerieComprobante;
import com.colegio.shuji.tesoreria.infrastructure.entity.SerieComprobanteEntity;
import com.colegio.shuji.tesoreria.infrastructure.repository.JpaSerieComprobanteRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SerieComprobanteRepositoryAdapter implements SerieComprobanteRepositoryPort {
  private final JpaSerieComprobanteRepository repository;

  @Override
  public Optional<SerieComprobante> bloquearPorSerie(String serie) {
    return repository.bloquearPorSerie(serie).map(this::toDomain);
  }

  @Override
  public SerieComprobante guardar(SerieComprobante serie) {
    var entity =
        repository
            .findById(serie.getSerie())
            .orElseThrow(
                () ->
                    new com.colegio.shuji.shared.domain.exception.BusinessException(
                        "Serie de comprobante no configurada: " + serie.getSerie()));
    entity.setUltimoCorrelativo(serie.getUltimoCorrelativo());
    entity.setUpdatedAt(serie.getUpdatedAt());
    return toDomain(repository.saveAndFlush(entity));
  }

  private SerieComprobante toDomain(SerieComprobanteEntity entity) {
    return SerieComprobante.builder()
        .serie(entity.getSerie())
        .ultimoCorrelativo(entity.getUltimoCorrelativo())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
