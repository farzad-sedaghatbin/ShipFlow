package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.entity.OrganizationSettings;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.TeamAssignment;
import com.github.farzadsedaghatbin.shipflow.repository.OrganizationSettingsRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for resolving capacity configuration with inheritance hierarchy.
 * 
 * Inheritance chain (most specific wins):
 * 1. TeamAssignment.hoursPerDayOverride (finest-grained, per person per team)
 * 2. Person.hoursPerDayOverride (person's default across all teams)
 * 3. Team.hoursPerDayOverride (team-level override)
 * 4. OrganizationSettings.defaultHoursPerDay (organization default)
 * 
 * For working days per week:
 * 1. Team.workingDaysPerWeekOverride (team-level override)
 * 2. OrganizationSettings.defaultWorkingDaysPerWeek (organization default)
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class CapacityConfigService {

  private static final double DEFAULT_HOURS_PER_DAY = 8.0;
  private static final int DEFAULT_WORKING_DAYS_PER_WEEK = 5;

  private final OrganizationSettingsRepository organizationSettingsRepository;

  /**
   * Get organization's default hours per day.
   */
  public double getOrganizationDefaultHoursPerDay() {
    return organizationSettingsRepository.findFirstByOrderByIdAsc()
        .map(OrganizationSettings::getDefaultHoursPerDay)
        .orElse(DEFAULT_HOURS_PER_DAY);
  }

  /**
   * Get organization's default working days per week.
   */
  public int getOrganizationDefaultWorkingDaysPerWeek() {
    return organizationSettingsRepository.findFirstByOrderByIdAsc()
        .map(OrganizationSettings::getDefaultWorkingDaysPerWeek)
        .orElse(DEFAULT_WORKING_DAYS_PER_WEEK);
  }

  /**
   * Get effective hours per day for a team assignment.
   * Resolution order: assignment → person → team → organization
   * 
   * @param assignment The team assignment
   * @return Effective hours per day with source info
   */
  public CapacityResolution getEffectiveHoursPerDay(TeamAssignment assignment) {
    // 1. Check assignment-level override (finest-grained)
    if (assignment.getHoursPerDayOverride() != null) {
      return new CapacityResolution(assignment.getHoursPerDayOverride(), "assignment");
    }

    // 2. Check person-level override
    Person person = assignment.getPerson();
    if (person != null && person.getHoursPerDayOverride() != null) {
      return new CapacityResolution(person.getHoursPerDayOverride(), "person");
    }

    // 3. Check team-level override
    Team team = assignment.getTeam();
    if (team != null && team.getHoursPerDayOverride() != null) {
      return new CapacityResolution(team.getHoursPerDayOverride(), "team");
    }

    // 4. Fall back to organization default
    double orgDefault = getOrganizationDefaultHoursPerDay();
    return new CapacityResolution(orgDefault, "organization");
  }

  /**
   * Get effective hours per day for a person (without team context).
   * Resolution order: person → organization
   * 
   * @param person The person
   * @return Effective hours per day with source info
   */
  public CapacityResolution getEffectiveHoursPerDay(Person person) {
    if (person.getHoursPerDayOverride() != null) {
      return new CapacityResolution(person.getHoursPerDayOverride(), "person");
    }
    return new CapacityResolution(getOrganizationDefaultHoursPerDay(), "organization");
  }

  /**
   * Get effective working days per week for a team.
   * Resolution order: team → organization
   * 
   * @param team The team
   * @return Effective working days per week with source info
   */
  public CapacityResolution getEffectiveWorkingDaysPerWeek(Team team) {
    if (team.getWorkingDaysPerWeekOverride() != null) {
      return new CapacityResolution(team.getWorkingDaysPerWeekOverride().doubleValue(), "team");
    }
    return new CapacityResolution(getOrganizationDefaultWorkingDaysPerWeek(), "organization");
  }

  /**
   * Calculate a person's total budget (in hours) for a pitch based on appetite days.
   * 
   * Budget = appetiteDays × effectiveHoursPerDay
   * 
   * This represents the maximum hours this person can work on the pitch
   * while respecting the appetite deadline (calendar constraint).
   * 
   * @param assignment The team assignment
   * @param appetiteDays The pitch appetite in days
   * @return PersonBudget with total hours and breakdown
   */
  public PersonBudget calculatePersonBudget(TeamAssignment assignment, int appetiteDays) {
    CapacityResolution resolution = getEffectiveHoursPerDay(assignment);
    double totalHours = appetiteDays * resolution.value();
    
    return PersonBudget.builder()
        .personId(assignment.getPerson().getId())
        .personName(assignment.getPerson().getName())
        .role(assignment.getRole())
        .hoursPerDay(resolution.value())
        .capacitySource(resolution.source())
        .appetiteDays(appetiteDays)
        .totalBudgetHours(totalHours)
        .build();
  }

  /**
   * Calculate total team budget (in hours) for a pitch.
   * 
   * Total budget = sum of all active member budgets
   *              = Σ(memberHoursPerDay × appetiteDays)
   * 
   * NOTE: Each member has appetiteDays of calendar time (parallel work).
   * The total budget is the sum of what each member can contribute.
   * 
   * Example: 3 members, 10-day appetite
   * - Member A: 8 hrs/day → 80 hrs budget
   * - Member B: 6 hrs/day → 60 hrs budget  
   * - Member C: 4 hrs/day → 40 hrs budget
   * - Total team budget: 180 hrs
   * 
   * @param team The team
   * @param appetiteDays The pitch appetite in days
   * @return TeamBudget with total and breakdown per member
   */
  public TeamBudget calculateTeamBudget(Team team, int appetiteDays) {
    List<TeamAssignment> activeAssignments = team.getAssignments().stream()
        .filter(a -> a.getIsActive() != null && a.getIsActive())
        .toList();

    List<PersonBudget> memberBudgets = activeAssignments.stream()
        .map(a -> calculatePersonBudget(a, appetiteDays))
        .toList();

    double totalBudgetHours = memberBudgets.stream()
        .mapToDouble(PersonBudget::getTotalBudgetHours)
        .sum();

    double totalDailyCapacity = memberBudgets.stream()
        .mapToDouble(PersonBudget::getHoursPerDay)
        .sum();

    // Determine capacity source for the team
    String capacitySource = determineTeamCapacitySource(memberBudgets);

    return TeamBudget.builder()
        .teamId(team.getId())
        .teamName(team.getName())
        .memberCount(activeAssignments.size())
        .appetiteDays(appetiteDays)
        .totalBudgetHours(totalBudgetHours)
        .totalDailyCapacityHours(totalDailyCapacity)
        .capacitySource(capacitySource)
        .memberBudgets(memberBudgets)
        .build();
  }

  /**
   * Calculate total team budget using working weeks instead of days.
   * Converts weeks to days using the team's effective working days per week.
   * 
   * @param team The team
   * @param appetiteWeeks The pitch appetite in weeks
   * @return TeamBudget with total and breakdown
   */
  public TeamBudget calculateTeamBudgetFromWeeks(Team team, int appetiteWeeks) {
    int workingDaysPerWeek = (int) getEffectiveWorkingDaysPerWeek(team).value();
    int appetiteDays = appetiteWeeks * workingDaysPerWeek;
    return calculateTeamBudget(team, appetiteDays);
  }

  /**
   * Convert hours to person-days based on hours per day.
   * 
   * @param hours Total hours
   * @param hoursPerDay Hours per day to use for conversion
   * @return Equivalent person-days
   */
  public double hoursToPersonDays(double hours, double hoursPerDay) {
    if (hoursPerDay <= 0) {
      return 0;
    }
    return hours / hoursPerDay;
  }

  /**
   * Calculate appetite hours for a pitch.
   * Uses team's capacity if available, otherwise organization default.
   * 
   * @param pitch The pitch
   * @return Appetite in hours
   */
  public double calculatePitchAppetiteHours(com.github.farzadsedaghatbin.shipflow.entity.Pitch pitch) {
    if (pitch == null || pitch.getAppetiteDays() == null) {
      return 0;
    }
    
    Team team = pitch.getTeam();
    if (team != null) {
      TeamBudget budget = calculateTeamBudget(team, pitch.getAppetiteDays());
      // If team has no active members, fall back to default calculation
      if (budget.getTotalBudgetHours() > 0) {
        return budget.getTotalBudgetHours();
      }
    }
    
    // No team or no active members, use organization default hours per day
    return pitch.getAppetiteDays() * getOrganizationDefaultHoursPerDay();
  }

  /**
   * Get effective hours per day for a pitch.
   * Uses team's capacity if available, otherwise organization default.
   * 
   * @param pitch The pitch
   * @return Effective hours per day for display/calculation
   */
  public double getEffectiveHoursPerDayForPitch(com.github.farzadsedaghatbin.shipflow.entity.Pitch pitch) {
    if (pitch == null) {
      return getOrganizationDefaultHoursPerDay();
    }
    
    Team team = pitch.getTeam();
    if (team != null && team.getHoursPerDayOverride() != null) {
      return team.getHoursPerDayOverride();
    }
    
    return getOrganizationDefaultHoursPerDay();
  }

  /**
   * Convert weeks to working days using organization default.
   * 
   * @param weeks Number of weeks
   * @return Number of working days
   */
  public int weeksToWorkingDays(int weeks) {
    return weeks * getOrganizationDefaultWorkingDaysPerWeek();
  }

  /**
   * Convert weeks to working days using team's effective setting.
   * 
   * @param team The team
   * @param weeks Number of weeks
   * @return Number of working days
   */
  public int weeksToWorkingDays(Team team, int weeks) {
    return weeks * (int) getEffectiveWorkingDaysPerWeek(team).value();
  }

  /**
   * Find the bottleneck member - the person closest to exhausting their budget.
   * 
   * @param memberBudgets List of member budgets with hours spent
   * @return The member with highest budget utilization percentage
   */
  public PersonBudget findBottleneckMember(List<PersonBudget> memberBudgets) {
    return memberBudgets.stream()
        .max((a, b) -> Double.compare(
            a.getHoursSpent() / a.getTotalBudgetHours(),
            b.getHoursSpent() / b.getTotalBudgetHours()))
        .orElse(null);
  }

  /**
   * Check if any team member has exceeded their individual budget.
   * 
   * @param memberBudgets List of member budgets with hours spent
   * @return True if any member is over budget
   */
  public boolean hasOverBudgetMember(List<PersonBudget> memberBudgets) {
    return memberBudgets.stream()
        .anyMatch(m -> m.getHoursSpent() > m.getTotalBudgetHours());
  }

  /**
   * Determine the capacity source for a team based on member configurations.
   * Returns "organization" if all use org defaults, "team" if team override applies,
   * or "mixed" if members have individual overrides.
   */
  private String determineTeamCapacitySource(List<PersonBudget> memberBudgets) {
    if (memberBudgets.isEmpty()) {
      return "organization";
    }

    long uniqueSources = memberBudgets.stream()
        .map(PersonBudget::getCapacitySource)
        .distinct()
        .count();

    if (uniqueSources == 1) {
      return memberBudgets.get(0).getCapacitySource();
    }
    return "mixed";
  }

  /**
   * Value record for capacity resolution result.
   */
  public record CapacityResolution(double value, String source) {}

  /**
   * Budget information for a single team member.
   */
  @lombok.Data
  @lombok.Builder
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class PersonBudget {
    private Long personId;
    private String personName;
    private com.github.farzadsedaghatbin.shipflow.entity.enums.TeamMemberRole role;
    private double hoursPerDay;
    private String capacitySource;
    private int appetiteDays;
    private double totalBudgetHours;
    private double hoursSpent;      // To be populated from work logs
    private double hoursRemaining;  // totalBudgetHours - hoursSpent
    private double utilizationPercentage; // (hoursSpent / totalBudgetHours) * 100
  }

  /**
   * Budget information for an entire team.
   */
  @lombok.Data
  @lombok.Builder
  @lombok.NoArgsConstructor
  @lombok.AllArgsConstructor
  public static class TeamBudget {
    private Long teamId;
    private String teamName;
    private int memberCount;
    private int appetiteDays;
    private double totalBudgetHours;
    private double totalDailyCapacityHours;
    private String capacitySource;
    private List<PersonBudget> memberBudgets;
    private double totalHoursSpent;     // Sum of all member hours spent
    private double totalHoursRemaining; // totalBudgetHours - totalHoursSpent
    private double utilizationPercentage; // (totalHoursSpent / totalBudgetHours) * 100
    private PersonBudget bottleneckMember; // Member closest to exhausting budget
  }
}
