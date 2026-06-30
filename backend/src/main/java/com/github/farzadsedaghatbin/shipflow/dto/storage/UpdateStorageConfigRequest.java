package com.github.farzadsedaghatbin.shipflow.dto.storage;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStorageConfigRequest {
  private StorageProviderType activeProvider;
  // May include accessKey/secretKey — if blank/absent, preserved from stored config
  private ObjectNode config;
}
