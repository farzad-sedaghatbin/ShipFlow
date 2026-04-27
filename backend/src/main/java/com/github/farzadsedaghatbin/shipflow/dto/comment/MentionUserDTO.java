package com.github.farzadsedaghatbin.shipflow.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user information when searching for @mentions in comments.
 * Includes profile details for displaying comprehensive user popover.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentionUserDTO {

  private Long id;
  private String username;
  private String displayName;
  private String email;
  private String avatarUrl;
  private String department;
  private String skills;
}
