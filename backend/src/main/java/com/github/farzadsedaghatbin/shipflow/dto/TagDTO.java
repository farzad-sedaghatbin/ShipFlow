package com.github.farzadsedaghatbin.shipflow.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

/**
 * DTO for Tag entity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagDTO {

  private Long id;
  private String name;
  private Long projectId;
  private String color;
  private String description;
  private LocalDateTime createdAt;
  private String createdByUsername;
  
  /** Count of retro items with this tag */
  private Long retroItemCount;
  
  /** Count of pitches with this tag */
  private Long pitchCount;
}
