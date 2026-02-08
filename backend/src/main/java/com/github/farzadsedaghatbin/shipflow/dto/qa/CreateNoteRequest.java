package com.github.farzadsedaghatbin.shipflow.dto.qa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for creating a manual note. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateNoteRequest {

  @NotBlank(message = "Title is required")
  @Size(max = 255, message = "Title must be at most 255 characters")
  private String title;

  @NotBlank(message = "Content is required")
  @Size(max = 10000, message = "Content must be at most 10000 characters")
  private String content;

  @NotBlank(message = "Context type is required")
  private String contextType;

  @NotNull(message = "Context ID is required")
  private Long contextId;

  /** Whether to include this note in the knowledge base. */
  @Builder.Default
  private Boolean includeInKnowledge = true;
}
