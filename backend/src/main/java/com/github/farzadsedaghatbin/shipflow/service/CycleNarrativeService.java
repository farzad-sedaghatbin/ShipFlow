package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.config.AIConfig;
import com.github.farzadsedaghatbin.shipflow.dto.narrative.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.NarrativeType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for generating cycle narrative summaries.
 * Supports both AI-generated narratives (when available) and template-based fallback.
 * 
 * Narratives answer: What we bet, What shipped, What we cut, What surprised us.
 */
@Service
@Slf4j
public class CycleNarrativeService {

  private static final double HOURS_PER_DAY = 8.0;
  private static final double SURPRISE_THRESHOLD = 0.25; // 25% variance

  private final CycleRepository cycleRepository;
  private final PitchRepository pitchRepository;
  private final WorkLogRepository workLogRepository;
  private final BettingDecisionRepository bettingDecisionRepository;
  private final CycleNarrativeRepository narrativeRepository;
  private final AIConfig aiConfig;
  private final ChatLanguageModel chatLanguageModel;
  private final UserService userService;
  private final UserRepository userRepository;

  @Autowired
  public CycleNarrativeService(
      CycleRepository cycleRepository,
      PitchRepository pitchRepository,
      WorkLogRepository workLogRepository,
      BettingDecisionRepository bettingDecisionRepository,
      CycleNarrativeRepository narrativeRepository,
      AIConfig aiConfig,
      @Autowired(required = false) ChatLanguageModel chatLanguageModel,
      UserService userService,
      UserRepository userRepository) {
    this.cycleRepository = cycleRepository;
    this.pitchRepository = pitchRepository;
    this.workLogRepository = workLogRepository;
    this.bettingDecisionRepository = bettingDecisionRepository;
    this.narrativeRepository = narrativeRepository;
    this.aiConfig = aiConfig;
    this.chatLanguageModel = chatLanguageModel;
    this.userService = userService;
    this.userRepository = userRepository;
  }

  /**
   * Helper to get current user from Spring Security context.
   */
  private User getCurrentUser() {
    String username = org.springframework.security.core.context.SecurityContextHolder
        .getContext().getAuthentication().getName();
    return userRepository.findByUsername(username)
        .orElseThrow(() -> new RuntimeException("User not found: " + username));
  }

  /**
   * Helper to get total logged hours for a cycle.
   * WorkLogRepository.getTotalHoursByCycleId returns List<Object[]> with [pitchId, hours].
   */
  private double getTotalHoursForCycle(Long cycleId) {
    List<Object[]> results = workLogRepository.getTotalHoursByCycleId(cycleId);
    if (results == null || results.isEmpty()) {
      return 0.0;
    }
    return results.stream()
        .filter(r -> r.length > 1 && r[1] != null)
        .mapToDouble(r -> ((Number) r[1]).doubleValue())
        .sum();
  }

