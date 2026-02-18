package com.github.farzadsedaghatbin.shipflow.service.wisearchitecture;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.*;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for generating technical solution documents using LLM.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TechnicalSolutionGeneratorService {

    private final ObjectMapper objectMapper;
    
    @Autowired(required = false)
    private ChatLanguageModel chatLanguageModel;

    /**
     * Generate a technical solution for a specific technology stack.
     *
     * @param pitch the pitch to generate solution for
     * @param stack the detected technology stack
     * @param codeContext relevant code snippets from the repository
     * @param existingServices list of existing services found in the codebase
     * @param teamSkills aggregated unique skills of team members (optional, can be null)
     * @param figmaContext design context from Figma (optional, can be null)
     * @param roadmapContext roadmap context including epic, initiative, and related pitches (optional, can be null)
     * @param projectConventions analysis of project conventions from pre-pass (optional, can be null)
     * @param previousStacksSummary summary of previously generated stacks' API contracts for cross-stack awareness (optional)
     * @return the generated stack solution
     */
    public StackSolutionDTO generateStackSolution(
            Pitch pitch,
            DetectedStackDTO stack,
            String codeContext,
            List<String> existingServices,
            String teamSkills,
            String figmaContext,
            String roadmapContext,
            String projectConventions,
            String previousStacksSummary) {
        
        if (chatLanguageModel == null) {
            log.warn("ChatLanguageModel not available, returning placeholder solution");
            return createPlaceholderSolution(stack);
        }

        String prompt = buildSolutionPrompt(pitch, stack, codeContext, existingServices, teamSkills, figmaContext, roadmapContext, projectConventions, previousStacksSummary);
        
        try {
            log.info("Generating solution for {} in pitch '{}'", stack.getStackType(), pitch.getTitle());
            String response = chatLanguageModel.generate(prompt);
            return parseSolutionResponse(response, stack);
        } catch (Exception e) {
            log.error("Error generating solution for {}: {}", stack.getStackType(), e.getMessage(), e);
            return createErrorSolution(stack, e.getMessage());
        }
    }

    /**
     * Generate a reduced scope solution that fits within the appetite.
     */
    public WiseArchitectureResponseDTO.ReducedScopeDTO generateReducedScope(
            Pitch pitch,
            Map<TechStackType, StackSolutionDTO> solutions,
            int availableHours,
            int totalEstimatedHours) {
        
        if (chatLanguageModel == null) {
            return createPlaceholderReducedScope(solutions, availableHours);
        }

        String prompt = buildReducedScopePrompt(pitch, solutions, availableHours, totalEstimatedHours);
        
        try {
            log.info("Generating reduced scope for pitch '{}' (need {} hours, have {} hours)", 
                pitch.getTitle(), totalEstimatedHours, availableHours);
            String response = chatLanguageModel.generate(prompt);
            return parseReducedScopeResponse(response, availableHours);
        } catch (Exception e) {
            log.error("Error generating reduced scope: {}", e.getMessage(), e);
            return createPlaceholderReducedScope(solutions, availableHours);
        }
    }

    /**
     * Analyze project conventions from code context.
     * Lightweight LLM call that feeds into all stack solution prompts.
     */
    public String analyzeProjectConventions(String codeContext) {
        if (chatLanguageModel == null || codeContext == null || codeContext.isBlank()) {
            return "";
        }
        
        String prompt = """
            Analyze the following code samples and return a concise summary of project conventions (max 300 words).
            Cover:
            1. Directory/package structure pattern (e.g. "controller → service → repository" layers)
            2. Naming conventions (e.g. suffix patterns like *Service, *Controller, *DTO)
            3. Design patterns used (e.g. Builder, Repository, Factory)
            4. Base classes or interfaces to extend
            5. Shared utilities or helpers available
            6. Configuration approach (e.g. application.properties, @Value, @ConfigurationProperties)
            
            Return ONLY the conventions summary as plain text, no JSON, no markdown headings.
            
            ## Code Samples
            """ + codeContext;
        
        try {
            log.debug("Analyzing project conventions from {} chars of code", codeContext.length());
            return chatLanguageModel.generate(prompt);
        } catch (Exception e) {
            log.warn("Failed to analyze project conventions: {}", e.getMessage());
            return "";
        }
    }

    private String buildSolutionPrompt(
            Pitch pitch,
            DetectedStackDTO stack,
            String codeContext,
            List<String> existingServices,
            String teamSkills,
            String figmaContext,
            String roadmapContext,
            String projectConventions,
            String previousStacksSummary) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a senior software architect producing an actionable implementation plan — not generic advice.\n");
        prompt.append("Every recommendation MUST be specific: real file paths, real method signatures, real endpoint definitions.\n");
        prompt.append("Do NOT produce vague descriptions like 'implement a modular architecture'. Instead, list exact components, endpoints, and data models.\n\n");
        
        prompt.append("## Pitch Information\n");
        prompt.append("Title: ").append(pitch.getTitle()).append("\n");
        prompt.append("Problem Statement: ").append(nullSafe(pitch.getProblemStatement())).append("\n");
        prompt.append("Proposed Solution: ").append(nullSafe(pitch.getSolution())).append("\n");
        prompt.append("Rabbit Holes (avoid): ").append(nullSafe(pitch.getRabbitHoles())).append("\n");
        prompt.append("No-gos (out of scope): ").append(nullSafe(pitch.getNoGos())).append("\n");
        prompt.append("Appetite (days): ").append(pitch.getAppetiteDays()).append("\n");
        
        if (teamSkills != null && !teamSkills.isBlank()) {
            prompt.append("Team expertise: ").append(teamSkills).append("\n");
        }
        prompt.append("\n");
        
        prompt.append("## Technology Stack\n");
        prompt.append("Stack: ").append(stack.getStackType().getDisplayName()).append("\n");
        prompt.append("Framework: ").append(stack.getFramework()).append("\n");
        prompt.append("Language: ").append(stack.getPrimaryLanguage()).append("\n");
        prompt.append("Repository: ").append(stack.getRepositoryName()).append("\n\n");
        
        // Project conventions from pre-analysis pass
        if (projectConventions != null && !projectConventions.isBlank()) {
            prompt.append("## Project Conventions (follow these patterns)\n");
            prompt.append(projectConventions).append("\n\n");
        }
        
        // Cross-stack awareness: APIs from previously generated stacks
        if (previousStacksSummary != null && !previousStacksSummary.isBlank()) {
            prompt.append("## APIs Available From Other Stacks (reference these, do not re-create)\n");
            prompt.append(previousStacksSummary).append("\n\n");
        }
        
        if (figmaContext != null && !figmaContext.isBlank()) {
            prompt.append("## Design Context (from Figma)\n");
            prompt.append(figmaContext).append("\n\n");
        }
        
        if (roadmapContext != null && !roadmapContext.isBlank()) {
            prompt.append("## Roadmap Context\n");
            prompt.append(roadmapContext).append("\n");
            prompt.append("Consider related work when designing for extensibility and shared patterns.\n\n");
        }
        
        if (codeContext != null && !codeContext.isEmpty()) {
            prompt.append("## Existing Code Context\n");
            prompt.append(codeContext).append("\n\n");
        }
        
        if (existingServices != null && !existingServices.isEmpty()) {
            prompt.append("## Existing Services to Reuse (ONLY reference services from this list)\n");
            existingServices.forEach(s -> prompt.append("- ").append(s).append("\n"));
            prompt.append("\n");
        }
        
        prompt.append("## Response Format\n");
        prompt.append("Respond with a JSON object. Be SPECIFIC — no vague descriptions.\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"architectureDetail\": {\n");
        prompt.append("    \"summary\": \"2-3 sentence overview of the approach\",\n");
        prompt.append("    \"components\": [\n");
        prompt.append("      {\"name\": \"CircleActivityService\", \"responsibility\": \"Manages CRUD for circle activities\", \"interactsWith\": [\"CircleRepository\", \"NotificationService\"]}\n");
        prompt.append("    ],\n");
        prompt.append("    \"apiContracts\": [\n");
        prompt.append("      {\"endpoint\": \"/api/v1/circles/{id}/activities\", \"method\": \"GET\", \"requestShape\": \"Query params: page (int), size (int)\", \"responseShape\": \"Page<ActivityDTO> with id, title, createdAt, createdBy\", \"description\": \"List activities for a circle\"}\n");
        prompt.append("    ],\n");
        prompt.append("    \"dataModel\": [\n");
        prompt.append("      {\"entityName\": \"CircleActivity\", \"fields\": [\"id: Long\", \"title: String\", \"circleId: Long\", \"createdAt: LocalDateTime\"], \"relationships\": [\"Circle ManyToOne\", \"User ManyToOne\"]}\n");
        prompt.append("    ],\n");
        prompt.append("    \"configChanges\": [\n");
        prompt.append("      {\"key\": \"app.circles.max-activities\", \"value\": \"100\", \"description\": \"Maximum activities per circle\"}\n");
        prompt.append("    ]\n");
        prompt.append("  },\n");
        prompt.append("  \"reusableServices\": [\n");
        prompt.append("    {\"serviceName\": \"NotificationService\", \"filePath\": \"src/main/java/.../NotificationService.java\", \"description\": \"Sends push/email notifications\", \"howToUse\": \"Inject via constructor, call sendNotification(userId, message)\", \"methodsToCall\": [\"sendNotification(Long userId, String message)\", \"sendBatch(List<Long> userIds, String message)\"], \"importStatement\": \"@Autowired NotificationService notificationService\"}\n");
        prompt.append("  ],\n");
        prompt.append("  \"recommendedLibraries\": [\n");
        prompt.append("    {\"name\": \"MapStruct\", \"version\": \"1.5.5\", \"purpose\": \"DTO-Entity mapping to reduce boilerplate\", \"documentationUrl\": \"https://mapstruct.org/\", \"alreadyInProject\": false}\n");
        prompt.append("  ],\n");
        prompt.append("  \"implementationSteps\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"stepNumber\": 1, \"title\": \"Create CircleActivity entity and repository\",\n");
        prompt.append("      \"description\": \"Define the JPA entity with proper annotations and create the Spring Data repository\",\n");
        prompt.append("      \"estimatedHours\": 3,\n");
        prompt.append("      \"filesToCreate\": [\"src/main/java/.../entity/CircleActivity.java\", \"src/main/java/.../repository/CircleActivityRepository.java\"],\n");
        prompt.append("      \"filesToModify\": [],\n");
        prompt.append("      \"subTasks\": [\n");
        prompt.append("        {\"task\": \"Create CircleActivity @Entity with JPA annotations\", \"acceptanceCriteria\": \"Entity compiles, has proper @Id, @ManyToOne for Circle and User\"},\n");
        prompt.append("        {\"task\": \"Create CircleActivityRepository extending JpaRepository\", \"acceptanceCriteria\": \"findByCircleId method works, pagination supported\"}\n");
        prompt.append("      ],\n");
        prompt.append("      \"methodSignatures\": [\"CircleActivityRepository.findByCircleId(Long circleId, Pageable pageable): Page<CircleActivity>\"],\n");
        prompt.append("      \"dependsOnSteps\": []\n");
        prompt.append("    }\n");
        prompt.append("  ],\n");
        prompt.append("  \"riskFactors\": [\"Database migration required — need Flyway script for new table\"],\n");
        prompt.append("  \"bestPractices\": [\"Follow existing *Service → *Repository layer pattern\"]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        prompt.append("## CRITICAL CONSTRAINTS\n");
        prompt.append("1. Every component, endpoint, and entity must be SPECIFIC to this pitch — no generic placeholders\n");
        prompt.append("2. Every reusableService MUST come from the 'Existing Services' list above — do NOT invent services\n");
        prompt.append("3. Every filesToCreate path must follow the project's directory convention");
        if (projectConventions != null && !projectConventions.isBlank()) {
            prompt.append(" as described in 'Project Conventions'");
        }
        prompt.append("\n");
        prompt.append("4. Every implementation step must have at least 2 sub-tasks with acceptance criteria\n");
        prompt.append("5. Every step must include at least one method signature\n");
        prompt.append("6. Realistic time estimates — total must fit within ").append(pitch.getAppetiteDays() * 8).append(" hours\n");
        prompt.append("7. If APIs from other stacks are listed above, reference them in your apiContracts or component interactions — do not duplicate them\n");
        int constraintNumber = 8;
        if (teamSkills != null && !teamSkills.isBlank()) {
            prompt.append(constraintNumber++).append(". Leverage the team's existing expertise where possible\n");
        }
        if (figmaContext != null && !figmaContext.isBlank()) {
            prompt.append(constraintNumber++).append(". Ensure implementation aligns with the provided design specifications\n");
        }
        if (roadmapContext != null && !roadmapContext.isBlank()) {
            prompt.append(constraintNumber++).append(". Design for extensibility to support related work in the same epic\n");
        }
        
        return prompt.toString();
    }

    private String buildReducedScopePrompt(
            Pitch pitch,
            Map<TechStackType, StackSolutionDTO> solutions,
            int availableHours,
            int totalEstimatedHours) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a senior software architect. The proposed solution exceeds the available time budget.\n\n");
        
        prompt.append("## Constraints\n");
        prompt.append("Available hours: ").append(availableHours).append("\n");
        prompt.append("Estimated hours: ").append(totalEstimatedHours).append("\n");
        prompt.append("Overflow: ").append(totalEstimatedHours - availableHours).append(" hours\n\n");
        
        prompt.append("## Pitch\n");
        prompt.append("Title: ").append(pitch.getTitle()).append("\n");
        prompt.append("Problem: ").append(nullSafe(pitch.getProblemStatement())).append("\n\n");
        
        prompt.append("## Current Implementation Steps\n");
        for (Map.Entry<TechStackType, StackSolutionDTO> entry : solutions.entrySet()) {
            prompt.append("\n### ").append(entry.getKey().getDisplayName()).append("\n");
            if (entry.getValue().getImplementationSteps() != null) {
                for (StackSolutionDTO.ImplementationStepDTO step : entry.getValue().getImplementationSteps()) {
                    prompt.append(step.getStepNumber()).append(". ")
                          .append(step.getTitle())
                          .append(" (").append(step.getEstimatedHours()).append("h)\n");
                }
            }
        }
        
        prompt.append("\n## Response Format\n");
        prompt.append("Respond with a JSON object:\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"explanation\": \"Why these items were selected for the MVP\",\n");
        prompt.append("  \"reducedSteps\": [\n");
        prompt.append("    {\"stepNumber\": 1, \"title\": \"...\", \"description\": \"...\", \"estimatedHours\": 4, \"filesToCreate\": [], \"filesToModify\": []}\n");
        prompt.append("  ],\n");
        prompt.append("  \"deferredItems\": [\"Feature X - can be added in next cycle\", \"Optimization Y\"],\n");
        prompt.append("  \"reducedTotalHours\": 32\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        prompt.append("Create a minimal viable scope that:\n");
        prompt.append("1. Fits within ").append(availableHours).append(" hours\n");
        prompt.append("2. Delivers the core value of the pitch\n");
        prompt.append("3. Clearly identifies what is deferred\n");
        
        return prompt.toString();
    }

    private StackSolutionDTO parseSolutionResponse(String response, DetectedStackDTO stack) {
        try {
            String json = extractJson(response);
            
            var parsed = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            
            // Parse the structured architecture detail
            StackSolutionDTO.ArchitectureDetailDTO architectureDetail = parseArchitectureDetail(parsed.get("architectureDetail"));
            
            // Backward compat: derive architectureOverview from the structured detail
            String architectureOverview;
            if (architectureDetail != null && architectureDetail.getSummary() != null) {
                architectureOverview = architectureDetail.getSummary();
            } else {
                // Fallback if LLM returned old flat format
                architectureOverview = (String) parsed.get("architectureOverview");
            }
            
            var steps = parseImplementationSteps(parsed.get("implementationSteps"));
            
            return StackSolutionDTO.builder()
                .stackType(stack.getStackType())
                .architectureOverview(architectureOverview)
                .architectureDetail(architectureDetail)
                .reusableServices(parseReusableServices(parsed.get("reusableServices")))
                .recommendedLibraries(parseRecommendedLibraries(parsed.get("recommendedLibraries")))
                .implementationSteps(steps)
                .riskFactors(parseStringList(parsed.get("riskFactors")))
                .bestPractices(parseStringList(parsed.get("bestPractices")))
                .estimatedHours(calculateTotalHours(steps))
                .build();
                
        } catch (Exception e) {
            log.error("Failed to parse solution response: {}", e.getMessage());
            return createErrorSolution(stack, "Failed to parse AI response");
        }
    }

    private WiseArchitectureResponseDTO.ReducedScopeDTO parseReducedScopeResponse(String response, int availableHours) {
        try {
            String json = extractJson(response);
            var parsed = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            
            return WiseArchitectureResponseDTO.ReducedScopeDTO.builder()
                .explanation((String) parsed.get("explanation"))
                .reducedSteps(parseImplementationSteps(parsed.get("reducedSteps")))
                .deferredItems(parseStringList(parsed.get("deferredItems")))
                .reducedTotalHours(((Number) parsed.getOrDefault("reducedTotalHours", availableHours)).intValue())
                .build();
                
        } catch (Exception e) {
            log.error("Failed to parse reduced scope response: {}", e.getMessage());
            return WiseArchitectureResponseDTO.ReducedScopeDTO.builder()
                .explanation("Unable to generate reduced scope automatically")
                .reducedSteps(List.of())
                .deferredItems(List.of("Review implementation plan manually"))
                .reducedTotalHours(availableHours)
                .build();
        }
    }

    private String extractJson(String response) {
        // Try to find JSON block in response
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        
        // Try to find in code block
        if (response.contains("```json")) {
            start = response.indexOf("```json") + 7;
            end = response.indexOf("```", start);
            if (end > start) {
                return response.substring(start, end).trim();
            }
        }
        
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<StackSolutionDTO.ReusableServiceDTO> parseReusableServices(Object obj) {
        if (obj == null) return List.of();
        
        List<StackSolutionDTO.ReusableServiceDTO> result = new ArrayList<>();
        List<Map<String, Object>> list = (List<Map<String, Object>>) obj;
        
        for (Map<String, Object> item : list) {
            result.add(StackSolutionDTO.ReusableServiceDTO.builder()
                .serviceName((String) item.get("serviceName"))
                .filePath((String) item.get("filePath"))
                .description((String) item.get("description"))
                .howToUse((String) item.get("howToUse"))
                .methodsToCall(parseStringList(item.get("methodsToCall")))
                .importStatement((String) item.get("importStatement"))
                .build());
        }
        
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<StackSolutionDTO.RecommendedLibraryDTO> parseRecommendedLibraries(Object obj) {
        if (obj == null) return List.of();
        
        List<StackSolutionDTO.RecommendedLibraryDTO> result = new ArrayList<>();
        List<Map<String, Object>> list = (List<Map<String, Object>>) obj;
        
        for (Map<String, Object> item : list) {
            result.add(StackSolutionDTO.RecommendedLibraryDTO.builder()
                .name((String) item.get("name"))
                .version((String) item.get("version"))
                .purpose((String) item.get("purpose"))
                .documentationUrl((String) item.get("documentationUrl"))
                .alreadyInProject((Boolean) item.getOrDefault("alreadyInProject", false))
                .build());
        }
        
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<StackSolutionDTO.ImplementationStepDTO> parseImplementationSteps(Object obj) {
        if (obj == null) return List.of();
        
        List<StackSolutionDTO.ImplementationStepDTO> result = new ArrayList<>();
        List<Map<String, Object>> list = (List<Map<String, Object>>) obj;
        
        for (Map<String, Object> item : list) {
            result.add(StackSolutionDTO.ImplementationStepDTO.builder()
                .stepNumber(((Number) item.getOrDefault("stepNumber", 0)).intValue())
                .title((String) item.get("title"))
                .description((String) item.get("description"))
                .estimatedHours(((Number) item.getOrDefault("estimatedHours", 0)).intValue())
                .filesToCreate(parseStringList(item.get("filesToCreate")))
                .filesToModify(parseStringList(item.get("filesToModify")))
                .subTasks(parseSubTasks(item.get("subTasks")))
                .methodSignatures(parseStringList(item.get("methodSignatures")))
                .dependsOnSteps(parseIntegerList(item.get("dependsOnSteps")))
                .build());
        }
        
        return result;
    }

    @SuppressWarnings("unchecked")
    private StackSolutionDTO.ArchitectureDetailDTO parseArchitectureDetail(Object obj) {
        if (obj == null) return null;
        
        Map<String, Object> map = (Map<String, Object>) obj;
        
        return StackSolutionDTO.ArchitectureDetailDTO.builder()
            .summary((String) map.get("summary"))
            .components(parseComponents(map.get("components")))
            .apiContracts(parseApiContracts(map.get("apiContracts")))
            .dataModel(parseDataModel(map.get("dataModel")))
            .configChanges(parseConfigChanges(map.get("configChanges")))
            .build();
    }

    @SuppressWarnings("unchecked")
    private List<StackSolutionDTO.ComponentDTO> parseComponents(Object obj) {
        if (obj == null) return List.of();
        
        List<StackSolutionDTO.ComponentDTO> result = new ArrayList<>();
        List<Map<String, Object>> list = (List<Map<String, Object>>) obj;
        
        for (Map<String, Object> item : list) {
            result.add(StackSolutionDTO.ComponentDTO.builder()
                .name((String) item.get("name"))
                .responsibility((String) item.get("responsibility"))
                .interactsWith(parseStringList(item.get("interactsWith")))
                .build());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<StackSolutionDTO.ApiContractDTO> parseApiContracts(Object obj) {
        if (obj == null) return List.of();
        
        List<StackSolutionDTO.ApiContractDTO> result = new ArrayList<>();
        List<Map<String, Object>> list = (List<Map<String, Object>>) obj;
        
        for (Map<String, Object> item : list) {
            result.add(StackSolutionDTO.ApiContractDTO.builder()
                .endpoint((String) item.get("endpoint"))
                .method((String) item.get("method"))
                .requestShape((String) item.get("requestShape"))
                .responseShape((String) item.get("responseShape"))
                .description((String) item.get("description"))
                .build());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<StackSolutionDTO.DataModelDTO> parseDataModel(Object obj) {
        if (obj == null) return List.of();
        
        List<StackSolutionDTO.DataModelDTO> result = new ArrayList<>();
        List<Map<String, Object>> list = (List<Map<String, Object>>) obj;
        
        for (Map<String, Object> item : list) {
            result.add(StackSolutionDTO.DataModelDTO.builder()
                .entityName((String) item.get("entityName"))
                .fields(parseStringList(item.get("fields")))
                .relationships(parseStringList(item.get("relationships")))
                .build());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<StackSolutionDTO.ConfigChangeDTO> parseConfigChanges(Object obj) {
        if (obj == null) return List.of();
        
        List<StackSolutionDTO.ConfigChangeDTO> result = new ArrayList<>();
        List<Map<String, Object>> list = (List<Map<String, Object>>) obj;
        
        for (Map<String, Object> item : list) {
            result.add(StackSolutionDTO.ConfigChangeDTO.builder()
                .key((String) item.get("key"))
                .value((String) item.get("value"))
                .description((String) item.get("description"))
                .build());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<StackSolutionDTO.SubTaskDTO> parseSubTasks(Object obj) {
        if (obj == null) return List.of();
        
        List<StackSolutionDTO.SubTaskDTO> result = new ArrayList<>();
        List<Map<String, Object>> list = (List<Map<String, Object>>) obj;
        
        for (Map<String, Object> item : list) {
            result.add(StackSolutionDTO.SubTaskDTO.builder()
                .task((String) item.get("task"))
                .acceptanceCriteria((String) item.get("acceptanceCriteria"))
                .build());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Integer> parseIntegerList(Object obj) {
        if (obj == null) return List.of();
        List<Number> numbers = (List<Number>) obj;
        return numbers.stream().map(Number::intValue).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private List<String> parseStringList(Object obj) {
        if (obj == null) return List.of();
        return (List<String>) obj;
    }

    private int calculateTotalHours(List<StackSolutionDTO.ImplementationStepDTO> steps) {
        return steps.stream()
            .mapToInt(s -> s.getEstimatedHours() != null ? s.getEstimatedHours() : 0)
            .sum();
    }

    private StackSolutionDTO createPlaceholderSolution(DetectedStackDTO stack) {
        return StackSolutionDTO.builder()
            .stackType(stack.getStackType())
            .architectureOverview("AI model not available. Please configure an LLM provider.")
            .reusableServices(List.of())
            .recommendedLibraries(List.of())
            .implementationSteps(List.of(
                StackSolutionDTO.ImplementationStepDTO.builder()
                    .stepNumber(1)
                    .title("Configure AI")
                    .description("Enable AI features in organization settings and configure an LLM provider")
                    .estimatedHours(1)
                    .filesToCreate(List.of())
                    .filesToModify(List.of())
                    .build()
            ))
            .estimatedHours(1)
            .riskFactors(List.of("AI features not configured"))
            .bestPractices(List.of())
            .build();
    }

    private StackSolutionDTO createErrorSolution(DetectedStackDTO stack, String error) {
        return StackSolutionDTO.builder()
            .stackType(stack.getStackType())
            .architectureOverview("Error generating solution: " + error)
            .reusableServices(List.of())
            .recommendedLibraries(List.of())
            .implementationSteps(List.of())
            .estimatedHours(0)
            .riskFactors(List.of("Solution generation failed: " + error))
            .bestPractices(List.of())
            .build();
    }

    private WiseArchitectureResponseDTO.ReducedScopeDTO createPlaceholderReducedScope(
            Map<TechStackType, StackSolutionDTO> solutions, int availableHours) {
        
        List<String> deferred = new ArrayList<>();
        solutions.values().forEach(s -> {
            if (s.getImplementationSteps() != null && s.getImplementationSteps().size() > 2) {
                for (int i = 2; i < s.getImplementationSteps().size(); i++) {
                    deferred.add(s.getImplementationSteps().get(i).getTitle());
                }
            }
        });
        
        return WiseArchitectureResponseDTO.ReducedScopeDTO.builder()
            .explanation("AI model not available. Showing first 2 steps of each stack as MVP.")
            .reducedSteps(List.of())
            .deferredItems(deferred)
            .reducedTotalHours(availableHours)
            .build();
    }

    private String nullSafe(String value) {
        return value != null ? value : "Not specified";
    }
}
