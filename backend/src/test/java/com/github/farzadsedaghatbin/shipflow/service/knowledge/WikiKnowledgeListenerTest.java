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
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpacePermissionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpaceRepository;
import com.github.farzadsedaghatbin.shipflow.service.KnowledgeIngestionService;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.RawChunk;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

class WikiKnowledgeListenerTest {

  private WikiPageRepository pageRepo;
  private WikiSpaceRepository spaceRepo;
  private KnowledgeItemRepository itemRepo;
  private WikiSpacePermissionRepository permRepo;
  private KnowledgeIngestionService ingestionService;
  private WikiKnowledgeListener listener;

  @BeforeEach
  void setUp() {
    pageRepo = mock(WikiPageRepository.class);
    spaceRepo = mock(WikiSpaceRepository.class);
    itemRepo = mock(KnowledgeItemRepository.class);
    permRepo = mock(WikiSpacePermissionRepository.class);
    ingestionService = mock(KnowledgeIngestionService.class);

    @SuppressWarnings("unchecked")
    ObjectProvider<KnowledgeIngestionService> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(ingestionService);

    listener = new WikiKnowledgeListener(pageRepo, spaceRepo, itemRepo, permRepo, provider);
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
    when(permRepo.existsBySpaceId(spaceId)).thenReturn(false);

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

  /**
   * Regression guard for the create-time ingestion race. {@code WikiService.createPage} publishes
   * {@link WikiPageChangedEvent} <em>inside</em> its transaction. A bare {@code @Async @EventListener}
   * fires on a separate thread before that transaction commits, so {@code findById} returns empty and
   * the new page is silently skipped (never ingested → AI Q&A can't answer about it). The listener
   * must therefore fire <em>after</em> commit. This pins the fix: the handler is a
   * {@code @TransactionalEventListener(AFTER_COMMIT)} and not a plain {@code @EventListener}.
   */
  @Test
  void handler_firesAfterCommit_notBeforeViaPlainEventListener() throws NoSuchMethodException {
    Method handler =
        WikiKnowledgeListener.class.getMethod("onWikiPageChanged", WikiPageChangedEvent.class);

    TransactionalEventListener txListener =
        handler.getAnnotation(TransactionalEventListener.class);
    assertThat(txListener)
        .as("listener must be @TransactionalEventListener so it reads committed data")
        .isNotNull();
    assertThat(txListener.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);

    assertThat(handler.getAnnotation(EventListener.class))
        .as("must not use the bare @EventListener that races the publishing transaction")
        .isNull();
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
    when(permRepo.existsBySpaceId(spaceId)).thenReturn(false);

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
    when(permRepo.existsBySpaceId(spaceId)).thenReturn(false);

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

  /**
   * Fix 1: When KnowledgeIngestionService is absent (QA disabled), an UPDATED event must be
   * handled without throwing and without attempting ingestion.
   */
  @Test
  void onUpdated_absentIngestionService_noOpAndNoThrow() {
    @SuppressWarnings("unchecked")
    ObjectProvider<KnowledgeIngestionService> absentProvider = mock(ObjectProvider.class);
    when(absentProvider.getIfAvailable()).thenReturn(null);

    WikiKnowledgeListener listenerWithoutService =
        new WikiKnowledgeListener(pageRepo, spaceRepo, itemRepo, permRepo, absentProvider);

    Long pageId = 5L;
    Long spaceId = 20L;

    WikiSpace space = new WikiSpace();
    space.setId(spaceId);
    space.setSpaceKey("qa-disabled");
    space.setProjectId(300L);

    WikiPage page = new WikiPage();
    page.setId(pageId);
    page.setSpaceId(spaceId);
    page.setTitle("Some Page");
    page.setContentText("Content that would normally be ingested.");

    when(pageRepo.findById(pageId)).thenReturn(Optional.of(page));
    when(spaceRepo.findById(spaceId)).thenReturn(Optional.of(space));
    when(permRepo.existsBySpaceId(spaceId)).thenReturn(false);

    // Must not throw
    assertThatCode(
            () ->
                listenerWithoutService.onWikiPageChanged(
                    new WikiPageChangedEvent(
                        pageId, spaceId, WikiPageChangedEvent.ChangeType.UPDATED)))
        .doesNotThrowAnyException();

    // Old items should still be cleaned from the DB index (uses KnowledgeItemRepository directly)
    verify(itemRepo).deleteByEntityTypeAndEntityId(KnowledgeEntityType.WIKI_PAGE, pageId);

    // But ingestion service must NOT have been called (it was absent)
    verifyNoInteractions(ingestionService);
  }

  @Test
  void onUpdated_restrictedSpace_removesChunksAndDoesNotIngest() {
    Long pageId = 10L;
    Long spaceId = 99L;

    when(permRepo.existsBySpaceId(spaceId)).thenReturn(true);

    listener.onWikiPageChanged(
        new WikiPageChangedEvent(pageId, spaceId, WikiPageChangedEvent.ChangeType.UPDATED));

    // Existing chunks must be purged even for restricted spaces
    verify(itemRepo).deleteByEntityTypeAndEntityId(KnowledgeEntityType.WIKI_PAGE, pageId);
    // No ingestion must happen
    verifyNoInteractions(ingestionService);
    // Page and space repos must NOT be consulted (early exit after permission check)
    verifyNoInteractions(pageRepo);
  }

  @Test
  void onUpdated_openSpace_ingestsChunks() {
    Long pageId = 11L;
    Long spaceId = 50L;

    WikiSpace space = new WikiSpace();
    space.setId(spaceId);
    space.setSpaceKey("open");
    space.setProjectId(500L);

    WikiPage page = new WikiPage();
    page.setId(pageId);
    page.setSpaceId(spaceId);
    page.setTitle("Open Page");
    page.setContentText("Content that should be ingested because the space is open to all.");

    when(pageRepo.findById(pageId)).thenReturn(Optional.of(page));
    when(spaceRepo.findById(spaceId)).thenReturn(Optional.of(space));
    when(permRepo.existsBySpaceId(spaceId)).thenReturn(false);

    listener.onWikiPageChanged(
        new WikiPageChangedEvent(pageId, spaceId, WikiPageChangedEvent.ChangeType.UPDATED));

    verify(itemRepo).deleteByEntityTypeAndEntityId(KnowledgeEntityType.WIKI_PAGE, pageId);
    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<RawChunk>> chunksCaptor = ArgumentCaptor.forClass(List.class);
    verify(ingestionService)
        .ingestChunks(
            chunksCaptor.capture(),
            eq(KnowledgeEntityType.WIKI_PAGE),
            eq(pageId),
            isNull(),
            eq(500L));
    assertThat(chunksCaptor.getValue()).isNotEmpty();
  }
}
