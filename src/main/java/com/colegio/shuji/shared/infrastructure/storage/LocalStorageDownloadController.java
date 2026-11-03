package com.colegio.shuji.shared.infrastructure.storage;

import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local-docker & !prod")
@RequestMapping("/api/v1/storage/local")
public class LocalStorageDownloadController {
  private final LocalPrivateFileStore storage;

  public LocalStorageDownloadController(LocalPrivateFileStore storage) {
    this.storage = storage;
  }

  @GetMapping("/download")
  @PreAuthorize("permitAll()")
  public ResponseEntity<byte[]> descargar(
      @RequestParam String bucket,
      @RequestParam String key,
      @RequestParam long expires,
      @RequestParam String sig) {
    var archivo = storage.descargar(bucket, key, expires, sig);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.noStore())
        .header(HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.inline().filename(archivo.filename()).build().toString())
        .header("X-Content-Type-Options", "nosniff")
        .contentType(MediaType.parseMediaType(archivo.mediaType()))
        .body(archivo.bytes());
  }
}
