package com.github.farzadsedaghatbin.shipflow.dto.customfield;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldEntityType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldType;
import java.time.OffsetDateTime;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldValueDTO {
  private Long definitionId;
  private String definitionName;
  private CustomFieldType fieldType;
  private CustomFieldEntityType entityType;
  private Long entityId;
  private String value;
  private String updatedByUsername;
  private OffsetDateTime updatedAt;
}
