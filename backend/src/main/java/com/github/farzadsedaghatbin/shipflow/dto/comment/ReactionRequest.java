package com.github.farzadsedaghatbin.shipflow.dto.comment;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CommentReaction;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for adding or removing a reaction to a comment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionRequest {

  @NotNull(message = "Reaction type is required")
  private CommentReaction reactionType;
}
