package com.github.farzadsedaghatbin.shipflow.dto.comment;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CommentReaction;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a reaction on a comment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReactionDTO {

  private Long id;
  private CommentReaction reactionType;
  private String emoji;
  private Long userId;
  private String userName;
  private LocalDateTime createdAt;
}
