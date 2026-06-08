package com.github.farzadsedaghatbin.shipflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight request body for the {@code PATCH /api/tasks/{id}/story-points} endpoint. Allows
 * callers to update only the story-points field without sending the full task payload — preventing
 * accidental data loss from partial-update callers that do not know the current field values.
 *
 * <p>A {@code null} value clears the story-points estimate (i.e. the task becomes un-pointed).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStoryPointsRequest {

  @Min(value = 0, message = "Story points must be 0 or greater")
  @Max(value = 999, message = "Story points must not exceed 999")
  private Integer storyPoints;
}
