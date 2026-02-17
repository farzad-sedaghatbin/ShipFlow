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
     * @return the generated stack solution
     */
    public StackSolutionDTO generateStackSolution(
            Pitch pitch,
            DetectedStackDTO stack,
            String codeContext,
            List<String> existingServices,
            String teamSkills,
            String figmaContext,
            String roadmapContext) {
        
        if (chatLanguageModel == null) {
            log.warn("ChatLanguageModel not available, returning placeholder solution");
            return createPlaceholderSolution(stack);
        }

        String prompt = buildSolutionPrompt(pitch, stack, codeContext, existingServices, teamSkills, figmaContext, roadmapContext);
        
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

    private String buildSolutionPrompt(
            Pitch pitch,
            DetectedStackDTO stack,
            String codeContext,
            List<String> existingServices,
            String teamSkills,
            String figmaContext,
            String roadmapContext) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a senior software architect. Generate a technical solution document for implementing a feature.\n\n");
        
        prompt.append("## Pitch Information\n");
        prompt.append("Title: ").append(pitch.getTitle()).append("\n");
        prompt.append("Problem Statement: ").append(nullSafe(pitch.getProblemStatement())).append("\n");
        prompt.append("Proposed Solution: ").append(nullSafe(pitch.getSolution())).append("\n");
        prompt.append("Rabbit Holes (avoid): ").append(nullSafe(pitch.getRabbitHoles())).append("\n");
        prompt.append("No-gos (out of scope): ").append(nullSafe(pitch.getNoGos())).append("\n");
        prompt.append("Appetite (days): ").append(pitch.getAppetiteDays()).append("\n");
        
        // Add team skills if available (token-efficient: ~25-35 tokens)
        if (teamSkills != null && !teamSkills.isBlank()) {
            prompt.append("Team expertise: ").append(teamSkills).append("\n");
        }
        prompt.append("\n");
        
        prompt.append("## Technology Stack\n");
        prompt.append("Stack: ").append(stack.getStackType().getDisplayName()).append("\n");
        prompt.append("Framework: ").append(stack.getFramework()).append("\n");
        prompt.append("Language: ").append(stack.getPrimaryLanguage()).append("\n");
        prompt.append("Repository: ").append(stack.getRepositoryName()).append("\n\n");
        
        // Add Figma design context if available (token-efficient: ~100-200 tokens)
        if (figmaContext != null && !figmaContext.isBlank()) {
            prompt.append("## Design Context (from Figma)\n");
            prompt.append(figmaContext).append("\n\n");
        }
        
        // Add roadmap context if available (token-efficient: ~100-150 tokens)
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
            prompt.append("## Existing Services to Reuse\n");
            existingServices.forEach(s -> prompt.append("- ").append(s).append("\n"));
            prompt.append("\n");
        }
        
        prompt.append("## Response Format\n");
        prompt.append("Respond with a JSON object containing:\n");
        prompt.append("```json\n");
        prompt.append("{\n");
        prompt.append("  \"architectureOverview\": \"High-level architecture description following best practices\",\n");
        prompt.append("  \"reusableServices\": [\n");
        prompt.append("    {\"serviceName\": \"...\", \"filePath\": \"...\", \"description\": \"...\", \"howToUse\": \"...\"}\n");
        prompt.append("  ],\n");
        prompt.append("  \"recommendedLibraries\": [\n");
        prompt.append("    {\"name\": \"...\", \"version\": \"...\", \"purpose\": \"...\", \"documentationUrl\": \"...\", \"alreadyInProject\": true/false}\n");
        prompt.append("  ],\n");
        prompt.append("  \"implementationSteps\": [\n");
        prompt.append("    {\"stepNumber\": 1, \"title\": \"...\", \"description\": \"...\", \"estimatedHours\": 4, \"filesToCreate\": [...], \"filesToModify\": [...]}\n");
        prompt.append("  ],\n");
        prompt.append("  \"riskFactors\": [\"Risk 1\", \"Risk 2\"],\n");
        prompt.append("  \"bestPractices\": [\"Best practice 1\", \"Best practice 2\", \"Best practice 3\"]\n");
        prompt.append("}\n");
        prompt.append("```\n\n");
        prompt.append("Focus on:\n");
        prompt.append("1. Best practices for ").append(stack.getFramework()).append("\n");
        prompt.append("2. Maximizing reuse of existing services\n");
        prompt.append("3. Recommending libraries that reduce development time\n");
        prompt.append("4. Realistic time estimates (total should fit within ").append(pitch.getAppetiteDays() * 8).append(" hours)\n");
        prompt.append("5. Identifying potential risks\n");
        int focusNumber = 6;
        if (teamSkills != null && !teamSkills.isBlank()) {
            prompt.append(focusNumber++).append(". Leveraging the team's existing expertise where possible\n");
        }
        if (figmaContext != null && !figmaContext.isBlank()) {
            prompt.append(focusNumber++).append(". Ensuring implementation aligns with the provided design specifications\n");
        }
        if (roadmapContext != null && !roadmapContext.isBlank()) {
            prompt.append(focusNumber++).append(". Designing for extensibility to support related work in the same epic\n");
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
            // Extract JSON from response
            String json = extractJson(response);
            
            var parsed = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            
            return StackSolutionDTO.builder()
                .stackType(stack.getStackType())
                .architectureOverview((String) parsed.get("architectureOverview"))
                .reusableServices(parseReusableServices(parsed.get("reusableServices")))
                .recommendedLibraries(parseRecommendedLibraries(parsed.get("recommendedLibraries")))
                .implementationSteps(parseImplementationSteps(parsed.get("implementationSteps")))
                .riskFactors(parseStringList(parsed.get("riskFactors")))
                .bestPractices(parseStringList(parsed.get("bestPractices")))
                .estimatedHours(calculateTotalHours(parseImplementationSteps(parsed.get("implementationSteps"))))
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
                .build());
        }
        
        return result;
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