  /**
   * Get complete cycle summary with all narrative sections.
   */
  @Transactional(readOnly = true)
  public CycleSummaryDTO getCycleSummary(Long cycleId) {
    Cycle cycle = cycleRepository.findById(cycleId)
        .orElseThrow(() -> new RuntimeException("Cycle not found: " + cycleId));

    List<Pitch> pitches = pitchRepository.findByCycleIdNotDeleted(cycleId);
    
    // Get or generate narratives
    CycleNarrativeDTO whatWeBet = getOrGenerateNarrative(cycleId, NarrativeType.WHAT_WE_BET, false);
    CycleNarrativeDTO whatShipped = getOrGenerateNarrative(cycleId, NarrativeType.WHAT_SHIPPED, false);
    CycleNarrativeDTO whatWeCut = getOrGenerateNarrative(cycleId, NarrativeType.WHAT_WE_CUT, false);
    CycleNarrativeDTO surprises = getOrGenerateNarrative(cycleId, NarrativeType.SURPRISES, false);
    CycleNarrativeDTO fullSummary = getOrGenerateNarrative(cycleId, NarrativeType.FULL_SUMMARY, false);

    // Calculate quick stats
    int committed = (int) pitches.stream()
        .filter(p -> p.getStatus() != PitchStatus.PENDING)
        .count();
    int shipped = (int) pitches.stream()
        .filter(p -> p.getStatus() == PitchStatus.DONE)
        .count();
    int cut = (int) pitches.stream()
        .filter(p -> p.getIsCircuitBreakerTriggered() || p.getStatus() == PitchStatus.CANCELLED)
        .count();
    int inProgress = (int) pitches.stream()
        .filter(p -> p.getStatus() == PitchStatus.IN_PROGRESS || p.getStatus() == PitchStatus.STARTED)
        .count();

    // Calculate average appetite accuracy
    double totalAppetite = pitches.stream()
        .mapToDouble(p -> p.getAppetiteDays() * HOURS_PER_DAY)
        .sum();
    double totalActual = getTotalHoursForCycle(cycleId);
    double accuracy = totalAppetite > 0 ? (1 - Math.abs(totalActual - totalAppetite) / totalAppetite) * 100 : 0;

    return CycleSummaryDTO.builder()
        .cycleId(cycleId)
        .cycleName(cycle.getName())
        .projectName(cycle.getProject().getName())
        .startDate(cycle.getStartDate())
        .endDate(cycle.getEndDate())
        .phase(cycle.getPhase().name())
        .whatWeBet(whatWeBet)
        .whatShipped(whatShipped)
        .whatWeCut(whatWeCut)
        .surprises(surprises)
        .fullSummary(fullSummary)
        .totalPitchesCommitted(committed)
        .pitchesShipped(shipped)
        .pitchesCut(cut)
        .pitchesInProgress(inProgress)
        .averageAppetiteAccuracy(accuracy)
        .generatedAt(LocalDateTime.now())
        .aiGenerated(isAIEnabled())
        .exportFormats(List.of("PDF", "MARKDOWN"))
        .build();
  }

  /**
   * Get or generate a specific narrative type.
   */
  @Transactional
  public CycleNarrativeDTO getOrGenerateNarrative(Long cycleId, NarrativeType type, boolean forceRegenerate) {
    if (!forceRegenerate) {
      Optional<CycleNarrative> existing = narrativeRepository.findByCycleIdAndNarrativeType(cycleId, type);
      if (existing.isPresent()) {
        return toDTO(existing.get());
      }
    }

    // Generate new narrative
    return generateNarrative(cycleId, type);
  }

  /**
   * Generate a narrative for a specific type.
   */
  @Transactional
  public CycleNarrativeDTO generateNarrative(Long cycleId, NarrativeType type) {
    Cycle cycle = cycleRepository.findById(cycleId)
        .orElseThrow(() -> new RuntimeException("Cycle not found: " + cycleId));

    String content;
    boolean isAI = false;
    String aiModel = null;

    if (isAIEnabled()) {
      try {
        content = generateAINarrative(cycle, type);
        isAI = true;
        aiModel = aiConfig.getModelName();
      } catch (Exception e) {
        log.warn("AI narrative generation failed, falling back to template: {}", e.getMessage());
        content = generateTemplateNarrative(cycle, type);
      }
    } else {
      content = generateTemplateNarrative(cycle, type);
    }

    // Save to database
    CycleNarrative narrative = CycleNarrative.builder()
        .cycle(cycle)
        .narrativeType(type)
        .content(content)
        .isAiGenerated(isAI)
        .aiModel(aiModel)
        .generatedAt(LocalDateTime.now())
        .generatedBy(getCurrentUser())
        .build();

    narrative = narrativeRepository.save(narrative);
    return toDTO(narrative);
  }

  /**
   * Regenerate all narratives for a cycle.
   */
  @Transactional
  public CycleSummaryDTO regenerateAllNarratives(Long cycleId) {
    for (NarrativeType type : NarrativeType.values()) {
      generateNarrative(cycleId, type);
    }
    return getCycleSummary(cycleId);
  }

