package com.github.farzadsedaghatbin.shipflow.service.retro;

import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.dto.RetroDTO;
import com.github.farzadsedaghatbin.shipflow.dto.RetroItemDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.RetroItem;
import com.github.farzadsedaghatbin.shipflow.entity.Retrospective;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.RetroItemRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RetroItemVoteRepository;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Centralized mapper for Retrospective DTOs.
 * Provides batch-aware mapping to avoid N+1 query patterns.
 */
@Component
@RequiredArgsConstructor
public class RetroMapper {

  private final RetroItemRepository retroItemRepository;
  private final RetroItemVoteRepository retroItemVoteRepository;

  // ==================== RETRO DTO MAPPING ====================

  /**
   * Map a single Retrospective to DTO with item count.
   * For single retro mapping when item count is needed.
   */
  public RetroDTO toDTO(Retrospective retro, long itemCount) {
    RetroDTO.RetroDTOBuilder builder = RetroDTO.builder()
        .id(retro.getId())
        .title(retro.getTitle())
        .notes(retro.getNotes())
        .status(retro.getStatus())
        .createdAt(retro.getCreatedAt())
        .updatedAt(retro.getUpdatedAt())
        .closedAt(retro.getClosedAt())
        .itemCount((int) itemCount);

    if (retro.getCycle() != null) {
      builder.cycleId(retro.getCycle().getId())
          .cycleName(retro.getCycle().getName());
    }

    if (retro.getProject() != null) {
      builder.projectId(retro.getProject().getId())
          .projectName(retro.getProject().getName())
          .projectKey(retro.getProject().getProjectKey());
    }

    if (retro.getCreatedBy() != null) {
      builder.createdById(retro.getCreatedBy().getId())
          .createdByName(retro.getCreatedBy().getUsername());
    }

    return builder.build();
  }

  /**
   * Map multiple Retrospectives to DTOs with batch item count lookup.
   * Fixes N+1: uses single query for all item counts.
   */
  public List<RetroDTO> toDTOBatch(List<Retrospective> retros) {
    if (retros.isEmpty()) {
      return Collections.emptyList();
    }

    // Batch fetch item counts for all retros
    List<Long> retroIds = retros.stream()
        .map(Retrospective::getId)
        .collect(Collectors.toList());
    
    Map<Long, Long> itemCounts = retroItemRepository.countByRetrospectiveIds(retroIds);

    return retros.stream()
        .map(retro -> toDTO(retro, itemCounts.getOrDefault(retro.getId(), 0L)))
        .collect(Collectors.toList());
  }

  /**
   * Map a Retrospective with its items to DTO.
   * Requires items to be pre-fetched (e.g., via findByIdWithItems).
   */
  public RetroDTO toDTOWithItems(Retrospective retro, User currentUser) {
    List<RetroItem> items = retro.getItems() != null ? retro.getItems() : Collections.emptyList();
    RetroDTO dto = toDTO(retro, items.size());
    
    List<RetroItemDTO> itemDTOs = toItemDTOBatch(items, currentUser);
    dto.setItems(itemDTOs);
    
    return dto;
  }

  // ==================== RETRO ITEM DTO MAPPING ====================

