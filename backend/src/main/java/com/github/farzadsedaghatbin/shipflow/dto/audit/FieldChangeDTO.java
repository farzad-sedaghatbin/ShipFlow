package com.github.farzadsedaghatbin.shipflow.dto.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single field change in an entity revision. Contains the
 * field name and the old/new values for display in history timeline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldChangeDTO {

  /**
   * The name of the field that was changed. This is the camelCase field name
   * (e.g., "status", "priority", "assignee").
   */
  private String fieldName;

  /**
   * The previous value of the field before the change. For entity references
   * (like assignee), this contains a display name. Null for the initial creation
   * revision.
   */
  private String oldValue;

  /**
   * The new value of the field after the change. For entity references (like
   * assignee), this contains a display name.
   */
  private String newValue;
}
