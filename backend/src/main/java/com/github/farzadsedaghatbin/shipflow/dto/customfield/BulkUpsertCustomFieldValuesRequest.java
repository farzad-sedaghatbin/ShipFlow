package com.github.farzadsedaghatbin.shipflow.dto.customfield;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldEntityType;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpsertCustomFieldValuesRequest {

  @NotNull
  private CustomFieldEntityType entityType;

  @NotNull
  private Long entityId;

  /** definitionId → encoded value (null value clears the field). */
  private Map<Long, String> values;
}
