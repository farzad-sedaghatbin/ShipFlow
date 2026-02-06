package com.github.farzadsedaghatbin.shipflow.service.wisearchitecture;

import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.*;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.TeamAssignment;
import com.github.farzadsedaghatbin.shipflow.entity.github.GitHubRepository;
import com.github.farzadsedaghatbin.shipflow.exception.FeatureDisabledException;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.github.GitHubRepositoryRepository;
import com.github.farzadsedaghatbin.shipflow.service.OrganizationSettingsService;
import com.github.farzadsedaghatbin.shipflow.service.mcp.FigmaMcpProvider;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Main orchestration service for the Wise Architecture feature.
 * Coordinates stack detection, solution generation, and follow-up conversations.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WiseArchitectureService {

    private static final int HOURS_PER_DAY = 8;

    private final OrganizationSettingsService settingsService;
    private final PitchRepository pitchRepository;
    private final GitHubRepositoryRepository repositoryRepository;
    private final TechStackDetectorService techStackDetectorService;
    private final TechnicalSolutionGeneratorService solutionGeneratorService;
    private final WiseArchitectureConversationService conversationService;
    private final FigmaMcpProvider figmaMcpProvider;

    /**
     * Check if the Wise Architecture feature is enabled.
     *
     * @throws FeatureDisabledException if the feature is not enabled
     */
    public void checkFeatureEnabled() {
        var settings = settingsService.getSettings();
        if (settings.getEnableWiseArchitecture() == null || !settings.getEnableWiseArchitecture()) {
            throw new FeatureDisabledException("Wise Architecture feature is not enabled. " +
                "Please enable it in Organization Settings > Features.");
        }
        
        if (settings.getEnableAIFeatures() == null || !settings.getEnableAIFeatures()) {
            throw new FeatureDisabledException("AI features are disabled. " +
                "Please enable AI features in Organization Settings > Features.");
        }
    }

    /**
     * Detect technology stacks in the specified repositories.
     */
    @Transactional(readOnly = true)
    public DetectStacksResponseDTO detectStacks(DetectStacksRequestDTO request) {
        checkFeatureEnabled();
        
        log.info("Detecting stacks for pitch {} in {} repositories", 
            request.getPitchId(), request.getRepositoryIds().size());
        
        // Get pitch
        Pitch pitch = pitchRepository.findByIdNotDeleted(request.getPitchId())
            .orElseThrow(() -> new ResourceNotFoundException("Pitch not found: " + request.getPitchId()));
        
        // Get repositories
        List<GitHubRepository> repositories = new ArrayList<>();
        for (Long repoId : request.getRepositoryIds()) {
            repositoryRepository.findById(repoId).ifPresent(repositories::add);
        }
        
        if (repositories.isEmpty()) {
            return DetectStacksResponseDTO.builder()
                .pitchId(pitch.getId())
                .pitchName(pitch.getTitle())
                .detectedStacks(List.of())
                .repositoriesScanned(0)
                .message("No valid repositories found")
                .build();
        }
        
        // Detect stacks in each repository
        List<DetectedStackDTO> allStacks = new ArrayList<>();
        for (GitHubRepository repo : repositories) {
            // In a real implementation, this would use MCP to get the file list
            // For now, we simulate with an empty list (stack detection will need actual MCP integration)
            List<String> fileList = getRepositoryFileList(repo);
            List<DetectedStackDTO> repoStacks = techStackDetectorService.detectStacks(repo, fileList);
            allStacks.addAll(repoStacks);
        }
        
        String message = String.format("Found %d tech stack(s) across %d repositor%s", 
            allStacks.size(), 
            repositories.size(),
            repositories.size() == 1 ? "y" : "ies");
        
        return DetectStacksResponseDTO.builder()
            .pitchId(pitch.getId())
            .pitchName(pitch.getTitle())
            .detectedStacks(allStacks)
            .repositoriesScanned(repositories.size())
            .message(message)
            .build();
    }

    /**
     * Generate technical solution document for the specified pitch and stacks.
     */
    @Transactional(readOnly = true)
    public WiseArchitectureResponseDTO analyze(WiseArchitectureRequestDTO request) {
        checkFeatureEnabled();
        
        log.info("Generating solution for pitch {} with {} stacks", 
            request.getPitchId(), request.getSelectedStacks().size());
        
        // Get pitch
        Pitch pitch = pitchRepository.findByIdNotDeleted(request.getPitchId())
            .orElseThrow(() -> new ResourceNotFoundException("Pitch not found: " + request.getPitchId()));
        
        // Get repositories for context
        List<GitHubRepository> repositories = new ArrayList<>();
        for (Long repoId : request.getRepositoryIds()) {
            repositoryRepository.findById(repoId).ifPresent(repositories::add);
        }
        
        // Extract team skills (token-efficient: ~25-35 tokens)
        String teamSkills = extractTeamSkills(pitch);
        boolean hasTeamSkills = teamSkills != null && !teamSkills.isBlank();
        log.debug("Team skills for pitch '{}': {}", pitch.getTitle(), hasTeamSkills ? teamSkills : "not available");
        
        // Extract Figma design context (token-efficient: ~100-200 tokens)
        String figmaContext = extractFigmaContext(pitch);
        boolean hasFigmaContext = figmaContext != null && !figmaContext.isBlank();
        log.debug("Figma context for pitch '{}': {}", pitch.getTitle(), hasFigmaContext ? "available" : "not available");
        
        // Generate solution for each selected stack
        Map<TechStackType, StackSolutionDTO> solutions = new LinkedHashMap<>();
        int totalEstimatedHours = 0;
        boolean hasCodeContext = false;
        
        for (TechStackType stackType : request.getSelectedStacks()) {
            // Find the repository for this stack (simplified - use first matching)
            GitHubRepository repo = repositories.isEmpty() ? null : repositories.get(0);
            
            DetectedStackDTO stack = DetectedStackDTO.builder()
                .stackType(stackType)
                .repositoryId(repo != null ? repo.getId() : null)
                .repositoryName(repo != null ? repo.getFullName() : "Unknown")
                .build();
            
            // Get code context from repository (via MCP in real implementation)
            String codeContext = getCodeContext(repo, stackType);
            List<String> existingServices = findExistingServices(repo, stackType);
            
            // Track if we have any code context
            if (codeContext != null && !codeContext.isEmpty() && 
                !codeContext.equals("// Code context will be populated via MCP integration")) {
                hasCodeContext = true;
            }
            
            StackSolutionDTO solution = solutionGeneratorService.generateStackSolution(
                pitch, stack, codeContext, existingServices, teamSkills, figmaContext);
            
            solutions.put(stackType, solution);
            totalEstimatedHours += solution.getEstimatedHours() != null ? solution.getEstimatedHours() : 0;
        }
        
        // Calculate appetite
        int availableHours = (pitch.getAppetiteDays() != null ? pitch.getAppetiteDays() : 0) * HOURS_PER_DAY;
        boolean appetitePassed = totalEstimatedHours <= availableHours;
        
        WiseArchitectureResponseDTO.AppetiteCheckDTO appetiteCheck = WiseArchitectureResponseDTO.AppetiteCheckDTO.builder()
            .passed(appetitePassed)
            .availableHours(availableHours)
            .estimatedHours(totalEstimatedHours)
            .hoursByStack(calculateHoursByStack(solutions))
            .message(appetitePassed 
                ? String.format("✅ Solution fits within appetite (%d/%d hours)", totalEstimatedHours, availableHours)
                : String.format("⚠️ Solution exceeds appetite (%d/%d hours)", totalEstimatedHours, availableHours))
            .build();
        
        // Generate reduced scope if needed
        WiseArchitectureResponseDTO.ReducedScopeDTO reducedScope = null;
        if (!appetitePassed) {
            reducedScope = solutionGeneratorService.generateReducedScope(
                pitch, solutions, availableHours, totalEstimatedHours);
        }
        
        // Build context sources information
        WiseArchitectureResponseDTO.ContextSourcesDTO contextSources = 
            WiseArchitectureResponseDTO.ContextSourcesDTO.create(hasCodeContext, hasTeamSkills, hasFigmaContext);
        
        // Build response
        WiseArchitectureResponseDTO response = WiseArchitectureResponseDTO.builder()
            .pitchId(pitch.getId())
            .pitchName(pitch.getTitle())
            .solutions(solutions)
            .appetiteCheck(appetiteCheck)
            .totalEstimatedHours(totalEstimatedHours)
            .reducedScope(reducedScope)
            .contextSources(contextSources)
            .generatedAt(LocalDateTime.now())
            .build();
        
        // Create conversation session for follow-ups
        String sessionId = conversationService.createSession(pitch, response);
        response.setSessionId(sessionId);
        
        log.info("Generated solution for pitch '{}' - {} hours estimated, appetite {}, context: code={}, team={}, figma={}",
            pitch.getTitle(), totalEstimatedHours, appetitePassed ? "PASSED" : "EXCEEDED",
            hasCodeContext, hasTeamSkills, hasFigmaContext);
        
        return response;
    }

    /**
     * Handle a follow-up question about a generated solution.
     */
    public FollowUpResponseDTO handleFollowUp(FollowUpQuestionDTO request) {
        checkFeatureEnabled();
        return conversationService.handleFollowUp(request);
    }

    /**
     * Get repository file list via MCP.
     * This is a placeholder - actual implementation would use MCP client.
     */
    private List<String> getRepositoryFileList(GitHubRepository repo) {
        // TODO: Implement MCP integration to read actual file structure
        log.debug("Getting file list for repository: {} (MCP integration pending)", repo.getFullName());
        
        // Return simulated structure based on common patterns
        // In production, this would call MCP to get actual files
        return List.of(
            "pom.xml",
            "src/main/java/Application.java",
            "src/main/resources/application.properties",
            "package.json",
            "src/App.tsx",
            "src/index.tsx"
        );
    }

    /**
     * Get code context from repository via MCP.
     * This is a placeholder - actual implementation would use MCP client.
     */
    private String getCodeContext(GitHubRepository repo, TechStackType stackType) {
        // TODO: Implement MCP integration to read actual code
        log.debug("Getting code context for {} in {} (MCP integration pending)", 
            stackType, repo != null ? repo.getFullName() : "unknown");
        
        // Return placeholder
        return "// Code context will be populated via MCP integration";
    }

    /**
     * Find existing services in the repository that can be reused.
     * This is a placeholder - actual implementation would analyze code via MCP.
     */
    private List<String> findExistingServices(GitHubRepository repo, TechStackType stackType) {
        // TODO: Implement MCP integration to discover services
        log.debug("Finding existing services for {} in {} (MCP integration pending)", 
            stackType, repo != null ? repo.getFullName() : "unknown");
        
        // Return common service patterns based on stack type
        return switch (stackType) {
            case BACKEND_JAVA -> List.of("UserService", "AuthService", "BaseRepository");
            case BACKEND_NODE -> List.of("userService", "authMiddleware", "databaseHelper");
            case WEB_REACT, WEB_NEXTJS -> List.of("useAuth hook", "ApiService", "ThemeContext");
            case MOBILE_KOTLIN -> List.of("NetworkModule", "UserRepository", "PreferencesManager");
            default -> List.of();
        };
    }

    /**
     * Calculate hours breakdown by stack type.
     */
    private Map<TechStackType, Integer> calculateHoursByStack(Map<TechStackType, StackSolutionDTO> solutions) {
        Map<TechStackType, Integer> result = new LinkedHashMap<>();
        for (Map.Entry<TechStackType, StackSolutionDTO> entry : solutions.entrySet()) {
            result.put(entry.getKey(), 
                entry.getValue().getEstimatedHours() != null ? entry.getValue().getEstimatedHours() : 0);
        }
        return result;
    }

    /**
     * Extract aggregated unique skills from the team assigned to the pitch.
     * Returns a compact comma-separated string for token efficiency (~25-35 tokens).
     *
     * @param pitch the pitch with assigned team
     * @return unique skills string, or null if no team/skills available
     */
    private String extractTeamSkills(Pitch pitch) {
        Team team = pitch.getTeam();
        if (team == null) {
            log.debug("No team assigned to pitch '{}'", pitch.getTitle());
            return null;
        }
        
        List<TeamAssignment> assignments = team.getAssignments();
        if (assignments == null || assignments.isEmpty()) {
            log.debug("No team assignments for pitch '{}'", pitch.getTitle());
            return null;
        }
        
        // Extract unique skills from all active team members
        Set<String> uniqueSkills = new LinkedHashSet<>();
        
        for (TeamAssignment assignment : assignments) {
            if (Boolean.TRUE.equals(assignment.getIsActive()) && assignment.getPerson() != null) {
                Person person = assignment.getPerson();
                String skills = person.getSkills();
                if (skills != null && !skills.isBlank()) {
                    // Skills are stored as comma-separated or newline-separated
                    Arrays.stream(skills.split("[,\\n;|]"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty() && s.length() < 50) // Filter out very long entries
                        .forEach(uniqueSkills::add);
                }
            }
        }
        
        if (uniqueSkills.isEmpty()) {
            log.debug("No skills found for team members on pitch '{}'", pitch.getTitle());
            return null;
        }
        
        // Return compact comma-separated string (token-efficient)
        String result = String.join(", ", uniqueSkills);
        log.debug("Extracted {} unique skills for pitch '{}': {}", uniqueSkills.size(), pitch.getTitle(), result);
        return result;
    }

    /**
     * Extract Figma design context from the pitch's wireframe links.
     * Returns a compact prompt-ready string for token efficiency (~100-200 tokens).
     *
     * @param pitch the pitch with wireframe links
     * @return Figma context string, or null if no Figma links or MCP not available
     */
    private String extractFigmaContext(Pitch pitch) {
        String wireframeLinks = pitch.getWireframeLinks();
        if (wireframeLinks == null || wireframeLinks.isBlank()) {
            log.debug("No wireframe links for pitch '{}'", pitch.getTitle());
            return null;
        }
        
        // Extract Figma file keys from wireframe links
        List<String> figmaFileKeys = figmaMcpProvider.extractFigmaFileKeys(wireframeLinks);
        if (figmaFileKeys.isEmpty()) {
            log.debug("No Figma URLs found in wireframe links for pitch '{}'", pitch.getTitle());
            return null;
        }
        
        // Get the organization's Figma access token
        String figmaToken = settingsService.getFigmaAccessToken();
        
        if (figmaToken == null || figmaToken.isBlank()) {
            log.debug("No Figma access token configured - cannot fetch design context");
            return null;
        }
        
        // Get design context for the first Figma file (to keep token usage reasonable)
        String fileKey = figmaFileKeys.get(0);
        FigmaMcpProvider.FigmaDesignContext context = figmaMcpProvider.getDesignContext(fileKey, figmaToken);
        
        if (context == null || !context.hasContent()) {
            log.debug("No design context retrieved from Figma for pitch '{}'", pitch.getTitle());
            return null;
        }
        
        return context.toPromptString();
    }
}
