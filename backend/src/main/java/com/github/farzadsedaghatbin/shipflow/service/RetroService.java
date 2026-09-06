package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.*;
import com.github.farzadsedaghatbin.shipflow.service.retro.RetroActionService;
import com.github.farzadsedaghatbin.shipflow.service.retro.RetroConversionService;
import com.github.farzadsedaghatbin.shipflow.service.retro.RetroCrudService;
import com.github.farzadsedaghatbin.shipflow.service.retro.RetroItemService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Facade service for retrospective operations.
 * Delegates to focused services for maintainability.
 *
 * <p>Composed of:
 * <ul>
 *   <li>{@link RetroCrudService} - Retrospective CRUD and lifecycle</li>
 *   <li>{@link RetroItemService} - RetroItem CRUD, voting, and merging</li>
 *   <li>{@link RetroActionService} - Action tracking and cycle status</li>
 *   <li>{@link RetroConversionService} - Retro to pitch conversion</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RetroService {

  private final RetroCrudService retroCrudService;
  private final RetroItemService retroItemService;
  private final RetroActionService retroActionService;
  private final RetroConversionService retroConversionService;

  // ==================== RETRO CRUD ====================

  public List<RetroDTO> getAllRetrosByProject(Long projectId) {
    return retroCrudService.getAllRetrosByProject(projectId);
  }

  public List<RetroDTO> getRetrosByCycle(Long cycleId) {
    return retroCrudService.getRetrosByCycle(cycleId);
  }

  public RetroDTO getRetroById(Long id) {
    return retroCrudService.getRetroById(id);
  }

  public RetroDTO getRetroWithItems(Long id) {
    return retroCrudService.getRetroWithItems(id);
  }

  public RetroDTO createRetro(CreateRetroRequest request) {
    return retroCrudService.createRetro(request);
  }

  public RetroDTO updateRetro(Long id, UpdateRetroRequest request) {
    return retroCrudService.updateRetro(id, request);
  }

  public RetroDTO openRetro(Long id) {
    return retroCrudService.openRetro(id);
  }

  public RetroDTO closeRetro(Long id) {
    return retroCrudService.closeRetro(id);
  }

  public void deleteRetro(Long id) {
    retroCrudService.deleteRetro(id);
  }

  // ==================== RETRO ITEMS ====================

  public List<RetroItemDTO> getItemsByRetro(Long retroId) {
    return retroItemService.getItemsByRetro(retroId);
  }

  public RetroItemDTO createRetroItem(CreateRetroItemRequest request) {
    return retroItemService.createRetroItem(request);
  }

  public RetroItemDTO updateRetroItem(Long itemId, String content) {
    return retroItemService.updateRetroItem(itemId, content);
  }

  public RetroItemDTO updateRetroItem(Long itemId, String content, Long expectedVersion) {
    return retroItemService.updateRetroItem(itemId, content, expectedVersion);
  }

  public void deleteRetroItem(Long itemId) {
    retroItemService.deleteRetroItem(itemId);
  }

  // ==================== VOTING ====================

  public RetroItemDTO toggleVote(Long itemId) {
    return retroItemService.toggleVote(itemId);
  }

  public RetroItemDTO toggleDislike(Long itemId) {
    return retroItemService.toggleDislike(itemId);
  }

  public RetroItemDTO markDiscussed(Long itemId, boolean discussed) {
    return retroItemService.markDiscussed(itemId, discussed);
  }

  // ==================== MERGING ====================

  public RetroItemDTO mergeItems(Long targetItemId, Long sourceItemId) {
    return retroItemService.mergeItems(targetItemId, sourceItemId);
  }

  public RetroItemDTO unmergeItem(Long itemId) {
    return retroItemService.unmergeItem(itemId);
  }

  // ==================== CYCLE RETRO STATUS ====================

  public CycleRetroStatusDTO getCycleRetroStatus(Long cycleId) {
    return retroActionService.getCycleRetroStatus(cycleId);
  }

  public boolean canCloseCycle(Long cycleId) {
    return retroActionService.canCloseCycle(cycleId);
  }

  // ==================== PROJECT SETTING ====================

  public boolean isRetrospectivesEnabled(Long projectId) {
    return retroCrudService.isRetrospectivesEnabled(projectId);
  }

  public void setRetrospectivesEnabled(Long projectId, boolean enabled) {
    retroCrudService.setRetrospectivesEnabled(projectId, enabled);
  }

  // ==================== ACTION TRACKING (v0.5) ====================

  /**
   * Mark a retro item as acted upon (or not).
   * Part of v0.5 "Insight, Not Metrics" - tracks whether teams follow through on retro decisions.
   */
  public RetroItemDTO markActedOn(Long itemId, Boolean actedOn, String notes) {
    return retroActionService.markActedOn(itemId, actedOn, notes);
  }

  /**
   * Get action tracking statistics for a retrospective.
   */
  public RetroActionStatsDTO getActionStats(Long retroId) {
    return retroActionService.getActionStats(retroId);
  }

  /**
   * Get pending action items across all retros in a project.
   */
  public List<RetroItemDTO> getPendingActionItems(Long projectId) {
    return retroActionService.getPendingActionItems(projectId);
  }

  // ==================== RETRO → PITCH CONVERSION (v0.5) ====================

  /**
   * Convert retrospective action/improvement items into a pitch draft.
   * This enables closed retro insights to feed into the next cycle's shaping phase.
   *
   * @param request the conversion request with retro ID and options
   * @return the created draft pitch
   */
  public PitchDTO convertToPitchDraft(ConvertRetroToPitchRequest request) {
    return retroConversionService.convertToPitchDraft(request);
  }
}
