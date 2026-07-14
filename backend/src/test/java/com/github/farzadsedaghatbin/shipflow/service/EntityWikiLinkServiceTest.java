package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.wikilink.LinkedWikiPageDTO;
import com.github.farzadsedaghatbin.shipflow.entity.EntityWikiLink;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.WikiPage;
import com.github.farzadsedaghatbin.shipflow.entity.WikiSpace;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.EntityWikiLinkRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiPageRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpaceRepository;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class EntityWikiLinkServiceTest {

  @Mock
  private EntityWikiLinkRepository linkRepository;

  @Mock
  private WikiPageRepository wikiPageRepository;

  @Mock
  private WikiSpaceRepository wikiSpaceRepository;

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private TaskRepository taskRepository;

  @Mock
  private UserRepository userRepository;

  private EntityWikiLinkService service;

  private WikiPage testPage;
  private WikiSpace testSpace;
  private User testUser;

  @BeforeEach
  void setUp() {
    service = new EntityWikiLinkService(
        linkRepository, wikiPageRepository, wikiSpaceRepository, pitchRepository, taskRepository,
        userRepository);

    testSpace = WikiSpace.builder().id(10L).name("Engineering").spaceKey("ENG").build();
    testPage = WikiPage.builder().id(100L).spaceId(10L).title("Research Notes").slug("research-notes")
        .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now()).build();
    testUser = User.builder().id(1L).username("alice").build();

    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("alice", null, List.of()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void linkWikiPage_toPitch_createsLink() {
    when(pitchRepository.findByIdNotDeleted(5L))
        .thenReturn(Optional.of(Pitch.builder().id(5L).build()));
    when(wikiPageRepository.findById(100L)).thenReturn(Optional.of(testPage));
    when(linkRepository.existsByEntityTypeAndEntityIdAndWikiPageId("PITCH", 5L, 100L))
        .thenReturn(false);
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
    when(wikiSpaceRepository.findById(10L)).thenReturn(Optional.of(testSpace));
    when(linkRepository.save(any(EntityWikiLink.class))).thenAnswer(invocation -> {
      EntityWikiLink link = invocation.getArgument(0);
      link.setId(1000L);
      link.setLinkedAt(LocalDateTime.now());
      return link;
    });

    LinkedWikiPageDTO result = service.linkWikiPage("pitch", 5L, 100L);

    assertThat(result.linkId()).isEqualTo(1000L);
    assertThat(result.wikiPageId()).isEqualTo(100L);
    assertThat(result.title()).isEqualTo("Research Notes");
    assertThat(result.spaceName()).isEqualTo("Engineering");
    assertThat(result.linkedByName()).isEqualTo("alice");
    verify(linkRepository).save(argThat(link -> "PITCH".equals(link.getEntityType())
        && link.getEntityId().equals(5L) && link.getWikiPage().getId().equals(100L)));
  }

  @Test
  void linkWikiPage_toTask_createsLink() {
    when(taskRepository.findByIdNotDeleted(7L)).thenReturn(Optional.of(Task.builder().id(7L).build()));
    when(wikiPageRepository.findById(100L)).thenReturn(Optional.of(testPage));
    when(linkRepository.existsByEntityTypeAndEntityIdAndWikiPageId("TASK", 7L, 100L)).thenReturn(false);
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(testUser));
    when(wikiSpaceRepository.findById(10L)).thenReturn(Optional.of(testSpace));
    when(linkRepository.save(any(EntityWikiLink.class))).thenAnswer(invocation -> {
      EntityWikiLink link = invocation.getArgument(0);
      link.setId(1001L);
      link.setLinkedAt(LocalDateTime.now());
      return link;
    });

    LinkedWikiPageDTO result = service.linkWikiPage("TASK", 7L, 100L);

    assertThat(result.linkId()).isEqualTo(1001L);
    verify(taskRepository).findByIdNotDeleted(7L);
    verifyNoInteractions(pitchRepository);
  }

  @Test
  void linkWikiPage_unsupportedEntityType_throws() {
    assertThatThrownBy(() -> service.linkWikiPage("BUG_REPORT", 1L, 100L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported entity type");
    verifyNoInteractions(linkRepository, wikiPageRepository);
  }

  @Test
  void linkWikiPage_pitchNotFound_throwsResourceNotFound() {
    when(pitchRepository.findByIdNotDeleted(5L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.linkWikiPage("PITCH", 5L, 100L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("PITCH not found");
    verifyNoInteractions(wikiPageRepository, linkRepository);
  }

  @Test
  void linkWikiPage_wikiPageNotFound_throwsResourceNotFound() {
    when(pitchRepository.findByIdNotDeleted(5L))
        .thenReturn(Optional.of(Pitch.builder().id(5L).build()));
    when(wikiPageRepository.findById(100L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.linkWikiPage("PITCH", 5L, 100L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Wiki page not found");
  }

  @Test
  void linkWikiPage_softDeletedWikiPage_treatedAsNotFound() {
    WikiPage deletedPage = WikiPage.builder().id(100L).spaceId(10L).title("Old")
        .deletedAt(OffsetDateTime.now()).build();
    when(pitchRepository.findByIdNotDeleted(5L))
        .thenReturn(Optional.of(Pitch.builder().id(5L).build()));
    when(wikiPageRepository.findById(100L)).thenReturn(Optional.of(deletedPage));

    assertThatThrownBy(() -> service.linkWikiPage("PITCH", 5L, 100L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void linkWikiPage_duplicateLink_throwsIllegalState() {
    when(pitchRepository.findByIdNotDeleted(5L))
        .thenReturn(Optional.of(Pitch.builder().id(5L).build()));
    when(wikiPageRepository.findById(100L)).thenReturn(Optional.of(testPage));
    when(linkRepository.existsByEntityTypeAndEntityIdAndWikiPageId("PITCH", 5L, 100L))
        .thenReturn(true);

    assertThatThrownBy(() -> service.linkWikiPage("PITCH", 5L, 100L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("already linked");
    verify(linkRepository, never()).save(any());
  }

  @Test
  void getLinkedWikiPages_returnsDtosForEntity() {
    EntityWikiLink link = EntityWikiLink.builder().id(1000L).entityType("PITCH").entityId(5L)
        .wikiPage(testPage).linkedAt(LocalDateTime.now()).linkedBy(testUser).build();
    when(linkRepository.findByEntityTypeAndEntityId("PITCH", 5L)).thenReturn(List.of(link));
    when(wikiSpaceRepository.findById(10L)).thenReturn(Optional.of(testSpace));

    List<LinkedWikiPageDTO> results = service.getLinkedWikiPages("pitch", 5L);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).title()).isEqualTo("Research Notes");
    assertThat(results.get(0).spaceName()).isEqualTo("Engineering");
  }

  @Test
  void getLinkedWikiPages_unsupportedEntityType_throws() {
    assertThatThrownBy(() -> service.getLinkedWikiPages("BUG_REPORT", 1L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void unlinkWikiPage_removesLink() {
    EntityWikiLink link = EntityWikiLink.builder().id(1000L).entityType("PITCH").entityId(5L)
        .wikiPage(testPage).linkedAt(LocalDateTime.now()).build();
    when(linkRepository.findByEntityTypeAndEntityIdAndWikiPageId("PITCH", 5L, 100L))
        .thenReturn(Optional.of(link));

    service.unlinkWikiPage("PITCH", 5L, 100L);

    verify(linkRepository).delete(link);
  }

  @Test
  void unlinkWikiPage_notFound_throwsResourceNotFound() {
    when(linkRepository.findByEntityTypeAndEntityIdAndWikiPageId("PITCH", 5L, 100L))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.unlinkWikiPage("PITCH", 5L, 100L))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(linkRepository, never()).delete(any());
  }
}
