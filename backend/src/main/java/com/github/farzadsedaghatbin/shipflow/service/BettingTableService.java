package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.*;
import com.github.farzadsedaghatbin.shipflow.dto.betting.*;
import com.github.farzadsedaghatbin.shipflow.entity.BettingSlot;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BusinessValue;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ComplexityLevel;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.Urgency;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BettingTableService {

    private final BettingSlotRepository bettingSlotRepository;
    private final CycleRepository cycleRepository;
    private final TeamRepository teamRepository;
    private final PitchRepository pitchRepository;
    private final WorkLogRepository workLogRepository;

    private static final double HOURS_PER_DAY = 8.0;

    /**
     * Get the full betting table view for a cycle
     */
    public BettingTableDTO getBettingTable(Long cycleId) {
        Cycle cycle = cycleRepository.findByIdWithProject(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found with id: " + cycleId));

        // Get shaped pitches not yet assigned to any slot
        List<Pitch> shapedPitches = pitchRepository.findByCycleIdAndStatus(cycleId, PitchStatus.SHAPED);
        List<BettingSlot> allSlots = bettingSlotRepository.findByCycleId(cycleId);
        
        // Filter out pitches that are already assigned to a slot
        Set<Long> assignedPitchIds = allSlots.stream()
                .filter(slot -> slot.getPitch() != null)
                .map(slot -> slot.getPitch().getId())
                .collect(Collectors.toSet());
        
        List<PitchDTO> availablePitches = shapedPitches.stream()
                .filter(pitch -> !assignedPitchIds.contains(pitch.getId()))
                .map(this::toPitchDTO)
                .collect(Collectors.toList());

        // Get all teams in the cycle
        List<Team> teams = teamRepository.findByCycleId(cycleId);
        
        // Group slots by team
        Map<Long, List<BettingSlot>> slotsByTeam = allSlots.stream()
                .collect(Collectors.groupingBy(slot -> slot.getTeam().getId()));

        // Calculate cycle duration
        long cycleDays = ChronoUnit.DAYS.between(cycle.getStartDate(), cycle.getEndDate());
        int cycleDurationWeeks = (int) Math.ceil(cycleDays / 7.0);

        // Build team tracks
        List<BettingTableDTO.TeamTrackDTO> teamTracks = teams.stream()
                .map(team -> buildTeamTrack(team, slotsByTeam.getOrDefault(team.getId(), new ArrayList<>()), cycleDurationWeeks))
                .collect(Collectors.toList());

        // Calculate totals
        int totalCapacity = teamTracks.stream().mapToInt(BettingTableDTO.TeamTrackDTO::getTotalCapacityWeeks).sum();
        int usedCapacity = teamTracks.stream().mapToInt(BettingTableDTO.TeamTrackDTO::getUsedCapacityWeeks).sum();

        return BettingTableDTO.builder()
                .cycleId(cycle.getId())
                .cycleName(cycle.getName())
                .projectId(cycle.getProject() != null ? cycle.getProject().getId() : null)
                .projectName(cycle.getProject() != null ? cycle.getProject().getName() : null)
                .projectKey(cycle.getProject() != null ? cycle.getProject().getProjectKey() : null)
                .cycleStartDate(cycle.getStartDate())
                .cycleEndDate(cycle.getEndDate())
                .cycleDurationWeeks(cycleDurationWeeks)
                .isCycleActive(cycle.getIsActive())
                .shapedPitches(availablePitches)
                .teamTracks(teamTracks)
                .totalShapedPitches(shapedPitches.size())
                .totalAssignedPitches(assignedPitchIds.size())
                .totalCapacityWeeks(totalCapacity)
                .usedCapacityWeeks(usedCapacity)
                .build();
    }

    /**
     * Get all shaped pitches available for betting (across all cycles or specific project)
     */
    public List<PitchDTO> getShapedPitchesForBetting(Long projectId) {
        List<Pitch> pitches;
        if (projectId != null) {
            // Get cycles for project, then get shaped pitches from those cycles
            List<Cycle> cycles = cycleRepository.findByProjectIdOrderByStartDateDesc(projectId);
            Set<Long> cycleIds = cycles.stream().map(Cycle::getId).collect(Collectors.toSet());
            pitches = pitchRepository.findAll().stream()
                    .filter(p -> p.getStatus() == PitchStatus.SHAPED && cycleIds.contains(p.getCycle().getId()))
                    .collect(Collectors.toList());
        } else {
            pitches = pitchRepository.findAll().stream()
                    .filter(p -> p.getStatus() == PitchStatus.SHAPED)
                    .collect(Collectors.toList());
        }
        
        // Filter out already assigned pitches
        Set<Long> assignedIds = bettingSlotRepository.findAll().stream()
                .filter(slot -> slot.getPitch() != null)
                .map(slot -> slot.getPitch().getId())
                .collect(Collectors.toSet());
        
        return pitches.stream()
                .filter(p -> !assignedIds.contains(p.getId()))
                .map(this::toPitchDTO)
                .collect(Collectors.toList());
    }

    /**
     * Create a new betting slot
     */
    public BettingSlotDTO createSlot(CreateBettingSlotRequest request) {
        Cycle cycle = cycleRepository.findById(request.getCycleId())
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found with id: " + request.getCycleId()));
        
        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new IllegalArgumentException("Team not found with id: " + request.getTeamId()));

        // Validate slot position is unique for this team in this cycle
        bettingSlotRepository.findByCycleIdAndTeamIdAndPosition(
                request.getCycleId(), request.getTeamId(), request.getPosition())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Slot position " + request.getPosition() + 
                            " already exists for team " + team.getName() + " in this cycle");
                });

        BettingSlot slot = BettingSlot.builder()
                .cycle(cycle)
                .team(team)
                .position(request.getPosition())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .notes(request.getNotes())
                .build();

        if (request.getPitchId() != null) {
            Pitch pitch = pitchRepository.findById(request.getPitchId())
                    .orElseThrow(() -> new IllegalArgumentException("Pitch not found with id: " + request.getPitchId()));
            validatePitchFits(pitch, slot);
            slot.setPitch(pitch);
        }

        BettingSlot saved = bettingSlotRepository.save(slot);
        return toDTO(saved);
    }

    /**
     * Auto-generate slots for all teams in a cycle based on cycle duration
     */
    public List<BettingSlotDTO> generateSlotsForCycle(Long cycleId) {
        Cycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found with id: " + cycleId));
        
        List<Team> teams = teamRepository.findByCycleId(cycleId);
        if (teams.isEmpty()) {
            throw new IllegalArgumentException("No teams found for cycle. Create teams first.");
        }

        List<BettingSlot> newSlots = new ArrayList<>();
        
        for (Team team : teams) {
            // Check if slots already exist
            List<BettingSlot> existingSlots = bettingSlotRepository.findByCycleIdAndTeamId(cycleId, team.getId());
            if (!existingSlots.isEmpty()) {
                continue; // Skip teams that already have slots
            }

            // Create one slot spanning the entire cycle by default
            BettingSlot slot = BettingSlot.builder()
                    .cycle(cycle)
                    .team(team)
                    .position(0)
                    .startDate(cycle.getStartDate())
                    .endDate(cycle.getEndDate())
                    .build();
            newSlots.add(bettingSlotRepository.save(slot));
        }

        return newSlots.stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Assign a pitch to a slot (the main betting action)
     * This will resize the slot to match the pitch's appetite and create a new empty slot for remaining time
     */
    public BettingSlotDTO assignPitchToSlot(Long slotId, Long pitchId) {
        BettingSlot slot = bettingSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Betting slot not found with id: " + slotId));
        
        Pitch pitch = pitchRepository.findById(pitchId)
                .orElseThrow(() -> new IllegalArgumentException("Pitch not found with id: " + pitchId));

        // Check if pitch is already assigned elsewhere
        bettingSlotRepository.findByPitchId(pitchId).ifPresent(existingSlot -> {
            if (!existingSlot.getId().equals(slotId)) {
                throw new IllegalArgumentException("Pitch is already assigned to another slot. Remove it first.");
            }
        });

        // Validate the pitch fits in the slot
        validatePitchFits(pitch, slot);

        // Calculate the new end date for this slot based on pitch appetite
        LocalDate slotStart = slot.getStartDate();
        LocalDate newSlotEndDate = slotStart.plusDays(pitch.getAppetiteDays());
        LocalDate originalSlotEndDate = slot.getEndDate();
        
        // If the slot was bigger than the pitch needs, create a new empty slot for remaining time
        if (newSlotEndDate.isBefore(originalSlotEndDate)) {
            // Get the next position for this team's slots
            Integer maxPosition = bettingSlotRepository.findMaxPositionByCycleAndTeam(
                    slot.getCycle().getId(), slot.getTeam().getId());
            if (maxPosition == null) maxPosition = -1;
            
            // Create new empty slot for remaining capacity
            BettingSlot remainingSlot = BettingSlot.builder()
                    .cycle(slot.getCycle())
                    .team(slot.getTeam())
                    .position(maxPosition + 1)
                    .startDate(newSlotEndDate)
                    .endDate(originalSlotEndDate)
                    .build();
            bettingSlotRepository.save(remainingSlot);
        }

        // Resize the original slot to match pitch appetite
        slot.setEndDate(newSlotEndDate);
        
        // Assign pitch and update its team
        slot.setPitch(pitch);
        pitch.setTeam(slot.getTeam());
        
        // Update pitch status to STARTED when assigned to betting table
        if (pitch.getStatus() == PitchStatus.SHAPED) {
            pitch.setStatus(PitchStatus.STARTED);
        }
        
        pitchRepository.save(pitch);
        BettingSlot saved = bettingSlotRepository.save(slot);
        
        return toDTO(saved);
    }

    /**
     * Remove a pitch from a slot (unassign)
     * This will merge the slot with any adjacent empty slot
     */
    public BettingSlotDTO removePitchFromSlot(Long slotId) {
        BettingSlot slot = bettingSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Betting slot not found with id: " + slotId));
        
        if (slot.getPitch() != null) {
            Pitch pitch = slot.getPitch();
            // Optionally revert status to SHAPED
            if (pitch.getStatus() == PitchStatus.STARTED) {
                pitch.setStatus(PitchStatus.SHAPED);
                pitchRepository.save(pitch);
            }
        }
        
        slot.setPitch(null);
        
        // Try to merge with adjacent empty slots
        List<BettingSlot> teamSlots = bettingSlotRepository.findByCycleIdAndTeamId(
                slot.getCycle().getId(), slot.getTeam().getId());
        teamSlots.sort(Comparator.comparing(BettingSlot::getStartDate));
        
        LocalDate mergedStartDate = slot.getStartDate();
        LocalDate mergedEndDate = slot.getEndDate();
        List<BettingSlot> slotsToDelete = new ArrayList<>();
        
        for (BettingSlot teamSlot : teamSlots) {
            if (teamSlot.getId().equals(slot.getId()) || teamSlot.getPitch() != null) {
                continue;
            }
            // Check if adjacent (ends where this starts, or starts where this ends)
            if (teamSlot.getEndDate().equals(mergedStartDate)) {
                mergedStartDate = teamSlot.getStartDate();
                slotsToDelete.add(teamSlot);
            } else if (teamSlot.getStartDate().equals(mergedEndDate)) {
                mergedEndDate = teamSlot.getEndDate();
                slotsToDelete.add(teamSlot);
            }
        }
        
        // Update slot with merged dates
        slot.setStartDate(mergedStartDate);
        slot.setEndDate(mergedEndDate);
        
        // Delete merged slots
        for (BettingSlot toDelete : slotsToDelete) {
            bettingSlotRepository.delete(toDelete);
        }
        
        BettingSlot saved = bettingSlotRepository.save(slot);
        
        return toDTO(saved);
    }

    /**
     * Update slot dates (resize)
     */
    public BettingSlotDTO updateSlotDates(Long slotId, LocalDate startDate, LocalDate endDate) {
        BettingSlot slot = bettingSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Betting slot not found with id: " + slotId));
        
        // Validate dates within cycle
        Cycle cycle = slot.getCycle();
        if (startDate.isBefore(cycle.getStartDate()) || endDate.isAfter(cycle.getEndDate())) {
            throw new IllegalArgumentException("Slot dates must be within cycle dates (" + 
                    cycle.getStartDate() + " to " + cycle.getEndDate() + ")");
        }

        // If pitch is assigned, validate it still fits
        if (slot.getPitch() != null) {
            long newDays = ChronoUnit.DAYS.between(startDate, endDate);
            if (slot.getPitch().getAppetiteDays() > newDays) {
                throw new IllegalArgumentException("Cannot resize slot: assigned pitch requires " + 
                        slot.getPitch().getAppetiteDays() + " days but new slot is only " + newDays + " days");
            }
        }

        slot.setStartDate(startDate);
        slot.setEndDate(endDate);
        BettingSlot saved = bettingSlotRepository.save(slot);
        
        return toDTO(saved);
    }

    /**
     * Delete a slot
     */
    public void deleteSlot(Long slotId) {
        BettingSlot slot = bettingSlotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Betting slot not found with id: " + slotId));
        
        // If pitch is assigned, revert its status
        if (slot.getPitch() != null) {
            Pitch pitch = slot.getPitch();
            if (pitch.getStatus() == PitchStatus.STARTED) {
                pitch.setStatus(PitchStatus.SHAPED);
                pitchRepository.save(pitch);
            }
        }
        
        bettingSlotRepository.delete(slot);
    }

    /**
     * Check if a pitch can fit in a slot (for drag preview)
     */
    public boolean canPitchFitInSlot(Long pitchId, Long slotId) {
        Pitch pitch = pitchRepository.findById(pitchId).orElse(null);
        BettingSlot slot = bettingSlotRepository.findById(slotId).orElse(null);
        
        if (pitch == null || slot == null) return false;
        
        return slot.canFitPitch(pitch.getAppetiteDays());
    }

    // === Helper Methods ===

    private void validatePitchFits(Pitch pitch, BettingSlot slot) {
        if (!slot.canFitPitch(pitch.getAppetiteDays())) {
            long slotDays = ChronoUnit.DAYS.between(slot.getStartDate(), slot.getEndDate());
            throw new IllegalArgumentException(
                    String.format("Pitch '%s' requires %d days but slot only has %d days available",
                            pitch.getTitle(), pitch.getAppetiteDays(), slotDays));
        }
    }

    private BettingTableDTO.TeamTrackDTO buildTeamTrack(Team team, List<BettingSlot> slots, int cycleDurationWeeks) {
        List<BettingSlotDTO> slotDTOs = slots.stream()
                .sorted(Comparator.comparing(BettingSlot::getPosition))
                .map(this::toDTO)
                .collect(Collectors.toList());

        int usedWeeks = slots.stream()
                .filter(s -> s.getPitch() != null)
                .mapToInt(s -> (int) Math.ceil(s.getPitch().getAppetiteDays() / 7.0))
                .sum();

        return BettingTableDTO.TeamTrackDTO.builder()
                .teamId(team.getId())
                .teamName(team.getName())
                .slots(slotDTOs)
                .totalCapacityWeeks(cycleDurationWeeks)
                .usedCapacityWeeks(usedWeeks)
                .availableCapacityWeeks(cycleDurationWeeks - usedWeeks)
                .build();
    }

    private BettingSlotDTO toDTO(BettingSlot slot) {
        return BettingSlotDTO.builder()
                .id(slot.getId())
                .cycleId(slot.getCycle().getId())
                .cycleName(slot.getCycle().getName())
                .teamId(slot.getTeam().getId())
                .teamName(slot.getTeam().getName())
                .pitchId(slot.getPitch() != null ? slot.getPitch().getId() : null)
                .pitchTitle(slot.getPitch() != null ? slot.getPitch().getTitle() : null)
                .pitchAppetiteDays(slot.getPitch() != null ? slot.getPitch().getAppetiteDays() : null)
                .pitchStatus(slot.getPitch() != null ? slot.getPitch().getStatus().name() : null)
                .position(slot.getPosition())
                .startDate(slot.getStartDate())
                .endDate(slot.getEndDate())
                .durationWeeks(slot.getDurationWeeks())
                .notes(slot.getNotes())
                .createdAt(slot.getCreatedAt())
                .updatedAt(slot.getUpdatedAt())
                .canFitPitch(true) // Default, will be recalculated in frontend
                .build();
    }

    private PitchDTO toPitchDTO(Pitch pitch) {
        Double totalHours = workLogRepository.getTotalHoursByPitchId(pitch.getId());
        if (totalHours == null) totalHours = 0.0;
        
        double appetiteHours = pitch.getAppetiteDays() * HOURS_PER_DAY;
        double progress = appetiteHours > 0 ? (totalHours / appetiteHours) * 100 : 0;
        
        return PitchDTO.builder()
                .id(pitch.getId())
                .title(pitch.getTitle())
                .description(pitch.getDescription())
                .appetiteDays(pitch.getAppetiteDays())
                .cycleId(pitch.getCycle().getId())
                .cycleName(pitch.getCycle().getName())
                .projectId(pitch.getCycle().getProject() != null ? pitch.getCycle().getProject().getId() : null)
                .projectName(pitch.getCycle().getProject() != null ? pitch.getCycle().getProject().getName() : null)
                .projectKey(pitch.getCycle().getProject() != null ? pitch.getCycle().getProject().getProjectKey() : null)
                .teamId(pitch.getTeam() != null ? pitch.getTeam().getId() : null)
                .teamName(pitch.getTeam() != null ? pitch.getTeam().getName() : null)
                .status(pitch.getStatus())
                .createdAt(pitch.getCreatedAt())
                .updatedAt(pitch.getUpdatedAt())
                .totalHoursSpent(totalHours)
                .appetiteHours(appetiteHours)
                .progressPercentage(Math.min(progress, 100))
                .build();
    }

    // === NEW: Pitch Comparison and Analytics Methods ===

    /**
     * Get historical performance metrics for a team
     */
    public TeamPerformanceHistoryDTO getTeamPerformanceHistory(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new IllegalArgumentException("Team not found with id: " + teamId));

        // Get all completed cycles for this team
        List<BettingSlot> allHistoricalSlots = bettingSlotRepository.findByTeamId(teamId);
        
        // Group by cycle
        Map<Long, List<BettingSlot>> slotsByCycle = allHistoricalSlots.stream()
                .filter(slot -> slot.getPitch() != null)
                .filter(slot -> !slot.getCycle().getIsActive()) // Only completed cycles
                .collect(Collectors.groupingBy(slot -> slot.getCycle().getId()));

        List<Long> cycleIds = new ArrayList<>(slotsByCycle.keySet());
        cycleIds.sort(Comparator.reverseOrder()); // Most recent first

        // Calculate metrics
        int totalCycles = cycleIds.size();
        int totalBets = allHistoricalSlots.stream()
                .filter(slot -> slot.getPitch() != null)
                .filter(slot -> !slot.getCycle().getIsActive())
                .mapToInt(slot -> 1).sum();
        
        int totalCompletedBets = (int) allHistoricalSlots.stream()
                .filter(slot -> slot.getPitch() != null)
                .filter(slot -> !slot.getCycle().getIsActive())
                .filter(slot -> slot.getPitch().getStatus() == PitchStatus.DONE)
                .count();

        // Last cycle metrics
        Integer lastCycleTotalBets = 0;
        Integer lastCycleCompletedBets = 0;
        if (!cycleIds.isEmpty()) {
            Long lastCycleId = cycleIds.get(0);
            List<BettingSlot> lastCycleSlots = slotsByCycle.get(lastCycleId);
            lastCycleTotalBets = lastCycleSlots.size();
            lastCycleCompletedBets = (int) lastCycleSlots.stream()
                    .filter(slot -> slot.getPitch().getStatus() == PitchStatus.DONE)
                    .count();
        }

        Double lastCycleCompletionRate = lastCycleTotalBets > 0 
                ? (lastCycleCompletedBets * 100.0 / lastCycleTotalBets) : 0.0;
        Double overallCompletionRate = totalBets > 0 
                ? (totalCompletedBets * 100.0 / totalBets) : 0.0;

        // Calculate trends (last 3 cycles)
        String trend = calculateTrend(cycleIds, slotsByCycle);
        String performanceRating = getPerformanceRating(overallCompletionRate);
        
        // Calculate average weeks per bet
        Double avgWeeksPerBet = calculateAvgWeeksPerBet(slotsByCycle);
        
        // Calculate average time overrun percentage
        Double avgTimeOverrun = calculateAvgTimeOverrun(slotsByCycle);

        return TeamPerformanceHistoryDTO.builder()
                .teamId(team.getId())
                .teamName(team.getName())
                .lastCycleTotalBets(lastCycleTotalBets)
                .lastCycleCompletedBets(lastCycleCompletedBets)
                .lastCycleCompletionRate(Math.round(lastCycleCompletionRate * 10.0) / 10.0)
                .totalCycles(totalCycles)
                .totalBets(totalBets)
                .totalCompletedBets(totalCompletedBets)
                .overallCompletionRate(Math.round(overallCompletionRate * 10.0) / 10.0)
                .avgBetsPerCycle(totalCycles > 0 ? Math.round((totalBets * 10.0 / totalCycles)) / 10.0 : 0.0)
                .avgWeeksPerBet(avgWeeksPerBet)
                .avgTimeOverrun(avgTimeOverrun)
                .trend(trend)
                .performanceRating(performanceRating)
                .build();
    }

    /**
     * Get pitch comparison analysis for betting meeting
     */
    public List<PitchComparisonDTO> getPitchComparisons(Long cycleId) {
        Cycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new IllegalArgumentException("Cycle not found with id: " + cycleId));

        List<Pitch> shapedPitches = pitchRepository.findByCycleIdAndStatus(cycleId, PitchStatus.SHAPED);
        List<Team> teams = teamRepository.findByCycleId(cycleId);

        return shapedPitches.stream()
                .map(pitch -> buildPitchComparison(pitch, teams, cycle))
                .collect(Collectors.toList());
    }

    /**
     * Get capacity analysis for the current betting table
     */
    public CapacityAnalysisDTO getCapacityAnalysis(Long cycleId) {
        BettingTableDTO bettingTable = getBettingTable(cycleId);
        
        List<CapacityAnalysisDTO.CapacityWarning> warnings = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        
        boolean isOverAllocated = false;
        
        for (BettingTableDTO.TeamTrackDTO track : bettingTable.getTeamTracks()) {
            double utilization = track.getTotalCapacityWeeks() > 0 
                    ? (track.getUsedCapacityWeeks() * 100.0 / track.getTotalCapacityWeeks()) : 0.0;
            
            if (utilization > 100) {
                isOverAllocated = true;
                warnings.add(CapacityAnalysisDTO.CapacityWarning.builder()
                        .severity("CRITICAL")
                        .teamId(track.getTeamId())
                        .teamName(track.getTeamName())
                        .message(String.format("Team is over-allocated by %d weeks (%.0f%% capacity)",
                                track.getUsedCapacityWeeks() - track.getTotalCapacityWeeks(), utilization))
                        .utilizationRate(utilization)
                        .type("OVER_ALLOCATED")
                        .build());
            } else if (utilization > 85) {
                warnings.add(CapacityAnalysisDTO.CapacityWarning.builder()
                        .severity("WARNING")
                        .teamId(track.getTeamId())
                        .teamName(track.getTeamName())
                        .message(String.format("Team capacity is tight: %.0f%% utilized", utilization))
                        .utilizationRate(utilization)
                        .type("TIGHT_SCHEDULE")
                        .build());
            } else if (utilization < 50) {
                warnings.add(CapacityAnalysisDTO.CapacityWarning.builder()
                        .severity("INFO")
                        .teamId(track.getTeamId())
                        .teamName(track.getTeamName())
                        .message(String.format("Team has significant available capacity: %.0f%% utilized", utilization))
                        .utilizationRate(utilization)
                        .type("UNDER_UTILIZED")
                        .build());
            }
        }
        
        // Generate recommendations
        if (isOverAllocated) {
            recommendations.add("⚠️ Some teams are over-allocated. Consider redistributing work or reducing scope.");
        }
        
        int totalAvailable = bettingTable.getTotalCapacityWeeks() - bettingTable.getUsedCapacityWeeks();
        if (totalAvailable > 0 && bettingTable.getShapedPitches().size() > 0) {
            recommendations.add(String.format("✅ %d weeks of capacity available for %d shaped pitches",
                    totalAvailable, bettingTable.getShapedPitches().size()));
        }
        
        double overallUtilization = bettingTable.getTotalCapacityWeeks() > 0
                ? (bettingTable.getUsedCapacityWeeks() * 100.0 / bettingTable.getTotalCapacityWeeks()) : 0.0;
        
        if (overallUtilization > 80 && overallUtilization <= 100) {
            recommendations.add("✅ Good capacity utilization - teams are well-allocated");
        }

        return CapacityAnalysisDTO.builder()
                .cycleId(cycleId)
                .cycleName(bettingTable.getCycleName())
                .totalTeams(bettingTable.getTeamTracks().size())
                .totalCapacityWeeks(bettingTable.getTotalCapacityWeeks())
                .allocatedWeeks(bettingTable.getUsedCapacityWeeks())
                .remainingWeeks(totalAvailable)
                .utilizationRate(overallUtilization / 100.0)  // Convert to decimal (0.0 to 1.0)
                .warnings(warnings)
                .isOverAllocated(isOverAllocated)
                .hasConflicts(false)
                .recommendations(recommendations)
                .build();
    }

    // === Helper Methods for Analytics ===

    private PitchComparisonDTO buildPitchComparison(Pitch pitch, List<Team> teams, Cycle cycle) {
        List<String> risks = pitch.getRisks() != null && !pitch.getRisks().isEmpty()
                ? Arrays.asList(pitch.getRisks().split("\\n"))
                : new ArrayList<>();
        
        List<String> rabbitHoles = pitch.getRabbitHoles() != null && !pitch.getRabbitHoles().isEmpty()
                ? Arrays.asList(pitch.getRabbitHoles().split("\\n"))
                : new ArrayList<>();

        ComplexityLevel complexityLevel = determineComplexity(pitch.getAppetiteDays(), risks.size());

        List<PitchComparisonDTO.TeamFitAnalysis> teamFitScores = teams.stream()
                .map(team -> analyzeTeamFit(team, pitch, cycle))
                .collect(Collectors.toList());

        return PitchComparisonDTO.builder()
                .pitch(toPitchDTO(pitch))
                .appetiteDays(pitch.getAppetiteDays())
                .complexityLevel(complexityLevel)
                .risks(risks.isEmpty() ? List.of("No risks identified") : risks)
                .rabbitHoles(rabbitHoles.isEmpty() ? List.of("No rabbit holes identified") : rabbitHoles)
                .teamFitScores(teamFitScores)
                .estimatedBusinessValue(null) // NOTE: Not currently calculated - intentionally returned as null until business value logic is implemented
                .urgency(null) // NOTE: Not currently calculated - intentionally returned as null until urgency logic is implemented
                .build();
    }

    private PitchComparisonDTO.TeamFitAnalysis analyzeTeamFit(Team team, Pitch pitch, Cycle cycle) {
        // Get current slots for this team
        List<BettingSlot> teamSlots = bettingSlotRepository.findByCycleIdAndTeamId(cycle.getId(), team.getId());
        
        int usedWeeks = teamSlots.stream()
                .filter(s -> s.getPitch() != null)
                .mapToInt(s -> (int) Math.ceil(s.getPitch().getAppetiteDays() / 7.0))
                .sum();
        
        long cycleDays = ChronoUnit.DAYS.between(cycle.getStartDate(), cycle.getEndDate());
        int totalWeeks = (int) Math.ceil(cycleDays / 7.0);
        int availableWeeks = totalWeeks - usedWeeks;
        int pitchWeeks = (int) Math.ceil(pitch.getAppetiteDays() / 7.0);
        
        boolean canFit = availableWeeks >= pitchWeeks;
        String capacityStatus;
        String recommendation;
        List<String> warnings = new ArrayList<>();
        
        if (availableWeeks >= pitchWeeks + 2) {
            capacityStatus = "AVAILABLE";
            recommendation = "GOOD_FIT";
        } else if (availableWeeks >= pitchWeeks) {
            capacityStatus = "TIGHT";
            recommendation = "POSSIBLE_WITH_TRADEOFFS";
            warnings.add(String.format("Only %d weeks buffer remaining", availableWeeks - pitchWeeks));
        } else {
            capacityStatus = "OVER_ALLOCATED";
            recommendation = "NOT_RECOMMENDED";
            warnings.add(String.format("Exceeds capacity by %d weeks", pitchWeeks - availableWeeks));
        }

        return PitchComparisonDTO.TeamFitAnalysis.builder()
                .teamId(team.getId())
                .teamName(team.getName())
                .availableCapacityWeeks(availableWeeks)
                .canFit(canFit)
                .capacityStatus(capacityStatus)
                .recommendation(recommendation)
                .warnings(warnings)
                .build();
    }

    private static final int HIGH_COMPLEXITY_APPETITE_DAYS_THRESHOLD = 35;
    private static final int MEDIUM_COMPLEXITY_APPETITE_DAYS_THRESHOLD = 14;
    private static final int HIGH_COMPLEXITY_RISK_COUNT_THRESHOLD = 5;
    private static final int MEDIUM_COMPLEXITY_RISK_COUNT_THRESHOLD = 2;

    private ComplexityLevel determineComplexity(int appetiteDays, int riskCount) {
        if (appetiteDays > HIGH_COMPLEXITY_APPETITE_DAYS_THRESHOLD
                || riskCount > HIGH_COMPLEXITY_RISK_COUNT_THRESHOLD) {
            return ComplexityLevel.HIGH;
        }
        if (appetiteDays > MEDIUM_COMPLEXITY_APPETITE_DAYS_THRESHOLD
                || riskCount > MEDIUM_COMPLEXITY_RISK_COUNT_THRESHOLD) {
            return ComplexityLevel.MEDIUM;
        }
        return ComplexityLevel.LOW;
    }

    private String calculateTrend(List<Long> cycleIds, Map<Long, List<BettingSlot>> slotsByCycle) {
        if (cycleIds.size() < 2) return "STABLE";
        
        List<Double> completionRates = new ArrayList<>();
        for (int i = 0; i < Math.min(3, cycleIds.size()); i++) {
            List<BettingSlot> slots = slotsByCycle.get(cycleIds.get(i));
            long total = slots.size();
            long completed = slots.stream()
                    .filter(s -> s.getPitch().getStatus() == PitchStatus.DONE)
                    .count();
            completionRates.add(total > 0 ? (completed * 100.0 / total) : 0.0);
        }
        
        if (completionRates.size() < 2) return "STABLE";
        
        double avgChange = 0;
        for (int i = 1; i < completionRates.size(); i++) {
            avgChange += (completionRates.get(i-1) - completionRates.get(i));
        }
        avgChange /= (completionRates.size() - 1);
        
        if (avgChange > 10) return "IMPROVING";
        if (avgChange < -10) return "DECLINING";
        return "STABLE";
    }

    private String getPerformanceRating(double completionRate) {
        if (completionRate >= 90) return "EXCELLENT";
        if (completionRate >= 75) return "GOOD";
        if (completionRate >= 50) return "FAIR";
        return "POOR";
    }

    /**
     * Calculate average weeks per bet from actual cycle durations
     */
    private Double calculateAvgWeeksPerBet(Map<Long, List<BettingSlot>> slotsByCycle) {
        if (slotsByCycle.isEmpty()) return null;
        
        double totalWeeks = 0.0;
        int totalBets = 0;
        
        for (Map.Entry<Long, List<BettingSlot>> entry : slotsByCycle.entrySet()) {
            List<BettingSlot> cycleSlots = entry.getValue();
            if (cycleSlots.isEmpty()) continue;
            
            // Get the cycle to calculate duration
            Cycle cycle = cycleSlots.get(0).getCycle();
            long cycleDays = ChronoUnit.DAYS.between(cycle.getStartDate(), cycle.getEndDate());
            double cycleWeeks = cycleDays / 7.0;
            
            // Count bets in this cycle
            int cycleBets = cycleSlots.size();
            
            if (cycleBets > 0) {
                totalWeeks += cycleWeeks;
                totalBets += cycleBets;
            }
        }
        
        return totalBets > 0 ? Math.round((totalWeeks / totalBets) * 10.0) / 10.0 : null;
    }

    /**
     * Calculate average time overrun percentage from work log analysis
     * Compares actual hours spent vs appetite hours for completed bets
     */
    private Double calculateAvgTimeOverrun(Map<Long, List<BettingSlot>> slotsByCycle) {
        if (slotsByCycle.isEmpty()) return null;
        
        List<Double> overrunPercentages = new ArrayList<>();
        
        for (List<BettingSlot> cycleSlots : slotsByCycle.values()) {
            for (BettingSlot slot : cycleSlots) {
                Pitch pitch = slot.getPitch();
                if (pitch == null) continue;
                
                // Only calculate for completed pitches
                if (pitch.getStatus() != PitchStatus.DONE) continue;
                
                // Get actual hours spent from work logs
                Double actualHours = workLogRepository.getTotalHoursByPitchId(pitch.getId());
                if (actualHours == null || actualHours == 0.0) continue;
                
                // Calculate appetite hours
                double appetiteHours = pitch.getAppetiteDays() * HOURS_PER_DAY;
                if (appetiteHours == 0.0) continue;
                
                // Calculate overrun percentage: (actual - appetite) / appetite * 100
                double overrun = ((actualHours - appetiteHours) / appetiteHours) * 100.0;
                overrunPercentages.add(overrun);
            }
        }
        
        if (overrunPercentages.isEmpty()) return null;
        
        // Calculate average overrun
        double avgOverrun = overrunPercentages.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        
        return Math.round(avgOverrun * 10.0) / 10.0;
    }
}

