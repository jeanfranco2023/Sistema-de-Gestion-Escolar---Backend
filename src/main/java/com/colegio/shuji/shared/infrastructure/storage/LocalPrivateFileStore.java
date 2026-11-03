package com.colegio.shuji.shared.infrastructure.storage;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Profile("local-docker & !prod")
public final class LocalPrivateFileStore {

  private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;
  private static final Map<String, Long> MAX_FILE_BYTES_BY_BUCKET = Map.of(
      "matricula-documentos", MAX_FILE_BYTES,
      "asistencia-justificaciones", MAX_FILE_BYTES,
      "expedientes-empleados", 10L * 1024 * 1024);
  private static final SecureRandom RANDOM = new SecureRandom();
  private static final Set<String> BUCKETS = MAX_FILE_BYTES_BY_BUCKET.keySet();
  private static final Set<PosixFilePermission> DIRECTORY_PERMISSIONS = Set.of(
      PosixFilePermission.OWNER_READ,
      PosixFilePermission.OWNER_WRITE,
      PosixFilePermission.OWNER_EXECUTE);
  private static final Set<PosixFilePermission> FILE_PERMISSIONS = Set.of(
      PosixFilePermission.OWNER_READ,
      PosixFilePermission.OWNER_WRITE);

  private final Path root;
  private final String publicBaseUrl;
  private final int signedUrlSeconds;
  private final byte[] signingKey;
  private final Clock clock;

  @Autowired
  public LocalPrivateFileStore(
      @Value("${app.storage.local-root:/var/lib/shuji/private}") String root,
      @Value("${app.storage.local-public-base-url:http://localhost:10000}") String publicBaseUrl,
      @Value("${app.storage.local-signed-url-seconds:60}") int signedUrlSeconds) {
    this(Path.of(root), publicBaseUrl, signedUrlSeconds, randomKey(), Clock.systemUTC());
  }

  LocalPrivateFileStore(Path root, String publicBaseUrl, int signedUrlSeconds, byte[] signingKey, Clock clock) {
    try {
      Files.createDirectories(root);
      this.root = root.toRealPath();
      setPrivatePermissions(this.root, DIRECTORY_PERMISSIONS);
    } catch (IOException ex) {
      throw new IllegalStateException("No se pudo inicializar el almacenamiento local privado", ex);
    }
    URI base = URI.create(sinBarrasFinales(publicBaseUrl));
    if (!("http".equalsIgnoreCase(base.getScheme()) || "https".equalsIgnoreCase(base.getScheme()))
        || base.getHost() == null
        || base.getUserInfo() != null
        || base.getQuery() != null
        || base.getFragment() != null) {
      throw new IllegalArgumentException("La URL pública del backend local debe ser un origen HTTP(S)");
    }
    this.publicBaseUrl = base.toString();
    this.signedUrlSeconds = Math.max(30, Math.min(600, signedUrlSeconds));
    if (signingKey == null || signingKey.length < 32) {
      throw new IllegalArgumentException("La clave temporal local debe tener al menos 32 bytes");
    }
    this.signingKey = signingKey.clone();
    this.clock = clock;
  }

