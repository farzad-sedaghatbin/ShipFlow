package com.github.farzadsedaghatbin.shipflow.service.knowledge;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.WikiPage;
import com.github.farzadsedaghatbin.shipflow.entity.WikiSpace;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import com.github.farzadsedaghatbin.shipflow.repository.WikiPageRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpacePermissionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpaceRepository;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.IngestionContext;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.IngestionResult;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.InvalidConfigException;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.RawChunk;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.provider.WikiProvider;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WikiProviderTest {

  private WikiSpaceRepository spaceRepo;
  private WikiPageRepository pageRepo;
  private WikiSpacePermissionRepository permRepo;
  private WikiProvider provider;
  private final ObjectMapper json = new ObjectMapper();

  @BeforeEach
  void setUp() {
    spaceRepo = mock(WikiSpaceRepository.class);
    pageRepo = mock(WikiPageRepository.class);
    permRepo = mock(WikiSpacePermissionRepository.class);
    provider = new WikiProvider(spaceRepo, pageRepo, permRepo);
  }

  @Test
  void getType_returnsWiki() {
    assertThat(provider.getType()).isEqualTo(KnowledgeProviderType.WIKI);
  }

  @Test
  void supportsRefresh_isTrue() {
    assertThat(provider.supportsRefresh()).isTrue();
  }

  @Test
  void validateConfig_throwsWhenSpaceIdMissing() {
    ObjectNode cfg = json.createObjectNode();
    assertThatThrownBy(() -> provider.validateConfig(cfg))
        .isInstanceOf(InvalidConfigException.class)
        .hasMessageContaining("spaceId");
  }

  @Test
  void validateConfig_throwsWhenSpaceNotFound() {
    ObjectNode cfg = json.createObjectNode();
    cfg.put("spaceId", 99L);
    when(spaceRepo.findById(99L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> provider.validateConfig(cfg))
        .isInstanceOf(InvalidConfigException.class)
        .hasMessageContaining("not found");
  }

  @Test
  void validateConfig_passesWhenSpaceExists() {
    WikiSpace space = new WikiSpace();
    space.setId(1L);
    space.setSpaceKey("eng");
    ObjectNode cfg = json.createObjectNode();
    cfg.put("spaceId", 1L);
    when(spaceRepo.findById(1L)).thenReturn(Optional.of(space));
    assertThatNoException().isThrownBy(() -> provider.validateConfig(cfg));
  }

  @Test
  void ingest_twoPages_producesChunksWithTitlesAndSourceUrls() throws Exception {
    Long spaceId = 10L;
    WikiSpace space = new WikiSpace();
    space.setId(spaceId);
    space.setSpaceKey("dev");

    WikiPage page1 = new WikiPage();
    page1.setId(1L);
    page1.setSpaceId(spaceId);
    page1.setTitle("Architecture");
    page1.setContentText(
        "Architecture overview content that is long enough to be ingested properly into chunks.");

    WikiPage page2 = new WikiPage();
    page2.setId(2L);
    page2.setSpaceId(spaceId);
    page2.setTitle("Onboarding");
    page2.setContentText(
        "Onboarding guide with steps for new team members joining the project.");

    when(spaceRepo.findById(spaceId)).thenReturn(Optional.of(space));
    when(pageRepo.findBySpaceIdAndDeletedAtIsNull(spaceId)).thenReturn(List.of(page1, page2));
    when(permRepo.existsBySpaceId(spaceId)).thenReturn(false);

    KnowledgeSource source = new KnowledgeSource();
    source.setConfig("{\"spaceId\":" + spaceId + "}");
    IngestionContext ctx = IngestionContext.builder().currentUserId(1L).build();

    IngestionResult result = provider.ingest(source, ctx);

    assertThat(result.getChunks()).hasSizeGreaterThanOrEqualTo(2);
    List<String> titles = result.getChunks().stream().map(RawChunk::getTitle).toList();
    assertThat(titles).anyMatch(t -> t.contains("Architecture"));
    assertThat(titles).anyMatch(t -> t.contains("Onboarding"));
    List<String> urls = result.getChunks().stream().map(RawChunk::getSourceUrl).toList();
    assertThat(urls).anyMatch(u -> u.equals("/wiki/dev/1"));
    assertThat(urls).anyMatch(u -> u.equals("/wiki/dev/2"));
  }

  @Test
  void ingest_skipsBlankContentPages() throws Exception {
    Long spaceId = 10L;
    WikiSpace space = new WikiSpace();
    space.setId(spaceId);
    space.setSpaceKey("dev");

    WikiPage page = new WikiPage();
    page.setId(1L);
    page.setSpaceId(spaceId);
    page.setTitle("Empty Page");
    page.setContentText("");

    when(spaceRepo.findById(spaceId)).thenReturn(Optional.of(space));
    when(pageRepo.findBySpaceIdAndDeletedAtIsNull(spaceId)).thenReturn(List.of(page));
    when(permRepo.existsBySpaceId(spaceId)).thenReturn(false);

    KnowledgeSource source = new KnowledgeSource();
    source.setConfig("{\"spaceId\":" + spaceId + "}");
    IngestionContext ctx = IngestionContext.builder().currentUserId(1L).build();

    IngestionResult result = provider.ingest(source, ctx);
    assertThat(result.getChunks()).isEmpty();
  }

  @Test
  void ingest_restrictedSpace_returnsEmptyResult() throws Exception {
    Long spaceId = 77L;
    WikiSpace space = new WikiSpace();
    space.setId(spaceId);
    space.setSpaceKey("restricted");

    when(spaceRepo.findById(spaceId)).thenReturn(Optional.of(space));
    when(permRepo.existsBySpaceId(spaceId)).thenReturn(true);

    KnowledgeSource source = new KnowledgeSource();
    source.setConfig("{\"spaceId\":" + spaceId + "}");
    IngestionContext ctx = IngestionContext.builder().currentUserId(1L).build();

    IngestionResult result = provider.ingest(source, ctx);

    assertThat(result.getChunks()).isEmpty();
    // Page repo must not be consulted — restricted space is bailed out early
    verifyNoInteractions(pageRepo);
  }

  @Test
  void ingest_openSpace_producesChunks() throws Exception {
    Long spaceId = 20L;
    WikiSpace space = new WikiSpace();
    space.setId(spaceId);
    space.setSpaceKey("open");

    WikiPage page = new WikiPage();
    page.setId(3L);
    page.setSpaceId(spaceId);
    page.setTitle("Guide");
    page.setContentText("Content to ingest for the open space knowledge center.");

    when(spaceRepo.findById(spaceId)).thenReturn(Optional.of(space));
    when(pageRepo.findBySpaceIdAndDeletedAtIsNull(spaceId)).thenReturn(List.of(page));
    when(permRepo.existsBySpaceId(spaceId)).thenReturn(false);

    KnowledgeSource source = new KnowledgeSource();
    source.setConfig("{\"spaceId\":" + spaceId + "}");
    IngestionContext ctx = IngestionContext.builder().currentUserId(1L).build();

    IngestionResult result = provider.ingest(source, ctx);

    assertThat(result.getChunks()).isNotEmpty();
    assertThat(result.getChunks().get(0).getTitle()).contains("Guide");
    verify(pageRepo).findBySpaceIdAndDeletedAtIsNull(spaceId);
  }
}
