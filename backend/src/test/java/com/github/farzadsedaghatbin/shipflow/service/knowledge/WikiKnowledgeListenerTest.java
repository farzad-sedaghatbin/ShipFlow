package com.github.farzadsedaghatbin.shipflow.service.knowledge;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.entity.WikiPage;
import com.github.farzadsedaghatbin.shipflow.entity.WikiSpace;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeEntityType;
import com.github.farzadsedaghatbin.shipflow.event.WikiPageChangedEvent;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeItemRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiPageRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpaceRepository;
import com.github.farzadsedaghatbin.shipflow.service.KnowledgeIngestionService;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.RawChunk;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class WikiKnowledgeListenerTest {

  private WikiPageRepository pageRepo;
  private WikiSpaceRepository spaceRepo;
  private KnowledgeItemRepository itemRepo;
  private KnowledgeIngestionService ingestionService;
  private WikiKnowledgeListener listener;

  @BeforeEach
  void setUp() {
    pageRepo = mock(WikiPageRepository.class);
    spaceRepo = mock(WikiSpaceRepository.class);
    itemRepo = mock(KnowledgeItemRepository.class);
    ingestionService = mock(KnowledgeIngestionService.class);
    listener = new WikiKnowledgeListener(pageRepo, spaceRepo, itemRepo, ingestionService);
  }

  @Test
  void onUpdated_removesOldItemsThenIngestsChunks() {
    Long pageId = 1L;
    Long spaceId = 10L;

    WikiSpace space = new WikiSpace();
    space.setId(spaceId);
    space.setSpaceKey("dev");
    space.setProjectId(100L);

    WikiPage page = new WikiPage();
    page.setId(pageId);
    page.setSpaceId(spaceId);
    page.setTitle("Guide");
    page.setContentText(
        "This is enough content for ingestion into the knowledge store.");

    when(pageRepo.findById(pageId)).thenReturn(Optional.of(page));
    when(spaceRepo.findById(spaceId)).thenReturn(Optional.of(space));

    listener.onWikiPageChanged(
        new WikiPageChangedEvent(pageId, spaceId, WikiPageChangedEvent.ChangeType.UPDATED));

    verify(itemRepo).deleteByEntityTypeAndEntityId(KnowledgeEntityType.WIKI_PAGE, pageId);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<RawChunk>> chunksCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<KnowledgeEntityType> entityTypeCaptor =
        ArgumentCaptor.forClass(KnowledgeEntityType.class);
    ArgumentCaptor<Long> entityIdCaptor = ArgumentCaptor.forClass(Long.class);

    verify(ingestionService)
        .ingestChunks(
            chunksCaptor.capture(),
            entityTypeCaptor.capture(),
            entityIdCaptor.capture(),
            isNull(),
            eq(100L));

    assertThat(entityTypeCaptor.getValue()).isEqualTo(KnowledgeEntityType.WIKI_PAGE);
    assertThat(entityIdCaptor.getValue()).isEqualTo(pageId);
    assertThat(chunksCaptor.getValue()).isNotEmpty();
  }

  @Test
  void onDeleted_removesItemsAndDoesNotIngest() {
    Long pageId = 1L;
    Long spaceId = 10L;

    listener.onWikiPageChanged(
        new WikiPageChangedEvent(pageId, spaceId, WikiPageChangedEvent.ChangeType.DELETED));

    verify(itemRepo).deleteByEntityTypeAndEntityId(KnowledgeEntityType.WIKI_PAGE, pageId);
    verifyNoInteractions(ingestionService);
  }

  @Test
  void onUpdated_blankContent_doesNotIngest() {
    Long pageId = 1L;
    Long spaceId = 10L;

    WikiSpace space = new WikiSpace();
    space.setId(spaceId);
    space.setSpaceKey("dev");

    WikiPage page = new WikiPage();
    page.setId(pageId);
    page.setSpaceId(spaceId);
    page.setTitle("Empty");
    page.setContentText("   ");

    when(pageRepo.findById(pageId)).thenReturn(Optional.of(page));
    when(spaceRepo.findById(spaceId)).thenReturn(Optional.of(space));

    listener.onWikiPageChanged(
        new WikiPageChangedEvent(pageId, spaceId, WikiPageChangedEvent.ChangeType.UPDATED));

    verify(itemRepo).deleteByEntityTypeAndEntityId(KnowledgeEntityType.WIKI_PAGE, pageId);
    verifyNoInteractions(ingestionService);
  }

  @Test
  void onCreated_ingestsChunksWithCorrectEntityType() {
    Long pageId = 2L;
    Long spaceId = 10L;

    WikiSpace space = new WikiSpace();
    space.setId(spaceId);
    space.setSpaceKey("eng");
    space.setProjectId(200L);

    WikiPage page = new WikiPage();
    page.setId(pageId);
    page.setSpaceId(spaceId);
    page.setTitle("Architecture");
    page.setContentText(
        "Architecture content for the engineering space knowledge base.");

    when(pageRepo.findById(pageId)).thenReturn(Optional.of(page));
    when(spaceRepo.findById(spaceId)).thenReturn(Optional.of(space));

    listener.onWikiPageChanged(
        new WikiPageChangedEvent(pageId, spaceId, WikiPageChangedEvent.ChangeType.CREATED));

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<RawChunk>> chunksCaptor = ArgumentCaptor.forClass(List.class);
    verify(ingestionService)
        .ingestChunks(
            chunksCaptor.capture(),
            eq(KnowledgeEntityType.WIKI_PAGE),
            eq(pageId),
            isNull(),
            eq(200L));
    assertThat(chunksCaptor.getValue()).isNotEmpty();
  }
}
