package com.github.farzadsedaghatbin.shipflow.dto.knowledge;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChunkPreview {

  private Long id;
  private String title;
  /** First ~400 characters of the chunk body. */
  private String contentPreview;

  private int ordinal;
  private boolean embedded;
}
