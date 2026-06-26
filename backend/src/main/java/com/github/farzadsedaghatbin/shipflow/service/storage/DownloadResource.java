package com.github.farzadsedaghatbin.shipflow.service.storage;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import lombok.Builder;
import lombok.Value;

/**
 * Wraps an object download: a stream plus the metadata needed to serve it to a client.
 *
 * <p>Owns the underlying {@link #stream}; callers that do not hand it off to the HTTP layer (e.g.
 * migration jobs) must {@link #close()} it so backend resources — such as an S3 client's connection
 * pool — are released.
 */
@Value
@Builder
public class DownloadResource implements Closeable {
  InputStream stream;
  String contentType;
  long sizeBytes;
  String filename;

  @Override
  public void close() throws IOException {
    if (stream != null) {
      stream.close();
    }
  }
}
