package com.github.farzadsedaghatbin.shipflow.service.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.UploadedDocument;
import com.github.farzadsedaghatbin.shipflow.entity.WikiPage;
import com.github.farzadsedaghatbin.shipflow.entity.WikiSpace;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeEntityType;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeSourceRepository;
import com.github.farzadsedaghatbin.shipflow.repository.MeetingRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ReleaseRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UploadedDocumentRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiPageRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpaceRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("KnowledgeProjectResolver")
class KnowledgeProjectResolverTest {

  @Mock private CycleRepository cycleRepository;
  @Mock private PitchRepository pitchRepository;
  @Mock private EpicRepository epicRepository;
  @Mock private InitiativeRepository initiativeRepository;
  @Mock private ReleaseRepository releaseRepository;
  @Mock private MeetingRepository meetingRepository;
  @Mock private WikiPageRepository wikiPageRepository;
  @Mock private WikiSpaceRepository wikiSpaceRepository;
  @Mock private KnowledgeSourceRepository knowledgeSourceRepository;
  @Mock private UploadedDocumentRepository documentRepository;

  private KnowledgeProjectResolver resolver;

  @BeforeEach
  void setUp() {
    resolver =
        new KnowledgeProjectResolver(
            cycleRepository, pitchRepository, epicRepository, initiativeRepository,
            releaseRepository, meetingRepository, wikiPageRepository, wikiSpaceRepository,
            knowledgeSourceRepository, documentRepository);
  }

  private static Project project(long id) {
    Project p = new Project();
    p.setId(id);
    return p;
  }

  @Test
  @DisplayName("A cycle resolves to its project")
  void cycle() {
    Cycle c = new Cycle();
    c.setProject(project(5));
    when(cycleRepository.findById(1L)).thenReturn(Optional.of(c));

    assertThat(resolver.resolve(KnowledgeEntityType.CYCLE, 1L, null, null)).contains(5L);
  }

  @Test
  @DisplayName("A pitch prefers its direct project, else its cycle's")
  void pitch() {
    Pitch direct = new Pitch();
    direct.setProject(project(7));
    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(direct));

    Pitch viaCycle = new Pitch();
    Cycle c = new Cycle();
    c.setProject(project(8));
    viaCycle.setCycle(c);
    when(pitchRepository.findByIdNotDeleted(2L)).thenReturn(Optional.of(viaCycle));

    assertThat(resolver.resolve(KnowledgeEntityType.PITCH, 1L, null, null)).contains(7L);
    assertThat(resolver.resolve(KnowledgeEntityType.PITCH, 2L, null, null)).contains(8L);
  }

  @Test
  @DisplayName("An epic resolves to its project")
  void epic() {
    Epic e = new Epic();
    e.setProject(project(9));
    when(epicRepository.findById(3L)).thenReturn(Optional.of(e));

    assertThat(resolver.resolve(KnowledgeEntityType.EPIC, 3L, null, null)).contains(9L);
  }

  @Test
  @DisplayName("A wiki page resolves through its space")
  void wikiPage() {
    WikiPage page = new WikiPage();
    page.setSpaceId(4L);
    WikiSpace space = new WikiSpace();
    space.setProjectId(11L);
    when(wikiPageRepository.findById(1L)).thenReturn(Optional.of(page));
    when(wikiSpaceRepository.findById(4L)).thenReturn(Optional.of(space));

    assertThat(resolver.resolve(KnowledgeEntityType.WIKI_PAGE, 1L, null, null)).contains(11L);
  }

  @Test
  @DisplayName("An uploaded document resolves through the pitch it is attached to")
  void documentViaPitch() {
    UploadedDocument doc = UploadedDocument.builder().id(1L).entityType("PITCH").entityId(2L).build();
    when(documentRepository.findById(1L)).thenReturn(Optional.of(doc));
    Pitch p = new Pitch();
    p.setProject(project(12));
    when(pitchRepository.findByIdNotDeleted(2L)).thenReturn(Optional.of(p));

    assertThat(resolver.resolve(KnowledgeEntityType.DOCUMENT, 1L, null, null)).contains(12L);
  }

  @Test
  @DisplayName("Falls back to the item's pitch, then its cycle, when the entity itself has no project")
  void fallbacks() {
    Pitch p = new Pitch();
    p.setProject(project(13));
    when(pitchRepository.findByIdNotDeleted(6L)).thenReturn(Optional.of(p));
    Cycle c = new Cycle();
    c.setProject(project(14));
    when(cycleRepository.findById(7L)).thenReturn(Optional.of(c));

    assertThat(resolver.resolve(KnowledgeEntityType.WORKLOG, 99L, 7L, 6L)).contains(13L);
    assertThat(resolver.resolve(KnowledgeEntityType.WORKLOG, 99L, 7L, null)).contains(14L);
  }

  @Test
  @DisplayName("Org-wide content with no determinable project stays project-less")
  void noProject() {
    assertThat(resolver.resolve(KnowledgeEntityType.REFERENCE_DOCUMENT, 1L, null, null)).isEmpty();
    assertThat(resolver.resolve(KnowledgeEntityType.MANUAL_NOTE, null, null, null)).isEmpty();
  }

  @Test
  @DisplayName("A lookup failure degrades to 'no project' instead of throwing")
  void neverThrows() {
    when(cycleRepository.findById(1L)).thenThrow(new RuntimeException("db down"));

    assertThat(resolver.resolve(KnowledgeEntityType.CYCLE, 1L, null, null)).isEmpty();
  }
}
