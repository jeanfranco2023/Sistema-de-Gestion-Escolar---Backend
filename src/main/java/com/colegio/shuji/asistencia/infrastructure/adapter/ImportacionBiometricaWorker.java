package com.colegio.shuji.asistencia.infrastructure.adapter;

import com.colegio.shuji.asistencia.application.port.in.ProcesarBiometricoUseCase;
import com.colegio.shuji.asistencia.application.service.BiometricoImportacionPayload;
import com.colegio.shuji.asistencia.application.service.ImportacionBiometricaService;
import com.colegio.shuji.config.security.UserPrincipal;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "app.biometrico", name = "queue-enabled", havingValue = "true")
public class ImportacionBiometricaWorker {
  private final RedisColaBiometrica cola;
  private final ImportacionBiometricaService importaciones;
  private final ProcesarBiometricoUseCase procesador;
  private final ThreadPoolTaskExecutor executor;

  public ImportacionBiometricaWorker(
      RedisColaBiometrica cola,
      ImportacionBiometricaService importaciones,
      ProcesarBiometricoUseCase procesador,
      @Qualifier("biometricoImportExecutor") ThreadPoolTaskExecutor executor) {
    this.cola = cola;
    this.importaciones = importaciones;
    this.procesador = procesador;
    this.executor = executor;
  }

  @Value("${app.biometrico.workers:2}")
  private int maxTrabajadores;

  private long ultimaRecuperacion;

  @Scheduled(fixedDelayString = "${app.biometrico.poll-ms:250}")
  public void despachar() {
    try {
      recuperarPendientesSiCorresponde();
      if (executor.getActiveCount() >= maxTrabajadores) return;
      var trabajo = cola.tomarSiguiente();
      if (trabajo == null || !importaciones.reclamar(trabajo.trabajoId())) return;
      try {
        executor.execute(() -> procesar(trabajo.trabajoId()));
      } catch (RejectedExecutionException ex) {
        importaciones.devolverACola(trabajo.trabajoId());
        cola.encolar(trabajo.usuarioId(), trabajo.trabajoId());
      }
    } catch (RuntimeException ex) {
      log.warn("No se pudo despachar un trabajo biométrico desde Redis");
    }
  }

  private void recuperarPendientesSiCorresponde() {
    long ahora = System.currentTimeMillis();
    if (ahora - ultimaRecuperacion < 5_000) return;
    ultimaRecuperacion = ahora;
    importaciones.recuperarEstancadas();
    importaciones.pendientes().forEach(
        pendiente -> cola.encolar(pendiente.getUsuarioId(), pendiente.getId()));
  }

  private void procesar(java.util.UUID id) {
    try {
      var payload = importaciones.payload(id).orElseThrow();
      establecerContexto(payload);
      var lote = procesador.importar(payload.solicitud());
      importaciones.completar(id, lote);
    } catch (Exception ex) {
      importaciones.fallar(id);
    } finally {
      SecurityContextHolder.clearContext();
    }
  }

  private void establecerContexto(BiometricoImportacionPayload payload) {
    List<SimpleGrantedAuthority> authorities = payload.roles().stream()
        .map(SimpleGrantedAuthority::new)
        .toList();
    var principal = UserPrincipal.builder()
        .id(payload.usuarioId())
        .username(payload.nombreUsuario())
        .email(payload.correoUsuario())
        .password("")
        .active(true)
        .authorities(authorities)
        .build();
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    SecurityContextHolder.setContext(context);
  }
}
