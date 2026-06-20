package com.github.farzadsedaghatbin.shipflow.service.knowledge.source;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class RawChunk {
  String title;
  String content;
  int ordinal;
  String sourceUrl;
  String hash;
}
