package com.github.farzadsedaghatbin.shipflow.service.knowledge;

import com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument;
import com.github.farzadsedaghatbin.shipflow.event.DocumentUploadedEvent;
import com.github.farzadsedaghatbin.shipflow.repository.UploadedDocumentRepository;
import com.github.farzadsedaghatbin.shipflow.service.KnowledgeIngestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Indexes an uploaded document into the Knowledge Center for AI Q&A, off the upload request thread.
 *
 * <p>Embedding generation for a document can take tens of seconds (a large PDF, minutes). Doing it
 * inline in {@code DocumentService.uploadDocument} blocked the HTTP response that long, so the UI
 * looked frozen — the file was already stored, but nothing came back. This listener runs
 * <em>after</em> the upload transaction commits ({@link TransactionPhase#AFTER_COMMIT}) so the row
 * is visible, on a background thread ({@code @Async}) so the request returns immediately. There is
 * no transaction to join after commit, so the handler opens its own ({@code REQUIRES_NEW}) — a
 * plain {@code @Transactional} fails at startup for an AFTER_COMMIT listener. Mirrors
 * {@link WikiKnowledgeListener}.
 */
@Component
@Slf4j
public class DocumentKnowledgeListener {

  private final UploadedDocumentRepository documentRepository;
  private final ObjectProvider<KnowledgeIngestionService> ingestionServiceProvider;

  public DocumentKnowledgeListener(
      UploadedDocumentRepository documentRepository,
      ObjectProvider<KnowledgeIngestionService> ingestionServiceProvider) {
    this.documentRepository = documentRepository;
    this.ingestionServiceProvider = ingestionServiceProvider;
  }

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void onDocumentUploaded(DocumentUploadedEvent event) {
    try {
      KnowledgeIngestionService ingestionService = ingestionServiceProvider.getIfAvailable();
      if (ingestionService == null) {
        log.debug(
            "KnowledgeIngestionService not available (QA disabled); skipping ingest for document {}",
            event.documentId());
        return;
      }

      UploadedDocument document = documentRepository.findById(event.documentId()).orElse(null);
      if (document == null) {
        log.debug("Document {} not found, skipping ingest", event.documentId());
        return;
      }

      ingestionService.ingestDocument(document);
      document.setIndexedForQA(true);
      documentRepository.save(document);
      log.info("Indexed document {} into the Knowledge Center", document.getId());
    } catch (Exception e) {
      log.error(
          "DocumentKnowledgeListener failed for document {}: {}",
          event.documentId(),
          e.getMessage(),
          e);
    }
  }
}
