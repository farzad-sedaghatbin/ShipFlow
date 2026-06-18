package com.github.farzadsedaghatbin.shipflow.service.storage.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.InvalidConfigException;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import org.springframework.stereotype.Component;

/**
 * AWS S3 implementation of {@link
 * com.github.farzadsedaghatbin.shipflow.service.storage.ObjectStorageProvider}.
 *
 * <p>Requires config fields: {@code bucket}, {@code region}, {@code accessKey}, {@code secretKey}.
 * {@code endpoint} and {@code pathStyleAccess} are optional.
 */
@Component
public class S3StorageProvider extends AwsS3BaseStorageProvider {

  @Override
  public StorageProviderType getType() {
    return StorageProviderType.S3;
  }

  @Override
  public void validateConfig(JsonNode config) throws InvalidConfigException {
    super.validateConfig(config);
    requireField(config, KEY_REGION);
    requireField(config, KEY_ACCESS_KEY);
    requireField(config, KEY_SECRET_KEY);
  }

}
