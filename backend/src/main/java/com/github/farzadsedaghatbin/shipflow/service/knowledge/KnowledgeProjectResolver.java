package com.github.farzadsedaghatbin.shipflow.service.knowledge;

import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
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
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Works out which project a knowledge chunk belongs to, so the chunk can carry a {@code projectId}
 * in its embedding metadata and {@code SecurityDocumentFilter} can keep it away from people who
 * cannot see that project.
 *
 * <p>Until this existed no chunk ever had a {@code projectId}, so the filter's project check never
 * fired and project isolation in RAG rested on team ids alone. Resolution is best-effort and
 * never throws: a chunk whose project cannot be determined (reference documents, help guides,
 * org-wide notes) stays project-less and therefore visible to everyone, exactly as before.
 *
 * <p>Runs in its own read-only transaction because callers such as the pending-embedding sweep are
 * not transactional and the entities' project associations are lazy.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KnowledgeProjectResolver {

  private final CycleRepository cycleRepository;
  private final PitchRepository pitchRepository;
  private final EpicRepository epicRepository;
  private final InitiativeRepository initiativeRepository;
  private final ReleaseRepository releaseRepository;
  private final MeetingRepository meetingRepository;
  private final WikiPageRepository wikiPageRepository;
  private final WikiSpaceRepository wikiSpaceRepository;
  private final KnowledgeSourceRepository knowledgeSourceRepository;
  private final UploadedDocumentRepository documentRepository;

  /**
   * @param entityType what the chunk was built from
   * @param entityId id of that entity
   * @param cycleId the item's cycle, used as a fallback
   * @param pitchId the item's pitch, used as a fallback
   * @return the owning project id, or empty when it cannot be determined
   */
  @Transactional(readOnly = true)
  public Optional<Long> resolve(
      KnowledgeEntityType entityType, Long entityId, Long cycleId, Long pitchId) {
    try {
      Optional<Long> direct = entityId == null ? Optional.empty() : fromEntity(entityType, entityId);
      if (direct.isPresent()) {
        return direct;
      }
      if (pitchId != null) {
        Optional<Long> viaPitch = fromPitch(pitchId);
        if (viaPitch.isPresent()) {
          return viaPitch;
        }
      }
      return cycleId == null ? Optional.empty() : fromCycle(cycleId);
    } catch (Exception e) {
      log.debug("Could not resolve project for {} {}: {}", entityType, entityId, e.getMessage());
      return Optional.empty();
    }
  }

  private Optional<Long> fromEntity(KnowledgeEntityType type, Long id) {
    return switch (type) {
      case CYCLE -> fromCycle(id);
      case PITCH -> fromPitch(id);
      case EPIC -> epicRepository.findById(id).map(e -> idOf(e.getProject()));
      case INITIATIVE -> initiativeRepository.findByIdNotDeleted(id).map(i -> idOf(i.getProject()));
      case RELEASE -> releaseRepository.findByIdNotDeleted(id).map(r -> idOf(r.getProject()));
      case MEETING -> meetingRepository.findById(id).map(m -> idOf(m.getProject()));
      case WIKI_PAGE ->
          wikiPageRepository
              .findById(id)
              .flatMap(p -> wikiSpaceRepository.findById(p.getSpaceId()))
              .map(s -> s.getProjectId());
      case KNOWLEDGE_SOURCE -> knowledgeSourceRepository.findById(id).map(s -> s.getProjectId());
      case DOCUMENT ->
          documentRepository
              .findById(id)
              .flatMap(
                  d -> {
                    KnowledgeEntityType owner = ownerType(d.getEntityType());
                    return owner == null || d.getEntityId() == null
                        ? Optional.<Long>empty()
                        : fromEntity(owner, d.getEntityId());
                  });
      default -> Optional.empty();
    };
  }

  private Optional<Long> fromCycle(Long cycleId) {
    return cycleRepository.findById(cycleId).map(c -> idOf(c.getProject()));
  }

  private Optional<Long> fromPitch(Long pitchId) {
    return pitchRepository
        .findByIdNotDeleted(pitchId)
        .map(
            (Function<Pitch, Long>)
                p -> {
                  Long direct = idOf(p.getProject());
                  if (direct != null) {
                    return direct;
                  }
                  return p.getCycle() == null ? null : idOf(p.getCycle().getProject());
                });
  }

  private static Long idOf(Project project) {
    return project == null ? null : project.getId();
  }

  private static KnowledgeEntityType ownerType(String documentEntityType) {
    if (documentEntityType == null) {
      return null;
    }
    return switch (documentEntityType.toUpperCase()) {
      case "PITCH" -> KnowledgeEntityType.PITCH;
      case "MEETING" -> KnowledgeEntityType.MEETING;
      case "CYCLE" -> KnowledgeEntityType.CYCLE;
      case "EPIC" -> KnowledgeEntityType.EPIC;
      case "INITIATIVE" -> KnowledgeEntityType.INITIATIVE;
      default -> null;
    };
  }
}
