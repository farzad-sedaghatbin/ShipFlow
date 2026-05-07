package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroColumnType;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetroItemDTO {
  private Long id;
  private String content;
  private RetroColumnType columnType;
  private Long retrospectiveId;
  private Long authorId;
  private String authorName;
  private Boolean isAnonymous;
  private Integer voteCount;
  private Boolean hasVoted; // Whether current user has voted
  private Long mergedIntoId;
  private List<Long> mergedItemIds; // IDs of items merged into this one
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  
  // Action tracking fields (v0.5)
  private Boolean actedOn;
  private String actedOnNotes;
  private LocalDateTime actedOnAt;
  private Long actedOnById;
  private String actedOnByName;
}
