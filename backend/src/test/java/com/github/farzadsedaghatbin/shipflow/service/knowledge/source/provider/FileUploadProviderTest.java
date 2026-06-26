package com.github.farzadsedaghatbin.shipflow.service.knowledge.source.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.IngestionContext;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.InvalidConfigException;
import com.github.farzadsedaghatbin.shipflow.service.storage.ObjectStorageService;
import com.github.farzadsedaghatbin.shipflow.service.storage.StorageProviderType;
import com.github.farzadsedaghatbin.shipflow.service.storage.StoredObjectRef;
import java.io.ByteArrayInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileUploadProviderTest {

  ObjectMapper json = new ObjectMapper();

  @Mock ObjectStorageService objectStorageService;

  FileUploadProvider p;

  @BeforeEach
  void setUp() {
    p = new FileUploadProvider(objectStorageService);
    lenient()
        .when(objectStorageService.storeWithoutValidation(any(), any(), any(), anyLong(), any()))
        .thenReturn(
            StoredObjectRef.builder()
                .bucket("bucket")
                .key("knowledge/1/uuid_hello.txt")
                .contentType("text/plain")
                .sizeBytes(22L)
                .build());
    lenient().when(objectStorageService.activeProvider()).thenReturn(StorageProviderType.LOCAL_FS);
  }

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
  void ingest_persists_original_file_and_records_storage_metadata() {
    var src = KnowledgeSource.builder().id(1L).providerType(KnowledgeProviderType.FILE_UPLOAD)
        .config("{\"originalFilename\":\"hello.txt\",\"contentType\":\"text/plain\"}").build();
    var ctx = IngestionContext.builder().uploadStream(new ByteArrayInputStream("Hello Knowledge Center".getBytes()))
        .uploadContentType("text/plain").uploadOriginalFilename("hello.txt").build();

    var r = p.ingest(src, ctx);

    // (a) chunks are still produced
    assertThat(r.getChunks()).isNotEmpty();
    // (b) storage metadata is recorded for persistence into the source config
    assertThat(r.getSourceMetadata()).containsEntry("storageKey", "knowledge/1/uuid_hello.txt");
    assertThat(r.getSourceMetadata()).containsEntry("storageProvider", "LOCAL_FS");
    // pre-existing metadata is preserved alongside the new keys
    assertThat(r.getSourceMetadata()).containsKey("sha256");
    assertThat(r.getSourceMetadata()).containsEntry("contentType", "text/plain");

    // the file is stored under the knowledge/{sourceId} prefix
    verify(objectStorageService)
        .storeWithoutValidation(startsWith("knowledge/"), eq("hello.txt"), eq("text/plain"), anyLong(), any());
  }

  @Test
  void ingest_with_no_stream_throws() {
    var src = KnowledgeSource.builder().id(1L).providerType(KnowledgeProviderType.FILE_UPLOAD).config("{}").build();
    assertThatThrownBy(() -> p.ingest(src, IngestionContext.builder().build()))
        .isInstanceOf(InvalidConfigException.class);
  }
}
