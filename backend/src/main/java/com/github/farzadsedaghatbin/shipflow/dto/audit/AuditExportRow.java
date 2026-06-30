package com.github.farzadsedaghatbin.shipflow.dto.audit;

import com.github.farzadsedaghatbin.shipflow.dto.audit.EntityHistoryDTO.RevisionType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single flattened audit-trail row for export (one row per changed field).
 * DELETE revisions and field-less changes emit one row with a null field name.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditExportRow {

  /** Logical entity type: task | bug | pitch | testcase. */
  private String entityType;

  private Long entityId;

  private Long revision;

  private LocalDateTime revisionDate;

  private String modifiedBy;

  private RevisionType changeType;

  private String field;

  private String oldValue;

  private String newValue;
}
