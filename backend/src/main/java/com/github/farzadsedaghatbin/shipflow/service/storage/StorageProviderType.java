package com.github.farzadsedaghatbin.shipflow.service.storage;

/** Identifies the backing object-storage technology. */
public enum StorageProviderType {
  LOCAL_FS,
  S3,
  MINIO
}
