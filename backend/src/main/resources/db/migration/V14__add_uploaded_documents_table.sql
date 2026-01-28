-- V14: Add uploaded_documents table for document storage and text extraction

CREATE TABLE uploaded_documents (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    file_name VARCHAR(500) NOT NULL,
    original_file_name VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT,
    storage_path VARCHAR(500),
    extracted_text TEXT,
    text_extracted BOOLEAN NOT NULL DEFAULT FALSE,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    uploader_id BIGINT,
    uploader_username VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    indexed_for_qa BOOLEAN NOT NULL DEFAULT FALSE
);

-- Add indexes
CREATE INDEX idx_uploaded_documents_entity ON uploaded_documents(entity_type, entity_id);
CREATE INDEX idx_uploaded_documents_uploader ON uploaded_documents(uploader_id);
CREATE INDEX idx_uploaded_documents_indexed ON uploaded_documents(indexed_for_qa);
