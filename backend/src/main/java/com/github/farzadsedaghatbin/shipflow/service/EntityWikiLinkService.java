package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.wikilink.LinkedWikiPageDTO;
import com.github.farzadsedaghatbin.shipflow.entity.EntityWikiLink;
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
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Links a {@link WikiPage} to a Pitch or Task for reference (research/docs), separate from the
 * file-attachment system. Bug reports are explicitly out of scope.
 */
@Service
@RequiredArgsConstructor
public class EntityWikiLinkService {

  private static final Set<String> SUPPORTED_ENTITY_TYPES = Set.of("PITCH", "TASK");

  private final EntityWikiLinkRepository linkRepository;
  private final WikiPageRepository wikiPageRepository;
  private final WikiSpaceRepository wikiSpaceRepository;
  private final PitchRepository pitchRepository;
  private final TaskRepository taskRepository;
  private final UserRepository userRepository;

  @Transactional(readOnly = true)
  public List<LinkedWikiPageDTO> getLinkedWikiPages(String entityType, Long entityId) {
    String normalizedType = normalizeEntityType(entityType);
    return linkRepository.findByEntityTypeAndEntityId(normalizedType, entityId).stream()
        .map(this::toDTO)
        .toList();
  }

  @Transactional
  public LinkedWikiPageDTO linkWikiPage(String entityType, Long entityId, Long wikiPageId) {
    String normalizedType = normalizeEntityType(entityType);
    validateEntityExists(normalizedType, entityId);

    WikiPage wikiPage = wikiPageRepository
        .findById(wikiPageId)
        .filter(p -> p.getDeletedAt() == null)
        .orElseThrow(() -> new ResourceNotFoundException("Wiki page not found: " + wikiPageId));

    if (linkRepository.existsByEntityTypeAndEntityIdAndWikiPageId(
        normalizedType, entityId, wikiPageId)) {
      throw new IllegalStateException("Wiki page " + wikiPageId + " is already linked to "
          + normalizedType + " " + entityId);
    }

    EntityWikiLink link = EntityWikiLink.builder()
        .entityType(normalizedType)
        .entityId(entityId)
        .wikiPage(wikiPage)
        .linkedBy(getCurrentUserOrNull())
        .build();

    return toDTO(linkRepository.save(link));
  }

  @Transactional
  public void unlinkWikiPage(String entityType, Long entityId, Long wikiPageId) {
    String normalizedType = normalizeEntityType(entityType);
    EntityWikiLink link = linkRepository
        .findByEntityTypeAndEntityIdAndWikiPageId(normalizedType, entityId, wikiPageId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Wiki link not found for " + normalizedType + " " + entityId + " -> page " + wikiPageId));
    linkRepository.delete(link);
  }

  private void validateEntityExists(String entityType, Long entityId) {
    boolean exists = switch (entityType) {
      case "PITCH" -> pitchRepository.findByIdNotDeleted(entityId).isPresent();
      case "TASK" -> taskRepository.findByIdNotDeleted(entityId).isPresent();
      default -> false;
    };
    if (!exists) {
      throw new ResourceNotFoundException(entityType + " not found: " + entityId);
    }
  }

  private String normalizeEntityType(String entityType) {
    String normalized = entityType == null ? "" : entityType.trim().toUpperCase();
    if (!SUPPORTED_ENTITY_TYPES.contains(normalized)) {
      throw new IllegalArgumentException(
          "Unsupported entity type for wiki linking: " + entityType
              + " (only PITCH and TASK are supported)");
    }
    return normalized;
  }

  private User getCurrentUserOrNull() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) {
      return null;
    }
    return userRepository.findByUsername(auth.getName()).orElse(null);
  }

  private LinkedWikiPageDTO toDTO(EntityWikiLink link) {
    WikiPage page = link.getWikiPage();
    WikiSpace space = wikiSpaceRepository.findById(page.getSpaceId()).orElse(null);
    User linkedBy = link.getLinkedBy();
    return new LinkedWikiPageDTO(
        link.getId(),
        page.getId(),
        page.getTitle(),
        page.getSlug(),
        page.getSpaceId(),
        space != null ? space.getName() : null,
        link.getLinkedAt(),
        linkedBy != null ? linkedBy.getUsername() : null);
  }
}
