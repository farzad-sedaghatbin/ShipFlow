package com.github.farzadsedaghatbin.shipflow.dto.storage;

import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageConfigDTO {
  private StorageProviderType activeProvider;
  // Non-secret config fields
  private String bucket;
  private String endpoint;
  private String region;
  private Boolean pathStyleAccess;
  // Secret projection — NEVER include accessKey/secretKey values
  private Boolean hasAccessKey;
  private Boolean hasSecretKey;
}