  /**
   * Export cycle summary as Markdown.
   */
  @Transactional(readOnly = true)
  public String exportAsMarkdown(Long cycleId) {
    CycleSummaryDTO summary = getCycleSummary(cycleId);
    StringBuilder md = new StringBuilder();

    md.append("# Cycle Summary: ").append(summary.getCycleName()).append("\n\n");
    md.append("**Project:** ").append(summary.getProjectName()).append("\n");
    md.append("**Period:** ").append(summary.getStartDate()).append(" to ").append(summary.getEndDate()).append("\n");
    md.append("**Phase:** ").append(summary.getPhase()).append("\n\n");

    md.append("## Quick Stats\n\n");
    md.append("| Metric | Value |\n");
    md.append("|--------|-------|\n");
    md.append("| Pitches Committed | ").append(summary.getTotalPitchesCommitted()).append(" |\n");
    md.append("| Shipped | ").append(summary.getPitchesShipped()).append(" |\n");
    md.append("| Cut | ").append(summary.getPitchesCut()).append(" |\n");
    md.append("| In Progress | ").append(summary.getPitchesInProgress()).append(" |\n");
    md.append("| Appetite Accuracy | ").append(String.format("%.1f%%", summary.getAverageAppetiteAccuracy())).append(" |\n\n");

    if (summary.getWhatWeBet() != null) {
      md.append("## What We Bet\n\n");
      md.append(summary.getWhatWeBet().getContent()).append("\n\n");
    }

    if (summary.getWhatShipped() != null) {
      md.append("## What Shipped\n\n");
      md.append(summary.getWhatShipped().getContent()).append("\n\n");
    }

    if (summary.getWhatWeCut() != null) {
      md.append("## What We Cut\n\n");
      md.append(summary.getWhatWeCut().getContent()).append("\n\n");
    }

    if (summary.getSurprises() != null) {
      md.append("## What Surprised Us\n\n");
      md.append(summary.getSurprises().getContent()).append("\n\n");
    }

    md.append("---\n\n");
    md.append("*Generated ").append(summary.getGeneratedAt()).append("*\n");
    if (Boolean.TRUE.equals(summary.getAiGenerated())) {
      md.append("*AI-enhanced summary*\n");
    }

    return md.toString();
  }

  // ============================================
  // AI Narrative Generation
  // ============================================

  private String generateAINarrative(Cycle cycle, NarrativeType type) {
    String prompt = buildPrompt(cycle, type);
    String response = chatLanguageModel.generate(prompt);
    return cleanAIResponse(response);
  }

  private String buildPrompt(Cycle cycle, NarrativeType type) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("You are writing a cycle summary for a Shape Up development team. ");
    prompt.append("Write in a conversational, team-friendly tone. Be concise but informative.\n\n");

    List<Pitch> pitches = pitchRepository.findByCycleIdNotDeleted(cycle.getId());
    
