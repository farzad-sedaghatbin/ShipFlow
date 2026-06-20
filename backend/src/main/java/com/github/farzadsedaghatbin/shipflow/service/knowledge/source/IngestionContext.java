package com.github.farzadsedaghatbin.shipflow.service.knowledge.source;

import java.io.InputStream;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class IngestionContext {
  Long currentUserId;
  InputStream uploadStream;
  String uploadOriginalFilename;
  String uploadContentType;
}
