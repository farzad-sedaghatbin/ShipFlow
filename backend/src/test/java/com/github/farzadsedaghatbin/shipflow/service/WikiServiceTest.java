package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.CreateWikiPageRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.MovePageRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiPageDTO;
import com.github.farzadsedaghatbin.shipflow.entity.WikiPage;
import com.github.farzadsedaghatbin.shipflow.entity.WikiSpace;
import com.github.farzadsedaghatbin.shipflow.event.WikiPageChangedEvent;
import com.github.farzadsedaghatbin.shipflow.repository.WikiPageRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpacePermissionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpaceRepository;
import com.github.farzadsedaghatbin.shipflow.service.wiki.WikiHistoryReader;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

class WikiServiceTest {

  private WikiSpaceRepository spaceRepository;
  private WikiPageRepository pageRepository;
  private WikiSpacePermissionRepository permissionRepository;
  private WikiPermissionService permissionService;
  private WikiHistoryReader historyReader;
  private ApplicationEventPublisher eventPublisher;
  private WikiService wikiService;

  @BeforeEach
  void setUp() {
    spaceRepository = mock(WikiSpaceRepository.class);
    pageRepository = mock(WikiPageRepository.class);
    permissionRepository = mock(WikiSpacePermissionRepository.class);
    permissionService = mock(WikiPermissionService.class);
    historyReader = mock(WikiHistoryReader.class);
    eventPublisher = mock(ApplicationEventPublisher.class);
    wikiService =
        new WikiService(
            spaceRepository,
            pageRepository,
            permissionRepository,
            permissionService,
            historyReader,
            eventPublisher,
            new ObjectMapper());
  }

  @Test
  void createPage_publishesCreatedEvent_andSetsSlugAndContentText() {
    Long userId = 1L;
    Long spaceId = 10L;
    String blockNoteJson =
        "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":"
            + "[{\"type\":\"text\",\"text\":\"Hello world\"}]}]}";

    WikiSpace space = new WikiSpace();
    space.setId(spaceId);
    space.setName("Test Space");
    space.setSpaceKey("test-space");
    space.setCreatedBy(userId);

    when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
    when(pageRepository.findBySpaceIdAndParentIdIsNullAndDeletedAtIsNullOrderByPositionAsc(spaceId))
        .thenReturn(Collections.emptyList());

    WikiPage savedPage = new WikiPage();
    savedPage.setId(100L);
    savedPage.setSpaceId(spaceId);
    savedPage.setTitle("My Page");
    savedPage.setSlug("my-page");
    savedPage.setContent(blockNoteJson);
    savedPage.setContentText("Hello world");
    savedPage.setPosition(0);
    savedPage.setCreatedBy(userId);
    savedPage.setCreatedAt(OffsetDateTime.now());
    savedPage.setUpdatedAt(OffsetDateTime.now());

    when(pageRepository.save(any(WikiPage.class))).thenReturn(savedPage);
    doNothing().when(permissionService).requireWrite(eq(userId), any(WikiSpace.class));

    CreateWikiPageRequest req = new CreateWikiPageRequest(spaceId, null, "My Page", blockNoteJson);

    WikiPageDTO result = wikiService.createPage(req, userId);

    ArgumentCaptor<WikiPageChangedEvent> eventCaptor =
        ArgumentCaptor.forClass(WikiPageChangedEvent.class);
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    WikiPageChangedEvent event = eventCaptor.getValue();
    assertThat(event.type()).isEqualTo(WikiPageChangedEvent.ChangeType.CREATED);
    assertThat(event.spaceId()).isEqualTo(spaceId);

    assertThat(result.slug()).isEqualTo("my-page");
    assertThat(result.contentText()).isEqualTo("Hello world");
  }

  @Test
  void extractText_extractsFromBlockNoteJson() {
    String json =
        "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\",\"content\":"
            + "[{\"type\":\"text\",\"text\":\"Hello\"},{\"type\":\"text\",\"text\":\"world\"}]}]}";
    String text = wikiService.extractText(json);
    assertThat(text).contains("Hello").contains("world");
  }

  @Test
  void extractText_returnsEmptyForNullInput() {
    assertThat(wikiService.extractText(null)).isEmpty();
    assertThat(wikiService.extractText("   ")).isEmpty();
    assertThat(wikiService.extractText("{invalid json")).isEmpty();
  }

