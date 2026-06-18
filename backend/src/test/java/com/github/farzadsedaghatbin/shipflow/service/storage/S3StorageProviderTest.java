package com.github.farzadsedaghatbin.shipflow.service.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.InvalidConfigException;
import com.github.farzadsedaghatbin.shipflow.service.storage.provider.AwsS3BaseStorageProvider;
import com.github.farzadsedaghatbin.shipflow.service.storage.provider.MinioStorageProvider;
import com.github.farzadsedaghatbin.shipflow.service.storage.provider.S3StorageProvider;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * Pure-unit tests for {@link S3StorageProvider} (and incidentally {@link MinioStorageProvider}).
 *
 * <p>No real S3/MinIO endpoints are used. The {@link S3Client} and {@link S3Presigner} are Mockito
 * mocks injected via the protected factory-method seam on {@link AwsS3BaseStorageProvider}.
 */
class S3StorageProviderTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  // ── Helpers to build config nodes ─────────────────────────────────────────

  private static JsonNode s3Config() throws Exception {
    return MAPPER.readTree(
        "{\"bucket\":\"my-bucket\",\"region\":\"us-east-1\","
            + "\"accessKey\":\"AK\",\"secretKey\":\"SK\"}");
  }

  private static JsonNode minioConfig() throws Exception {
    return MAPPER.readTree(
        "{\"bucket\":\"minio-bucket\",\"endpoint\":\"http://localhost:9000\","
            + "\"accessKey\":\"minioadmin\",\"secretKey\":\"minioadmin\"}");
  }

  // ── Test subclass that injects mocked S3Client / S3Presigner ──────────────

  /**
   * Thin subclass of S3StorageProvider that returns pre-built mocks from the factory methods so
   * tests never open a real socket.
   */
  private static class MockableS3Provider extends S3StorageProvider {

    private final S3Client mockClient;
    private final S3Presigner mockPresigner;

    MockableS3Provider(S3Client mockClient, S3Presigner mockPresigner) {
      this.mockClient = mockClient;
      this.mockPresigner = mockPresigner;
    }

    @Override
    protected S3Client s3Client(JsonNode config) {
      return mockClient;
    }

    @Override
    protected S3Presigner s3Presigner(JsonNode config) {
      return mockPresigner;
    }
  }

  // ── Shared mocks ──────────────────────────────────────────────────────────

  private S3Client mockClient;
  private S3Presigner mockPresigner;
  private MockableS3Provider provider;

  @BeforeEach
  void setUp() {
    mockClient = mock(S3Client.class);
    mockPresigner = mock(S3Presigner.class);
    provider = new MockableS3Provider(mockClient, mockPresigner);
  }

  // ── getType ───────────────────────────────────────────────────────────────

  @Test
  void getType_returnsS3() {
    assertThat(provider.getType()).isEqualTo(StorageProviderType.S3);
  }

  @Test
  void supportsPresign_isTrue() {
    assertThat(provider.supportsPresign()).isTrue();
  }

  // ── store ─────────────────────────────────────────────────────────────────

  @Test
  void store_callsPutObjectWithCorrectBucketAndKey() throws Exception {
    when(mockClient.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .thenReturn(PutObjectResponse.builder().build());

    byte[] data = "hello s3".getBytes(StandardCharsets.UTF_8);
    StorePutContext ctx =
        StorePutContext.builder()
            .bucket("my-bucket")
            .key("uploads/test.txt")
            .stream(new ByteArrayInputStream(data))
            .contentType("text/plain")
            .sizeBytes(data.length)
            .config(s3Config())
            .build();

    StoredObjectRef ref = provider.store(ctx);

    // Verify PutObjectRequest has correct bucket + key + contentType
    ArgumentCaptor<PutObjectRequest> reqCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
    verify(mockClient).putObject(reqCaptor.capture(), any(RequestBody.class));

    PutObjectRequest captured = reqCaptor.getValue();
    assertThat(captured.bucket()).isEqualTo("my-bucket");
    assertThat(captured.key()).isEqualTo("uploads/test.txt");
    assertThat(captured.contentType()).isEqualTo("text/plain");

    // Verify returned ref mirrors the context
    assertThat(ref.getBucket()).isEqualTo("my-bucket");
    assertThat(ref.getKey()).isEqualTo("uploads/test.txt");
    assertThat(ref.getContentType()).isEqualTo("text/plain");
    assertThat(ref.getSizeBytes()).isEqualTo(data.length);
  }

  // ── retrieve ──────────────────────────────────────────────────────────────

  @Test
  void retrieve_issuesGetObjectRequestWithCorrectBucketAndKey() throws Exception {
    byte[] payload = "file content".getBytes(StandardCharsets.UTF_8);
    GetObjectResponse meta =
        GetObjectResponse.builder().contentType("application/octet-stream").contentLength((long) payload.length).build();

    InputStream wrappedStream =
        AbortableInputStream.create(new ByteArrayInputStream(payload));
    ResponseInputStream<GetObjectResponse> responseStream =
        new ResponseInputStream<>(meta, AbortableInputStream.create(new ByteArrayInputStream(payload)));

    ArgumentCaptor<GetObjectRequest> reqCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
    when(mockClient.getObject(reqCaptor.capture())).thenReturn(responseStream);

    DownloadResource resource = provider.retrieve("my-bucket", "uploads/test.txt", s3Config());

    // Verify GetObjectRequest
    GetObjectRequest captured = reqCaptor.getValue();
    assertThat(captured.bucket()).isEqualTo("my-bucket");
    assertThat(captured.key()).isEqualTo("uploads/test.txt");

    // Verify returned DownloadResource
    assertThat(resource.getContentType()).isEqualTo("application/octet-stream");
    assertThat(resource.getSizeBytes()).isEqualTo(payload.length);
    assertThat(resource.getFilename()).isEqualTo("test.txt");

    byte[] returnedBytes = resource.getStream().readAllBytes();
    assertThat(returnedBytes).isEqualTo(payload);
  }

  // ── delete ────────────────────────────────────────────────────────────────

  @Test
  void delete_issuesDeleteObjectRequestWithCorrectBucketAndKey() throws Exception {
    ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);

    provider.delete("my-bucket", "uploads/file.bin", s3Config());

    verify(mockClient).deleteObject(captor.capture());
    DeleteObjectRequest captured = captor.getValue();
    assertThat(captured.bucket()).isEqualTo("my-bucket");
    assertThat(captured.key()).isEqualTo("uploads/file.bin");
  }

  // ── presignUrl ────────────────────────────────────────────────────────────

  @Test
  void presignUrl_returnsPresignedUrlString() throws Exception {
    PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
    when(presigned.url()).thenReturn(new URL("https://my-bucket.s3.amazonaws.com/test.txt?X-Amz=sig"));
    when(mockPresigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presigned);

    java.util.Optional<String> url =
        provider.presignUrl("my-bucket", "test.txt", 3600L, s3Config());

    assertThat(url).isPresent();
    assertThat(url.get()).startsWith("https://my-bucket.s3.amazonaws.com/test.txt");
  }

  // ── testConnection ────────────────────────────────────────────────────────

  @Test
  void testConnection_returnsOkWhenHeadBucketSucceeds() throws Exception {
    when(mockClient.headBucket(any(HeadBucketRequest.class)))
        .thenReturn(HeadBucketResponse.builder().build());

    com.github.farzadsedaghatbin.shipflow.service.knowledge.source.ConnectionStatus status =
        provider.testConnection(s3Config());

    assertThat(status.isOk()).isTrue();
  }

  @Test
  void testConnection_returnsFailWhenHeadBucketThrows() throws Exception {
    when(mockClient.headBucket(any(HeadBucketRequest.class)))
        .thenThrow(
            NoSuchBucketException.builder().message("No such bucket").build());

    com.github.farzadsedaghatbin.shipflow.service.knowledge.source.ConnectionStatus status =
        provider.testConnection(s3Config());

    assertThat(status.isOk()).isFalse();
    assertThat(status.getMessage()).contains("No such bucket");
  }

  // ── validateConfig ────────────────────────────────────────────────────────

  @Nested
  class ValidateConfig {

    @Test
    void passes_withAllRequiredFields() throws Exception {
      org.assertj.core.api.Assertions.assertThatNoException()
          .isThrownBy(() -> provider.validateConfig(s3Config()));
    }

    @Test
    void throws_whenBucketMissing() throws Exception {
      JsonNode cfg =
          MAPPER.readTree(
              "{\"region\":\"us-east-1\",\"accessKey\":\"AK\",\"secretKey\":\"SK\"}");
      assertThatThrownBy(() -> provider.validateConfig(cfg))
          .isInstanceOf(InvalidConfigException.class)
          .hasMessageContaining("bucket");
    }

    @Test
    void throws_whenRegionMissing() throws Exception {
      JsonNode cfg =
          MAPPER.readTree(
              "{\"bucket\":\"b\",\"accessKey\":\"AK\",\"secretKey\":\"SK\"}");
      assertThatThrownBy(() -> provider.validateConfig(cfg))
          .isInstanceOf(InvalidConfigException.class)
          .hasMessageContaining("region");
    }

    @Test
    void throws_whenAccessKeyMissing() throws Exception {
      JsonNode cfg =
          MAPPER.readTree(
              "{\"bucket\":\"b\",\"region\":\"us-east-1\",\"secretKey\":\"SK\"}");
      assertThatThrownBy(() -> provider.validateConfig(cfg))
          .isInstanceOf(InvalidConfigException.class)
          .hasMessageContaining("accessKey");
    }

    @Test
    void throws_whenSecretKeyMissing() throws Exception {
      JsonNode cfg =
          MAPPER.readTree(
              "{\"bucket\":\"b\",\"region\":\"us-east-1\",\"accessKey\":\"AK\"}");
      assertThatThrownBy(() -> provider.validateConfig(cfg))
          .isInstanceOf(InvalidConfigException.class)
          .hasMessageContaining("secretKey");
    }
  }

  // ── MinioStorageProvider validateConfig ───────────────────────────────────

  @Nested
  class MinioValidateConfig {

    private MinioStorageProvider minioProvider;

    @BeforeEach
    void setUp() {
      // Plain MinioStorageProvider — only testing validateConfig (no network calls)
      minioProvider = new MinioStorageProvider();
    }

    @Test
    void passes_withAllRequiredFields() throws Exception {
      org.assertj.core.api.Assertions.assertThatNoException()
          .isThrownBy(() -> minioProvider.validateConfig(minioConfig()));
    }

    @Test
    void throws_whenEndpointMissing() throws Exception {
      JsonNode cfg =
          MAPPER.readTree(
              "{\"bucket\":\"b\",\"accessKey\":\"admin\",\"secretKey\":\"secret\"}");
      assertThatThrownBy(() -> minioProvider.validateConfig(cfg))
          .isInstanceOf(InvalidConfigException.class)
          .hasMessageContaining("endpoint");
    }

    @Test
    void throws_whenBucketMissing() throws Exception {
      JsonNode cfg =
          MAPPER.readTree(
              "{\"endpoint\":\"http://localhost:9000\",\"accessKey\":\"admin\",\"secretKey\":\"s\"}");
      assertThatThrownBy(() -> minioProvider.validateConfig(cfg))
          .isInstanceOf(InvalidConfigException.class)
          .hasMessageContaining("bucket");
    }

    @Test
    void getType_returnsMinio() {
      assertThat(minioProvider.getType()).isEqualTo(StorageProviderType.MINIO);
    }
  }
}
