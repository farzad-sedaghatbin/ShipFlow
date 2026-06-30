package com.github.farzadsedaghatbin.shipflow.service.storage.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.InvalidConfigException;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import java.net.URI;
import java.util.Optional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import org.springframework.stereotype.Component;

/**
 * MinIO implementation of {@link
 * com.github.farzadsedaghatbin.shipflow.service.storage.ObjectStorageProvider}.
 *
 * <p>Requires config fields: {@code bucket}, {@code endpoint}, {@code accessKey}, {@code
 * secretKey}. {@code region} is optional (defaults to {@value #DEFAULT_REGION}). Path-style access
 * is always forced to {@code true} because MinIO uses path-style URLs.
 */
@Component
public class MinioStorageProvider extends AwsS3BaseStorageProvider {

  @Override
  public StorageProviderType getType() {
    return StorageProviderType.MINIO;
  }

  @Override
  public void validateConfig(JsonNode config) throws InvalidConfigException {
    super.validateConfig(config); // validates bucket
    requireField(config, KEY_ENDPOINT);
    requireField(config, KEY_ACCESS_KEY);
    requireField(config, KEY_SECRET_KEY);
  }

  // ── Client factories — MinIO always uses path-style + endpoint ────────────

  @Override
  protected S3Client s3Client(JsonNode config) {
    String accessKey = cfgStr(config, KEY_ACCESS_KEY);
    String secretKey = cfgStr(config, KEY_SECRET_KEY);
    String region = Optional.ofNullable(cfgStr(config, KEY_REGION)).orElse(DEFAULT_REGION);
    String endpoint = cfgStr(config, KEY_ENDPOINT);

    S3ClientBuilder builder =
        S3Client.builder()
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)))
            .region(Region.of(region))
            .endpointOverride(URI.create(endpoint))
            .serviceConfiguration(
                S3Configuration.builder().pathStyleAccessEnabled(true).build());

    return builder.build();
  }

  @Override
  protected S3Presigner s3Presigner(JsonNode config) {
    String accessKey = cfgStr(config, KEY_ACCESS_KEY);
    String secretKey = cfgStr(config, KEY_SECRET_KEY);
    String region = Optional.ofNullable(cfgStr(config, KEY_REGION)).orElse(DEFAULT_REGION);
    String endpoint = cfgStr(config, KEY_ENDPOINT);

    return S3Presigner.builder()
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretKey)))
        .region(Region.of(region))
        .endpointOverride(URI.create(endpoint))
        .serviceConfiguration(
            S3Configuration.builder().pathStyleAccessEnabled(true).build())
        .build();
  }

}
