package com.github.farzadsedaghatbin.shipflow.service.retro;

import com.github.farzadsedaghatbin.shipflow.dto.CreateRetroItemRequest;
import com.github.farzadsedaghatbin.shipflow.dto.RetroItemDTO;
import com.github.farzadsedaghatbin.shipflow.entity.RetroItem;
import com.github.farzadsedaghatbin.shipflow.entity.RetroItemVote;
import com.github.farzadsedaghatbin.shipflow.entity.Retrospective;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.RetroItemRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RetroItemVoteRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RetroRepository;
import com.github.farzadsedaghatbin.shipflow.service.LocalizationService;
import com.github.farzadsedaghatbin.shipflow.service.MessageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for RetroItem CRUD operations, voting, and merging.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RetroItemService {

  private final RetroRepository retroRepository;
  private final RetroItemRepository retroItemRepository;
  private final RetroItemVoteRepository retroItemVoteRepository;
  private final LocalizationService localizationService;
  private final MessageService messageService;
  private final RetroMapper retroMapper;
  private final RetroCrudService retroCrudService;

  // ==================== ITEM CRUD ====================

  public List<RetroItemDTO> getItemsByRetro(Long retroId) {
    Retrospective retro = retroRepository.findById(retroId)
        .orElseThrow(() -> new ResourceNotFoundException("Retrospective not found with id: " + retroId));

    if (retro.getProject() != null) {
      retroCrudService.validateRetrospectivesEnabled(retro.getProject().getId());
    }

    List<RetroItem> items = retroItemRepository.findByRetrospectiveIdOrderByCreatedAtAsc(retroId);
    return retroMapper.toItemDTOBatch(items, retroCrudService.getCurrentUser());
  }

  public RetroItemDTO createRetroItem(CreateRetroItemRequest request) {
    Retrospective retro = retroRepository.findById(request.getRetrospectiveId())
        .orElseThrow(() -> new ResourceNotFoundException(
            "Retrospective not found with id: " + request.getRetrospectiveId()));

    if (retro.getProject() != null) {
      retroCrudService.validateRetrospectivesEnabled(retro.getProject().getId());
    }

    if (retro.getStatus() == RetroStatus.CLOSED) {
      throw new IllegalStateException(localizationService.getMessage("retro.cannot.add.items.closed"));
    }

    User currentUser = retroCrudService.getCurrentUser();
    Boolean isAnonymous = request.getIsAnonymous() != null ? request.getIsAnonymous() : false;

    RetroItem item = RetroItem.builder()
        .content(request.getContent())
        .columnType(request.getColumnType())
        .retrospective(retro)
        .author(isAnonymous ? null : currentUser)
        .isAnonymous(isAnonymous)
        .build();

    RetroItem saved = retroItemRepository.save(item);
    return retroMapper.toItemDTOWithLookup(saved, currentUser);
  }

  public RetroItemDTO updateRetroItem(Long itemId, String content) {
    RetroItem item = retroItemRepository.findById(itemId)
        .orElseThrow(() -> new ResourceNotFoundException("Retro item not found with id: " + itemId));

    Retrospective retro = item.getRetrospective();
    if (retro.getProject() != null) {
      retroCrudService.validateRetrospectivesEnabled(retro.getProject().getId());
    }

    if (retro.getStatus() == RetroStatus.CLOSED) {
      throw new IllegalStateException(localizationService.getMessage("retro.cannot.update.items.closed"));
    }

    item.setContent(content);
    RetroItem saved = retroItemRepository.save(item);
    return retroMapper.toItemDTOWithLookup(saved, retroCrudService.getCurrentUser());
  }

  public void deleteRetroItem(Long itemId) {
    RetroItem item = retroItemRepository.findById(itemId)
        .orElseThrow(() -> new ResourceNotFoundException("Retro item not found with id: " + itemId));

    Retrospective retro = item.getRetrospective();
    if (retro.getProject() != null) {
      retroCrudService.validateRetrospectivesEnabled(retro.getProject().getId());
    }

    if (retro.getStatus() == RetroStatus.CLOSED) {
      throw new IllegalStateException(localizationService.getMessage("retro.cannot.delete.items.closed"));
    }

    retroItemRepository.deleteById(itemId);
  }

  // ==================== VOTING ====================

  public RetroItemDTO toggleVote(Long itemId) {
    RetroItem item = retroItemRepository.findById(itemId)
        .orElseThrow(() -> new ResourceNotFoundException("Retro item not found with id: " + itemId));

    Retrospective retro = item.getRetrospective();
    if (retro.getProject() != null) {
      retroCrudService.validateRetrospectivesEnabled(retro.getProject().getId());
    }

    if (retro.getStatus() == RetroStatus.CLOSED) {
      throw new IllegalStateException(localizationService.getMessage("retro.cannot.vote.closed"));
    }

    // Cannot vote on merged items
    if (item.getMergedInto() != null) {
      throw new IllegalStateException(localizationService.getMessage("retro.cannot.vote.merged"));
    }

    User currentUser = retroCrudService.getCurrentUser();
    if (currentUser == null) {
      throw new IllegalStateException(localizationService.getMessage("retro.user.not.found"));
    }

    boolean hasVoted = retroItemVoteRepository.existsByRetroItemIdAndUserId(itemId, currentUser.getId());

    if (hasVoted) {
      // Remove vote
      retroItemVoteRepository.deleteByRetroItemIdAndUserId(itemId, currentUser.getId());
      item.setVoteCount(Math.max(0, item.getVoteCount() - 1));
    } else {
      // Add vote
      RetroItemVote vote = RetroItemVote.builder()
          .retroItem(item)
          .user(currentUser)
          .build();
      retroItemVoteRepository.save(vote);
      item.setVoteCount(item.getVoteCount() + 1);
    }

    RetroItem saved = retroItemRepository.save(item);
    return retroMapper.toItemDTOWithLookup(saved, currentUser);
  }

  // ==================== MERGING ====================

  public RetroItemDTO mergeItems(Long targetItemId, Long sourceItemId) {
    RetroItem targetItem = retroItemRepository.findById(targetItemId)
        .orElseThrow(() -> new ResourceNotFoundException("Target item not found with id: " + targetItemId));

    RetroItem sourceItem = retroItemRepository.findById(sourceItemId)
        .orElseThrow(() -> new ResourceNotFoundException("Source item not found with id: " + sourceItemId));

    // Validate same retrospective
    if (!targetItem.getRetrospective().getId().equals(sourceItem.getRetrospective().getId())) {
      throw new IllegalStateException(messageService.getMessage("error.retro.merge.different.retros"));
    }

    // Validate same column
    if (targetItem.getColumnType() != sourceItem.getColumnType()) {
      throw new IllegalStateException(messageService.getMessage("error.retro.merge.different.columns"));
    }

    Retrospective retro = targetItem.getRetrospective();
    if (retro.getProject() != null) {
      retroCrudService.validateRetrospectivesEnabled(retro.getProject().getId());
    }

    if (retro.getStatus() == RetroStatus.CLOSED) {
      throw new IllegalStateException(messageService.getMessage("error.retro.merge.closed"));
    }

    // Cannot merge into an already merged item
    if (targetItem.getMergedInto() != null) {
      throw new IllegalStateException(messageService.getMessage("error.retro.merge.into.merged"));
    }

    // Cannot merge an item that is already merged
    if (sourceItem.getMergedInto() != null) {
      throw new IllegalStateException(messageService.getMessage("error.retro.merge.already.merged"));
    }

    // Transfer votes from source to target
    List<RetroItemVote> sourceVotes = retroItemVoteRepository.findByRetroItemId(sourceItemId);
    for (RetroItemVote vote : sourceVotes) {
      // Only add if user hasn't already voted on target
      if (!retroItemVoteRepository.existsByRetroItemIdAndUserId(targetItemId, vote.getUser().getId())) {
        RetroItemVote newVote = RetroItemVote.builder()
            .retroItem(targetItem)
            .user(vote.getUser())
            .build();
        retroItemVoteRepository.save(newVote);
        targetItem.setVoteCount(targetItem.getVoteCount() + 1);
      }
    }

    // Mark source as merged
    sourceItem.setMergedInto(targetItem);
    sourceItem.setVoteCount(0); // Reset vote count on merged item
    retroItemRepository.save(sourceItem);

    // Save target with updated vote count
    RetroItem saved = retroItemRepository.save(targetItem);
    return retroMapper.toItemDTOWithLookup(saved, retroCrudService.getCurrentUser());
  }

  public RetroItemDTO unmergeItem(Long itemId) {
    RetroItem item = retroItemRepository.findById(itemId)
        .orElseThrow(() -> new ResourceNotFoundException("Retro item not found with id: " + itemId));

    if (item.getMergedInto() == null) {
      throw new IllegalStateException(messageService.getMessage("error.retro.item.not.merged"));
    }

    Retrospective retro = item.getRetrospective();
    if (retro.getProject() != null) {
      retroCrudService.validateRetrospectivesEnabled(retro.getProject().getId());
    }

    if (retro.getStatus() == RetroStatus.CLOSED) {
      throw new IllegalStateException(messageService.getMessage("error.retro.unmerge.closed"));
    }

    item.setMergedInto(null);
    RetroItem saved = retroItemRepository.save(item);
    return retroMapper.toItemDTOWithLookup(saved, retroCrudService.getCurrentUser());
  }

  /**
   * Get item entity by ID (for internal use by other services).
   */
  public RetroItem getItemEntity(Long itemId) {
    return retroItemRepository.findById(itemId)
        .orElseThrow(() -> new ResourceNotFoundException("Retro item not found with id: " + itemId));
  }
}
