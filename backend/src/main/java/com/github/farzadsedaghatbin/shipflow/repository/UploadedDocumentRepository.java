package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UploadedDocumentRepository extends JpaRepository<UploadedDocument, Long> {

  List<UploadedDocument> findByEntityTypeAndEntityId(String entityType, Long entityId);

  List<UploadedDocument> findByEntityTypeAndEntityIdAndIndexedForQAFalse(
      String entityType, Long entityId);

  List<UploadedDocument> findByIndexedForQAFalse();

  List<UploadedDocument> findByUploaderId(Long uploaderId);
}
