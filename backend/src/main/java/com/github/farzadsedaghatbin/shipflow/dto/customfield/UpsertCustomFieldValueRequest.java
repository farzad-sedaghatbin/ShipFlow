package com.github.farzadsedaghatbin.shipflow.dto.customfield;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldEntityType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpsertCustomFieldValueRequest {

  @NotNull
  private Long definitionId;

  @NotNull
  private CustomFieldEntityType entityType;

  @NotNull
  private Long entityId;

  /** Null clears the value. Encoded per field type (see CustomFieldService). */
  private String value;
}
