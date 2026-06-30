package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.audit.AuditExportRow;
import com.github.farzadsedaghatbin.shipflow.dto.audit.EntityHistoryDTO;
import com.github.farzadsedaghatbin.shipflow.dto.audit.EntityHistoryDTO.RevisionType;
import com.github.farzadsedaghatbin.shipflow.dto.audit.FieldChangeDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.audit.AuditRevisionEntity;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
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

  private final UserRepository userRepository;

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

    // Resolve usernames to display names in batch to avoid N+1 lookups
    List<String> usernames = historyList.stream()
        .map(EntityHistoryDTO::getModifiedBy)
        .filter(name -> name != null && !"system".equals(name))
        .distinct()
        .collect(Collectors.toList());

    if (!usernames.isEmpty()) {
      Map<String, String> displayNames = userRepository.findByUsernameIn(usernames).stream()
          .collect(Collectors.toMap(
              User::getUsername,
              u -> u.getPerson() != null ? u.getPerson().getName() : u.getUsername()));

      historyList.replaceAll(dto -> {
        String resolved = displayNames.get(dto.getModifiedBy());
        if (resolved != null) {
          return EntityHistoryDTO.builder()
              .revisionNumber(dto.getRevisionNumber())
              .revisionDate(dto.getRevisionDate())
              .modifiedBy(resolved)
              .revisionType(dto.getRevisionType())
              .changes(dto.getChanges())
              .build();
        }
        return dto;
      });
    }

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

  // ========== Audit trail export (v1.9.0 — Production-Grade Self-Hosting) ==========

  /** Entity-type keys supported by the audit export. */
  private static final List<String> EXPORTABLE_TYPES = List.of("task", "bug", "pitch", "testcase");

  /**
   * Export the audit trail as flattened rows (one row per changed field) for the
   * given entity type and inclusive date range. Pass {@code "all"} (or null) to
   * export every supported type. Rows are returned newest-first.
   *
   * @param entityType task | bug | pitch | testcase | all
   * @param from       inclusive lower bound on revision date (nullable = no bound)
   * @param to         inclusive upper bound on revision date (nullable = no bound)
   */
  public List<AuditExportRow> exportAuditTrail(String entityType, LocalDate from, LocalDate to) {
    String key = entityType == null || entityType.isBlank() ? "all" : entityType.trim().toLowerCase();
    if (!"all".equals(key) && !EXPORTABLE_TYPES.contains(key)) {
      throw new IllegalArgumentException(
          "Unsupported audit entity type: " + entityType + ". Allowed: task, bug, pitch, testcase, all");
    }
    if (from != null && to != null && from.isAfter(to)) {
      throw new IllegalArgumentException("'from' date must not be after 'to' date");
    }

    Instant fromInstant = from != null ? from.atStartOfDay(ZoneId.systemDefault()).toInstant() : null;
    // 'to' is inclusive of the whole day — use an exclusive upper bound at next midnight.
    Instant toExclusive = to != null ? to.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant() : null;

    List<String> types = "all".equals(key) ? EXPORTABLE_TYPES : List.of(key);
    List<AuditExportRow> rows = new ArrayList<>();
    for (String type : types) {
      switch (type) {
        case "task" -> rows.addAll(
            collectAuditRows(Task.class, "task", Task::getId, this::getTaskFieldValue, fromInstant, toExclusive));
        case "bug" -> rows.addAll(collectAuditRows(BugReport.class, "bug", BugReport::getId,
            this::getBugReportFieldValue, fromInstant, toExclusive));
        case "pitch" -> rows.addAll(collectAuditRows(Pitch.class, "pitch", Pitch::getId, this::getPitchFieldValue,
            fromInstant, toExclusive));
        case "testcase" -> rows.addAll(collectAuditRows(TestCase.class, "testcase", TestCase::getId,
            this::getTestCaseFieldValue, fromInstant, toExclusive));
        default -> { /* unreachable — validated above */ }
      }
    }

    resolveModifiedByDisplayNames(rows);
    rows.sort(Comparator.comparing(AuditExportRow::getRevisionDate,
        Comparator.nullsLast(Comparator.naturalOrder())).reversed());
    return rows;
  }

  /**
   * Serialize the audit trail to CSV bytes (RFC-4180, formula-injection safe).
   */
  public byte[] exportAuditTrailCsv(String entityType, LocalDate from, LocalDate to) {
    List<AuditExportRow> rows = exportAuditTrail(entityType, from, to);
    StringBuilder sb = new StringBuilder();
    sb.append("entityType,entityId,revision,timestamp,modifiedBy,changeType,field,oldValue,newValue\n");
    for (AuditExportRow r : rows) {
      sb.append(csvEscape(r.getEntityType())).append(',')
          .append(r.getEntityId() != null ? r.getEntityId() : "").append(',')
          .append(r.getRevision() != null ? r.getRevision() : "").append(',')
          .append(csvEscape(r.getRevisionDate() != null ? r.getRevisionDate().toString() : "")).append(',')
          .append(csvEscape(r.getModifiedBy())).append(',')
          .append(csvEscape(r.getChangeType() != null ? r.getChangeType().name() : "")).append(',')
          .append(csvEscape(r.getField())).append(',').append(csvEscape(r.getOldValue())).append(',')
          .append(csvEscape(r.getNewValue())).append('\n');
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Query all Envers revisions for an entity class, group by entity id, compute
   * chronological field diffs, and emit one row per changed field whose revision
   * date falls within [fromInstant, toExclusive). The full revision chain is always
   * walked so diffs stay correct even when the date window starts mid-history.
   */
  private <T> List<AuditExportRow> collectAuditRows(Class<T> entityClass, String typeLabel,
      Function<T, Long> idExtractor, FieldValueExtractor<T> fieldExtractor, Instant fromInstant,
      Instant toExclusive) {

    AuditReader auditReader = AuditReaderFactory.get(entityManager);

    @SuppressWarnings("unchecked")
    List<Object[]> revisions = auditReader.createQuery().forRevisionsOfEntity(entityClass, false, true)
        .addOrder(AuditEntity.revisionNumber().asc()).getResultList();

    // Group revisions per entity id, preserving chronological order.
    Map<Long, List<Object[]>> byId = new LinkedHashMap<>();
    for (Object[] rev : revisions) {
      @SuppressWarnings("unchecked")
      T entity = (T) rev[0];
      if (entity == null) {
        continue;
      }
      Long id = idExtractor.apply(entity);
      byId.computeIfAbsent(id, k -> new ArrayList<>()).add(rev);
    }

    List<AuditExportRow> rows = new ArrayList<>();
    for (List<Object[]> entityRevs : byId.values()) {
      T previous = null;
      for (Object[] rev : entityRevs) {
        @SuppressWarnings("unchecked")
        T current = (T) rev[0];
        AuditRevisionEntity revEntity = (AuditRevisionEntity) rev[1];
        org.hibernate.envers.RevisionType enversType = (org.hibernate.envers.RevisionType) rev[2];
        Instant ts = Instant.ofEpochMilli(revEntity.getTimestamp());

        boolean inRange = (fromInstant == null || !ts.isBefore(fromInstant))
            && (toExclusive == null || ts.isBefore(toExclusive));

        if (inRange) {
          LocalDateTime when = convertToLocalDateTime(revEntity.getTimestamp());
          String modifiedBy = revEntity.getModifiedBy() != null ? revEntity.getModifiedBy() : "system";
          RevisionType changeType = convertRevisionType(enversType);
          Long revNo = (long) revEntity.getId();
          Long entityId = idExtractor.apply(current);

          List<FieldChangeDTO> changes = computeFieldChanges(previous, current, fieldExtractor, enversType);
          if (changes.isEmpty()) {
            // Deletion or a revision with no audited-field change — record the revision itself.
            rows.add(AuditExportRow.builder().entityType(typeLabel).entityId(entityId).revision(revNo)
                .revisionDate(when).modifiedBy(modifiedBy).changeType(changeType).build());
          } else {
            for (FieldChangeDTO change : changes) {
              rows.add(AuditExportRow.builder().entityType(typeLabel).entityId(entityId).revision(revNo)
                  .revisionDate(when).modifiedBy(modifiedBy).changeType(changeType).field(change.getFieldName())
                  .oldValue(change.getOldValue()).newValue(change.getNewValue()).build());
            }
          }
        }
        previous = current;
      }
    }
    return rows;
  }

  /** Batch-resolve stored usernames to display names, mutating the rows in place. */
  private void resolveModifiedByDisplayNames(List<AuditExportRow> rows) {
    List<String> usernames = rows.stream().map(AuditExportRow::getModifiedBy)
        .filter(name -> name != null && !"system".equals(name)).distinct().collect(Collectors.toList());
    if (usernames.isEmpty()) {
      return;
    }
    Map<String, String> displayNames = userRepository.findByUsernameIn(usernames).stream().collect(
        Collectors.toMap(User::getUsername, u -> u.getPerson() != null ? u.getPerson().getName() : u.getUsername()));
    rows.forEach(r -> {
      String resolved = displayNames.get(r.getModifiedBy());
      if (resolved != null) {
        r.setModifiedBy(resolved);
      }
    });
  }

  /** CSV-escape a value: RFC-4180 quoting plus spreadsheet formula-injection guard. */
  private static String csvEscape(String val) {
    if (val == null || val.isEmpty()) {
      return "";
    }
    String safe = val;
    if ("=+-@\t\r".indexOf(safe.charAt(0)) >= 0) {
      safe = "'" + safe;
    }
    if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
      return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
    return safe;
  }

  /**
   * Functional interface for extracting field values from entities.
   */
  @FunctionalInterface
  private interface FieldValueExtractor<T> {
    String getValue(T entity, String fieldName);
  }
}
