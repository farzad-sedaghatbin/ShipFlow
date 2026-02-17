package com.github.farzadsedaghatbin.shipflow.service.wisearchitecture;

import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.*;
import com.github.farzadsedaghatbin.shipflow.entity.Epic;
import com.github.farzadsedaghatbin.shipflow.entity.Initiative;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.TeamAssignment;
import com.github.farzadsedaghatbin.shipflow.entity.github.GitHubRepository;
import com.github.farzadsedaghatbin.shipflow.exception.FeatureDisabledException;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.github.GitHubRepositoryRepository;
import com.github.farzadsedaghatbin.shipflow.service.OrganizationSettingsService;
import com.github.farzadsedaghatbin.shipflow.service.mcp.FigmaMcpProvider;
import com.github.farzadsedaghatbin.shipflow.service.mcp.GitHubMcpProvider;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final EpicRepository epicRepository;
    private final GitHubRepositoryRepository repositoryRepository;
    private final TechStackDetectorService techStackDetectorService;
    private final TechnicalSolutionGeneratorService solutionGeneratorService;
    private final WiseArchitectureConversationService conversationService;
    private final FigmaMcpProvider figmaMcpProvider;
    private final GitHubMcpProvider githubMcpProvider;

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
     * Note: Not read-only because it may clear cache and persist new detections.
     */
    @Transactional
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
        Map<Long, String> branchMap = request.getRepositoryBranches() != null 
            ? request.getRepositoryBranches() 
            : Map.of();
        
        for (GitHubRepository repo : repositories) {
            // Clear cache if force re-detection is requested
            if (Boolean.TRUE.equals(request.getForceRedetection())) {
                techStackDetectorService.clearCache(repo);
            }
            
            // Get custom branch for this repo, or use default
            String branch = branchMap.getOrDefault(repo.getId(), repo.getDefaultBranch());
            
            // Use MCP to get the file list with the specified branch
            List<String> fileList = getRepositoryFileList(repo, branch);
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
        
        // Extract roadmap context - epic, initiative, and related pitches (token-efficient: ~100-150 tokens)
        String roadmapContext = extractRoadmapContext(pitch);
        boolean hasRoadmapContext = roadmapContext != null && !roadmapContext.isBlank();
        log.debug("Roadmap context for pitch '{}': {}", pitch.getTitle(), hasRoadmapContext ? "available" : "not available");
        
        // Generate solution for each selected stack
        Map<TechStackType, StackSolutionDTO> solutions = new LinkedHashMap<>();
        int totalEstimatedHours = 0;
        boolean hasCodeContext = false;
        
        // Get branch map for custom branches
        Map<Long, String> branchMap = request.getRepositoryBranches() != null 
            ? request.getRepositoryBranches() 
            : Map.of();
        
        for (TechStackType stackType : request.getSelectedStacks()) {
            // Find the repository for this stack (simplified - use first matching)
            GitHubRepository repo = repositories.isEmpty() ? null : repositories.get(0);
            
            DetectedStackDTO stack = DetectedStackDTO.builder()
                .stackType(stackType)
                .repositoryId(repo != null ? repo.getId() : null)
                .repositoryName(repo != null ? repo.getFullName() : "Unknown")
                .build();
            
            // Get custom branch for this repo
            String branch = repo != null ? branchMap.getOrDefault(repo.getId(), repo.getDefaultBranch()) : null;
            
            // Get code context from repository (via MCP in real implementation)
            String codeContext = getCodeContext(repo, stackType, branch);
            List<String> existingServices = findExistingServices(repo, stackType);
            
            // Track if we have any code context
            if (codeContext != null && !codeContext.isEmpty() && 
                !codeContext.equals("// Code context will be populated via MCP integration")) {
                hasCodeContext = true;
            }
            
            StackSolutionDTO solution = solutionGeneratorService.generateStackSolution(
                pitch, stack, codeContext, existingServices, teamSkills, figmaContext, roadmapContext);
            
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
            WiseArchitectureResponseDTO.ContextSourcesDTO.create(hasCodeContext, hasTeamSkills, hasFigmaContext, hasRoadmapContext);
        
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
        
        log.info("Generated solution for pitch '{}' - {} hours estimated, appetite {}, context: code={}, team={}, figma={}, roadmap={}",
            pitch.getTitle(), totalEstimatedHours, appetitePassed ? "PASSED" : "EXCEEDED",
            hasCodeContext, hasTeamSkills, hasFigmaContext, hasRoadmapContext);
        
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
     * Uses GitHub MCP provider to fetch actual repository file structure.
     * 
     * @param repo The repository to get files from
     * @param branch The branch to use (if null, uses repo's default branch)
     */
    private List<String> getRepositoryFileList(GitHubRepository repo, String branch) {
        if (repo == null || repo.getFullName() == null) {
            log.debug("No repository provided for file listing");
            return List.of();
        }

        // Check if GitHub MCP is available
        if (!githubMcpProvider.isAvailable()) {
            log.debug("GitHub MCP not available, skipping repository file listing");
            // Return empty - don't provide fake data, let LLM work with pitch context
            return List.of();
        }

        String[] parts = repo.getFullName().split("/");
        if (parts.length != 2) {
            log.warn("Invalid repository full name format: {}", repo.getFullName());
            return List.of();
        }

        // Use provided branch, or fall back to repo's default, or "main"
        String effectiveBranch = branch != null && !branch.isBlank() 
            ? branch 
            : (repo.getDefaultBranch() != null ? repo.getDefaultBranch() : "main");

        Map<String, String> context = Map.of(
            "owner", parts[0],
            "repo", parts[1],
            "branch", effectiveBranch
        );

        log.info("Fetching file list for {} (branch: {}) via GitHub MCP", 
            repo.getFullName(), effectiveBranch);
        
        List<String> files = githubMcpProvider.listFiles(context);
        
        if (files.isEmpty()) {
            log.warn("No files retrieved from repository {} via MCP", repo.getFullName());
        } else {
            log.info("Retrieved {} files from repository {} via MCP", files.size(), repo.getFullName());
            log.debug("First 10 files: {}", 
                files.stream().limit(10).collect(java.util.stream.Collectors.joining(", ")));
        }
        
        return files;
    }

    /**
     * Get code context from repository via MCP.
     * Reads relevant source files based on tech stack type.
     * 
     * @param repo The repository to get code from
     * @param stackType The technology stack type to look for
     * @param branch The branch to use (if null, uses repo's default branch)
     */
    private String getCodeContext(GitHubRepository repo, TechStackType stackType, String branch) {
        if (repo == null || repo.getFullName() == null) {
            log.debug("No repository provided for code context");
            return "";
        }

        // Check if GitHub MCP is available
        if (!githubMcpProvider.isAvailable()) {
            log.debug("GitHub MCP not available for code context");
            // Return empty - LLM will generate based on pitch context and tech stack
            return "";
        }

        String[] parts = repo.getFullName().split("/");
        if (parts.length != 2) {
            return "// Invalid repository format";
        }

        // Use provided branch, or fall back to repo's default, or "main"
        String effectiveBranch = branch != null && !branch.isBlank() 
            ? branch 
            : (repo.getDefaultBranch() != null ? repo.getDefaultBranch() : "main");

        Map<String, String> context = Map.of(
            "owner", parts[0],
            "repo", parts[1],
            "branch", effectiveBranch
        );

        // Get file patterns based on stack type
        List<String> patterns = getFilePatterns(stackType);
        
        // OPTIMIZATION: Collect all files to read first, then batch read them in parallel
        List<String> filesToRead = new ArrayList<>();
        for (String pattern : patterns) {
            List<String> matchingFiles = githubMcpProvider.searchFiles(context, pattern);
            // Limit to first few matches to keep context manageable
            matchingFiles.stream().limit(3).forEach(filesToRead::add);
        }
        
        // Batch read files in parallel for better performance
        StringBuilder codeContext = new StringBuilder();
        if (!filesToRead.isEmpty()) {
            List<CompletableFuture<Void>> readFutures = filesToRead.stream()
                .distinct()
                .limit(10) // Cap at 10 files total
                .map(filePath -> CompletableFuture.runAsync(() -> {
                    githubMcpProvider.readFile(context, filePath).ifPresent(content -> {
                        // Truncate large files
                        String truncated = content.length() > 2000 
                            ? content.substring(0, 2000) + "\n// ... (truncated)" 
                            : content;
                        synchronized (codeContext) {
                            codeContext.append("// File: ").append(filePath).append("\n");
                            codeContext.append(truncated).append("\n\n");
                        }
                    });
                }))
                .toList();
            
            // Wait for all reads to complete
            CompletableFuture.allOf(readFutures.toArray(new CompletableFuture[0])).join();
        }

        String result = codeContext.toString();
        if (result.isBlank()) {
            return "// No code context retrieved from " + repo.getFullName();
        }
        
        log.debug("Retrieved code context ({} chars) for {} from {}", 
            result.length(), stackType, repo.getFullName());
        return result;
    }

    /**
     * Get file patterns to search based on tech stack type.
     */
    private List<String> getFilePatterns(TechStackType stackType) {
        return switch (stackType) {
            case BACKEND_JAVA -> List.of("*Service.java", "*Controller.java", "*Repository.java", "pom.xml");
            case BACKEND_NODE -> List.of("*service*.js", "*controller*.js", "package.json");
            case WEB_REACT -> List.of("*.tsx", "*.jsx", "package.json");
            case WEB_NEXTJS -> List.of("pages/*.tsx", "app/*.tsx", "package.json");
            case BACKEND_PYTHON -> List.of("*.py", "requirements.txt", "setup.py");
            case BACKEND_GO -> List.of("*.go", "go.mod");
            case MOBILE_KOTLIN -> List.of("*.kt", "build.gradle.kts");
            case MOBILE_SWIFT -> List.of("*.swift", "Package.swift");
            case MOBILE_FLUTTER -> List.of("*.dart", "pubspec.yaml");
            case MOBILE_REACT_NATIVE -> List.of("*.tsx", "*.jsx", "package.json");
            default -> List.of("*.java", "*.ts", "*.json");
        };
    }

    /**
     * Find existing services in the repository that can be reused.
     * Analyzes code structure via MCP to discover service classes.
     */
    private List<String> findExistingServices(GitHubRepository repo, TechStackType stackType) {
        if (repo == null || repo.getFullName() == null) {
            log.debug("No repository provided for service discovery");
            // Return empty - don't provide fake service names
            return List.of();
        }

        // Check if GitHub MCP is available
        if (!githubMcpProvider.isAvailable()) {
            log.debug("GitHub MCP not available, skipping service discovery");
            return List.of();
        }

        String[] parts = repo.getFullName().split("/");
        if (parts.length != 2) {
            return List.of();
        }

        Map<String, String> context = Map.of(
            "owner", parts[0],
            "repo", parts[1],
            "branch", repo.getDefaultBranch() != null ? repo.getDefaultBranch() : "main"
        );

        // Search for service-related files
        String servicePattern = getServicePattern(stackType);
        List<String> serviceFiles = githubMcpProvider.searchFiles(context, servicePattern);
        
        if (serviceFiles.isEmpty()) {
            log.debug("No service files found in repository for {}", stackType);
            return List.of();
        }

        // Extract service names from file paths
        List<String> services = serviceFiles.stream()
            .map(this::extractServiceName)
            .filter(Objects::nonNull)
            .distinct()
            .limit(10)
            .collect(Collectors.toList());

        log.debug("Found {} services in {} via MCP", services.size(), repo.getFullName());
        return services;
    }

    /**
     * Get search pattern for services based on stack type.
     */
    private String getServicePattern(TechStackType stackType) {
        return switch (stackType) {
            case BACKEND_JAVA -> "*Service.java";
            case BACKEND_NODE -> "*service*.js";
            case WEB_REACT, WEB_NEXTJS -> "*Service.ts";
            case MOBILE_KOTLIN -> "*Service.kt";
            default -> "*Service*";
        };
    }

    /**
     * Extract service name from file path.
     */
    private String extractServiceName(String filePath) {
        if (filePath == null) return null;
        String fileName = filePath.contains("/") 
            ? filePath.substring(filePath.lastIndexOf("/") + 1)
            : filePath;
        // Remove extension
        int dotIndex = fileName.lastIndexOf(".");
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
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

    /**
     * Extract roadmap context from the pitch's epic and initiative.
     * Returns a compact prompt-ready string for token efficiency (~100-150 tokens).
     * Includes epic name/description, initiative name, and related pitches.
     *
     * @param pitch the pitch to extract roadmap context from
     * @return roadmap context string, or null if pitch has no epic
     */
    private String extractRoadmapContext(Pitch pitch) {
        Epic epic = pitch.getEpic();
        if (epic == null) {
            log.debug("No epic assigned to pitch '{}'", pitch.getTitle());
            return null;
        }
        
        StringBuilder context = new StringBuilder();
        
        // Epic information
        context.append("Epic: ").append(epic.getName());
        if (epic.getStatus() != null) {
            context.append(" [").append(epic.getStatus()).append("]");
        }
        context.append("\n");
        
        if (epic.getDescription() != null && !epic.getDescription().isBlank()) {
            // Truncate long descriptions
            String desc = epic.getDescription();
            if (desc.length() > 200) {
                desc = desc.substring(0, 197) + "...";
            }
            context.append("Epic Goal: ").append(desc).append("\n");
        }
        
        // Initiative information (if present)
        Initiative initiative = epic.getInitiative();
        if (initiative != null) {
            context.append("Initiative: ").append(initiative.getName());
            if (initiative.getStatus() != null) {
                context.append(" [").append(initiative.getStatus()).append("]");
            }
            context.append("\n");
        }
        
        // Find related pitches in the same epic
        List<Pitch> siblingPitches = pitchRepository.findByEpicIdNotDeleted(epic.getId());
        List<Pitch> otherPitches = siblingPitches.stream()
            .filter(p -> !p.getId().equals(pitch.getId()))
            .collect(Collectors.toList());
        
        if (!otherPitches.isEmpty()) {
            context.append("Related pitches in epic:\n");
            for (Pitch sibling : otherPitches) {
                context.append("- ").append(sibling.getTitle());
                if (sibling.getStatus() != null) {
                    context.append(" [").append(sibling.getStatus()).append("]");
                }
                context.append("\n");
            }
        }
        
        String result = context.toString().trim();
        log.debug("Extracted roadmap context for pitch '{}': {} chars", pitch.getTitle(), result.length());
        return result;
    }

    // =====================================================================
    // ASYNC METHODS WITH PROGRESS CALLBACKS
    // =====================================================================

    /**
     * Detect tech stacks with progress callback for async execution.
     * Uses granular progress tracking with descriptive messages.
     * 
     * Progress phases:
     * - 0-10%: Initialization (loading pitch, validating repos)
     * - 10-55%: File listing for each repository
     * - 55-95%: Stack detection for each repository
     * - 95-100%: Finalization and summary
     */
    @Transactional
    public DetectStacksResponseDTO detectStacksWithProgress(
            DetectStacksRequestDTO request, 
            WiseArchitectureExecutor.ProgressCallback callback) {
        checkFeatureEnabled();
        
        log.info("Detecting stacks (async) for pitch {} in {} repositories", 
            request.getPitchId(), request.getRepositoryIds().size());
        
        // Phase 1: Initialization (0-10%)
        callback.onProgress(2, "Initializing stack detection...");
        
        Pitch pitch = pitchRepository.findByIdNotDeleted(request.getPitchId())
            .orElseThrow(() -> new ResourceNotFoundException("Pitch not found: " + request.getPitchId()));
        
        callback.onProgress(5, "Loading repository information...");
        
        List<GitHubRepository> repositories = new ArrayList<>();
        for (Long repoId : request.getRepositoryIds()) {
            repositoryRepository.findById(repoId).ifPresent(repositories::add);
        }
        
        if (repositories.isEmpty()) {
            callback.onProgress(100, "No repositories to scan");
            return DetectStacksResponseDTO.builder()
                .pitchId(pitch.getId())
                .pitchName(pitch.getTitle())
                .detectedStacks(List.of())
                .repositoriesScanned(0)
                .message("No valid repositories found")
                .build();
        }
        
        callback.onProgress(10, String.format("Found %d repositor%s to scan", 
            repositories.size(), repositories.size() == 1 ? "y" : "ies"));
        
        Map<Long, String> branchMap = request.getRepositoryBranches() != null 
            ? request.getRepositoryBranches() 
            : Map.of();
        
        int totalRepos = repositories.size();
        
        // Phase 2: File listing (10-55%) - Sequential for accurate progress
        Map<GitHubRepository, List<String>> repoFileLists = new LinkedHashMap<>();
        for (int i = 0; i < repositories.size(); i++) {
            GitHubRepository repo = repositories.get(i);
            int progressPercent = 10 + (int) ((i * 1.0 / totalRepos) * 45);
            callback.onProgress(progressPercent, 
                String.format("Listing files for repository %d/%d (%s)...", i + 1, totalRepos, repo.getName()));
            
            try {
                if (Boolean.TRUE.equals(request.getForceRedetection())) {
                    techStackDetectorService.clearCache(repo);
                }
                
                String branch = branchMap.getOrDefault(repo.getId(), repo.getDefaultBranch());
                List<String> fileList = getRepositoryFileList(repo, branch);
                repoFileLists.put(repo, fileList);
                
                callback.onProgress(progressPercent + (int) (45.0 / totalRepos), 
                    String.format("Listed %d files from %s", fileList.size(), repo.getName()));
            } catch (Exception e) {
                log.error("Error listing files from repository {}: {}", repo.getFullName(), e.getMessage());
                repoFileLists.put(repo, List.of());
            }
        }
        
        // Phase 3: Stack detection (55-95%) - Process in parallel for performance
        callback.onProgress(55, "Starting stack detection...");
        
        List<DetectedStackDTO> allStacks = Collections.synchronizedList(new ArrayList<>());
        ExecutorService parallelExecutor = Executors.newFixedThreadPool(Math.min(totalRepos, 4));
        
        try {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (int i = 0; i < repositories.size(); i++) {
                final int repoIndex = i;
                final GitHubRepository repo = repositories.get(i);
                final List<String> fileList = repoFileLists.getOrDefault(repo, List.of());
                
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    try {
                        List<DetectedStackDTO> repoStacks = techStackDetectorService.detectStacks(repo, fileList);
                        allStacks.addAll(repoStacks);
                        
                        int progressPercent = 55 + (int) ((repoIndex + 1.0) / totalRepos * 40);
                        String stackNames = repoStacks.stream()
                            .map(s -> s.getStackType().getDisplayName())
                            .collect(Collectors.joining(", "));
                        
                        if (repoStacks.isEmpty()) {
                            callback.onProgress(progressPercent, 
                                String.format("No stacks detected in %s (%d/%d)", repo.getName(), repoIndex + 1, totalRepos));
                        } else {
                            callback.onProgress(progressPercent, 
                                String.format("Detected %d stack%s: %s in %s (%d/%d)", 
                                    repoStacks.size(), repoStacks.size() == 1 ? "" : "s", 
                                    stackNames, repo.getName(), repoIndex + 1, totalRepos));
                        }
                    } catch (Exception e) {
                        log.error("Error detecting stacks in repository {}: {}", repo.getFullName(), e.getMessage());
                    }
                }, parallelExecutor);
                
                futures.add(future);
            }
            
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
        } finally {
            parallelExecutor.shutdown();
        }
        
        // Phase 4: Finalization (95-100%)
        callback.onProgress(95, "Finalizing detection results...");
        
        String stackSummary = allStacks.stream()
            .map(s -> s.getStackType().getDisplayName())
            .distinct()
            .collect(Collectors.joining(", "));
        
        String message = allStacks.isEmpty() 
            ? String.format("No tech stacks found in %d repositor%s", 
                repositories.size(), repositories.size() == 1 ? "y" : "ies")
            : String.format("Found %d tech stack%s: %s across %d repositor%s", 
                allStacks.size(), allStacks.size() == 1 ? "" : "s",
                stackSummary, repositories.size(), repositories.size() == 1 ? "y" : "ies");
        
        callback.onProgress(100, message);
        
        return DetectStacksResponseDTO.builder()
            .pitchId(pitch.getId())
            .pitchName(pitch.getTitle())
            .detectedStacks(new ArrayList<>(allStacks))
            .repositoriesScanned(repositories.size())
            .message(message)
            .build();
    }

    /**
     * Generate solution with progress callback for async execution.
     * Uses granular progress tracking with descriptive messages.
     * 
     * Progress phases:
     * - 0-10%: Initialization (loading pitch, repos)
     * - 10-20%: Context extraction (team skills, figma, roadmap)
     * - 20-85%: Solution generation for each stack
     * - 85-95%: Appetite analysis and reduced scope
     * - 95-100%: Finalization
     */
    @Transactional(readOnly = true)
    public WiseArchitectureResponseDTO analyzeWithProgress(
            WiseArchitectureRequestDTO request,
            WiseArchitectureExecutor.ProgressCallback callback) {
        checkFeatureEnabled();
        
        log.info("Generating solution (async) for pitch {} with {} stacks", 
            request.getPitchId(), request.getSelectedStacks().size());
        
        // Phase 1: Initialization (0-10%)
        callback.onProgress(2, "Initializing solution analysis...");
        
        Pitch pitch = pitchRepository.findByIdNotDeleted(request.getPitchId())
            .orElseThrow(() -> new ResourceNotFoundException("Pitch not found: " + request.getPitchId()));
        
        callback.onProgress(5, "Loading repository information...");
        
        List<GitHubRepository> repositories = new ArrayList<>();
        for (Long repoId : request.getRepositoryIds()) {
            repositoryRepository.findById(repoId).ifPresent(repositories::add);
        }
        
        callback.onProgress(10, String.format("Loaded %d repositor%s for analysis", 
            repositories.size(), repositories.size() == 1 ? "y" : "ies"));
        
        // Phase 2: Context extraction (10-20%)
        callback.onProgress(12, "Extracting team skills context...");
        
        String[] teamSkillsHolder = new String[1];
        String[] figmaContextHolder = new String[1];
        String[] roadmapContextHolder = new String[1];
        
        CompletableFuture<Void> teamSkillsFuture = CompletableFuture.runAsync(() ->
            teamSkillsHolder[0] = extractTeamSkills(pitch));
        CompletableFuture<Void> figmaFuture = CompletableFuture.runAsync(() ->
            figmaContextHolder[0] = extractFigmaContext(pitch));
        CompletableFuture<Void> roadmapFuture = CompletableFuture.runAsync(() ->
            roadmapContextHolder[0] = extractRoadmapContext(pitch));
        
        CompletableFuture.allOf(teamSkillsFuture, figmaFuture, roadmapFuture).join();
        
        String teamSkills = teamSkillsHolder[0];
        String figmaContext = figmaContextHolder[0];
        String roadmapContext = roadmapContextHolder[0];
        
        boolean hasTeamSkills = teamSkills != null && !teamSkills.isBlank();
        boolean hasFigmaContext = figmaContext != null && !figmaContext.isBlank();
        boolean hasRoadmapContext = roadmapContext != null && !roadmapContext.isBlank();
        
        List<String> contextSummary = new ArrayList<>();
        if (hasTeamSkills) contextSummary.add("team skills");
        if (hasFigmaContext) contextSummary.add("Figma designs");
        if (hasRoadmapContext) contextSummary.add("roadmap");
        
        String contextMsg = contextSummary.isEmpty() 
            ? "No additional context available" 
            : "Extracted context: " + String.join(", ", contextSummary);
        callback.onProgress(20, contextMsg);
        
        // Phase 3: Solution generation (20-85%)
        Map<TechStackType, StackSolutionDTO> solutions = new LinkedHashMap<>();
        int totalEstimatedHours = 0;
        boolean hasCodeContext = false;
        
        Map<Long, String> branchMap = request.getRepositoryBranches() != null 
            ? request.getRepositoryBranches() 
            : Map.of();
        
        int totalStacks = request.getSelectedStacks().size();
        int stackIndex = 0;
        
        for (TechStackType stackType : request.getSelectedStacks()) {
            stackIndex++;
            
            // Progress for this stack: 20% to 85% divided among stacks
            int progressStart = 20 + (int) (((stackIndex - 1) * 1.0) / totalStacks * 65);
            int progressEnd = 20 + (int) ((stackIndex * 1.0) / totalStacks * 65);
            
            callback.onProgress(progressStart, 
                String.format("Generating solution for %s (step %d/%d)...", 
                    stackType.getDisplayName(), stackIndex, totalStacks));
            
            GitHubRepository repo = repositories.isEmpty() ? null : repositories.get(0);
            
            DetectedStackDTO stack = DetectedStackDTO.builder()
                .stackType(stackType)
                .repositoryId(repo != null ? repo.getId() : null)
                .repositoryName(repo != null ? repo.getFullName() : "Unknown")
                .build();
            
            String branch = repo != null ? branchMap.getOrDefault(repo.getId(), repo.getDefaultBranch()) : null;
            
            // Sub-progress: Fetching code context
            callback.onProgress(progressStart + (progressEnd - progressStart) / 3, 
                String.format("Fetching code context for %s...", stackType.getDisplayName()));
            
            String codeContext = getCodeContext(repo, stackType, branch);
            List<String> existingServices = findExistingServices(repo, stackType);
            
            if (codeContext != null && !codeContext.isEmpty() && 
                !codeContext.equals("// Code context will be populated via MCP integration")) {
                hasCodeContext = true;
            }
            
            // Sub-progress: AI generation
            callback.onProgress(progressStart + (progressEnd - progressStart) * 2 / 3, 
                String.format("AI generating architecture for %s...", stackType.getDisplayName()));
            
            StackSolutionDTO solution = solutionGeneratorService.generateStackSolution(
                pitch, stack, codeContext, existingServices, teamSkills, figmaContext, roadmapContext);
            
            solutions.put(stackType, solution);
            totalEstimatedHours += solution.getEstimatedHours() != null ? solution.getEstimatedHours() : 0;
            
            callback.onProgress(progressEnd, 
                String.format("Completed %s solution (%d hours estimated)", 
                    stackType.getDisplayName(), solution.getEstimatedHours() != null ? solution.getEstimatedHours() : 0));
        }
        
        // Phase 4: Appetite analysis (85-95%)
        callback.onProgress(85, "Analyzing appetite and time estimates...");
        
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
        
        callback.onProgress(88, appetitePassed 
            ? String.format("Appetite check passed (%d/%d hours)", totalEstimatedHours, availableHours)
            : String.format("Appetite exceeded (%d/%d hours)", totalEstimatedHours, availableHours));
        
        WiseArchitectureResponseDTO.ReducedScopeDTO reducedScope = null;
        if (!appetitePassed) {
            callback.onProgress(90, "Analyzing appetite and reducing scope...");
            reducedScope = solutionGeneratorService.generateReducedScope(
                pitch, solutions, availableHours, totalEstimatedHours);
            callback.onProgress(95, "Generated scope reduction recommendations");
        } else {
            callback.onProgress(95, "No scope reduction needed");
        }
        
        // Phase 5: Finalization (95-100%)
        callback.onProgress(96, "Building response...");
        
        WiseArchitectureResponseDTO.ContextSourcesDTO contextSources = 
            WiseArchitectureResponseDTO.ContextSourcesDTO.create(hasCodeContext, hasTeamSkills, hasFigmaContext, hasRoadmapContext);
        
        callback.onProgress(98, "Creating conversation session...");
        
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
        
        String sessionId = conversationService.createSession(pitch, response);
        response.setSessionId(sessionId);
        
        String summaryMsg = String.format("Solution complete: %d stack%s, %d hours, appetite %s", 
            solutions.size(), solutions.size() == 1 ? "" : "s", 
            totalEstimatedHours, appetitePassed ? "passed" : "exceeded");
        callback.onProgress(100, summaryMsg);
        
        log.info("Generated solution for pitch '{}' - {} hours estimated, appetite {}, context: code={}, team={}, figma={}, roadmap={}",
            pitch.getTitle(), totalEstimatedHours, appetitePassed ? "PASSED" : "EXCEEDED",
            hasCodeContext, hasTeamSkills, hasFigmaContext, hasRoadmapContext);
        
        return response;
    }
}
