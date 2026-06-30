package com.github.farzadsedaghatbin.shipflow.service.knowledge.source.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.*;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;

@Component
public class UrlProvider implements KnowledgeSourceProvider {

  private static final int CHUNK_SIZE = 1200;
  private static final int CHUNK_OVERLAP = 150;
  private static final ObjectMapper JSON = new ObjectMapper();

  @Override
  public KnowledgeProviderType getType() {
    return KnowledgeProviderType.URL;
  }

  @Override
  public boolean supportsRefresh() {
    return true;
  }

  @Override
  public void validateConfig(JsonNode config) {
    if (!config.hasNonNull("url"))
      throw new InvalidConfigException("url is required");
    try {
      new java.net.URI(config.get("url").asText()).toURL();
    } catch (Exception e) {
      throw new InvalidConfigException("invalid url: " + e.getMessage());
    }
  }

  @Override
  public ConnectionStatus testConnection(JsonNode config) {
    try {
      Jsoup.connect(config.get("url").asText()).timeout(5000).method(Connection.Method.HEAD)
          .ignoreContentType(true).execute();
      return ConnectionStatus.ok();
    } catch (Exception e) {
      return ConnectionStatus.fail(e.getMessage());
    }
  }

  @Override
  public IngestionResult ingest(KnowledgeSource source, IngestionContext ctx) {
    try {
      var cfg = JSON.readTree(source.getConfig());
      var conn = Jsoup.connect(cfg.get("url").asText()).timeout(15_000).ignoreContentType(true);
      if (cfg.hasNonNull("etag"))
        conn.header("If-None-Match", cfg.get("etag").asText());
      var resp = conn.execute();

      if (resp.statusCode() == 304) {
        return IngestionResult.builder().chunks(List.of())
            .sourceMetadata(Map.of("fetchedAt", OffsetDateTime.now().toString())).build();
      }

      var doc = resp.parse();
      doc.select("nav, footer, header, script, style, aside").remove();
      String text = doc.body() != null ? doc.body().text() : "";

      List<RawChunk> chunks = new ArrayList<>();
      int ord = 0;
      String docTitle = doc.title() == null || doc.title().isEmpty() ? null : doc.title();

      if (!text.isEmpty()) {
        for (int i = 0; i < text.length(); i += CHUNK_SIZE - CHUNK_OVERLAP) {
          String body = text.substring(i, Math.min(i + CHUNK_SIZE, text.length()));
          chunks.add(RawChunk.builder()
              .title(docTitle != null ? docTitle + " — part " + (ord + 1) : "URL chunk " + (ord + 1))
              .content(body).ordinal(ord++).sourceUrl(resp.url().toString()).hash(sha256Hex(body))
              .build());
          if (i + CHUNK_SIZE >= text.length())
            break;
        }
      }

      Map<String, Object> meta = new HashMap<>();
      meta.put("finalUrl", resp.url().toString());
      meta.put("fetchedAt", OffsetDateTime.now().toString());
      if (resp.header("ETag") != null)
        meta.put("etag", resp.header("ETag"));
      if (docTitle != null)
        meta.put("title", docTitle);

      return IngestionResult.builder().chunks(chunks).sourceMetadata(meta).build();
    } catch (Exception e) {
      throw new RuntimeException("URL fetch failed: " + e.getMessage(), e);
    }
  }

  private static String sha256Hex(String s) {
    try {
      var d = MessageDigest.getInstance("SHA-256").digest(s.getBytes());
      var sb = new StringBuilder();
      for (byte x : d)
        sb.append(String.format("%02x", x));
      return sb.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
