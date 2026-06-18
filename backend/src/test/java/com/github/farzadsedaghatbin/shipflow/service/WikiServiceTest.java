package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.CreateWikiPageRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.CreateWikiSpaceRequest;

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
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import java.util.Optional;
import java.util.stream.Collectors;
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
  void movePage_sameParent_resequencesContiguousPositions() {
    Long userId = 1L;
    Long spaceId = 10L;
    Long pageId = 1L;

    WikiSpace space = new WikiSpace();
    space.setId(spaceId);
    space.setName("Space");
    space.setSpaceKey("space");
    space.setCreatedBy(userId);

    // Three root-level pages: page(pos=0), sibling1(pos=1), sibling2(pos=2).
    // Move page from position 0 to index 1 within the same root level.
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
    // getSiblings(spaceId, null, pageId) excludes the moved page → [sibling1, sibling2]
    when(pageRepository.findBySpaceIdAndParentIdIsNullAndDeletedAtIsNullOrderByPositionAsc(spaceId))
        .thenReturn(new ArrayList<>(List.of(page, sibling1, sibling2)));
    when(pageRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    doNothing().when(permissionService).requireWrite(eq(userId), any(WikiSpace.class));

    // Move page (currently pos=0) to index 1 within the same parent (null)
    MovePageRequest req = new MovePageRequest(null, 1);
    wikiService.movePage(pageId, req, userId);

    @SuppressWarnings("rawtypes")
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(pageRepository, times(1)).saveAll(captor.capture());

    // Single saveAll should contain all 3 siblings with contiguous positions 0,1,2
    @SuppressWarnings("unchecked")
    List<WikiPage> saved = captor.getValue();
    assertThat(saved).hasSize(3);
    List<Integer> positions = saved.stream()
        .map(WikiPage::getPosition)
        .sorted()
        .collect(Collectors.toList());
    assertThat(positions).containsExactly(0, 1, 2);
    // page should now be at position 1 (inserted at index 1)
    assertThat(page.getPosition()).isEqualTo(1);
  }

  @Test
  void movePage_crossParent_resequencesBothSourceAndDestContiguously() {
    Long userId = 1L;
    Long spaceId = 10L;
    Long pageId = 1L;
    Long destParentId = 99L;

    WikiSpace space = new WikiSpace();
    space.setId(spaceId);
    space.setName("Space");
    space.setSpaceKey("space");
    space.setCreatedBy(userId);

    // Source: page(pos=0) and srcSibling(pos=1) under parent=null
    WikiPage page = new WikiPage();
    page.setId(pageId);
    page.setSpaceId(spaceId);
    page.setParentId(null);
    page.setPosition(0);
    page.setTitle("MovedPage");
    page.setSlug("moved-page");

    WikiPage srcSibling = new WikiPage();
    srcSibling.setId(2L);
    srcSibling.setSpaceId(spaceId);
    srcSibling.setParentId(null);
    srcSibling.setPosition(1);
    srcSibling.setTitle("SrcSibling");
    srcSibling.setSlug("src-sibling");

    // Destination: destSibling(pos=0) under parent=destParentId
    WikiPage destSibling = new WikiPage();
    destSibling.setId(3L);
    destSibling.setSpaceId(spaceId);
    destSibling.setParentId(destParentId);
    destSibling.setPosition(0);
    destSibling.setTitle("DestSibling");
    destSibling.setSlug("dest-sibling");

    when(pageRepository.findById(pageId)).thenReturn(Optional.of(page));
    when(spaceRepository.findById(spaceId)).thenReturn(Optional.of(space));
    // Descendants check
    when(pageRepository.findBySpaceIdAndDeletedAtIsNull(spaceId))
        .thenReturn(List.of(page, srcSibling, destSibling));
    // getSiblings for source parent (null, excludeId=pageId) → [srcSibling]
    when(pageRepository.findBySpaceIdAndParentIdIsNullAndDeletedAtIsNullOrderByPositionAsc(spaceId))
        .thenReturn(new ArrayList<>(List.of(page, srcSibling)));
    // getSiblings for dest parent (destParentId, excludeId=null) → [destSibling]
    when(pageRepository.findBySpaceIdAndParentIdAndDeletedAtIsNullOrderByPositionAsc(spaceId, destParentId))
        .thenReturn(new ArrayList<>(List.of(destSibling)));
    when(pageRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));
    doNothing().when(permissionService).requireWrite(eq(userId), any(WikiSpace.class));

    // Move page to dest parent at index 0
    MovePageRequest req = new MovePageRequest(destParentId, 0);
    wikiService.movePage(pageId, req, userId);

    @SuppressWarnings("rawtypes")
    ArgumentCaptor<List> captor = ArgumentCaptor.forClass(List.class);
    verify(pageRepository, times(2)).saveAll(captor.capture());

    List<List> allCalls = captor.getAllValues();

    // Source list: only srcSibling remains, re-numbered to position 0
    @SuppressWarnings("unchecked")
    List<WikiPage> sourceList = allCalls.get(0);
    assertThat(sourceList).hasSize(1);
    assertThat(sourceList.get(0).getId()).isEqualTo(2L);
    assertThat(sourceList.get(0).getPosition()).isEqualTo(0);

    // Dest list: page inserted at index 0, destSibling pushed to index 1
    @SuppressWarnings("unchecked")
    List<WikiPage> destList = allCalls.get(1);
    assertThat(destList).hasSize(2);
    // Positions must be contiguous 0,1
    List<Integer> destPositions = destList.stream()
        .map(WikiPage::getPosition)
        .sorted()
        .collect(Collectors.toList());
    assertThat(destPositions).containsExactly(0, 1);
    // The moved page should be at position 0 and have the new parent
    assertThat(page.getParentId()).isEqualTo(destParentId);
    assertThat(page.getPosition()).isEqualTo(0);
    // destSibling should be at position 1
    assertThat(destSibling.getPosition()).isEqualTo(1);
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
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void createSpace_propagatesAccessDeniedExceptionWhenGuardDenies() {
    Long userId = 1L;
    doThrow(new AccessDeniedException("No createSpace permission"))
        .when(permissionService)
        .requireCreateSpace(eq(userId));

    CreateWikiSpaceRequest req = new CreateWikiSpaceRequest("My Space", "my-space", "desc");

    assertThatThrownBy(() -> wikiService.createSpace(req, userId))
        .isInstanceOf(AccessDeniedException.class);

    // Space must not have been persisted
    verify(spaceRepository, never()).save(any());
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
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Page not found");
  }
}
