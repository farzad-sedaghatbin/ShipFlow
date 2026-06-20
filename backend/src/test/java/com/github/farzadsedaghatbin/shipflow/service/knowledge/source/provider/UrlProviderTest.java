package com.github.farzadsedaghatbin.shipflow.service.knowledge.source.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.IngestionContext;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.InvalidConfigException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.Test;

class UrlProviderTest {

  ObjectMapper json = new ObjectMapper();
  UrlProvider provider = new UrlProvider();

  @Test
  void validateConfig_rejects_missing_url() {
    assertThatThrownBy(() -> provider.validateConfig(json.createObjectNode()))
        .isInstanceOf(InvalidConfigException.class);
  }

  @Test
  void fetches_html_strips_chrome_and_emits_chunks() throws Exception {
    try (var server = new MockWebServer()) {
      server.enqueue(new MockResponse().setHeader("Content-Type", "text/html; charset=utf-8").setBody(
          "<html><head><title>Standards</title></head><body><nav>ignore</nav><main><h1>Title</h1><p>Body of standards</p></main></body></html>")
          .addHeader("ETag", "\"v1\""));
      server.start();
      var url = server.url("/page").toString();

      var src = KnowledgeSource.builder().id(1L).providerType(KnowledgeProviderType.URL)
          .config("{\"url\":\"" + url + "\"}").build();
      var r = provider.ingest(src, IngestionContext.builder().build());

      assertThat(r.getChunks()).isNotEmpty();
      assertThat(r.getChunks().get(0).getContent()).contains("Body of standards");
      assertThat(r.getSourceMetadata()).containsKeys("finalUrl", "fetchedAt", "etag");
    }
  }
}
