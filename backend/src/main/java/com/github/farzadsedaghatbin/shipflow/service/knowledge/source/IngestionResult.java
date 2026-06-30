package com.github.farzadsedaghatbin.shipflow.service.knowledge.source;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IngestionResult {
  List<RawChunk> chunks;
  Map<String, Object> sourceMetadata;
}