    switch (type) {
      case WHAT_WE_BET:
        prompt.append("Write a summary of what this team committed to in their betting meeting.\n\n");
        prompt.append("Cycle: ").append(cycle.getName()).append("\n");
        prompt.append("Pitches committed:\n");
        for (Pitch pitch : pitches) {
          if (pitch.getStatus() != PitchStatus.PENDING) {
            prompt.append("- ").append(pitch.getTitle());
            prompt.append(" (").append(pitch.getAppetiteDays()).append(" day appetite)");
            Optional<BettingDecision> decision = bettingDecisionRepository.findByPitchIdAndCycleId(pitch.getId(), cycle.getId());
            if (decision.isPresent() && decision.get().getReason() != null) {
              prompt.append(" - Reason: ").append(decision.get().getReason());
            }
            prompt.append("\n");
          }
        }
        break;

      case WHAT_SHIPPED:
        prompt.append("Write a summary of what shipped (completed) in this cycle.\n\n");
        prompt.append("Completed pitches:\n");
        for (Pitch pitch : pitches) {
          if (pitch.getStatus() == PitchStatus.DONE) {
            Double actual = workLogRepository.getTotalHoursByPitchId(pitch.getId());
            prompt.append("- ").append(pitch.getTitle());
            prompt.append(" (appetite: ").append(pitch.getAppetiteDays()).append(" days");
            if (actual != null) {
              prompt.append(", actual: ").append(String.format("%.1f", actual / HOURS_PER_DAY)).append(" days");
            }
            prompt.append(")\n");
          }
        }
        break;

      case WHAT_WE_CUT:
        prompt.append("Write a summary of what was cut or cancelled in this cycle.\n\n");
        prompt.append("Cut/cancelled pitches:\n");
        for (Pitch pitch : pitches) {
          if (pitch.getIsCircuitBreakerTriggered() || pitch.getStatus() == PitchStatus.CANCELLED) {
            prompt.append("- ").append(pitch.getTitle());
            if (pitch.getIsCircuitBreakerTriggered() && pitch.getCircuitBreakerReason() != null) {
              prompt.append(" - Circuit breaker: ").append(pitch.getCircuitBreakerReason());
            }
            prompt.append("\n");
          }
        }
        if (pitches.stream().noneMatch(p -> p.getIsCircuitBreakerTriggered() || p.getStatus() == PitchStatus.CANCELLED)) {
          prompt.append("(Nothing was cut this cycle)\n");
        }
        break;

      case SURPRISES:
        prompt.append("Write a summary of surprising outcomes in this cycle - things that went much better or worse than expected.\n\n");
        prompt.append("Pitch outcomes:\n");
        for (Pitch pitch : pitches) {
          Double actual = workLogRepository.getTotalHoursByPitchId(pitch.getId());
          if (actual != null) {
            double appetite = pitch.getAppetiteDays() * HOURS_PER_DAY;
            double variance = (actual - appetite) / appetite;
            if (Math.abs(variance) > SURPRISE_THRESHOLD) {
              prompt.append("- ").append(pitch.getTitle());
              prompt.append(": Expected ").append(pitch.getAppetiteDays()).append(" days");
              prompt.append(", took ").append(String.format("%.1f", actual / HOURS_PER_DAY)).append(" days");
              prompt.append(" (").append(variance > 0 ? "+" : "").append(String.format("%.0f%%", variance * 100)).append(")\n");
            }
          }
        }
        break;

      case FULL_SUMMARY:
        prompt.append("Write a complete cycle summary covering what was committed, shipped, cut, and any surprises.\n\n");
        prompt.append("Cycle: ").append(cycle.getName()).append("\n");
        prompt.append("Period: ").append(cycle.getStartDate()).append(" to ").append(cycle.getEndDate()).append("\n\n");
        // Include all pitch info
        for (Pitch pitch : pitches) {
          prompt.append("- ").append(pitch.getTitle());
          prompt.append(" (").append(pitch.getStatus()).append(")");
          Double actual = workLogRepository.getTotalHoursByPitchId(pitch.getId());
          if (actual != null) {
            prompt.append(" - ").append(String.format("%.1f", actual / HOURS_PER_DAY)).append("/").append(pitch.getAppetiteDays()).append(" days");
          }
          prompt.append("\n");
        }
        break;
    }

