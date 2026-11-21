package com.github.farzadsedaghatbin.shipflow.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpaWebConfigTest {

  @Test
  void unknownPagesFallBackToTheWebApp() {
    assertTrue(SpaWebConfig.servesSpaFallback("backlog"));
    assertTrue(SpaWebConfig.servesSpaFallback("pitches/55/hill-chart"));
  }

  @Test
  void apiPathsDoNotFallBackToTheWebApp() {
    assertFalse(SpaWebConfig.servesSpaFallback("api/nothing"));
  }

  @Test
  void wellKnownDiscoveryPathsDoNotFallBackToTheWebApp() {
    assertFalse(SpaWebConfig.servesSpaFallback(".well-known/oauth-authorization-server"));
    assertFalse(SpaWebConfig.servesSpaFallback(".well-known/oauth-protected-resource/mcp/sse"));
  }
}
