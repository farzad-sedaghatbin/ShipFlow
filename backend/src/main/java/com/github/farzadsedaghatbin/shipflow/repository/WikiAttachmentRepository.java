package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.WikiAttachment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WikiAttachmentRepository extends JpaRepository<WikiAttachment, Long> {

  List<WikiAttachment> findByPageIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long pageId);
}
