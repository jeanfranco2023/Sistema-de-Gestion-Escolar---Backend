package com.colegio.shuji.shared.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;

import com.colegio.shuji.asistencia.application.port.out.AlmacenJustificacionPort;
import com.colegio.shuji.asistencia.infrastructure.adapter.LocalStorageJustificacionAdapter;
import com.colegio.shuji.asistencia.infrastructure.adapter.SupabaseJustificacionStorageAdapter;
import com.colegio.shuji.matricula.application.port.out.AlmacenDocumentosMatriculaPort;
import com.colegio.shuji.matricula.infrastructure.adapter.LocalStorageMatriculaAdapter;
import com.colegio.shuji.matricula.infrastructure.adapter.SupabaseStorageMatriculaAdapter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class LocalStorageProfileTest {

  @TempDir Path tempDir;

  @Test
  void localDockerActivaUnSoloAdapterPorPuertoSinConectarBaseDeDatos() {
    contextRunner("local-docker")
        .withPropertyValues("app.storage.local-root=" + tempDir.resolve("private-files"))
        .run(context -> {
          assertThat(context).hasSingleBean(AlmacenDocumentosMatriculaPort.class);
          assertThat(context).hasSingleBean(AlmacenJustificacionPort.class);
          assertThat(context).hasSingleBean(LocalPrivateFileStore.class);
          assertThat(context).hasSingleBean(LocalStorageDownloadController.class);
          assertThat(context).hasSingleBean(LocalStorageMatriculaAdapter.class);
          assertThat(context).hasSingleBean(LocalStorageJustificacionAdapter.class);
          assertThat(context).doesNotHaveBean(SupabaseStorageMatriculaAdapter.class);
          assertThat(context).doesNotHaveBean(SupabaseJustificacionStorageAdapter.class);
        });
  }

  @Test
  void prodTienePrioridadYNoRegistraFilesystemAunqueLocalDockerTambienEsteActivo() {
    contextRunner("prod,local-docker")
        .run(context -> {
          assertThat(context).hasSingleBean(AlmacenDocumentosMatriculaPort.class);
          assertThat(context).hasSingleBean(AlmacenJustificacionPort.class);
          assertThat(context).hasSingleBean(SupabaseStorageMatriculaAdapter.class);
          assertThat(context).hasSingleBean(SupabaseJustificacionStorageAdapter.class);
          assertThat(context).doesNotHaveBean(LocalPrivateFileStore.class);
          assertThat(context).doesNotHaveBean(LocalStorageDownloadController.class);
          assertThat(context).doesNotHaveBean(LocalStorageMatriculaAdapter.class);
          assertThat(context).doesNotHaveBean(LocalStorageJustificacionAdapter.class);
        });
  }

  private ApplicationContextRunner contextRunner(String activeProfiles) {
    return new ApplicationContextRunner()
        .withUserConfiguration(StorageAdaptersConfiguration.class)
        .withPropertyValues("spring.profiles.active=" + activeProfiles);
  }

  @Configuration(proxyBeanMethods = false)
  @Import({
    LocalPrivateFileStore.class,
    LocalStorageDownloadController.class,
    LocalStorageMatriculaAdapter.class,
    LocalStorageJustificacionAdapter.class,
    SupabaseStorageMatriculaAdapter.class,
    SupabaseJustificacionStorageAdapter.class
  })
  static class StorageAdaptersConfiguration {}
}
