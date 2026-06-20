package com.github.farzadsedaghatbin.shipflow.service.knowledge.source.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.IngestionContext;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.InvalidConfigException;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.Test;

class FileUploadProviderTest {

  ObjectMapper json = new ObjectMapper();
  FileUploadProvider p = new FileUploadProvider();

  @Test
  void validateConfig_rejects_missing_filename() {
    assertThatThrownBy(() -> p.validateConfig(json.createObjectNode())).isInstanceOf(InvalidConfigException.class);
  }

  @Test
  void validateConfig_accepts_filename() {
    p.validateConfig(json.createObjectNode().put("originalFilename", "doc.txt"));
  }

  @Test
  void ingest_text_file_emits_chunks_with_hash() {
    var src = KnowledgeSource.builder().id(1L).providerType(KnowledgeProviderType.FILE_UPLOAD)
        .config("{\"originalFilename\":\"hello.txt\",\"contentType\":\"text/plain\"}").build();
    var ctx = IngestionContext.builder().uploadStream(new ByteArrayInputStream("Hello Knowledge Center".getBytes()))
        .uploadContentType("text/plain").uploadOriginalFilename("hello.txt").build();

    var r = p.ingest(src, ctx);

    assertThat(r.getChunks()).isNotEmpty();
    assertThat(r.getChunks().get(0).getContent()).contains("Hello Knowledge Center");
    assertThat(r.getChunks().get(0).getHash()).hasSize(64);
    assertThat(r.getSourceMetadata()).containsKey("sha256");
  }

  @Test
  void ingest_with_no_stream_throws() {
    var src = KnowledgeSource.builder().id(1L).providerType(KnowledgeProviderType.FILE_UPLOAD).config("{}").build();
    assertThatThrownBy(() -> p.ingest(src, IngestionContext.builder().build()))
        .isInstanceOf(InvalidConfigException.class);
  }
}
