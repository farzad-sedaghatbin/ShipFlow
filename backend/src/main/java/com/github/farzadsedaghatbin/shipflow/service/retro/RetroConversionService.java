package com.github.farzadsedaghatbin.shipflow.service.retro;

import com.github.farzadsedaghatbin.shipflow.dto.ConvertRetroToPitchRequest;
import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.RetroItem;
import com.github.farzadsedaghatbin.shipflow.entity.Retrospective;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroColumnType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RetroItemRepository;
import com.github.farzadsedaghatbin.shipflow.service.LocalizationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for converting retrospective items into pitch drafts.
 * Includes hardened invariant validation.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class RetroConversionService {

  private final RetroItemRepository retroItemRepository;
  private final CycleRepository cycleRepository;
  private final PitchRepository pitchRepository;
  private final LocalizationService localizationService;
  private final RetroCrudService retroCrudService;
  private final RetroMapper retroMapper;

  /**
   * Columns that are valid for conversion to pitch drafts.
   * Only ACTIONS and TRY_NEXT items are actionable.
   */
  private static final Set<RetroColumnType> ACTIONABLE_COLUMNS = EnumSet.of(
      RetroColumnType.ACTIONS,
      RetroColumnType.TRY_NEXT
  );

  /**
   * Convert retrospective action/improvement items into a pitch draft.
   * This enables closed retro insights to feed into the next cycle's shaping phase.
   * 
   * <p>Hardened invariants:
   * <ul>
   *   <li>Retro must be closed</li>
   *   <li>Selected item IDs must belong to actionable columns (ACTIONS, TRY_NEXT)</li>
   *   <li>Target cycle must belong to the same project</li>
   *   <li>Current user is fetched once, not per-item</li>
   * </ul>
   *
   * @param request the conversion request with retro ID and options
   * @return the created draft pitch
   */
  public PitchDTO convertToPitchDraft(ConvertRetroToPitchRequest request) {
    Retrospective retro = retroCrudService.getRetroEntityWithItems(request.getRetrospectiveId());

    // Invariant: Retro must be closed
    if (retro.getStatus() != RetroStatus.CLOSED) {
      throw new IllegalStateException(
          localizationService.getMessageWithDefault(
              "retro.convert.must.be.closed",
              "Retrospective must be closed before converting to pitch draft"));
    }

    // Get items to convert
    List<RetroItem> itemsToConvert = getItemsToConvert(retro, request.getRetroItemIds());

    if (itemsToConvert.isEmpty()) {
      throw new IllegalStateException(
          localizationService.getMessageWithDefault(
              "retro.convert.no.items",
              "No actionable items found to convert"));
    }

    // Determine effective project for cycle resolution and validation
    Long effectiveProjectId = request.getTargetProjectId() != null
        ? request.getTargetProjectId()
        : retro.getProject().getId();

    // Determine target cycle
    Cycle targetCycle = resolveTargetCycle(request.getTargetCycleId(), retro, effectiveProjectId);

    // Invariant: Target cycle must belong to the effective project
    validateTargetCycleBelongsToProject(targetCycle, effectiveProjectId);

    // Fetch current user once (not inside loop)
    User currentUser = retroCrudService.getCurrentUser();

    // Generate pitch title
    String pitchTitle = request.getCustomTitle() != null
        ? request.getCustomTitle()
        : "Improvements from: " + retro.getTitle();

    // Build problem statement from retro items
    String problemStatement = buildProblemStatement(retro, itemsToConvert, request.getAdditionalNotes());

    // Create the pitch as a PENDING draft
    Pitch pitch = Pitch.builder()
        .title(pitchTitle)
        .description("Auto-generated from retrospective: " + retro.getTitle())
        .problemStatement(problemStatement)
        .appetiteDays(request.getAppetiteDays() != null ? request.getAppetiteDays() : 1)
        .cycle(targetCycle)
        .status(PitchStatus.PENDING)
        .build();

    Pitch saved = pitchRepository.save(pitch);

    // Mark retro items as acted on (batch update with pre-fetched user)
    markItemsAsActedOn(itemsToConvert, saved.getTitle(), currentUser);

    return retroMapper.toPitchDTO(saved);
  }

  /**
   * Gets items to convert, validating they belong to actionable columns.
   */
  private List<RetroItem> getItemsToConvert(Retrospective retro, List<Long> requestedItemIds) {
    List<RetroItem> allItems = retro.getItems();
    
    if (requestedItemIds != null && !requestedItemIds.isEmpty()) {
      // Use specific items if specified - validate they're actionable
      Set<Long> requestedIdSet = Set.copyOf(requestedItemIds);
      
      List<RetroItem> filtered = allItems.stream()
          .filter(item -> requestedIdSet.contains(item.getId()))
          .filter(item -> item.getMergedInto() == null) // Exclude merged items
          .collect(Collectors.toList());

      // Invariant: All selected items must belong to actionable columns
      List<RetroItem> nonActionable = filtered.stream()
          .filter(item -> !ACTIONABLE_COLUMNS.contains(item.getColumnType()))
          .collect(Collectors.toList());

      if (!nonActionable.isEmpty()) {
        String invalidIds = nonActionable.stream()
            .map(item -> String.valueOf(item.getId()))
            .collect(Collectors.joining(", "));
        throw new IllegalStateException(
            localizationService.getMessageWithDefault(
                "retro.convert.invalid.columns",
                "The following items are not from actionable columns (ACTIONS, TRY_NEXT): " + invalidIds));
      }

      return filtered;
    } else {
      // Default: get ACTIONS and TRY_NEXT items
      return allItems.stream()
          .filter(item -> ACTIONABLE_COLUMNS.contains(item.getColumnType()))
          .filter(item -> item.getMergedInto() == null)
          .collect(Collectors.toList());
    }
  }

  /**
   * Resolves the target cycle for the pitch, searching within the effective project.
   */
  private Cycle resolveTargetCycle(Long targetCycleId, Retrospective retro, Long effectiveProjectId) {
    if (targetCycleId != null) {
      return cycleRepository.findById(targetCycleId)
          .orElseThrow(() -> new ResourceNotFoundException(
              "Target cycle not found with id: " + targetCycleId));
    } else {
      // Find next upcoming cycle in the effective project
      return cycleRepository.findFirstByProjectIdAndStartDateAfterOrderByStartDateAsc(
          effectiveProjectId, LocalDate.now())
          .orElseThrow(() -> new ResourceNotFoundException(
              localizationService.getMessageWithDefault(
                  "retro.convert.no.target.cycle",
                  "No upcoming cycle found for pitch draft")));
    }
  }

  /**
   * Validates that the target cycle belongs to the effective project.
   */
  private void validateTargetCycleBelongsToProject(Cycle targetCycle, Long effectiveProjectId) {
    Long cycleProjectId = targetCycle.getProject() != null ? targetCycle.getProject().getId() : null;

    if (cycleProjectId == null) {
      throw new IllegalStateException(
          localizationService.getMessageWithDefault(
              "retro.convert.missing.project",
              "Target cycle is not associated with a project"));
    }

    if (!effectiveProjectId.equals(cycleProjectId)) {
      throw new IllegalStateException(
          localizationService.getMessageWithDefault(
              "retro.convert.different.project",
              "Target cycle must belong to the selected project"));
    }
  }

  /**
   * Builds the problem statement from retro items.
   */
  private String buildProblemStatement(Retrospective retro, List<RetroItem> items, String additionalNotes) {
    StringBuilder problemStatement = new StringBuilder();
    problemStatement.append("Based on retrospective insights from cycle: ")
        .append(retro.getCycle().getName()).append("\n\n");

    for (RetroItem item : items) {
      String columnLabel = item.getColumnType() == RetroColumnType.ACTIONS ? "[Action]" : "[Try Next]";
      problemStatement.append(columnLabel).append(" ")
          .append(item.getContent())
          .append(" (").append(item.getVoteCount() != null ? item.getVoteCount() : 0).append(" votes)\n");
    }

    // Add additional notes if provided
    if (additionalNotes != null && !additionalNotes.isBlank()) {
      problemStatement.append("\n--- Additional Context ---\n")
          .append(additionalNotes);
    }

    return problemStatement.toString();
  }

  /**
   * Marks all items as acted upon in batch.
   * Uses pre-fetched user to avoid N+1.
   */
  private void markItemsAsActedOn(List<RetroItem> items, String pitchTitle, User currentUser) {
    LocalDateTime now = LocalDateTime.now();
    for (RetroItem item : items) {
      item.setActedOn(true);
      item.setActedOnNotes("Converted to pitch draft: " + pitchTitle);
      item.setActedOnAt(now);
      item.setActedOnBy(currentUser);
    }
    retroItemRepository.saveAll(items);
  }
}
