import api from './api';

export type StorageProvider = 'LOCAL_FS' | 'S3' | 'MINIO';

export interface StorageConfigDTO {
  activeProvider: StorageProvider;
  bucket?: string;
  endpoint?: string;
  region?: string;
  pathStyleAccess?: boolean;
  hasAccessKey: boolean;
  hasSecretKey: boolean;
}

export interface StorageProviderConfig {
  bucket?: string;
  endpoint?: string;
  region?: string;
  pathStyleAccess?: boolean;
  accessKey?: string;
  secretKey?: string;
}

export interface StorageConfigRequest {
  activeProvider: StorageProvider;
  config: StorageProviderConfig;
}

export interface StorageTestResult {
  ok: boolean;
  message: string;
}

export interface StorageMigrationResult {
  migrated: number;
  skipped: number;
  failed: number;
  total: number;
}

/**
 * Service for managing object storage configuration.
 * Secret values (accessKey, secretKey) are never returned by the API —
 * the DTO only carries hasAccessKey / hasSecretKey booleans.
 */
export const storageService = {
  /**
   * Load the current storage configuration.
   * Never returns secret credentials — only presence flags.
   */
  getStorageConfig: () => api.get<StorageConfigDTO>('/admin/storage'),

  /**
   * Update storage configuration.
   * Omit accessKey/secretKey from the request body to preserve existing credentials.
   */
  updateStorageConfig: (req: StorageConfigRequest) =>
    api.put<StorageConfigDTO>('/admin/storage', req),

  /**
   * Test connectivity with the supplied configuration without persisting it.
   */
  testStorageConnection: (req: StorageConfigRequest) =>
    api.post<StorageTestResult>('/admin/storage/test', req),

  /**
   * Migrate all existing uploaded files to the currently-configured storage backend.
   * Long-running — returns counts of migrated / skipped / failed files.
   */
  migrateStorage: () => api.post<StorageMigrationResult>('/admin/storage/migrate'),
};
