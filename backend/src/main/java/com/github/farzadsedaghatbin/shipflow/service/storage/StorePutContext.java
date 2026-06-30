package com.github.farzadsedaghatbin.shipflow.service.storage;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.InputStream;
import lombok.Builder;
import lombok.Value;

/** Carries all data needed to upload an object to backing storage. */
@Value
@Builder
public class StorePutContext {
  String bucket;
  String key;
  InputStream stream;
  String contentType;
  long sizeBytes;
  /** Provider-specific configuration (e.g. region, endpoint). May be {@code null}. */
  JsonNode config;
}
