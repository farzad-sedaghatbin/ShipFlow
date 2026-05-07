package com.github.farzadsedaghatbin.shipflow.dto.admin;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Preset risk calculation profiles for quick organization setup. Provides
 * predefined weight distributions optimized for different risk management
 * approaches.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskProfilesDTO {

  private List<RiskProfile> profiles;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class RiskProfile {
    private String name;
    private String displayName;
    private String description;
    private Integer budgetWeight;
    private Integer bugsWeight;
    private Integer scopeWeight;
    private Integer timeWeight;
  }

  /** Get all predefined risk profiles */
  public static RiskProfilesDTO getDefaultProfiles() {
    return RiskProfilesDTO.builder().profiles(List.of(
        RiskProfile.builder().name("balanced").displayName("Balanced")
            .description(
                "Balanced approach: Equal priority across all factors with slight emphasis on bugs")
            .budgetWeight(25).bugsWeight(30).scopeWeight(25).timeWeight(20).build(),
        RiskProfile.builder().name("conservative").displayName("Conservative")
            .description("Conservative approach: Emphasizes budget control and bug prevention")
            .budgetWeight(35).bugsWeight(35).scopeWeight(15).timeWeight(15).build(),
        RiskProfile.builder().name("aggressive").displayName("Aggressive")
            .description("Aggressive approach: Prioritizes scope delivery and timeline over budget")
            .budgetWeight(15).bugsWeight(25).scopeWeight(35).timeWeight(25).build(),
        RiskProfile.builder().name("quality_focused").displayName("Quality-Focused")
            .description("Quality-focused: Heavy emphasis on bug prevention and scope completion")
            .budgetWeight(15).bugsWeight(40).scopeWeight(30).timeWeight(15).build(),
        RiskProfile.builder().name("time_critical").displayName("Time-Critical")
            .description("Time-critical: Optimized for deadline-driven projects").budgetWeight(20)
            .bugsWeight(25).scopeWeight(20).timeWeight(35).build()))
        .build();
  }

  /** Get a specific profile by name */
  public static OrganizationSettingsDTO.RiskWeights getProfileWeights(String profileName) {
    return getDefaultProfiles().getProfiles().stream().filter(p -> p.getName().equalsIgnoreCase(profileName))
        .findFirst()
        .map(profile -> OrganizationSettingsDTO.RiskWeights.builder().budgetWeight(profile.getBudgetWeight())
            .bugsWeight(profile.getBugsWeight()).scopeWeight(profile.getScopeWeight())
            .timeWeight(profile.getTimeWeight()).build())
        .orElse(OrganizationSettingsDTO.RiskWeights.builder().budgetWeight(25).bugsWeight(30).scopeWeight(25)
            .timeWeight(20).build());
  }
}
