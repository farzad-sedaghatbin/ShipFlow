package com.github.farzadsedaghatbin.shipflow.service.storage.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.ConnectionStatus;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.InvalidConfigException;
import com.github.farzadsedaghatbin.shipflow.service.storage.DownloadResource;
import com.github.farzadsedaghatbin.shipflow.service.storage.ObjectStorageProvider;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorePutContext;
import com.github.farzadsedaghatbin.shipflow.service.storage.StoredObjectRef;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * Abstract base for AWS-SDK-v2-backed object-storage providers (S3 and MinIO).
 *
 * <p>Concrete subclasses supply {@link #getType()} and extend {@link #validateConfig}. The client
 * factories ({@link #s3Client} / {@link #s3Presigner}) are {@code protected} so test subclasses can
 * override them to inject Mockito mocks — no real network connection is ever opened in unit tests.
 */
@Slf4j
public abstract class AwsS3BaseStorageProvider implements ObjectStorageProvider {

  // ── Config key constants ──────────────────────────────────────────────────

  protected static final String KEY_BUCKET = "bucket";
  protected static final String KEY_REGION = "region";
  protected static final String KEY_ACCESS_KEY = "accessKey";
  protected static final String KEY_SECRET_KEY = "secretKey";
  protected static final String KEY_ENDPOINT = "endpoint";
  protected static final String KEY_PATH_STYLE = "pathStyleAccess";
  protected static final String DEFAULT_REGION = "us-east-1";

  // ── Helpers ───────────────────────────────────────────────────────────────

  /** Returns a non-blank string from a JsonNode config field, or {@code null} if absent/blank. */
  protected static String cfgStr(JsonNode config, String key) {
    if (config == null || !config.has(key)) return null;
    String v = config.get(key).asText(null);
    return (v == null || v.isBlank()) ? null : v;
  }

  protected static boolean cfgBool(JsonNode config, String key, boolean defaultValue) {
    if (config == null || !config.has(key)) return defaultValue;
    return config.get(key).asBoolean(defaultValue);
  }

  // ── Client factories (overridable for tests) ──────────────────────────────

  /**
   * Builds and returns an {@link S3Client} from the given provider configuration.
   *
   * <p>Subclasses (or test inner-classes) may override this to return a mock.
   */
  protected S3Client s3Client(JsonNode config) {
    String accessKey = cfgStr(config, KEY_ACCESS_KEY);
    String secretKey = cfgStr(config, KEY_SECRET_KEY);
    String region = Optional.ofNullable(cfgStr(config, KEY_REGION)).orElse(DEFAULT_REGION);
    String endpoint = cfgStr(config, KEY_ENDPOINT);
    boolean pathStyle = cfgBool(config, KEY_PATH_STYLE, endpoint != null);

    S3ClientBuilder builder =
        S3Client.builder()
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)))
            .region(Region.of(region));

    if (endpoint != null) {
      builder
          .endpointOverride(URI.create(endpoint))
          .serviceConfiguration(
              S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build());
    }

    return builder.build();
  }

  /**
   * Builds and returns an {@link S3Presigner} from the given provider configuration.
   *
   * <p>Subclasses (or test inner-classes) may override this to return a mock.
   */
  protected S3Presigner s3Presigner(JsonNode config) {
    String accessKey = cfgStr(config, KEY_ACCESS_KEY);
    String secretKey = cfgStr(config, KEY_SECRET_KEY);
    String region = Optional.ofNullable(cfgStr(config, KEY_REGION)).orElse(DEFAULT_REGION);
    String endpoint = cfgStr(config, KEY_ENDPOINT);
    boolean pathStyle = cfgBool(config, KEY_PATH_STYLE, endpoint != null);

    S3Presigner.Builder builder =
        S3Presigner.builder()
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)))
            .region(Region.of(region));

    if (endpoint != null) {
      builder
          .endpointOverride(URI.create(endpoint))
          .serviceConfiguration(
              S3Configuration.builder().pathStyleAccessEnabled(pathStyle).build());
    }

    return builder.build();
  }

  // ── ObjectStorageProvider — default implementations ───────────────────────

  /**
   * Validates that {@code bucket} is present in the config. Concrete subclasses call {@code
   * super.validateConfig(config)} and then add their own required-field checks.
   */
  @Override
  public void validateConfig(JsonNode config) throws InvalidConfigException {
    if (cfgStr(config, KEY_BUCKET) == null) {
      throw new InvalidConfigException("Storage config missing required field: " + KEY_BUCKET);
    }
  }

  @Override
  public StoredObjectRef store(StorePutContext ctx) {
    S3Client client = s3Client(ctx.getConfig());
    PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(ctx.getBucket())
            .key(ctx.getKey())
            .contentType(ctx.getContentType())
            .build();

    client.putObject(request, RequestBody.fromInputStream(ctx.getStream(), ctx.getSizeBytes()));

    log.debug("Stored object: bucket={} key={}", ctx.getBucket(), ctx.getKey());

    return StoredObjectRef.builder()
        .bucket(ctx.getBucket())
        .key(ctx.getKey())
        .contentType(ctx.getContentType())
        .sizeBytes(ctx.getSizeBytes())
        .build();
  }

  @Override
  public DownloadResource retrieve(String bucket, String key, JsonNode config) {
    S3Client client = s3Client(config);
    GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(key).build();

    software.amazon.awssdk.core.ResponseInputStream<GetObjectResponse> response =
        client.getObject(request);
    GetObjectResponse meta = response.response();

    String contentType = meta.contentType();
    long sizeBytes = meta.contentLength() != null ? meta.contentLength() : -1L;

    try {
      byte[] bytes = response.readAllBytes();
      log.debug("Retrieved object: bucket={} key={} size={}", bucket, key, bytes.length);
      return DownloadResource.builder()
          .stream(new ByteArrayInputStream(bytes))
          .contentType(contentType)
          .sizeBytes(sizeBytes)
          .filename(extractFilename(key))
          .build();
    } catch (Exception e) {
      throw new RuntimeException(
          "Failed to read object from S3: bucket=" + bucket + " key=" + key, e);
    }
  }

  @Override
  public void delete(String bucket, String key, JsonNode config) {
    S3Client client = s3Client(config);
    client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
    log.debug("Deleted object: bucket={} key={}", bucket, key);
  }

  @Override
  public Optional<String> presignUrl(
      String bucket, String key, long expirySeconds, JsonNode config) {
    S3Presigner presigner = s3Presigner(config);
    GetObjectPresignRequest presignRequest =
        GetObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(expirySeconds))
            .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
            .build();

    PresignedGetObjectRequest presigned = presigner.presignGetObject(presignRequest);
    String url = presigned.url().toString();
    log.debug("Pre-signed URL generated: bucket={} key={} expirySeconds={}", bucket, key, expirySeconds);
    return Optional.of(url);
  }

  @Override
  public boolean supportsPresign() {
    return true;
  }

  @Override
  public ConnectionStatus testConnection(JsonNode config) {
    try {
      S3Client client = s3Client(config);
      String bucket = cfgStr(config, KEY_BUCKET);
      client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
      return ConnectionStatus.ok();
    } catch (Exception e) {
      log.warn("S3/MinIO connection test failed: {}", e.getMessage());
      return ConnectionStatus.fail(e.getMessage());
    }
  }

  // ── Private helpers ───────────────────────────────────────────────────────

  private static String extractFilename(String key) {
    if (key == null || key.isBlank()) return "attachment";
    int slash = key.lastIndexOf('/');
    return slash >= 0 ? key.substring(slash + 1) : key;
  }
}
