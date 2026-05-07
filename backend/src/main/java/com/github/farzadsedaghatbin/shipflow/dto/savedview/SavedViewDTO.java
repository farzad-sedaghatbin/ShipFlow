package com.github.farzadsedaghatbin.shipflow.dto.savedview;

import java.time.LocalDateTime;
import lombok.*;

/** Response DTO for a saved filter view. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SavedViewDTO {

  private Long id;
  private Long userId;
  private Long projectId;
  private String name;
  /** Raw JSON string representing the saved filter state. */
  private String filters;
  private boolean isDefault;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