    prompt.append("\nWrite the summary in 2-4 paragraphs. Do not use bullet points unless listing items.");
    return prompt.toString();
  }

  private String cleanAIResponse(String response) {
    if (response == null) return "";
    // Remove any markdown code blocks or extra formatting
    return response.replaceAll("```[\\s\\S]*?```", "")
        .replaceAll("^#+\\s+", "")
        .trim();
  }

  // ============================================
  // Template-based Narrative Generation
  // ============================================

  private String generateTemplateNarrative(Cycle cycle, NarrativeType type) {
    List<Pitch> pitches = pitchRepository.findByCycleIdNotDeleted(cycle.getId());

    switch (type) {
      case WHAT_WE_BET:
        return generateWhatWeBetTemplate(cycle, pitches);
      case WHAT_SHIPPED:
        return generateWhatShippedTemplate(cycle, pitches);
      case WHAT_WE_CUT:
        return generateWhatWeCutTemplate(cycle, pitches);
      case SURPRISES:
        return generateSurprisesTemplate(cycle, pitches);
      case FULL_SUMMARY:
        return generateFullSummaryTemplate(cycle, pitches);
      default:
        return "No template available for this narrative type.";
    }
  }

  private String generateWhatWeBetTemplate(Cycle cycle, List<Pitch> pitches) {
    StringBuilder sb = new StringBuilder();
    
    List<Pitch> committed = pitches.stream()
        .filter(p -> p.getStatus() != PitchStatus.PENDING)
        .collect(Collectors.toList());

    if (committed.isEmpty()) {
      return "No pitches were committed in this cycle's betting meeting.";
    }

    int totalAppetite = committed.stream().mapToInt(Pitch::getAppetiteDays).sum();
    
    sb.append("In the betting meeting for ").append(cycle.getName()).append(", the team committed to ");
    sb.append(committed.size()).append(" pitch").append(committed.size() > 1 ? "es" : "");
    sb.append(" with a total appetite of ").append(totalAppetite).append(" days.\n\n");

    sb.append("The bets included:\n");
    for (Pitch pitch : committed) {
      sb.append("• **").append(pitch.getTitle()).append("** (").append(pitch.getAppetiteDays()).append(" days)");
      Optional<BettingDecision> decision = bettingDecisionRepository.findByPitchIdAndCycleId(pitch.getId(), cycle.getId());
      if (decision.isPresent() && decision.get().getReason() != null && !decision.get().getReason().isEmpty()) {
        sb.append(" - ").append(decision.get().getReason());
      }
      sb.append("\n");
    }

    return sb.toString();
  }

  private String generateWhatShippedTemplate(Cycle cycle, List<Pitch> pitches) {
    StringBuilder sb = new StringBuilder();
    
    List<Pitch> shipped = pitches.stream()
        .filter(p -> p.getStatus() == PitchStatus.DONE)
        .collect(Collectors.toList());

    if (shipped.isEmpty()) {
      return "No pitches were completed in this cycle yet.";
    }

    sb.append("The team successfully shipped ").append(shipped.size()).append(" pitch");
    sb.append(shipped.size() > 1 ? "es" : "").append(" in ").append(cycle.getName()).append(":\n\n");

    for (Pitch pitch : shipped) {
      sb.append("• **").append(pitch.getTitle()).append("**");
      Double actual = workLogRepository.getTotalHoursByPitchId(pitch.getId());
      if (actual != null) {
        double appetite = pitch.getAppetiteDays() * HOURS_PER_DAY;
        double variance = (actual - appetite) / appetite * 100;
        sb.append(" - Completed in ").append(String.format("%.1f", actual / HOURS_PER_DAY)).append(" days");
        sb.append(" (").append(variance > 0 ? "+" : "").append(String.format("%.0f%%", variance)).append(" vs appetite)");
      }
      sb.append("\n");
    }

    return sb.toString();
  }

  private String generateWhatWeCutTemplate(Cycle cycle, List<Pitch> pitches) {
    StringBuilder sb = new StringBuilder();
    
    List<Pitch> cut = pitches.stream()
        .filter(p -> p.getIsCircuitBreakerTriggered() || p.getStatus() == PitchStatus.CANCELLED)
        .collect(Collectors.toList());

    if (cut.isEmpty()) {
      return "No pitches were cut or cancelled in this cycle. All committed work proceeded as planned.";
    }

    sb.append("The following pitches were cut in ").append(cycle.getName()).append(":\n\n");

    for (Pitch pitch : cut) {
      sb.append("• **").append(pitch.getTitle()).append("**");
      if (pitch.getIsCircuitBreakerTriggered()) {
        sb.append(" - Circuit breaker triggered");
        if (pitch.getCircuitBreakerReason() != null && !pitch.getCircuitBreakerReason().isEmpty()) {
          sb.append(": ").append(pitch.getCircuitBreakerReason());
        }
      } else {
        sb.append(" - Cancelled");
      }
      sb.append("\n");
    }

    return sb.toString();
  }

  private String generateSurprisesTemplate(Cycle cycle, List<Pitch> pitches) {
    StringBuilder sb = new StringBuilder();
    
    List<Pitch> underBudget = new ArrayList<>();
    List<Pitch> overBudget = new ArrayList<>();

    for (Pitch pitch : pitches) {
      Double actual = workLogRepository.getTotalHoursByPitchId(pitch.getId());
      if (actual != null && actual > 0) {
        double appetite = pitch.getAppetiteDays() * HOURS_PER_DAY;
        double variance = (actual - appetite) / appetite;
        if (variance < -SURPRISE_THRESHOLD) {
          underBudget.add(pitch);
        } else if (variance > SURPRISE_THRESHOLD) {
          overBudget.add(pitch);
        }
      }
    }

    if (underBudget.isEmpty() && overBudget.isEmpty()) {
      return "No major surprises in this cycle - work proceeded largely as expected within the appetite tolerance.";
    }

    if (!overBudget.isEmpty()) {
      sb.append("**Took longer than expected:**\n");
      for (Pitch pitch : overBudget) {
        Double actual = workLogRepository.getTotalHoursByPitchId(pitch.getId());
        sb.append("• ").append(pitch.getTitle()).append(" - ");
        sb.append(String.format("%.1f", actual / HOURS_PER_DAY)).append(" days vs ");
        sb.append(pitch.getAppetiteDays()).append(" day appetite\n");
      }
      sb.append("\n");
    }

    if (!underBudget.isEmpty()) {
      sb.append("**Finished faster than expected:**\n");
      for (Pitch pitch : underBudget) {
        Double actual = workLogRepository.getTotalHoursByPitchId(pitch.getId());
        sb.append("• ").append(pitch.getTitle()).append(" - ");
        sb.append(String.format("%.1f", actual / HOURS_PER_DAY)).append(" days vs ");
        sb.append(pitch.getAppetiteDays()).append(" day appetite\n");
      }
    }

    return sb.toString();
  }

  private String generateFullSummaryTemplate(Cycle cycle, List<Pitch> pitches) {
    StringBuilder sb = new StringBuilder();
    
    sb.append("## Cycle Summary: ").append(cycle.getName()).append("\n\n");
    sb.append(generateWhatWeBetTemplate(cycle, pitches)).append("\n\n");
    sb.append(generateWhatShippedTemplate(cycle, pitches)).append("\n\n");
    sb.append(generateWhatWeCutTemplate(cycle, pitches)).append("\n\n");
    sb.append(generateSurprisesTemplate(cycle, pitches));
    
    return sb.toString();
  }

  // ============================================
  // Helpers
  // ============================================

  private boolean isAIEnabled() {
    return aiConfig.isAiRiskAnalysisEnabled() && chatLanguageModel != null;
  }

  private CycleNarrativeDTO toDTO(CycleNarrative entity) {
    return CycleNarrativeDTO.builder()
        .id(entity.getId())
        .cycleId(entity.getCycle().getId())
        .cycleName(entity.getCycle().getName())
        .narrativeType(entity.getNarrativeType())
        .content(entity.getContent())
        .isAiGenerated(entity.getIsAiGenerated())
        .aiModel(entity.getAiModel())
        .generatedAt(entity.getGeneratedAt())
        .generatedByUsername(entity.getGeneratedBy() != null ? entity.getGeneratedBy().getUsername() : null)
        .build();
  }
}
