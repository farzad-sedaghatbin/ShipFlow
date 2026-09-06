package org.springframework.web.servlet.mvc.method.annotation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.http.MediaType;

/**
 * Test-only bridge that lives in this package purely to reach {@link
 * ResponseBodyEmitter}'s package-private {@code initialize(Handler)} method and the
 * package-private {@link ResponseBodyEmitter.Handler} interface itself — the exact seam Spring's
 * own MVC infrastructure ({@code ResponseBodyEmitterReturnValueHandler}) uses to wire a live
 * emitter to the servlet response. Neither is public, so a unit test outside this package cannot
 * implement {@code Handler} or call {@code initialize} directly; this class does both and exposes
 * a small public surface (plain JDK types only) for tests in other packages to drive {@link
 * SseEmitter}/{@link ResponseBodyEmitter} lifecycle callbacks (onCompletion/onTimeout/onError)
 * without a real servlet container.
 *
 * <p>Note: {@code ResponseBodyEmitter} buffers anything sent before {@code initialize()} is called
 * and replays it once a handler is attached — so a value sent by production code (e.g. the SSE
 * "connected" heartbeat) before a test calls {@link #attach} still shows up in {@link
 * #getReceived()}.
 */
public final class RecordingEmitterHandler implements ResponseBodyEmitter.Handler {

  private final List<Object> received = new ArrayList<>();
  private volatile boolean failOnSend = false;

  private RecordingEmitterHandler() {}

  /**
   * Attaches a new recording handler to the given emitter, replaying any already-buffered sends.
   */
  public static RecordingEmitterHandler attach(ResponseBodyEmitter emitter) throws IOException {
    RecordingEmitterHandler handler = new RecordingEmitterHandler();
    emitter.initialize(handler);
    return handler;
  }

  /** Every payload delivered to this handler so far, in send order. */
  public List<Object> getReceived() {
    return received;
  }

  /** Makes every subsequent {@code send} throw an {@link IOException}, simulating a dead client. */
  public void failOnNextSend() {
    this.failOnSend = true;
  }

  @Override
  public void send(Object data, MediaType mediaType) throws IOException {
    if (failOnSend) {
      throw new IOException("Simulated SSE send failure");
    }
    received.add(data);
  }

  @Override
  public void send(Set<ResponseBodyEmitter.DataWithMediaType> items) throws IOException {
    for (ResponseBodyEmitter.DataWithMediaType item : items) {
      send(item.getData(), item.getMediaType());
    }
  }

  @Override
  public void complete() {
    // No-op: this test double has no servlet response to finalize.
  }

  @Override
  public void completeWithError(Throwable ex) {
    // No-op: this test double has no servlet response to finalize.
  }

  @Override
  public void onTimeout(Runnable callback) {
    // No-op: the manager's own onTimeout callback (registered on the emitter directly) is what
    // production/tests exercise; this handler doesn't need to re-trigger it.
  }

  @Override
  public void onError(Consumer<Throwable> callback) {
    // No-op: see onTimeout.
  }

  @Override
  public void onCompletion(Runnable callback) {
    // No-op: see onTimeout.
  }
}
