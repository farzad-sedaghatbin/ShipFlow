package com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing the technical solution for a specific technology stack.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StackSolutionDTO {

    /**
     * The technology stack this solution is for.
     */
    private TechStackType stackType;

    /**
     * High-level architecture overview (kept for backward compatibility).
     * For new solutions, this is populated from architectureDetail.summary.
     */
    private String architectureOverview;

    /**
     * Structured architecture detail with components, APIs, data model, and config.
     */
    private ArchitectureDetailDTO architectureDetail;

    /**
     * List of existing services in the codebase that can be reused.
     */
    private List<ReusableServiceDTO> reusableServices;

    /**
     * Recommended libraries and tools to use.
     */
    private List<RecommendedLibraryDTO> recommendedLibraries;

    /**
     * Step-by-step implementation plan with time estimates.
     */
    private List<ImplementationStepDTO> implementationSteps;

    /**
     * Total estimated hours for this stack's implementation.
     */
    private Integer estimatedHours;

    /**
     * Risk factors specific to this stack's implementation.
     */
    private List<String> riskFactors;

    /**
     * Best practices specific to this stack's implementation.
     */
    @Builder.Default
    private List<String> bestPractices = new java.util.ArrayList<>();

    // ─── Nested DTOs ───────────────────────────────────────────

    /**
     * Structured architecture detail breaking the overview into actionable sections.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ArchitectureDetailDTO {
        /** 2-3 sentence high-level summary. */
        private String summary;

        /** Components/modules to build with responsibilities and interactions. */
        @Builder.Default
        private List<ComponentDTO> components = new java.util.ArrayList<>();

        /** API contracts (REST/GraphQL endpoints) this stack exposes or consumes. */
        @Builder.Default
        private List<ApiContractDTO> apiContracts = new java.util.ArrayList<>();

        /** New or modified data model entities. */
        @Builder.Default
        private List<DataModelDTO> dataModel = new java.util.ArrayList<>();

        /** Configuration/environment variable changes required. */
        @Builder.Default
        private List<ConfigChangeDTO> configChanges = new java.util.ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ComponentDTO {
        private String name;
        private String responsibility;
        /** Names of other components this interacts with. */
        @Builder.Default
        private List<String> interactsWith = new java.util.ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApiContractDTO {
        /** e.g. "/api/v1/circles/{id}/activities" */
        private String endpoint;
        /** HTTP method: GET, POST, PUT, DELETE */
        private String method;
        /** Description of request body shape or query params. */
        private String requestShape;
        /** Description of response body shape. */
        private String responseShape;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DataModelDTO {
        private String entityName;
        /** Key fields with types, e.g. ["id: Long", "name: String", "createdAt: LocalDateTime"] */
        @Builder.Default
        private List<String> fields = new java.util.ArrayList<>();
        /** e.g. ["User ManyToOne", "Activity OneToMany"] */
        @Builder.Default
        private List<String> relationships = new java.util.ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConfigChangeDTO {
        /** Property key or env var name. */
        private String key;
        /** Default or example value. */
        private String value;
        private String description;
    }

    /**
     * DTO for a reusable service found in the codebase.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReusableServiceDTO {
        private String serviceName;
        private String filePath;
        private String description;
        private String howToUse;
        /** Specific methods to call from this service. */
        @Builder.Default
        private List<String> methodsToCall = new java.util.ArrayList<>();
        /** Import/injection statement. */
        private String importStatement;
    }

    /**
     * DTO for a recommended library or tool.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RecommendedLibraryDTO {
        private String name;
        private String version;
        private String purpose;
        private String documentationUrl;
        private Boolean alreadyInProject;
    }

    /**
     * DTO for an implementation step.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImplementationStepDTO {
        private Integer stepNumber;
        private String title;
        private String description;
        private Integer estimatedHours;
        private List<String> filesToCreate;
        private List<String> filesToModify;
        /** Concrete sub-tasks with acceptance criteria. */
        @Builder.Default
        private List<SubTaskDTO> subTasks = new java.util.ArrayList<>();
        /** Key method/function signatures to implement. */
        @Builder.Default
        private List<String> methodSignatures = new java.util.ArrayList<>();
        /** Step numbers this step depends on. */
        @Builder.Default
        private List<Integer> dependsOnSteps = new java.util.ArrayList<>();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SubTaskDTO {
        private String task;
        private String acceptanceCriteria;
    }
}
