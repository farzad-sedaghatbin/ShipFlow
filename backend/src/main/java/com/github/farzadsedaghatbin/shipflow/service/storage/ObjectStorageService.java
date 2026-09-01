package com.github.farzadsedaghatbin.shipflow.service.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.StorageConfig;
import com.github.farzadsedaghatbin.shipflow.service.StorageConfigService;
import java.io.InputStream;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Façade over all registered {@link ObjectStorageProvider} backends.
 *
 * <p>Provides a single entry point for upload/download/delete/presign operations. Validation
 * (size and content-type) is enforced here so every caller benefits uniformly. The constants
 * {@link #MAX_FILE_SIZE} and {@link #ALLOWED_CONTENT_TYPES} are public so other services (e.g.
 * wiki attachment services) can reuse them without duplicating the policy.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ObjectStorageService {

  /** Maximum permitted upload size: 10 MB. */
  public static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

  /** MIME types accepted by ShipFlow's object-storage façade. */
  public static final Set<String> ALLOWED_CONTENT_TYPES =
      Set.of(
          "image/jpeg",
          "image/png",
          "image/gif",
          "image/webp",
          "image/svg+xml",
          "application/pdf",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "application/msword",
          "text/plain",
          "text/markdown",
          "application/json",
          "application/zip",
          "application/x-zip-compressed");

  private final StorageConfigService configService;
  private final ObjectStorageRegistry registry;
  private final ObjectMapper objectMapper;

  // ── Public API ────────────────────────────────────────────────────────────

  /**
   * Validates and stores a file to the currently active storage backend.
   *
   * @param keyHint logical prefix (e.g. {@code "wiki/pages"} or {@code "tasks/123"})
   * @param originalFilename filename supplied by the upload client
   * @param contentType MIME type of the file
   * @param sizeBytes declared byte length
   * @param stream raw byte stream
   * @return a {@link StoredObjectRef} identifying the stored object
   * @throws IllegalArgumentException if size or content-type validation fails
   */
  public StoredObjectRef store(
      String keyHint,
      String originalFilename,
      String contentType,
      long sizeBytes,
      InputStream stream) {
    validateFileInput(contentType, sizeBytes);
    return doStore(keyHint, originalFilename, contentType, sizeBytes, stream);
  }

  /**
   * Stores a file WITHOUT enforcing the façade's size/MIME allowlist.
   *
   * <p>Intended for callers that enforce their OWN, broader upload policy before invoking this
   * method — notably {@link com.github.farzadsedaghatbin.shipflow.service.DocumentService}, which
   * permits video and larger media for bug-report attachments. The façade must NOT re-impose its
   * stricter image/PDF/DOC allowlist (which excludes video) or those uploads would be wrongly
   * rejected. The caller is responsible for validating size and type beforehand.
   *
   * @param keyHint logical prefix (e.g. {@code "documents"} or {@code "bug-attachments"})
   * @param originalFilename filename supplied by the upload client
   * @param contentType MIME type of the file
   * @param sizeBytes declared byte length
   * @param stream raw byte stream
   * @return a {@link StoredObjectRef} identifying the stored object
   */
  public StoredObjectRef storeWithoutValidation(
      String keyHint,
      String originalFilename,
      String contentType,
      long sizeBytes,
      InputStream stream) {
    return doStore(keyHint, originalFilename, contentType, sizeBytes, stream);
  }

  /**
   * Performs the actual store against the active backend. Shared by the validating {@link #store}
   * and the non-validating {@link #storeWithoutValidation} entry points.
   */
  private StoredObjectRef doStore(
      String keyHint,
      String originalFilename,
      String contentType,
      long sizeBytes,
      InputStream stream) {
    StorageConfig cfg = configService.getActiveConfig();
    JsonNode cfgNode = parseConfig(cfg.getConfig());
    String bucket = extractBucket(cfgNode);
    String key = StorageKeyGenerator.generate(keyHint, originalFilename);

    StorePutContext ctx =
        StorePutContext.builder()
            .bucket(bucket)
            .key(key)
            .stream(stream)
            .contentType(contentType)
            .sizeBytes(sizeBytes)
            .config(cfgNode)
            .build();

    log.debug("Storing object: keyHint={} key={} contentType={} size={}", keyHint, key, contentType, sizeBytes);
    return registry.get(cfg.getActiveProvider()).store(ctx);
  }

  /**
   * Retrieves an object from the specified provider backend.
   *
   * @param objectProvider the provider that originally stored the object
   * @param storageKey the key returned at store time
   * @return a {@link DownloadResource} wrapping the byte stream and metadata
   */
  public DownloadResource retrieve(StorageProviderType objectProvider, String storageKey) {
    Object[] triple = resolveProviderConfig(objectProvider);
    ObjectStorageProvider provider = (ObjectStorageProvider) triple[0];
    String bucket = (String) triple[1];
    JsonNode cfgNode = (JsonNode) triple[2];
    return provider.retrieve(bucket, storageKey, cfgNode);
  }

  /**
   * Deletes an object from the specified provider backend. Best-effort — providers log warnings
   * rather than throwing on missing objects.
   *
   * @param objectProvider the provider that originally stored the object
   * @param storageKey the key returned at store time
   */
  public void delete(StorageProviderType objectProvider, String storageKey) {
    Object[] triple = resolveProviderConfig(objectProvider);
    ObjectStorageProvider provider = (ObjectStorageProvider) triple[0];
    String bucket = (String) triple[1];
    JsonNode cfgNode = (JsonNode) triple[2];
    provider.delete(bucket, storageKey, cfgNode);
  }

  /**
   * Returns a pre-signed URL for direct access (supported by S3/MinIO only).
   *
   * @param objectProvider the provider that originally stored the object
   * @param storageKey the key returned at store time
   * @param expirySeconds how long the URL remains valid
   * @return an {@link Optional} containing the URL, or empty for LOCAL_FS
   */
  public Optional<String> presignUrl(
      StorageProviderType objectProvider, String storageKey, long expirySeconds) {
    Object[] triple = resolveProviderConfig(objectProvider);
    ObjectStorageProvider provider = (ObjectStorageProvider) triple[0];
    String bucket = (String) triple[1];
    JsonNode cfgNode = (JsonNode) triple[2];
    return provider.presignUrl(bucket, storageKey, expirySeconds, cfgNode);
  }

  /** Returns the {@link StorageProviderType} currently active. */
  public StorageProviderType activeProvider() {
    return configService.getActiveConfig().getActiveProvider();
  }

  /**
   * Validates content-type and size without performing any I/O. Reusable by other services that
   * need to enforce the same policy before calling {@link #store}.
   *
   * @throws IllegalArgumentException on policy violation
   */
  public void validateFileInput(String contentType, long sizeBytes) {
    if (sizeBytes > MAX_FILE_SIZE) {
      throw new IllegalArgumentException("File exceeds the 10 MB limit");
    }
    if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
      throw new IllegalArgumentException(
          "Unsupported file type '"
              + contentType
              + "'. Allowed: images, PDF, DOCX, DOC, TXT, MD, JSON, ZIP");
    }
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  /**
   * Resolves the provider instance, bucket, and config node for a given objectProvider type.
   * If {@code objectProvider} matches the currently active provider the active config is used;
   * otherwise a minimal empty config is used (for legacy data written by a different provider).
   *
   * @return {@code Object[]{ObjectStorageProvider, String bucket, JsonNode cfgNode}}
   */
  private Object[] resolveProviderConfig(StorageProviderType objectProvider) {
    StorageConfig cfg = configService.getActiveConfig();
    JsonNode cfgNode;
    String bucket;
    if (objectProvider == cfg.getActiveProvider()) {
      cfgNode = parseConfig(cfg.getConfig());
      bucket = extractBucket(cfgNode);
    } else {
      // Legacy object stored by a different provider — use empty config, no bucket.
      cfgNode = objectMapper.createObjectNode();
      bucket = null;
    }
    return new Object[] {registry.get(objectProvider), bucket, cfgNode};
  }

  /** Parses a JSON string into a {@link JsonNode}; wraps {@link java.io.IOException} as runtime. */
  private JsonNode parseConfig(String configJson) {
    try {
      return objectMapper.readTree(configJson);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse storage config JSON: " + e.getMessage(), e);
    }
  }

  /** Extracts the {@code bucket} field from a config node, returning {@code null} if absent. */
  private static String extractBucket(JsonNode cfgNode) {
    if (cfgNode != null && cfgNode.has("bucket")) {
      return cfgNode.get("bucket").asText(null);
    }
    return null;
  }
}
