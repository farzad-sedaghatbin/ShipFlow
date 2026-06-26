package com.github.farzadsedaghatbin.shipflow.event;

/**
 * Published after an {@code UploadedDocument} row is committed so its Q&A indexing (slow embedding
 * generation) runs in the background instead of blocking the upload HTTP response.
 *
 * @param documentId the id of the freshly uploaded document
 */
public record DocumentUploadedEvent(Long documentId) {}
