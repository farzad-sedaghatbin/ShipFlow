package com.github.farzadsedaghatbin.shipflow.dto.qa;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for manual notes. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteDTO {

  private Long id;
  private String title;
  private String content;
  private String contextType;
  private Long contextId;
  private Long cycleId;
  private Long teamId;
  private Long pitchId;
  private Long authorId;
  private String authorName;
  private Boolean includeInKnowledge;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
