package com.github.farzadsedaghatbin.shipflow.service.mcp.server.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.CreateWikiPageRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.GrantPermissionRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.MovePageRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.UpdateWikiPageRequest;
import com.github.farzadsedaghatbin.shipflow.dto.wiki.WikiPageDTO;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.WikiGranteeType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.WikiPermissionLevel;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.WikiService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class WikiMcpToolsTest {

  private static final Long USER_ID = 7L;

  @Mock private WikiService wikiService;
  @Mock private UserRepository userRepository;
  @Mock private Authentication auth;

  private final ObjectMapper mapper = new ObjectMapper();
  private WikiMcpTools tools;

  @BeforeEach
  void setUp() {
    tools = new WikiMcpTools(wikiService, userRepository, mapper);
    User u = new User();
    u.setId(USER_ID);
    org.mockito.Mockito.lenient().when(auth.getName()).thenReturn("farzad");
    org.mockito.Mockito.lenient().when(userRepository.findByUsername("farzad")).thenReturn(Optional.of(u));
  }

  private static WikiPageDTO page(long id) {
    return new WikiPageDTO(id, 1L, null, "T", "t", "[]", "text", 0, 1L, null, null, List.of(), 3L);
  }

  @Test
  void markdown_isConvertedToBlockNoteBlocks() throws Exception {
    String json = tools.markdownToBlocks("# Title\n\nIntro line\n- one\n* two\n1. first\n### Deep");
    JsonNode blocks = mapper.readTree(json);
    assertThat(blocks).hasSize(6);
    assertThat(blocks.get(0).get("type").asText()).isEqualTo("heading");
    assertThat(blocks.get(0).get("props").get("level").asInt()).isEqualTo(1);
    assertThat(blocks.get(0).get("content").get(0).get("text").asText()).isEqualTo("Title");
    assertThat(blocks.get(1).get("type").asText()).isEqualTo("paragraph");
    assertThat(blocks.get(2).get("type").asText()).isEqualTo("bulletListItem");
    assertThat(blocks.get(3).get("type").asText()).isEqualTo("bulletListItem");
    assertThat(blocks.get(4).get("type").asText()).isEqualTo("numberedListItem");
    assertThat(blocks.get(5).get("props").get("level").asInt()).isEqualTo(3);
  }

  @Test
  void body_prefersContentAndRejectsInvalidJson() {
    assertThat(tools.body(Map.of("content", "[]", "markdown", "# x"))).isEqualTo("[]");
    assertThat(tools.body(Map.of())).isNull();
    assertThatThrownBy(() -> tools.body(Map.of("content", "{not json")))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> tools.body(Map.of("content", "{}")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("array");
  }

  @Test
  void createPage_runsAsTheAuthenticatedUser() {
    when(wikiService.createPage(any(), eq(USER_ID))).thenReturn(page(5));
    Map<String, Object> out =
        tools.createPage(
            Map.of("spaceId", 1, "parentId", 2, "title", " Hello ", "markdown", "text"), auth);
    ArgumentCaptor<CreateWikiPageRequest> req = ArgumentCaptor.forClass(CreateWikiPageRequest.class);
    verify(wikiService).createPage(req.capture(), eq(USER_ID));
    assertThat(req.getValue().spaceId()).isEqualTo(1L);
    assertThat(req.getValue().parentId()).isEqualTo(2L);
    assertThat(req.getValue().title()).isEqualTo("Hello");
    assertThat(req.getValue().content()).contains("\"paragraph\"");
    assertThat(out).containsEntry("id", 5L).containsEntry("version", 3L);
  }

  @Test
  void updatePage_passesExpectedVersionAndRequiresSomethingToChange() {
    when(wikiService.updatePage(eq(5L), any(), eq(USER_ID))).thenReturn(page(5));
    tools.updatePage(Map.of("pageId", 5, "title", "New", "expectedVersion", 3), auth);
    ArgumentCaptor<UpdateWikiPageRequest> req = ArgumentCaptor.forClass(UpdateWikiPageRequest.class);
    verify(wikiService).updatePage(eq(5L), req.capture(), eq(USER_ID));
    assertThat(req.getValue().title()).isEqualTo("New");
    assertThat(req.getValue().content()).isNull();
    assertThat(req.getValue().expectedVersion()).isEqualTo(3L);

    assertThatThrownBy(() -> tools.updatePage(Map.of("pageId", 5), auth))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Nothing to update");
  }

  @Test
  void movePage_defaultsIndexToZero() {
    Map<String, Object> out = tools.movePage(Map.of("pageId", 5), auth);
    verify(wikiService).movePage(eq(5L), eq(new MovePageRequest(null, 0)), eq(USER_ID));
    assertThat(out).containsEntry("moved", true);
  }

  @Test
  void deletePageAndSpace_areServiceSoftDeletes() {
    tools.deletePage(Map.of("pageId", 5), auth);
    tools.deleteSpace(Map.of("spaceId", 1), auth);
    verify(wikiService).deletePage(5L, USER_ID);
    verify(wikiService).deleteSpace(1L, USER_ID);
  }

  @Test
  void grantPermission_parsesEnumsCaseInsensitively_andRejectsUnknown() {
    tools.grantPermission(
        Map.of("spaceId", 1, "granteeType", "user", "granteeRef", "sara", "level", "write"), auth);
    verify(wikiService)
        .grantPermission(
            eq(1L),
            eq(new GrantPermissionRequest(WikiGranteeType.USER, "sara", WikiPermissionLevel.WRITE)),
            eq(USER_ID));
    assertThatThrownBy(
            () ->
                tools.grantPermission(
                    Map.of("spaceId", 1, "granteeType", "TEAM", "granteeRef", "x", "level", "READ"),
                    auth))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("granteeType");
  }

  @Test
  void restoreAndRevoke_delegate() {
    when(wikiService.restoreRevision(5L, 2, USER_ID)).thenReturn(page(5));
    assertThat(tools.restoreRevision(Map.of("pageId", 5, "revision", 2), auth)).containsKey("id");
    assertThat(tools.revokePermission(Map.of("permissionId", 9), auth)).containsEntry("revoked", true);
    verify(wikiService).revokePermission(9L, USER_ID);
  }

  @Test
  void search_requiresQuery_andMissingIdsAreRejected() {
    assertThatThrownBy(() -> tools.search(Map.of(), auth)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> tools.getPage(Map.of(), auth)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> tools.getPage(Map.of("pageId", "abc"), auth))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void noAuthentication_isRefused() {
    assertThatThrownBy(() -> tools.listSpaces(null)).isInstanceOf(SecurityException.class);
  }
}
