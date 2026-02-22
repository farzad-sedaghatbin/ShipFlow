package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.audit.EntityHistoryDTO;
import com.github.farzadsedaghatbin.shipflow.dto.audit.EntityHistoryDTO.RevisionType;
import com.github.farzadsedaghatbin.shipflow.dto.audit.FieldChangeDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.audit.AuditRevisionEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for querying entity revision history using Hibernate Envers. Provides
 * methods to retrieve change history with computed field-level diffs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuditService {

  @PersistenceContext
  private EntityManager entityManager;

  /**
   * Get the change history for a Task entity.
   */
  public Page<EntityHistoryDTO> getTaskHistory(Long taskId, Pageable pageable) {
    return getEntityHistory(Task.class, taskId, pageable, this::getTaskFieldValue);
  }

  /**
   * Get the change history for a BugReport entity.
   */
  public Page<EntityHistoryDTO> getBugReportHistory(Long bugId, Pageable pageable) {
    return getEntityHistory(BugReport.class, bugId, pageable, this::getBugReportFieldValue);
  }

  /**
   * Get the change history for a Pitch entity.
   */
  public Page<EntityHistoryDTO> getPitchHistory(Long pitchId, Pageable pageable) {
    return getEntityHistory(Pitch.class, pitchId, pageable, this::getPitchFieldValue);
  }

  /**
   * Get the change history for a TestCase entity.
   */
  public Page<EntityHistoryDTO> getTestCaseHistory(Long testCaseId, Pageable pageable) {
    return getEntityHistory(TestCase.class, testCaseId, pageable, this::getTestCaseFieldValue);
  }

  /**
   * Generic method to get entity history with computed field diffs.
   */
  private <T> Page<EntityHistoryDTO> getEntityHistory(Class<T> entityClass, Long entityId, Pageable pageable,
      FieldValueExtractor<T> fieldExtractor) {

    AuditReader auditReader = AuditReaderFactory.get(entityManager);

    // Get all revisions for this entity
    @SuppressWarnings("unchecked")
    List<Object[]> revisions = auditReader.createQuery().forRevisionsOfEntity(entityClass, false, true)
        .add(AuditEntity.id().eq(entityId)).addOrder(AuditEntity.revisionNumber().desc()).getResultList();

    if (revisions.isEmpty()) {
      return Page.empty(pageable);
    }

    // Convert to DTOs with computed diffs
    List<EntityHistoryDTO> historyList = new ArrayList<>();
    T previousEntity = null;

    // Process revisions in chronological order for diff calculation
    List<Object[]> chronologicalRevisions = new ArrayList<>(revisions);
    Collections.reverse(chronologicalRevisions);

    for (Object[] revisionData : chronologicalRevisions) {
      @SuppressWarnings("unchecked")
      T entity = (T) revisionData[0];
      AuditRevisionEntity revisionEntity = (AuditRevisionEntity) revisionData[1];
      org.hibernate.envers.RevisionType enversRevisionType = (org.hibernate.envers.RevisionType) revisionData[2];

      EntityHistoryDTO historyDTO = EntityHistoryDTO.builder().revisionNumber((long) revisionEntity.getId())
          .revisionDate(convertToLocalDateTime(revisionEntity.getTimestamp()))
          .modifiedBy(revisionEntity.getModifiedBy() != null ? revisionEntity.getModifiedBy() : "system")
          .revisionType(convertRevisionType(enversRevisionType))
          .changes(computeFieldChanges(previousEntity, entity, fieldExtractor, enversRevisionType)).build();

      historyList.add(historyDTO);
      previousEntity = entity;
    }

    // Reverse to show newest first
    Collections.reverse(historyList);

    // Apply pagination
    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), historyList.size());

    if (start >= historyList.size()) {
      return new PageImpl<>(Collections.emptyList(), pageable, historyList.size());
    }

    List<EntityHistoryDTO> pagedList = historyList.subList(start, end);
    return new PageImpl<>(pagedList, pageable, historyList.size());
  }

  /**
   * Compute field-level changes between two entity states.
   */
  private <T> List<FieldChangeDTO> computeFieldChanges(T previousEntity, T currentEntity,
      FieldValueExtractor<T> fieldExtractor, org.hibernate.envers.RevisionType revisionType) {

    List<FieldChangeDTO> changes = new ArrayList<>();

    if (revisionType == org.hibernate.envers.RevisionType.DEL) {
      // For deletions, no field changes to show
      return changes;
    }

    // Get the audited field names for this entity type
    List<String> auditedFields = getAuditedFieldNames(currentEntity.getClass());

    for (String fieldName : auditedFields) {
      String oldValue = previousEntity != null ? fieldExtractor.getValue(previousEntity, fieldName) : null;
      String newValue = fieldExtractor.getValue(currentEntity, fieldName);

      // Only include if there's an actual change (or it's a creation)
      if (previousEntity == null || !Objects.equals(oldValue, newValue)) {
        changes.add(
            FieldChangeDTO.builder().fieldName(fieldName).oldValue(oldValue).newValue(newValue).build());
      }
    }

    return changes;
  }

  /**
   * Get the list of audited field names for an entity class.
   */
  private List<String> getAuditedFieldNames(Class<?> entityClass) {
    if (entityClass.equals(Task.class)) {
      return List.of("title", "description", "status", "priority", "category", "assignee", "pairAssignee");
    } else if (entityClass.equals(BugReport.class)) {
      return List.of("title", "description", "status", "severity", "assignee", "resolution");
    } else if (entityClass.equals(Pitch.class)) {
      return List.of("title", "description", "status", "team", "isCircuitBreakerTriggered",
          "circuitBreakerReason");
    } else if (entityClass.equals(TestCase.class)) {
      return List.of("title", "description", "status", "priority", "type");
    }
    return Collections.emptyList();
  }

  /**
   * Extract field value from a Task entity.
   */
  private String getTaskFieldValue(Task task, String fieldName) {
    if (task == null)
      return null;

    return switch (fieldName) {
      case "title" -> task.getTitle();
      case "description" -> task.getDescription();
      case "status" -> task.getStatus() != null ? task.getStatus().name() : null;
      case "priority" -> task.getPriority() != null ? task.getPriority().name() : null;
      case "category" -> task.getCategory() != null ? task.getCategory().name() : null;
      case "assignee" -> task.getAssignee() != null ? task.getAssignee().getName() : null;
      case "pairAssignee" -> task.getPairAssignee() != null ? task.getPairAssignee().getName() : null;
      default -> null;
    };
  }

  /**
   * Extract field value from a BugReport entity.
   */
  private String getBugReportFieldValue(BugReport bugReport, String fieldName) {
    if (bugReport == null)
      return null;

    return switch (fieldName) {
      case "title" -> bugReport.getTitle();
      case "description" -> bugReport.getDescription();
      case "status" -> bugReport.getStatus() != null ? bugReport.getStatus().name() : null;
      case "severity" -> bugReport.getSeverity() != null ? bugReport.getSeverity().name() : null;
      case "assignee" -> bugReport.getAssignee() != null ? bugReport.getAssignee().getName() : null;
      case "resolution" -> bugReport.getResolution();
      default -> null;
    };
  }

  /**
   * Extract field value from a Pitch entity.
   */
  private String getPitchFieldValue(Pitch pitch, String fieldName) {
    if (pitch == null)
      return null;

    return switch (fieldName) {
      case "title" -> pitch.getTitle();
      case "description" -> pitch.getDescription();
      case "status" -> pitch.getStatus() != null ? pitch.getStatus().name() : null;
      case "team" -> pitch.getTeam() != null ? pitch.getTeam().getName() : null;
      case "isCircuitBreakerTriggered" ->
        pitch.getIsCircuitBreakerTriggered() != null ? pitch.getIsCircuitBreakerTriggered().toString() : null;
      case "circuitBreakerReason" -> pitch.getCircuitBreakerReason();
      default -> null;
    };
  }

  /**
   * Extract field value from a TestCase entity.
   */
  private String getTestCaseFieldValue(TestCase testCase, String fieldName) {
    if (testCase == null)
      return null;

    return switch (fieldName) {
      case "title" -> testCase.getTitle();
      case "description" -> testCase.getDescription();
      case "status" -> testCase.getStatus() != null ? testCase.getStatus().name() : null;
      case "priority" -> testCase.getPriority() != null ? testCase.getPriority().name() : null;
      case "type" -> testCase.getType() != null ? testCase.getType().name() : null;
      default -> null;
    };
  }

  /**
   * Convert Envers revision type to our DTO revision type.
   */
  private RevisionType convertRevisionType(org.hibernate.envers.RevisionType enversType) {
    return switch (enversType) {
      case ADD -> RevisionType.CREATED;
      case MOD -> RevisionType.MODIFIED;
      case DEL -> RevisionType.DELETED;
    };
  }

  /**
   * Convert timestamp to LocalDateTime.
   */
  private LocalDateTime convertToLocalDateTime(long timestamp) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
  }

  /**
   * Functional interface for extracting field values from entities.
   */
  @FunctionalInterface
  private interface FieldValueExtractor<T> {
    String getValue(T entity, String fieldName);
  }
}
