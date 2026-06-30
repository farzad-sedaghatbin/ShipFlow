package com.github.farzadsedaghatbin.shipflow.service.storage;

import java.util.UUID;

/**
 * Shared utility for generating object-storage keys.
 *
 * <p>Format: {@code {keyHint}/{uuid}_{sanitizedFilename}}
 *
 * <p>Used by all {@link ObjectStorageProvider} implementations so key generation logic is not
 * duplicated. The sanitization mirrors {@code TaskAttachmentService#sanitize}.
 */
public final class StorageKeyGenerator {

  private StorageKeyGenerator() {}

  /**
   * Generates a storage key from a hint (e.g. folder path or feature namespace) and the original
   * filename supplied by the caller.
   *
   * @param keyHint the logical prefix / folder (may contain {@code /} separators)
   * @param originalFilename the filename as provided by the upload client
   * @return a safe, unique key in the form {@code {keyHint}/{uuid}_{sanitizedFilename}}
   */
  public static String generate(String keyHint, String originalFilename) {
    String sanitized = sanitize(originalFilename);
    return keyHint + "/" + UUID.randomUUID() + "_" + sanitized;
  }

  /**
   * Strips path separators and null bytes; truncates to 200 characters — mirrors
   * {@code TaskAttachmentService#sanitize}.
   */
  public static String sanitize(String name) {
    if (name == null || name.isBlank()) return "attachment";
    String safe = name.replaceAll("[/\\\\:\\*\\?\"<>|\\x00]", "_");
    return safe.length() > 200 ? safe.substring(0, 200) : safe;
  }
}
