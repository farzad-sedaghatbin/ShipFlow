package com.github.farzadsedaghatbin.shipflow.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Guards {@code server.compression.*}. The SPA bundle this app serves is ~1.9 MB of JS plus
 * ~200 KB of CSS; with Tomcat's default (compression off) every cold load transferred all of it
 * raw, which is what made the app slow to load over any non-local connection. gzip takes that
 * pair to roughly 580 KB.
 *
 * <p>Uses {@link HttpClient} rather than {@code TestRestTemplate} deliberately: the JDK client
 * does not transparently decompress, so the {@code Content-Encoding} header and the real
 * on-the-wire byte count are both observable. A client that decompresses for you would make this
 * test pass whether or not compression was actually enabled.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ResponseCompressionIntegrationTest {

  @LocalServerPort
  private int port;

  /**
   * A test-only static asset (src/test/resources/static), standing in for the real SPA bundle:
   * public, served as text/javascript, and comfortably larger than min-response-size. Using a
   * fixture rather than a real endpoint keeps this test about compression alone — it can't start
   * failing because some unrelated controller changed.
   */
  private static final String LARGE_PUBLIC_ASSET = "/compression-fixture.js";

  private HttpResponse<byte[]> get(String path, String acceptEncoding) throws Exception {
    HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    if (acceptEncoding != null) {
      request.header("Accept-Encoding", acceptEncoding);
    }
    return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
  }

  @Test
  void largeResponse_isGzipped_whenTheClientAcceptsIt() throws Exception {
    HttpResponse<byte[]> response = get(LARGE_PUBLIC_ASSET, "gzip");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Encoding")).contains("gzip");

    byte[] decompressed =
        new GZIPInputStream(new ByteArrayInputStream(response.body())).readAllBytes();
    assertThat(decompressed.length).isGreaterThan(1024);
    // The whole point: fewer bytes actually cross the wire.
    assertThat(response.body().length).isLessThan(decompressed.length);
  }

  @Test
  void response_isNotCompressed_whenTheClientDoesNotAcceptIt() throws Exception {
    HttpResponse<byte[]> response = get(LARGE_PUBLIC_ASSET, "identity");

    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("Content-Encoding")).isEmpty();
  }
}
