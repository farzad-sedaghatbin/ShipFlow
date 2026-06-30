package com.github.farzadsedaghatbin.shipflow.service.storage.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.InvalidConfigException;
import com.github.farzadsedaghatbin.shipflow.service.storage.DownloadResource;
import com.github.farzadsedaghatbin.shipflow.service.storage.ObjectStorageProvider;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorePutContext;
import com.github.farzadsedaghatbin.shipflow.service.storage.StoredObjectRef;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

/**
 * Local-filesystem implementation of {@link ObjectStorageProvider}.
 *
 * <p>The base directory is read from {@code app.upload.dir} (default {@code uploads}) and is
 * constructor-injectable for pure-unit testing with {@code @TempDir}.
 *
 * <h2>On-disk layout</h2>
 *
 * <p>For each object key the provider writes two files:
 *
 * <ul>
 *   <li>{@code {baseDir}/{key}} — the raw object bytes, stored verbatim at the key supplied by the
 *       caller (the caller is responsible for generating a safe, unique key).
 *   <li>{@code {baseDir}/{key}.ct} — a tiny sidecar file containing the MIME content-type as a
 *       UTF-8 string. The sidecar lets the correct content-type survive cross-backend migration
 *       (e.g. when data is copied from local-fs to S3 the sidecar can be consulted instead of
 *       re-detecting the type). Objects written outside this provider (e.g. legacy files placed
 *       directly on disk) have no sidecar; {@link #retrieve} falls back to {@code
 *       application/octet-stream} for those objects.
 * </ul>
 *
 * <p>{@link #presignUrl} always returns {@link java.util.Optional#empty()} — the local backend has
 * no pre-sign capability. Deletion is best-effort ({@code Files.deleteIfExists}).
 */
@Component
@Slf4j
public class LocalFsStorageProvider implements ObjectStorageProvider {

  private final Path baseDir;

  /**
   * Primary constructor used by Spring — reads {@code app.upload.dir} from the environment.
   *
   * @param uploadDir value of {@code app.upload.dir}; defaults to {@code uploads}
   */
  public LocalFsStorageProvider(
      @Value("${app.upload.dir:uploads}") String uploadDir) {
    this.baseDir = Paths.get(uploadDir).toAbsolutePath().normalize();
  }

  // ── ObjectStorageProvider ─────────────────────────────────────────────────

  @Override
  public StorageProviderType getType() {
    return StorageProviderType.LOCAL_FS;
  }

  /**
   * Accepts any config (including {@code null} or {@code {}}). The local backend uses the
   * server-side {@code app.upload.dir} — no per-call configuration is needed.
   */
  @Override
  public void validateConfig(JsonNode config) throws InvalidConfigException {
    // LOCAL_FS requires no per-call config — always valid.
  }

  /**
   * Stores the stream to {@code {baseDir}/{key}} where {@code key} is taken verbatim from {@code
   * ctx.getKey()}. Key generation is the caller's responsibility; this provider stores at exactly
   * the key it receives.
   */
  @Override
  public StoredObjectRef store(StorePutContext ctx) {
    Path dest = baseDir.resolve(ctx.getKey()).normalize();
    if (!dest.startsWith(baseDir)) {
      throw new IllegalArgumentException("Resolved path escapes the upload directory: " + dest);
    }

    long bytesWritten;
    try {
      Files.createDirectories(dest.getParent());
      bytesWritten = Files.copy(ctx.getStream(), dest, StandardCopyOption.REPLACE_EXISTING);
      // Write a tiny sidecar so retrieve() can return the correct content-type.
      if (ctx.getContentType() != null) {
        Files.writeString(sidecarPath(dest), ctx.getContentType(), StandardCharsets.UTF_8);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to store object at " + dest + ": " + e.getMessage(), e);
    }

    log.debug("Stored object: bucket={} key={} path={}", ctx.getBucket(), ctx.getKey(), dest);

    return StoredObjectRef.builder()
        .bucket(ctx.getBucket())
        .key(ctx.getKey())
        .contentType(ctx.getContentType())
        .sizeBytes(bytesWritten)
        .build();
  }

  /** Returns a {@link DownloadResource} backed by a {@link UrlResource} for the given key. */
  @Override
  public DownloadResource retrieve(String bucket, String key, JsonNode config) {
    Path filePath = baseDir.resolve(key).normalize();
    if (!filePath.startsWith(baseDir)) {
      throw new IllegalArgumentException("Key escapes the upload directory: " + key);
    }

    try {
      UrlResource resource = new UrlResource(filePath.toUri());
      if (!resource.exists() || !resource.isReadable()) {
        throw new IllegalArgumentException("Object not found or unreadable: " + key);
      }
      long size;
      try {
        size = Files.size(filePath);
      } catch (IOException e) {
        size = -1L;
      }
      String contentType = readSidecar(filePath);
      return DownloadResource.builder()
          .stream(resource.getInputStream())
          .contentType(contentType)
          .sizeBytes(size)
          .filename(extractFilename(key))
          .build();
    } catch (MalformedURLException e) {
      throw new RuntimeException("Could not resolve file path for key: " + key, e);
    } catch (IOException e) {
      throw new RuntimeException("Failed to open stream for key: " + key, e);
    }
  }

  /** Best-effort deletion — logs a warning if the file cannot be removed but never throws. */
  @Override
  public void delete(String bucket, String key, JsonNode config) {
    Path filePath = baseDir.resolve(key).normalize();
    if (!filePath.startsWith(baseDir)) {
      log.warn("Refusing to delete path outside upload directory: {}", key);
      return;
    }
    try {
      boolean deleted = Files.deleteIfExists(filePath);
      Files.deleteIfExists(sidecarPath(filePath));
      if (deleted) {
        log.debug("Deleted object: bucket={} key={}", bucket, key);
      } else {
        log.debug("Object not found on disk (already gone?): bucket={} key={}", bucket, key);
      }
    } catch (IOException e) {
      log.warn("Could not delete object from disk: bucket={} key={} error={}", bucket, key,
          e.getMessage());
    }
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  /** Extracts the trailing filename from a slash-separated key/path. */
  private static String extractFilename(String key) {
    if (key == null || key.isBlank()) return "attachment";
    int slash = key.lastIndexOf('/');
    return slash >= 0 ? key.substring(slash + 1) : key;
  }

  /** Returns the sidecar path for a given file path (same name + {@code .ct} extension). */
  private static Path sidecarPath(Path filePath) {
    return filePath.resolveSibling(filePath.getFileName().toString() + ".ct");
  }

  /**
   * Reads the content-type sidecar file. Falls back to {@code application/octet-stream} if missing
   * or unreadable.
   */
  private static String readSidecar(Path filePath) {
    Path sc = sidecarPath(filePath);
    try {
      if (Files.exists(sc)) {
        String ct = Files.readString(sc, StandardCharsets.UTF_8).trim();
        if (!ct.isBlank()) return ct;
      }
    } catch (IOException e) {
      // fall through to default
    }
    return "application/octet-stream";
  }
}
