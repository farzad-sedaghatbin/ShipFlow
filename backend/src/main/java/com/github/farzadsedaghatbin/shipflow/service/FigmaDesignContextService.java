package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.service.mcp.FigmaMcpProvider;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Shared extraction of Figma design context from a pitch's wireframe links, used by every AI
 * feature that wants design grounding (task suggestions, test generation, ...).
 *
 * <p>Guard chain: wireframe links present → links contain a parseable Figma URL → the org has a
 * Figma access token configured → the Figma API returned usable content. Returns {@code null}
 * (never throws) when any guard fails so callers can degrade gracefully to a pitch-only prompt.
 */
@Service
@Slf4j
public class FigmaDesignContextService {

  private final OrganizationSettingsService settingsService;
  private final FigmaMcpProvider figmaMcpProvider;

  @Autowired
  public FigmaDesignContextService(OrganizationSettingsService settingsService,
      FigmaMcpProvider figmaMcpProvider) {
    this.settingsService = settingsService;
    this.figmaMcpProvider = figmaMcpProvider;
  }

  /** Extract a prompt-ready design context string for the pitch, or {@code null} if unavailable. */
  public String extractForPitch(Pitch pitch) {
    String wireframeLinks = pitch.getWireframeLinks();
    if (wireframeLinks == null || wireframeLinks.isBlank()) {
      return null;
    }

    List<FigmaMcpProvider.FigmaFileReference> figmaRefs =
        figmaMcpProvider.extractFigmaFileReferences(wireframeLinks);
    if (figmaRefs.isEmpty()) {
      return null;
    }

    String figmaToken = settingsService.getFigmaAccessToken();
    if (figmaToken == null || figmaToken.isBlank()) {
      log.debug("Figma URLs present on pitch '{}' but no org Figma token configured", pitch.getTitle());
      return null;
    }

    FigmaMcpProvider.FigmaFileReference fileRef = figmaRefs.get(0);
    FigmaMcpProvider.FigmaDesignContext context = figmaMcpProvider.getDesignContext(fileRef, figmaToken);

    if (context == null || !context.hasContent()) {
      return null;
    }

    return context.toPromptString();
  }
}
