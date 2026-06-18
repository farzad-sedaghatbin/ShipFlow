package com.github.farzadsedaghatbin.shipflow.service.storage;

import java.io.InputStream;
import lombok.Builder;
import lombok.Value;

/** Wraps an object download: a stream plus the metadata needed to serve it to a client. */
@Value
@Builder
public class DownloadResource {
  InputStream stream;
  String contentType;
  long sizeBytes;
  String filename;
}