  @Test
  void movePage_intoOwnDescendant_throws() {
    Long userId = 1L;
    Long spaceId = 10L;
    Long pageId = 1L;
    Long childId = 2L;

    WikiSpace space = new WikiSpace();
    space.setId(spaceId);
    space.setName("Space");
    space.setSpaceKey("space");
    space.setCreatedBy(userId);

    WikiPage page = new WikiPage();
    page.setId(pageId);
    page.setSpaceId(spaceId);
    page.setParentId(null);
    page.setPosition(0);
    page.setTitle("Parent");
    page.setSlug("parent");

    WikiPage child = new WikiPage();
    child.setId(childId);
    child.setSpaceId(spaceId);
    child.setParentId(pageId);
    child.setPosition(0);
    child.setTitle("Child");
    child.setSlug("child");

    when(pageRepository.findById(pageId)).thenReturn(Optional.of(page));
    when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
    when(pageRepository.findBySpaceIdAndDeletedAtIsNull(spaceId))
        .thenReturn(List.of(page, child));
    doNothing().when(permissionService).requireWrite(eq(userId), any(WikiSpace.class));

    MovePageRequest req = new MovePageRequest(childId, 0);

    assertThatThrownBy(() -> wikiService.movePage(pageId, req, userId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("cycle");
  }

  @Test
  void movePage_intoItself_throws() {
    Long userId = 1L;
    Long spaceId = 10L;
    Long pageId = 1L;

    WikiSpace space = new WikiSpace();
    space.setId(spaceId);
    space.setName("Space");
    space.setSpaceKey("space");
    space.setCreatedBy(userId);

    WikiPage page = new WikiPage();
    page.setId(pageId);
    page.setSpaceId(spaceId);
    page.setParentId(null);
    page.setPosition(0);
    page.setTitle("Page");
    page.setSlug("page");

    when(pageRepository.findById(pageId)).thenReturn(Optional.of(page));
    when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
    doNothing().when(permissionService).requireWrite(eq(userId), any(WikiSpace.class));

    MovePageRequest req = new MovePageRequest(pageId, 0);

    assertThatThrownBy(() -> wikiService.movePage(pageId, req, userId))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("itself");
  }

  @Test
  void movePage_resequencesSiblingPositions() {
    Long userId = 1L;
    Long spaceId = 10L;
    Long pageId = 1L;

    WikiSpace space = new WikiSpace();
    space.setId(spaceId);
    space.setName("Space");
    space.setSpaceKey("space");
    space.setCreatedBy(userId);

    WikiPage page = new WikiPage();
    page.setId(pageId);
    page.setSpaceId(spaceId);
    page.setParentId(null);
    page.setPosition(0);
    page.setTitle("Page1");
    page.setSlug("page1");

    WikiPage sibling1 = new WikiPage();
    sibling1.setId(2L);
    sibling1.setSpaceId(spaceId);
    sibling1.setParentId(null);
    sibling1.setPosition(1);
    sibling1.setTitle("Page2");
    sibling1.setSlug("page2");

    WikiPage sibling2 = new WikiPage();
    sibling2.setId(3L);
    sibling2.setSpaceId(spaceId);
    sibling2.setParentId(null);
    sibling2.setPosition(2);
    sibling2.setTitle("Page3");
    sibling2.setSlug("page3");

    when(pageRepository.findById(pageId)).thenReturn(Optional.of(page));
    when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
    when(pageRepository.findBySpaceIdAndDeletedAtIsNull(spaceId))
        .thenReturn(List.of(page, sibling1, sibling2));
    // First call (old siblings, excludes the moved page → 2 items after filter)
    // Second call (new siblings, excludeId=null → returns 2 items; page is then inserted)
    when(pageRepository.findBySpaceIdAndParentIdIsNullAndDeletedAtIsNullOrderByPositionAsc(spaceId))
        .thenReturn(
            new ArrayList<>(List.of(page, sibling1, sibling2)), // first call
            new ArrayList<>(List.of(sibling1, sibling2))); // second call (page already removed)
    when(pageRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    doNothing().when(permissionService).requireWrite(eq(userId), any(WikiSpace.class));

    MovePageRequest req = new MovePageRequest(null, 1);
    wikiService.movePage(pageId, req, userId);

    @SuppressWarnings("rawtypes")
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(pageRepository, atLeastOnce()).saveAll(captor.capture());
    // After reorder, the second saveAll call should contain 3 items (two original + inserted page)
    List<List> allCalls = captor.getAllValues();
    boolean found = allCalls.stream().anyMatch(l -> l.size() == 3);
    assertThat(found).isTrue();
  }

  @Test
  void deletePage_setsDeletedAtAndPublishesDeletedEvent() {
    Long userId = 1L;
    Long spaceId = 10L;
    Long pageId = 1L;

    WikiSpace space = new WikiSpace();
    space.setId(spaceId);
    space.setCreatedBy(userId);

    WikiPage page = new WikiPage();
    page.setId(pageId);
    page.setSpaceId(spaceId);
    page.setTitle("Page");
    page.setSlug("page");

    when(pageRepository.findById(pageId)).thenReturn(Optional.of(page));
    when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
    when(pageRepository.save(any())).thenReturn(page);
    doNothing().when(permissionService).requireWrite(eq(userId), any(WikiSpace.class));

    wikiService.deletePage(pageId, userId);

    assertThat(page.getDeletedAt()).isNotNull();

    ArgumentCaptor<WikiPageChangedEvent> captor =
        ArgumentCaptor.forClass(WikiPageChangedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().type()).isEqualTo(WikiPageChangedEvent.ChangeType.DELETED);
  }

  @Test
  void restoreRevision_readsFromHistoryReaderAndPublishesRestoredEvent() {
    Long userId = 1L;
    Long spaceId = 10L;
    Long pageId = 1L;
    int revision = 3;

    WikiSpace space = new WikiSpace();
    space.setId(spaceId);
    space.setCreatedBy(userId);

    WikiPage page = new WikiPage();
    page.setId(pageId);
    page.setSpaceId(spaceId);
    page.setTitle("Current");
    page.setSlug("current");

    WikiPage historicPage = new WikiPage();
    historicPage.setId(pageId);
    historicPage.setSpaceId(spaceId);
    historicPage.setTitle("Old Title");
    historicPage.setContent("{\"content\":[{\"text\":\"Old content\"}]}");

    when(pageRepository.findById(pageId)).thenReturn(Optional.of(page));
    when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
    when(historyReader.revision(pageId, revision)).thenReturn(Optional.of(historicPage));
    when(pageRepository.save(any())).thenReturn(page);
    doNothing().when(permissionService).requireWrite(eq(userId), any(WikiSpace.class));

    wikiService.restoreRevision(pageId, revision, userId);

    verify(historyReader).revision(pageId, revision);
    assertThat(page.getTitle()).isEqualTo("Old Title");

    ArgumentCaptor<WikiPageChangedEvent> captor =
        ArgumentCaptor.forClass(WikiPageChangedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertThat(captor.getValue().type()).isEqualTo(WikiPageChangedEvent.ChangeType.RESTORED);
  }

  @Test
  void writeOpWithoutPermission_propagatesAccessDeniedException() {
    Long userId = 1L;
    Long spaceId = 10L;
    Long pageId = 1L;

    WikiSpace space = new WikiSpace();
    space.setId(spaceId);
    space.setCreatedBy(2L); // different owner

    WikiPage page = new WikiPage();
    page.setId(pageId);
    page.setSpaceId(spaceId);
    page.setTitle("Page");
    page.setSlug("page");

    when(pageRepository.findById(pageId)).thenReturn(Optional.of(page));
    when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
    doThrow(new AccessDeniedException("No write permission"))
        .when(permissionService)
        .requireWrite(eq(userId), any(WikiSpace.class));

    assertThatThrownBy(() -> wikiService.deletePage(pageId, userId))
        .isInstanceOf(AccessDeniedException.class);
  }

  @Test
  void getPage_notFound_throwsNoSuchElement() {
    when(pageRepository.findById(999L)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> wikiService.getPage(999L, 1L))
        .isInstanceOf(NoSuchElementException.class);
  }

  @Test
  void getPage_softDeleted_throwsNoSuchElement() {
    WikiPage deleted = new WikiPage();
    deleted.setId(1L);
    deleted.setSpaceId(10L);
    deleted.setDeletedAt(OffsetDateTime.now().minusDays(1));
    deleted.setTitle("Deleted");
    deleted.setSlug("deleted");

    when(pageRepository.findById(1L)).thenReturn(Optional.of(deleted));

    assertThatThrownBy(() -> wikiService.getPage(1L, 1L))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessageContaining("Page not found");
  }
}
