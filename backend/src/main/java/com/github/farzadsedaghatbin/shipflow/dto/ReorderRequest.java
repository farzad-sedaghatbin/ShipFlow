package com.github.farzadsedaghatbin.shipflow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;

/**
 * Request DTO for batch-reordering entities (initiatives, epics, or pitches).
 * Contains a list of id + sortOrder pairs to apply in a single transaction.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReorderRequest {

  @NotNull(message = "Items list is required")
  @Valid
  private List<ReorderItem> items;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class ReorderItem {
    @NotNull(message = "ID is required")
    private Long id;

    @NotNull(message = "Sort order is required")
    private Integer sortOrder;
  }
}
