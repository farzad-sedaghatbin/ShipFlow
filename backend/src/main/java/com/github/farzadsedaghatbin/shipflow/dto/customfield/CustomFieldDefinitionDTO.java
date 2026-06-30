package com.github.farzadsedaghatbin.shipflow.dto.customfield;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldEntityType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldType;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldDefinitionDTO {
  private Long id;
  private String name;
  private String description;
  private CustomFieldType fieldType;
  private CustomFieldEntityType entityType;
  private Long projectId;
  private String projectName;
  private Boolean required;
  private Integer sortOrder;
  private List<String> options;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
