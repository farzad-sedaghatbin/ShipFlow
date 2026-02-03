package com.github.farzadsedaghatbin.shipflow.dto.comment;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CommentEntityType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new comment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

  @NotBlank(message = "Comment content is required")
  @Size(min = 1, max = 10000, message = "Comment content must be between 1 and 10000 characters")
  private String content;

  @NotNull(message = "Entity type is required")
  private CommentEntityType entityType;

  @NotNull(message = "Entity ID is required")
  private Long entityId;
}
