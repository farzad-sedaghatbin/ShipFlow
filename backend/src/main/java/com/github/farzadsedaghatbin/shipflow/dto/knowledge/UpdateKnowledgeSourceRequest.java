package com.github.farzadsedaghatbin.shipflow.dto.knowledge;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateKnowledgeSourceRequest {

  @Size(max = 255)
  private String name;

  @Size(max = 4000)
  private String description;
}
