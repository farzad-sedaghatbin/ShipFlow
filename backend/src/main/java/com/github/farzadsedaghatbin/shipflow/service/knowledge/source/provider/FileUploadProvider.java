package com.github.farzadsedaghatbin.shipflow.service.knowledge.source.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.*;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

@Component
public class FileUploadProvider implements KnowledgeSourceProvider {

  private static final int CHUNK_SIZE = 1200;
  private static final int CHUNK_OVERLAP = 150;

  private final Tika tika = new Tika();

  @Override
  public KnowledgeProviderType getType() {
    return KnowledgeProviderType.FILE_UPLOAD;
  }

  @Override
  public void validateConfig(JsonNode config) {
    if (!config.hasNonNull("originalFilename")) {
      throw new InvalidConfigException("originalFilename is required");
    }
  }

  @Override
  public IngestionResult ingest(KnowledgeSource source, IngestionContext ctx) {
    try (InputStream in = ctx.getUploadStream()) {
      if (in == null) {
        throw new InvalidConfigException("No upload stream — re-upload required for refresh");
      }
      byte[] bytes = in.readAllBytes();
      String text = tika.parseToString(new ByteArrayInputStream(bytes));
      String sha = sha256Hex(bytes);

      List<RawChunk> chunks = new ArrayList<>();
      int ord = 0;
      if (text.isEmpty()) {
        // edge case — preserve a single empty marker chunk so downstream knows the file
        // was parsed
        chunks.add(RawChunk.builder().title(ctx.getUploadOriginalFilename() + " — empty").content("").ordinal(0)
            .sourceUrl(ctx.getUploadOriginalFilename()).hash(sha256Hex(new byte[0])).build());
      } else {
        for (int i = 0; i < text.length(); i += CHUNK_SIZE - CHUNK_OVERLAP) {
          String body = text.substring(i, Math.min(i + CHUNK_SIZE, text.length()));
          chunks.add(RawChunk.builder().title(ctx.getUploadOriginalFilename() + " — part " + (ord + 1))
              .content(body).ordinal(ord++).sourceUrl(ctx.getUploadOriginalFilename())
              .hash(sha256Hex(body.getBytes())).build());
          if (i + CHUNK_SIZE >= text.length())
            break;
        }
      }

      Map<String, Object> meta = new HashMap<>();
      meta.put("sha256", sha);
      if (ctx.getUploadContentType() != null)
        meta.put("contentType", ctx.getUploadContentType());
      meta.put("byteSize", bytes.length);

      return IngestionResult.builder().chunks(chunks).sourceMetadata(meta).build();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse upload", e);
    }
  }

  private static String sha256Hex(byte[] b) {
    try {
      var d = MessageDigest.getInstance("SHA-256").digest(b);
      var sb = new StringBuilder();
      for (byte x : d)
        sb.append(String.format("%02x", x));
      return sb.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
