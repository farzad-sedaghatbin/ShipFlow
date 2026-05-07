package com.github.farzadsedaghatbin.shipflow.service.retro;

import com.github.farzadsedaghatbin.shipflow.dto.CycleRetroStatusDTO;
import com.github.farzadsedaghatbin.shipflow.dto.RetroActionStatsDTO;
import com.github.farzadsedaghatbin.shipflow.dto.RetroItemDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.RetroItem;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroColumnType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RetroItemRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RetroRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for retrospective action tracking and cycle status.
 * Handles marking items as acted upon and tracking follow-through metrics.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RetroActionService {

  private final RetroRepository retroRepository;
  private final RetroItemRepository retroItemRepository;
  private final CycleRepository cycleRepository;
  private final RetroCrudService retroCrudService;
  private final RetroMapper retroMapper;

  // ==================== ACTION TRACKING ====================

  /**
   * Mark a retro item as acted upon (or not).
   * Part of v0.5 "Insight, Not Metrics" - tracks whether teams follow through on retro decisions.
   */
  public RetroItemDTO markActedOn(Long itemId, Boolean actedOn, String notes) {
    RetroItem item = retroItemRepository.findById(itemId)
        .orElseThrow(() -> new ResourceNotFoundException("Retro item not found with id: " + itemId));

    User currentUser = retroCrudService.getCurrentUser();

    item.setActedOn(actedOn);
    if (Boolean.TRUE.equals(actedOn)) {
      item.setActedOnNotes(notes);
      item.setActedOnAt(LocalDateTime.now());
      item.setActedOnBy(currentUser);
    } else {
      item.setActedOnNotes(null);
      item.setActedOnAt(null);
      item.setActedOnBy(null);
    }

    RetroItem saved = retroItemRepository.save(item);
    return retroMapper.toItemDTOWithLookup(saved, currentUser);
  }

  /**
   * Get action tracking statistics for a retrospective.
   */
  public RetroActionStatsDTO getActionStats(Long retroId) {
    if (!retroRepository.existsById(retroId)) {
      throw new ResourceNotFoundException("Retrospective not found with id: " + retroId);
    }

    // Count action items (things that should be acted on)
    List<RetroItem> actionItems = retroItemRepository.findByRetrospectiveIdAndColumnType(
        retroId, RetroColumnType.ACTIONS);

    long totalActions = actionItems.size();
    long actedOnCount = retroItemRepository.countActedOnByRetrospectiveId(retroId);

    double followThroughRate = totalActions > 0 ? (double) actedOnCount / totalActions * 100 : 0;

    return RetroActionStatsDTO.builder()
        .retrospectiveId(retroId)
        .totalActionItems(totalActions)
        .actedOnCount(actedOnCount)
        .pendingCount(totalActions - actedOnCount)
        .followThroughRate(Math.round(followThroughRate * 10) / 10.0)
        .build();
  }

  /**
   * Get pending action items across all retros in a project.
   */
  public List<RetroItemDTO> getPendingActionItems(Long projectId) {
    retroCrudService.validateRetrospectivesEnabled(projectId);
    List<RetroItem> items = retroItemRepository.findUnactedActionItemsByProjectId(projectId);
    return retroMapper.toItemDTOBatch(items, retroCrudService.getCurrentUser());
  }

  // ==================== CYCLE RETRO STATUS ====================

  public CycleRetroStatusDTO getCycleRetroStatus(Long cycleId) {
    Cycle cycle = cycleRepository.findByIdWithProject(cycleId)
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + cycleId));

    boolean retroEnabled = cycle.getProject() != null
        && Boolean.TRUE.equals(cycle.getProject().getEnableRetrospectives());

    long totalRetros = retroRepository.countByCycleId(cycleId);
    long closedRetros = retroRepository.countByCycleIdAndStatus(cycleId, RetroStatus.CLOSED);

    boolean canClose;
    String message;

    if (!retroEnabled) {
      // If retrospectives are disabled, cycle can be closed without retro requirement
      canClose = true;
      message = "Retrospectives are disabled for this project";
    } else if (closedRetros >= 1) {
      canClose = true;
      message = "Cycle can be closed - retrospective completed";
    } else {
      canClose = false;
      message = "You can't close this cycle yet — create and close at least one Retro for this cycle.";
    }

    return CycleRetroStatusDTO.builder()
        .cycleId(cycleId)
        .cycleName(cycle.getName())
        .totalRetros((int) totalRetros)
        .closedRetros((int) closedRetros)
        .canCloseCycle(canClose)
        .message(message)
        .build();
  }

  public boolean canCloseCycle(Long cycleId) {
    CycleRetroStatusDTO status = getCycleRetroStatus(cycleId);
    return status.getCanCloseCycle();
  }
}
