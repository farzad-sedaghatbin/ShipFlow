package com.github.farzadsedaghatbin.shipflow.service.wisedesigner;

import com.github.farzadsedaghatbin.shipflow.dto.wisedesigner.*;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.WiseDesignerArtifact;
import com.github.farzadsedaghatbin.shipflow.exception.FeatureDisabledException;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WiseDesignerArtifactRepository;
import com.github.farzadsedaghatbin.shipflow.service.OrganizationSettingsService;
import com.github.farzadsedaghatbin.shipflow.service.wisearchitecture.WiseArchitectureConversationService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core service for the Wise Designer feature.
 *
 * <p>Generates self-contained HTML UI prototypes from pitch context using the configured LLM
 * provider. The resulting HTML (Tailwind CDN, inline JS, multi-screen navigation) can be rendered
 * directly inside a sandboxed {@code <iframe>} on the pitch detail page.
 *
 * <p>Optionally accepts a Wise Architecture session ID to align the UI prototype with the planned
 * technical architecture (component list, API contracts).
 *
 * <p>Iteration is supported: each call to {@link #iterate} creates a new artifact row with the
 * previous HTML as context, so the full design history is preserved.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WiseDesignerService {

  private final OrganizationSettingsService settingsService;
  private final PitchRepository pitchRepository;
  private final WiseDesignerArtifactRepository artifactRepository;
  private final WiseArchitectureConversationService conversationService;

  @Autowired(required = false)
  private ChatLanguageModel chatLanguageModel;

  // ──────────────────────────────────────────────────────────────────────────
  // Feature check
  // ──────────────────────────────────────────────────────────────────────────

  public void checkFeatureEnabled() {
    var settings = settingsService.getSettings();
    if (settings.getEnableAIFeatures() == null || !settings.getEnableAIFeatures()) {
      throw new FeatureDisabledException(
          "AI features are disabled. Enable them in Organization Settings > Features.");
    }
    if (settings.getEnableWiseDesigner() == null || !settings.getEnableWiseDesigner()) {
      throw new FeatureDisabledException(
          "Wise Designer is not enabled. Enable it in Organization Settings > Features.");
    }
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Generate
  // ──────────────────────────────────────────────────────────────────────────

  @Transactional
  public WiseDesignerResponseDTO generate(WiseDesignerRequestDTO request, Long userId) {
    checkFeatureEnabled();

    Pitch pitch = pitchRepository.findByIdNotDeleted(request.getPitchId())
        .orElseThrow(() -> new ResourceNotFoundException("Pitch not found: " + request.getPitchId()));

    // Resolve optional Wise Architecture context
    String archContext = resolveArchContext(request.getWiseArchSessionId());
    boolean usedArchContext = archContext != null;

    // Build the generation prompt
    String prompt = buildGenerationPrompt(pitch, archContext, request.getAdditionalContext());

    log.info("Generating UI prototype for pitch '{}' (arch context: {})", pitch.getTitle(), usedArchContext);

    String htmlContent = callLlm(prompt);
    String description = extractDescription(pitch, request.getAdditionalContext(), false);
    String sessionId = UUID.randomUUID().toString();

    WiseDesignerArtifact artifact = WiseDesignerArtifact.builder()
        .pitchId(pitch.getId())
        .htmlContent(htmlContent)
        .description(description)
        .sessionId(sessionId)
        .wiseArchSessionId(request.getWiseArchSessionId())
        .createdByUserId(userId)
        .build();

    artifact = artifactRepository.save(artifact);

    log.info("Saved Wise Designer artifact {} for pitch '{}' ({} chars)",
        artifact.getId(), pitch.getTitle(), htmlContent.length());

    return toResponseDTO(artifact, pitch, usedArchContext);
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Iterate
  // ──────────────────────────────────────────────────────────────────────────

  @Transactional
  public WiseDesignerResponseDTO iterate(WiseDesignerIterateRequestDTO request, Long userId) {
    checkFeatureEnabled();

    WiseDesignerArtifact previous = artifactRepository.findById(request.getArtifactId())
        .orElseThrow(() -> new ResourceNotFoundException("Artifact not found: " + request.getArtifactId()));

    Pitch pitch = pitchRepository.findByIdNotDeleted(previous.getPitchId())
        .orElseThrow(() -> new ResourceNotFoundException("Pitch not found: " + previous.getPitchId()));

    String prompt = buildIterationPrompt(pitch, previous.getHtmlContent(), request.getInstruction());

    log.info("Iterating Wise Designer artifact {} for pitch '{}': {}",
        previous.getId(), pitch.getTitle(), request.getInstruction());

    String htmlContent = callLlm(prompt);
    String sessionId = UUID.randomUUID().toString();
    boolean usedArchContext = previous.getWiseArchSessionId() != null;

    WiseDesignerArtifact artifact = WiseDesignerArtifact.builder()
        .pitchId(pitch.getId())
        .htmlContent(htmlContent)
        .description("Iteration: " + request.getInstruction())
        .sessionId(sessionId)
        .wiseArchSessionId(previous.getWiseArchSessionId())
        .createdByUserId(userId)
        .build();

    artifact = artifactRepository.save(artifact);

    return toResponseDTO(artifact, pitch, usedArchContext);
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Query
  // ──────────────────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<WiseDesignerArtifactSummaryDTO> getArtifactsForPitch(Long pitchId) {
    return artifactRepository.findByPitchIdOrderByCreatedAtDesc(pitchId)
        .stream()
        .map(a -> WiseDesignerArtifactSummaryDTO.builder()
            .artifactId(a.getId())
            .pitchId(a.getPitchId())
            .description(a.getDescription())
            .sessionId(a.getSessionId())
            .usedArchContext(a.getWiseArchSessionId() != null)
            .createdAt(a.getCreatedAt())
            .build())
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public WiseDesignerResponseDTO getArtifact(Long artifactId) {
    WiseDesignerArtifact artifact = artifactRepository.findById(artifactId)
        .orElseThrow(() -> new ResourceNotFoundException("Artifact not found: " + artifactId));
    Pitch pitch = pitchRepository.findByIdNotDeleted(artifact.getPitchId())
        .orElseThrow(() -> new ResourceNotFoundException("Pitch not found: " + artifact.getPitchId()));
    return toResponseDTO(artifact, pitch, artifact.getWiseArchSessionId() != null);
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Prompt building
  // ──────────────────────────────────────────────────────────────────────────

  private String buildGenerationPrompt(Pitch pitch, String archContext, String additionalContext) {
    StringBuilder sb = new StringBuilder();
    sb.append("You are an expert UI/UX designer and frontend engineer. ");
    sb.append("Generate a complete, self-contained HTML UI prototype for the following product pitch.\n\n");

    sb.append("## Pitch\n");
    sb.append("**Title**: ").append(pitch.getTitle()).append("\n");
    if (pitch.getProblemStatement() != null && !pitch.getProblemStatement().isBlank()) {
      sb.append("**Problem**: ").append(pitch.getProblemStatement()).append("\n");
    }
    if (pitch.getSolution() != null && !pitch.getSolution().isBlank()) {
      sb.append("**Solution**: ").append(pitch.getSolution()).append("\n");
    }
    if (pitch.getAppetiteDays() != null) {
      sb.append("**Appetite**: ").append(pitch.getAppetiteDays()).append(" days\n");
    }

    if (archContext != null) {
      sb.append("\n## Architecture Context (from Wise Architecture)\n");
      sb.append(archContext).append("\n");
    }

    if (additionalContext != null && !additionalContext.isBlank()) {
      sb.append("\n## Additional Design Hints\n");
      sb.append(additionalContext).append("\n");
    }

    sb.append("\n## Output Requirements\n");
    sb.append("Return ONLY a single complete HTML document. No markdown fences, no explanation.\n");
    sb.append("The document must:\n");
    sb.append("- Include Tailwind CSS via CDN: <script src=\"https://cdn.tailwindcss.com\"></script>\n");
    sb.append("- Be fully self-contained — no external images, no external fonts (use system fonts)\n");
    sb.append("- Include multiple screens/views navigable via a tab bar or sidebar (at least 3 screens)\n");
    sb.append("- Use realistic placeholder data that matches the pitch domain\n");
    sb.append("- Be responsive (works on mobile 375px and desktop 1280px)\n");
    sb.append("- Include interactive elements: buttons show hover states, nav switches views with JS\n");
    sb.append("- Use a clean, modern design with a consistent colour palette derived from the pitch\n");
    sb.append("- Show all major user flows described in the pitch solution\n");
    sb.append("- Include a small title bar at the top showing the pitch name and a 'Prototype' badge\n");

    return sb.toString();
  }

  private String buildIterationPrompt(Pitch pitch, String currentHtml, String instruction) {
    StringBuilder sb = new StringBuilder();
    sb.append("You are an expert UI/UX designer and frontend engineer. ");
    sb.append("Modify the following HTML UI prototype according to the instruction.\n\n");

    sb.append("## Pitch\n**Title**: ").append(pitch.getTitle()).append("\n\n");

    sb.append("## Instruction\n");
    sb.append(instruction).append("\n\n");

    sb.append("## Current HTML Prototype\n");
    sb.append(currentHtml).append("\n\n");

    sb.append("## Output Requirements\n");
    sb.append("Return ONLY the complete modified HTML document. ");
    sb.append("Preserve all existing screens and functionality. ");
    sb.append("Only apply the changes described in the instruction. ");
    sb.append("No markdown fences, no explanation — just the raw HTML.\n");

    return sb.toString();
  }

  // ──────────────────────────────────────────────────────────────────────────
  // LLM call
  // ──────────────────────────────────────────────────────────────────────────

  private String callLlm(String prompt) {
    if (chatLanguageModel == null) {
      log.warn("No LLM provider configured — returning placeholder prototype");
      return buildPlaceholderPrototype();
    }
    try {
      String raw = chatLanguageModel.generate(prompt);
      // Strip markdown code fences if the model wraps the output anyway
      return stripCodeFences(raw);
    } catch (Exception e) {
      log.error("LLM call failed in Wise Designer: {}", e.getMessage(), e);
      throw new RuntimeException("Design generation failed: " + e.getMessage(), e);
    }
  }

  /** Remove ```html ... ``` wrappers that some models add despite instructions. */
  private String stripCodeFences(String raw) {
    if (raw == null) return buildPlaceholderPrototype();
    String trimmed = raw.trim();
    if (trimmed.startsWith("```")) {
      int firstNewline = trimmed.indexOf('\n');
      if (firstNewline > 0) {
        trimmed = trimmed.substring(firstNewline + 1);
      }
      if (trimmed.endsWith("```")) {
        trimmed = trimmed.substring(0, trimmed.length() - 3).trim();
      }
    }
    return trimmed.isBlank() ? buildPlaceholderPrototype() : trimmed;
  }

  // ──────────────────────────────────────────────────────────────────────────
  // Helpers
  // ──────────────────────────────────────────────────────────────────────────

  private String resolveArchContext(String sessionId) {
    if (sessionId == null || sessionId.isBlank()) return null;
    try {
      var session = conversationService.getSession(sessionId);
      if (session == null) return null;

      var solution = session.getGeneratedSolution();
      if (solution == null || solution.getSolutions() == null) return null;

      StringBuilder ctx = new StringBuilder();
      solution.getSolutions().forEach((stackType, stack) -> {
        ctx.append("Stack: ").append(stackType.getDisplayName()).append("\n");
        if (stack.getArchitectureDetail() != null
            && stack.getArchitectureDetail().getApiContracts() != null) {
          stack.getArchitectureDetail().getApiContracts().forEach(api ->
              ctx.append("  API: ").append(api.getMethod()).append(" ").append(api.getEndpoint())
                 .append(api.getDescription() != null ? " — " + api.getDescription() : "")
                 .append("\n"));
        }
        if (stack.getImplementationSteps() != null) {
          stack.getImplementationSteps().stream().limit(5).forEach(step -> {
            String title = step.getTitle() != null ? step.getTitle() : "";
            ctx.append("  Step: ").append(title).append("\n");
          });
        }
      });
      String result = ctx.toString().trim();
      return result.isBlank() ? null : result;
    } catch (Exception e) {
      log.warn("Could not resolve Wise Architecture context for session {}: {}", sessionId, e.getMessage());
      return null;
    }
  }

  private String extractDescription(Pitch pitch, String additionalContext, boolean isIteration) {
    String base = "UI prototype for \"" + pitch.getTitle() + "\"";
    if (additionalContext != null && !additionalContext.isBlank()) {
      String hint = additionalContext.length() > 60
          ? additionalContext.substring(0, 57) + "..."
          : additionalContext;
      return base + " — " + hint;
    }
    return base;
  }

  private WiseDesignerResponseDTO toResponseDTO(WiseDesignerArtifact artifact, Pitch pitch, boolean usedArchContext) {
    return WiseDesignerResponseDTO.builder()
        .artifactId(artifact.getId())
        .pitchId(pitch.getId())
        .pitchName(pitch.getTitle())
        .htmlContent(artifact.getHtmlContent())
        .description(artifact.getDescription())
        .sessionId(artifact.getSessionId())
        .usedArchContext(usedArchContext)
        .generatedAt(artifact.getCreatedAt())
        .build();
  }

  /** Placeholder shown when no LLM is configured (dev / test environments). */
  private String buildPlaceholderPrototype() {
    return "<!DOCTYPE html><html lang=\"en\">"
        + "<head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">"
        + "<script src=\"https://cdn.tailwindcss.com\"></script><title>Prototype</title></head>"
        + "<body class=\"bg-gray-50 flex items-center justify-center min-h-screen\">"
        + "<div class=\"text-center p-8\">"
        + "<div class=\"text-4xl mb-4\">🎨</div>"
        + "<h1 class=\"text-2xl font-bold text-gray-800 mb-2\">No LLM Configured</h1>"
        + "<p class=\"text-gray-500\">Configure an AI provider in Organisation Settings to generate UI prototypes.</p>"
        + "</div></body></html>";
  }
}
