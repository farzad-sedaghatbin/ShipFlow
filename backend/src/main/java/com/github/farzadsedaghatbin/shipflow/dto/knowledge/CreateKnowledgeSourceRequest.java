package com.github.farzadsedaghatbin.shipflow.dto.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceScope;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateKnowledgeSourceRequest {

  @NotBlank
  @Size(max = 255)
  private String name;

  @Size(max = 4000)
  private String description;

  @NotNull private KnowledgeProviderType providerType;

  @NotNull private KnowledgeSourceScope scope;

  private Long teamId;

  private Long projectId;

  @NotNull private JsonNode config;
}