  public void validarConfiguracion() {
    if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS) || !Files.isWritable(root)) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "El almacenamiento local privado no está disponible");
    }
  }

  public void guardar(String bucket, String objectKey, byte[] bytes) {
    validarConfiguracion();
    if (bytes == null || bytes.length == 0 || bytes.length > maxFileBytes(bucket)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El archivo excede el límite permitido para su almacenamiento");
    }
    Path target = resolver(bucket, objectKey);
    Path temporary = null;
    try {
      crearDirectoriosPrivados(target.getParent());
      temporary = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
      setPrivatePermissions(temporary, FILE_PERMISSIONS);
      Files.write(temporary, bytes, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
      Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
      temporary = null;
    } catch (AtomicMoveNotSupportedException ex) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "El volumen local no admite la escritura atómica de documentos");
    } catch (java.nio.file.FileAlreadyExistsException ex) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "El archivo ya existe");
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "No se pudo guardar el documento en el almacenamiento local privado");
    } finally {
      if (temporary != null) {
        try {
          Files.deleteIfExists(temporary);
        } catch (IOException ignored) {
          // El archivo temporal permanece dentro del directorio privado.
        }
      }
    }
  }

  public String urlFirmada(String bucket, String objectKey) {
    validarConfiguracion();
    Path file = resolver(bucket, objectKey);
    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
      throw noEncontrado();
    }
    long expiresAt = clock.instant().getEpochSecond() + signedUrlSeconds;
    String signature = firmar(bucket, objectKey, expiresAt);
    return UriComponentsBuilder.fromUriString(publicBaseUrl)
        .path("/api/v1/storage/local/download")
        .queryParam("bucket", bucket)
        .queryParam("key", objectKey)
        .queryParam("expires", expiresAt)
        .queryParam("sig", signature)
        .build()
        .encode()
        .toUriString();
  }

  public ArchivoDescargable descargar(String bucket, String objectKey, long expiresAt, String signature) {
    validarConfiguracion();
    long now = clock.instant().getEpochSecond();
    if (expiresAt <= now || expiresAt > now + signedUrlSeconds
        || signature == null
        || !signature.matches("[0-9a-f]{64}")
        || !MessageDigest.isEqual(
            firmar(bucket, objectKey, expiresAt).getBytes(StandardCharsets.US_ASCII),
            signature.getBytes(StandardCharsets.US_ASCII))) {
      throw noEncontrado();
    }
    Path file = resolver(bucket, objectKey);
    try {
      long size = Files.size(file);
      if (size < 1 || size > maxFileBytes(bucket) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
        throw noEncontrado();
      }
      String filename = file.getFileName().toString();
      return new ArchivoDescargable(Files.readAllBytes(file), mediaType(filename), filename);
    } catch (IOException ex) {
      throw noEncontrado();
    }
  }

  public void eliminar(String bucket, String objectKey) {
    validarConfiguracion();
    Path file = resolver(bucket, objectKey);
    try {
      Files.deleteIfExists(file);
    } catch (IOException ex) {
      throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
          "No se pudo eliminar el documento del almacenamiento local privado");
    }
  }

  public record ArchivoDescargable(byte[] bytes, String mediaType, String filename) {
    public ArchivoDescargable {
      bytes = bytes.clone();
    }

    @Override
    public byte[] bytes() {
      return bytes.clone();
    }

    @Override
    public boolean equals(Object other) {
      return other instanceof ArchivoDescargable archivo
          && Arrays.equals(bytes, archivo.bytes)
          && Objects.equals(mediaType, archivo.mediaType)
          && Objects.equals(filename, archivo.filename);
    }

    @Override
    public int hashCode() {
      return Objects.hash(Arrays.hashCode(bytes), mediaType, filename);
    }

    @Override
    public String toString() {
      // El contenido del documento privado nunca debe aparecer en logs.
      return "ArchivoDescargable[size=" + bytes.length + ", mediaType=" + mediaType
          + ", filename=" + filename + "]";
    }
  }

  private static String sinBarrasFinales(String value) {
    String trimmed = value == null ? "" : value.trim();
    int end = trimmed.length();
    while (end > 0 && trimmed.charAt(end - 1) == '/') end--;
    return trimmed.substring(0, end);
  }

  private Path resolver(String bucket, String objectKey) {
    if (!BUCKETS.contains(bucket)
        || objectKey == null
        || !objectKey.matches("[A-Za-z0-9._/-]{1,512}")) {
      throw noEncontrado();
    }
    String[] segments = objectKey.split("/", -1);
    for (String segment : segments) {
      if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) throw noEncontrado();
    }
    Path target = root.resolve(bucket).resolve(objectKey).normalize();
    if (!target.startsWith(root)) throw noEncontrado();
    verificarSinEnlacesSimbolicos(target);
    return target;
  }

  private void crearDirectoriosPrivados(Path directory) throws IOException {
    Path current = root;
    for (Path segment : root.relativize(directory)) {
      current = current.resolve(segment);
      if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
        if (Files.isSymbolicLink(current) || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
          throw noEncontrado();
        }
      } else {
        try {
          Files.createDirectory(current);
        } catch (java.nio.file.FileAlreadyExistsException raced) {
          if (Files.isSymbolicLink(current) || !Files.isDirectory(current, LinkOption.NOFOLLOW_LINKS)) {
            throw noEncontrado();
          }
        }
      }
      setPrivatePermissions(current, DIRECTORY_PERMISSIONS);
    }
    verificarSinEnlacesSimbolicos(directory);
  }

  private void verificarSinEnlacesSimbolicos(Path target) {
    Path current = root;
    for (Path segment : root.relativize(target)) {
      current = current.resolve(segment);
      if (Files.isSymbolicLink(current)) throw noEncontrado();
    }
  }

  private String firmar(String bucket, String objectKey, long expiresAt) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(signingKey, "HmacSHA256"));
      byte[] payload = (bucket + "\n" + objectKey + "\n" + expiresAt).getBytes(StandardCharsets.UTF_8);
      return HexFormat.of().formatHex(mac.doFinal(payload));
    } catch (GeneralSecurityException ex) {
      throw new IllegalStateException("HMAC-SHA256 no está disponible", ex);
    }
  }

  private static byte[] randomKey() {
    byte[] key = new byte[32];
    RANDOM.nextBytes(key);
    return key;
  }

  private static String mediaType(String filename) {
    return switch (filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT)) {
      case "pdf" -> "application/pdf";
      case "jpg", "jpeg" -> "image/jpeg";
      case "png" -> "image/png";
      case "webp" -> "image/webp";
      default -> throw noEncontrado();
    };
  }

  private static long maxFileBytes(String bucket) {
    if (bucket == null) return MAX_FILE_BYTES;
    return MAX_FILE_BYTES_BY_BUCKET.getOrDefault(bucket, MAX_FILE_BYTES);
  }

  private static void setPrivatePermissions(Path path, Set<PosixFilePermission> permissions) throws IOException {
    try {
      Files.setPosixFilePermissions(path, permissions);
    } catch (UnsupportedOperationException ignored) {
      // En plataformas sin permisos POSIX, el directorio dedicado y fuera del webroot sigue aislado.
    }
  }

  private static ResponseStatusException noEncontrado() {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento no disponible");
  }
}
