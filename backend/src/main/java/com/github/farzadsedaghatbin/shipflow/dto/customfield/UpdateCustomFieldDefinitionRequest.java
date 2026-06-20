package com.github.farzadsedaghatbin.shipflow.dto.customfield;

import java.util.List;
import lombok.*;

/** fieldType and entityType are immutable after creation. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomFieldDefinitionRequest {
  private String name;
  private String description;
  private Boolean required;
  private Integer sortOrder;
  private List<String> options;
}
