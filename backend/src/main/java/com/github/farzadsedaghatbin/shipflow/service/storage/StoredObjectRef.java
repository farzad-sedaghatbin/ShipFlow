package com.github.farzadsedaghatbin.shipflow.service.storage;

import lombok.Builder;
import lombok.Value;

/** Identifies a stored object by its bucket, key, and basic metadata. */
@Value
@Builder
public class StoredObjectRef {
  String bucket;
  String key;
  String contentType;
  long sizeBytes;
}
