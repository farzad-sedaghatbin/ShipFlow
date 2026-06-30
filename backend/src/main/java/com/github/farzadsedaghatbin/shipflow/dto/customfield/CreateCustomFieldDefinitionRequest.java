package com.github.farzadsedaghatbin.shipflow.dto.customfield;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldEntityType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomFieldDefinitionRequest {

  @NotBlank
  private String name;

  private String description;

  @NotNull
  private CustomFieldType fieldType;

  @NotNull
  private CustomFieldEntityType entityType;

  /** Null → org-wide (ADMIN only). Non-null → project-scoped. */
  private Long projectId;

  private Boolean required = false;

  private Integer sortOrder = 0;

  /** Required for SELECT / MULTISELECT field types. */
  private List<String> options;
}
