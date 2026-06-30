-- Route uploaded_documents (pitch/meeting/cycle/note docs + bug-report media) through the
-- object-storage SPI, mirroring task_attachments and wiki_attachments.
-- H2-safe: VARCHAR only, nullable, ADD COLUMN IF NOT EXISTS.
--
-- Legacy rows keep storage_provider/storage_key NULL and continue to be served from the local
-- filesystem via the existing storage_path column (read/delete fallback in DocumentService).
-- New uploads set storage_provider + storage_key and leave storage_path NULL.
-- StorageMigrationService.migrateDocuments() backfills legacy rows onto the active backend.

ALTER TABLE uploaded_documents ADD COLUMN IF NOT EXISTS storage_provider VARCHAR(20);
ALTER TABLE uploaded_documents ADD COLUMN IF NOT EXISTS storage_key VARCHAR(512);