  /**
   * Map a single RetroItem to DTO.
   * For single item mapping when vote/merge data is provided.
   */
  public RetroItemDTO toItemDTO(RetroItem item, boolean hasVoted, List<Long> mergedItemIds) {
    Boolean isAnonymous = item.getIsAnonymous() != null ? item.getIsAnonymous() : false;

    RetroItemDTO.RetroItemDTOBuilder builder = RetroItemDTO.builder()
        .id(item.getId())
        .content(item.getContent())
        .columnType(item.getColumnType())
        .retrospectiveId(item.getRetrospective().getId())
        .isAnonymous(isAnonymous)
        .voteCount(item.getVoteCount() != null ? item.getVoteCount() : 0)
        .createdAt(item.getCreatedAt())
        .updatedAt(item.getUpdatedAt())
        .hasVoted(hasVoted)
        .mergedItemIds(mergedItemIds != null ? mergedItemIds : Collections.emptyList());

    // Always include author fields (null for anonymous items)
    if (!isAnonymous && item.getAuthor() != null) {
      builder.authorId(item.getAuthor().getId())
          .authorName(item.getAuthor().getUsername());
    } else {
      builder.authorId(null).authorName(null);
    }

    // Merged into
    if (item.getMergedInto() != null) {
      builder.mergedIntoId(item.getMergedInto().getId());
    }

    // Include action tracking fields (v0.5)
    builder.actedOn(item.getActedOn() != null ? item.getActedOn() : false);
    builder.actedOnNotes(item.getActedOnNotes());
    builder.actedOnAt(item.getActedOnAt());
    if (item.getActedOnBy() != null) {
      builder.actedOnById(item.getActedOnBy().getId());
      builder.actedOnByName(item.getActedOnBy().getUsername());
    }

    return builder.build();
  }

  /**
   * Map multiple RetroItems to DTOs with batch vote/merge lookups.
   * Fixes N+1: uses batch queries for votes and merged items.
   */
  public List<RetroItemDTO> toItemDTOBatch(List<RetroItem> items, User currentUser) {
    if (items.isEmpty()) {
      return Collections.emptyList();
    }

    List<Long> itemIds = items.stream()
        .map(RetroItem::getId)
        .collect(Collectors.toList());

    // Batch fetch vote status for current user
    Set<Long> votedItemIds = currentUser != null
        ? retroItemVoteRepository.findVotedItemIds(itemIds, currentUser.getId())
        : Collections.emptySet();

    // Batch fetch merged item mappings
    Map<Long, List<Long>> mergedItemsMap = retroItemRepository.findMergedItemIdsByTargetIds(itemIds);

    return items.stream()
        .map(item -> toItemDTO(
            item,
            votedItemIds.contains(item.getId()),
            mergedItemsMap.getOrDefault(item.getId(), Collections.emptyList())
        ))
        .collect(Collectors.toList());
  }

  /**
   * Map a single RetroItem with lazy lookups (for updates/creates).
   * Use sparingly - prefer batch methods for lists.
   */
  public RetroItemDTO toItemDTOWithLookup(RetroItem item, User currentUser) {
    boolean hasVoted = false;
    if (currentUser != null) {
      hasVoted = retroItemVoteRepository.existsByRetroItemIdAndUserId(item.getId(), currentUser.getId());
    }

    List<Long> mergedItemIds = retroItemRepository.findByMergedIntoId(item.getId())
        .stream()
        .map(RetroItem::getId)
        .collect(Collectors.toList());

    return toItemDTO(item, hasVoted, mergedItemIds);
  }

  // ==================== PITCH DTO MAPPING ====================

  public PitchDTO toPitchDTO(Pitch pitch) {
    return PitchDTO.builder()
        .id(pitch.getId())
        .title(pitch.getTitle())
        .description(pitch.getDescription())
        .problemStatement(pitch.getProblemStatement())
        .solution(pitch.getSolution())
        .rabbitHoles(pitch.getRabbitHoles())
        .risks(pitch.getRisks())
        .noGos(pitch.getNoGos())
        .wireframeLinks(pitch.getWireframeLinks())
        .appetiteDays(pitch.getAppetiteDays())
        .cycleId(pitch.getCycle().getId())
        .cycleName(pitch.getCycle().getName())
        .status(pitch.getStatus())
        .teamId(pitch.getTeam() != null ? pitch.getTeam().getId() : null)
        .teamName(pitch.getTeam() != null ? pitch.getTeam().getName() : null)
        .projectId(pitch.getCycle().getProject() != null ? pitch.getCycle().getProject().getId() : null)
        .projectName(pitch.getCycle().getProject() != null ? pitch.getCycle().getProject().getName() : null)
        .projectKey(pitch.getCycle().getProject() != null ? pitch.getCycle().getProject().getProjectKey() : null)
        .createdAt(pitch.getCreatedAt())
        .updatedAt(pitch.getUpdatedAt())
        .build();
  }
}
