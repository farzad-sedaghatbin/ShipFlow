package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.GlobalSearchResultDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GlobalSearchServiceTest {

  @Mock private EntityManager entityManager;

  @Mock private Query nativeQuery;

  @InjectMocks private GlobalSearchService globalSearchService;

  @BeforeEach
  void setUp() {
    lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
    lenient().when(nativeQuery.setParameter(anyString(), any())).thenReturn(nativeQuery);
  }

  @Test
  void search_WithNullQuery_ShouldReturnEmptyList() {
    List<GlobalSearchResultDTO> results = globalSearchService.search(null, 1L, 10);
    assertThat(results).isEmpty();
    verifyNoInteractions(entityManager);
  }

  @Test
  void search_WithEmptyQuery_ShouldReturnEmptyList() {
    List<GlobalSearchResultDTO> results = globalSearchService.search("", 1L, 10);
    assertThat(results).isEmpty();
    verifyNoInteractions(entityManager);
  }

  @Test
  void search_WithSingleCharQuery_ShouldReturnEmptyList() {
    List<GlobalSearchResultDTO> results = globalSearchService.search("a", 1L, 10);
    assertThat(results).isEmpty();
    verifyNoInteractions(entityManager);
  }

  @Test
  void search_WithWhitespaceOnlyQuery_ShouldReturnEmptyList() {
    List<GlobalSearchResultDTO> results = globalSearchService.search("   ", 1L, 10);
    assertThat(results).isEmpty();
    verifyNoInteractions(entityManager);
  }

  @Test
  void search_WithValidQuery_ShouldReturnMappedResults() {
    Object[] taskRow = new Object[]{"TASK", 1L, "Login Page", "IN_PROGRESS", "/backlog/1", 0.85, "TRIGRAM"};
    Object[] bugRow = new Object[]{"BUG_REPORT", 42L, "Button broken", "BUG-42 · NEW", "/qa/bug-reports/42", 1.0, "EXACT_KEY"};

    when(nativeQuery.getResultList()).thenReturn(Arrays.asList(bugRow, taskRow));

    List<GlobalSearchResultDTO> results = globalSearchService.search("login", 5L, 10);

    assertThat(results).hasSize(2);

    GlobalSearchResultDTO first = results.get(0);
    assertThat(first.getEntityType()).isEqualTo("BUG_REPORT");
    assertThat(first.getEntityId()).isEqualTo(42L);
    assertThat(first.getTitle()).isEqualTo("Button broken");
    assertThat(first.getSubtitle()).isEqualTo("BUG-42 · NEW");
    assertThat(first.getRoute()).isEqualTo("/qa/bug-reports/42");
    assertThat(first.getScore()).isEqualTo(1.0);
    assertThat(first.getMatchedBy()).isEqualTo("EXACT_KEY");

    GlobalSearchResultDTO second = results.get(1);
    assertThat(second.getEntityType()).isEqualTo("TASK");
    assertThat(second.getEntityId()).isEqualTo(1L);
    assertThat(second.getTitle()).isEqualTo("Login Page");
    assertThat(second.getScore()).isEqualTo(0.85);
  }

  @Test
  void search_ShouldSetCorrectParameters() {
    when(nativeQuery.getResultList()).thenReturn(List.of());

    globalSearchService.search("login", 7L, 15);

    verify(nativeQuery).setParameter("query", "login");
    verify(nativeQuery).setParameter("queryPattern", "%login%");
    verify(nativeQuery).setParameter("projectId", 7L);
    verify(nativeQuery).setParameter("resultLimit", 15);
  }

  @Test
  void search_ShouldTrimQuery() {
    when(nativeQuery.getResultList()).thenReturn(List.of());

    globalSearchService.search("  login  ", 7L, 10);

    verify(nativeQuery).setParameter("query", "login");
    verify(nativeQuery).setParameter("queryPattern", "%login%");
  }

  @Test
  void search_WithNoResults_ShouldReturnEmptyList() {
    when(nativeQuery.getResultList()).thenReturn(List.of());

    List<GlobalSearchResultDTO> results = globalSearchService.search("nonexistent", 1L, 10);
    assertThat(results).isEmpty();
  }

  @Test
  void search_WithSubtaskResult_ShouldMapCorrectly() {
    Object[] subtaskRow = new Object[]{"SUBTASK", 99L, "Fix CSS", "DONE", "/backlog/99", 0.7, "TRIGRAM"};
    List<Object[]> rows = new ArrayList<>();
    rows.add(subtaskRow);
    when(nativeQuery.getResultList()).thenReturn(rows);

    List<GlobalSearchResultDTO> results = globalSearchService.search("CSS", 1L, 10);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getEntityType()).isEqualTo("SUBTASK");
    assertThat(results.get(0).getRoute()).isEqualTo("/backlog/99");
  }

  @Test
  void search_WithPitchResult_ShouldMapCorrectly() {
    Object[] pitchRow = new Object[]{"PITCH", 10L, "Mobile Checkout", "SHAPED", "/pitches/10", 0.65, "TRIGRAM"};
    List<Object[]> rows = new ArrayList<>();
    rows.add(pitchRow);
    when(nativeQuery.getResultList()).thenReturn(rows);

    List<GlobalSearchResultDTO> results = globalSearchService.search("checkout", 2L, 5);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getEntityType()).isEqualTo("PITCH");
    assertThat(results.get(0).getTitle()).isEqualTo("Mobile Checkout");
    assertThat(results.get(0).getRoute()).isEqualTo("/pitches/10");
  }

  @Test
  void search_WithEpicResult_ShouldMapCorrectly() {
    Object[] epicRow = new Object[]{"EPIC", 3L, "Platform Redesign", "IN_PROGRESS", "/epics/3", 0.55, "TRIGRAM"};
    List<Object[]> rows = new ArrayList<>();
    rows.add(epicRow);
    when(nativeQuery.getResultList()).thenReturn(rows);

    List<GlobalSearchResultDTO> results = globalSearchService.search("redesign", 4L, 10);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).getEntityType()).isEqualTo("EPIC");
    assertThat(results.get(0).getEntityId()).isEqualTo(3L);
    assertThat(results.get(0).getRoute()).isEqualTo("/epics/3");
  }

  @Test
  void search_SqlQueryShouldContainAllEntityTypes() {
    when(nativeQuery.getResultList()).thenReturn(List.of());

    globalSearchService.search("test", 1L, 10);

    var sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(entityManager).createNativeQuery(sqlCaptor.capture());
    String sql = sqlCaptor.getValue();

    assertThat(sql).contains("tasks t");
    assertThat(sql).contains("bug_reports br");
    assertThat(sql).contains("pitches p");
    assertThat(sql).contains("epics e");
    assertThat(sql).contains("UNION ALL");
    assertThat(sql).contains("similarity");
    assertThat(sql).contains("ORDER BY score DESC");
  }

  @Test
  void search_SqlQueryShouldFilterByProjectId() {
    when(nativeQuery.getResultList()).thenReturn(List.of());

    globalSearchService.search("test", 1L, 10);

    var sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(entityManager).createNativeQuery(sqlCaptor.capture());
    String sql = sqlCaptor.getValue();

    assertThat(sql).contains(":projectId");
    assertThat(sql).contains("c.project_id = :projectId");
    assertThat(sql).contains("br.project_id = :projectId");
    assertThat(sql).contains("e.project_id = :projectId");
  }

  @Test
  void search_SqlQueryShouldRespectSoftDeletes() {
    when(nativeQuery.getResultList()).thenReturn(List.of());

    globalSearchService.search("test", 1L, 10);

    var sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(entityManager).createNativeQuery(sqlCaptor.capture());
    String sql = sqlCaptor.getValue();

    assertThat(sql).contains("t.deleted_at IS NULL");
    assertThat(sql).contains("p.deleted_at IS NULL");
    assertThat(sql).contains("e.deleted_at IS NULL");
  }

  @Test
  void search_SqlQueryShouldContainWikiBranch() {
    when(nativeQuery.getResultList()).thenReturn(List.of());

    globalSearchService.search("test", 1L, 10);

    var sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(entityManager).createNativeQuery(sqlCaptor.capture());
    String sql = sqlCaptor.getValue();

    assertThat(sql).contains("wiki_pages");
    assertThat(sql).contains("wiki_spaces");
    assertThat(sql).contains("WIKI_PAGE");
    assertThat(sql).contains("/wiki/");
    assertThat(sql).contains("content_text");
    assertThat(sql).contains("ws.project_id = :projectId");
    assertThat(sql).contains("wp.deleted_at IS NULL");
    assertThat(sql).contains("ws.deleted_at IS NULL");
  }

  @Test
  void search_SqlQueryShouldContainWikiSpaceBranch() {
    when(nativeQuery.getResultList()).thenReturn(List.of());

    globalSearchService.search("test", 1L, 10);

    var sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(entityManager).createNativeQuery(sqlCaptor.capture());
    String sql = sqlCaptor.getValue();

    // Wiki space names are searchable and route to the space by numeric id.
    assertThat(sql).contains("WIKI_SPACE");
    assertThat(sql).contains("similarity(ws.name, :query)");
    assertThat(sql).contains("CONCAT('/wiki/', ws.id)");
  }

  @Test
  void search_WikiPageRoute_UsesNumericSpaceId_NotSpaceKey() {
    when(nativeQuery.getResultList()).thenReturn(List.of());

    globalSearchService.search("test", 1L, 10);

    var sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(entityManager).createNativeQuery(sqlCaptor.capture());
    String sql = sqlCaptor.getValue();

    // The router uses Number(spaceId); the route must carry the numeric space id, not space_key.
    assertThat(sql).contains("CONCAT('/wiki/', ws.id, '/', wp.id)");
    assertThat(sql).doesNotContain("ws.space_key, '/', wp.id");
  }

  @Test
  void search_WithProject_WikiScopeIncludesOrgGlobalSpaces() {
    when(nativeQuery.getResultList()).thenReturn(List.of());

    globalSearchService.search("test", 1L, 10);

    var sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(entityManager).createNativeQuery(sqlCaptor.capture());
    String sql = sqlCaptor.getValue();

    assertThat(sql).contains("(ws.project_id = :projectId OR ws.project_id IS NULL)");
  }

  @Test
  void search_NullProjectId_SearchesOrgGlobalWikiOnly_AndOmitsProjectIdParam() {
    when(nativeQuery.getResultList()).thenReturn(List.of());

    globalSearchService.search("test", null, 10);

    var sqlCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
    verify(entityManager).createNativeQuery(sqlCaptor.capture());
    String sql = sqlCaptor.getValue();

    // Wiki is searched org-globally...
    assertThat(sql).contains("WIKI_SPACE");
    assertThat(sql).contains("WIKI_PAGE");
    assertThat(sql).contains("ws.project_id IS NULL");
    // ...but project-scoped entities are excluded entirely (no :projectId reference).
    assertThat(sql).doesNotContain("tasks t");
    assertThat(sql).doesNotContain("bug_reports br");
    assertThat(sql).doesNotContain("epics e");
    assertThat(sql).doesNotContain(":projectId");

    // And the :projectId parameter must NOT be bound (it isn't in the SQL).
    verify(nativeQuery, never()).setParameter(eq("projectId"), any());
  }

  @Test
  void search_WithWikiPageResult_ShouldMapCorrectly() {
    Object[] wikiRow =
        new Object[] {
          "WIKI_PAGE", 7L, "Getting Started", "Engineering", "/wiki/eng/7", 0.72, "LIKE_TITLE"
        };
    List<Object[]> rows = new ArrayList<>();
    rows.add(wikiRow);
    when(nativeQuery.getResultList()).thenReturn(rows);

    List<GlobalSearchResultDTO> results = globalSearchService.search("started", 3L, 10);

    assertThat(results).hasSize(1);
    GlobalSearchResultDTO dto = results.get(0);
    assertThat(dto.getEntityType()).isEqualTo("WIKI_PAGE");
    assertThat(dto.getEntityId()).isEqualTo(7L);
    assertThat(dto.getTitle()).isEqualTo("Getting Started");
    assertThat(dto.getSubtitle()).isEqualTo("Engineering");
    assertThat(dto.getRoute()).isEqualTo("/wiki/eng/7");
    assertThat(dto.getScore()).isEqualTo(0.72);
    assertThat(dto.getMatchedBy()).isEqualTo("LIKE_TITLE");
  }
}
