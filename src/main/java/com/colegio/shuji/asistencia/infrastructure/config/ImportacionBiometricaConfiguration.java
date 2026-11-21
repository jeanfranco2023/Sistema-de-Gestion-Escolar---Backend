package com.colegio.shuji.asistencia.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableScheduling
public class ImportacionBiometricaConfiguration {
  @Bean(name = "biometricoImportExecutor")
  @ConditionalOnProperty(prefix = "app.biometrico", name = "queue-enabled", havingValue = "true")
  ThreadPoolTaskExecutor biometricoImportExecutor(
      @Value("${app.biometrico.workers:2}") int trabajadores) {
    var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(Math.max(1, trabajadores));
    executor.setMaxPoolSize(Math.max(1, trabajadores));
    executor.setQueueCapacity(0);
    executor.setThreadNamePrefix("biometrico-import-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    return executor;
  }
}
