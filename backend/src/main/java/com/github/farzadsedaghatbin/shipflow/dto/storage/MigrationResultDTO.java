package com.github.farzadsedaghatbin.shipflow.dto.storage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MigrationResultDTO {
  private int migrated;
  private int skipped;
  private int failed;
  private int total;
}
