package com.github.farzadsedaghatbin.shipflow.service.wisearchitecture;

import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.DetectedStackDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.TechStackType;
import com.github.farzadsedaghatbin.shipflow.entity.RepositoryTechStack;
import com.github.farzadsedaghatbin.shipflow.entity.github.GitHubRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RepositoryTechStackRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for detecting technology stacks in repositories.
 * Uses file patterns and project structure to identify tech stacks.
 * Caches detected stacks to avoid repeated detection.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TechStackDetectorService {

    private final RepositoryTechStackRepository techStackRepository;

    // File patterns for stack detection
    private static final Map<TechStackType, List<Pattern>> STACK_PATTERNS = new HashMap<>();

    static {
        // Mobile - Kotlin/Android
        STACK_PATTERNS.put(TechStackType.MOBILE_KOTLIN, List.of(
            Pattern.compile(".*\\.kt$"),
            Pattern.compile(".*build\\.gradle(\\.kts)?$"),
            Pattern.compile(".*AndroidManifest\\.xml$")
        ));

        // Mobile - Swift/iOS
        STACK_PATTERNS.put(TechStackType.MOBILE_SWIFT, List.of(
            Pattern.compile(".*\\.swift$"),
            Pattern.compile(".*Package\\.swift$"),
            Pattern.compile(".*\\.xcodeproj/[^/]+$"),
            Pattern.compile(".*Podfile$")
        ));

        // Mobile - React Native
        STACK_PATTERNS.put(TechStackType.MOBILE_REACT_NATIVE, List.of(
            Pattern.compile(".*(react-native|ReactNative)/.*$"),
            Pattern.compile(".*metro\\.config\\.js$"),
            Pattern.compile(".*app\\.json$")
        ));

        // Mobile - Flutter
        STACK_PATTERNS.put(TechStackType.MOBILE_FLUTTER, List.of(
            Pattern.compile(".*pubspec\\.yaml$"),
            Pattern.compile(".*\\.dart$")
        ));

        // Backend - Java/Spring
        STACK_PATTERNS.put(TechStackType.BACKEND_JAVA, List.of(
            Pattern.compile(".*pom\\.xml$"),
            Pattern.compile(".*build\\.gradle$"),
            Pattern.compile(".*\\.java$"),
            Pattern.compile(".*application\\.properties$"),
            Pattern.compile(".*application\\.ya?ml$")
        ));

        // Backend - Node.js
        STACK_PATTERNS.put(TechStackType.BACKEND_NODE, List.of(
            Pattern.compile(".*package\\.json$"),
            Pattern.compile(".*server\\.js$"),
            Pattern.compile(".*index\\.js$")
        ));

        // Backend - Python
        STACK_PATTERNS.put(TechStackType.BACKEND_PYTHON, List.of(
            Pattern.compile(".*requirements\\.txt$"),
            Pattern.compile(".*setup\\.py$"),
            Pattern.compile(".*pyproject\\.toml$"),
            Pattern.compile(".*\\.py$")
        ));

        // Backend - Go
        STACK_PATTERNS.put(TechStackType.BACKEND_GO, List.of(
            Pattern.compile(".*go\\.mod$"),
            Pattern.compile(".*go\\.sum$"),
            Pattern.compile(".*\\.go$")
        ));

        // Backend - .NET
        STACK_PATTERNS.put(TechStackType.BACKEND_DOTNET, List.of(
            Pattern.compile(".*\\.csproj$"),
            Pattern.compile(".*\\.sln$"),
            Pattern.compile(".*\\.cs$")
        ));

        // Web - React
        STACK_PATTERNS.put(TechStackType.WEB_REACT, List.of(
            Pattern.compile(".*package\\.json$"),
            Pattern.compile(".*\\.jsx$"),
            Pattern.compile(".*\\.tsx$"),
            Pattern.compile(".*vite\\.config\\.(js|ts|mjs|mts)$")
        ));

        // Web - Angular
        STACK_PATTERNS.put(TechStackType.WEB_ANGULAR, List.of(
            Pattern.compile(".*angular\\.json$"),
            Pattern.compile(".*\\.component\\.ts$"),
            Pattern.compile(".*\\.module\\.ts$")
        ));

        // Web - Vue
        STACK_PATTERNS.put(TechStackType.WEB_VUE, List.of(
            Pattern.compile(".*\\.vue$"),
            Pattern.compile(".*vue\\.config\\.js$"),
            Pattern.compile(".*nuxt\\.config\\.(js|ts)$")
        ));

        // Web - Next.js
        STACK_PATTERNS.put(TechStackType.WEB_NEXTJS, List.of(
            Pattern.compile(".*next\\.config\\.(js|mjs|ts)$"),
            Pattern.compile(".*/pages/[^/]+\\.(jsx|tsx)$"),
            Pattern.compile(".*/app/[^/]+\\.(jsx|tsx)$")
        ));
    }

    /**
     * Detect technology stacks in a repository based on file structure.
     * Uses cached results if available, otherwise performs detection and caches the result.
     *
     * @param repository the GitHub repository to analyze
     * @param fileList list of file paths in the repository (from MCP)
     * @return list of detected technology stacks
     */
    @Transactional
    public List<DetectedStackDTO> detectStacks(GitHubRepository repository, List<String> fileList) {
        log.info("Detecting tech stacks in repository: {} with {} files", 
            repository.getFullName(), fileList != null ? fileList.size() : 0);
        
        if (fileList == null || fileList.isEmpty()) {
            log.warn("File list is empty for repository: {}, cannot detect tech stacks", 
                repository.getFullName());
            return List.of();
        }
        
        log.debug("Sample files from repository {}: {}", 
            repository.getFullName(),
            fileList.stream().limit(20).collect(java.util.stream.Collectors.joining(", ")));
        
        // Check if we have cached results
        List<RepositoryTechStack> cachedStacks = techStackRepository.findByRepository(repository);
        if (!cachedStacks.isEmpty()) {
            log.info("Using cached tech stacks for repository: {} ({} stacks)", 
                repository.getFullName(), cachedStacks.size());
            return convertCachedToDTO(cachedStacks);
        }
        
        log.info("No cached stacks found, performing detection for: {}", repository.getFullName());
        List<DetectedStackDTO> detectedStacks = new ArrayList<>();
        
        for (Map.Entry<TechStackType, List<Pattern>> entry : STACK_PATTERNS.entrySet()) {
            TechStackType stackType = entry.getKey();
            List<Pattern> patterns = entry.getValue();
            
            List<String> matchedFiles = new ArrayList<>();
            int matchCount = 0;
            
            for (String filePath : fileList) {
                // Prevent ReDoS by skipping excessively long paths
                if (filePath != null && filePath.length() > 500) {
                    continue;
                }
                for (Pattern pattern : patterns) {
                    if (filePath != null && pattern.matcher(filePath).matches()) {
                        matchedFiles.add(filePath);
                        matchCount++;
                        break; // Only count each file once
                    }
                }
            }
            
            // Calculate confidence based on match ratio and specific key files
            if (matchCount > 0) {
                int confidence = calculateConfidence(stackType, matchedFiles, fileList.size());
                
                if (confidence >= 30) { // Minimum confidence threshold
                    DetectedStackDTO detected = DetectedStackDTO.builder()
                        .stackType(stackType)
                        .confidence(confidence)
                        .keyFilesFound(matchedFiles.stream().limit(5).toList())
                        .primaryLanguage(getPrimaryLanguage(stackType))
                        .framework(detectFramework(stackType, matchedFiles))
                        .repositoryId(repository.getId())
                        .repositoryName(repository.getFullName())
                        .build();
                    
                    detectedStacks.add(detected);
                    log.debug("Detected {} with {}% confidence in {}", 
                        stackType, confidence, repository.getFullName());
                }
            }
        }
        
        // Remove overlapping detections (e.g., if both BACKEND_NODE and WEB_REACT detected, keep both)
        // But remove lower confidence duplicates in same category
        detectedStacks = filterOverlappingStacks(detectedStacks);
        
        log.info("Detected {} tech stacks in {}: {}", 
            detectedStacks.size(), 
            repository.getFullName(),
            detectedStacks.stream().map(d -> d.getStackType().name()).toList());
        
        // Cache the detected stacks
        cacheDetectedStacks(repository, detectedStacks);
        
        return detectedStacks;
    }

    /**
     * Force re-detection of tech stacks for a repository by clearing the cache.
     * Useful when repository structure has changed significantly.
     *
     * @param repository the repository to clear cache for
     */
    @Transactional
    public void clearCache(GitHubRepository repository) {
        log.info("Clearing tech stack cache for repository: {}", repository.getFullName());
        techStackRepository.deleteByRepository(repository);
    }

    /**
     * Convert cached stack entities to DTOs.
     */
    private List<DetectedStackDTO> convertCachedToDTO(List<RepositoryTechStack> cachedStacks) {
        return cachedStacks.stream()
            .map(cached -> {
                List<String> keyFiles = cached.getDetectedByFiles() != null 
                    ? java.util.Arrays.stream(cached.getDetectedByFiles().split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList())
                    : List.of();
                
                // Use persisted values if available, otherwise fall back to computed values
                String framework = cached.getFramework() != null 
                    ? cached.getFramework() 
                    : detectFramework(cached.getStackType(), keyFiles);
                
                String primaryLanguage = cached.getPrimaryLanguage() != null
                    ? cached.getPrimaryLanguage()
                    : getPrimaryLanguage(cached.getStackType());
                
                return DetectedStackDTO.builder()
                    .stackType(cached.getStackType())
                    .confidence(cached.getConfidenceScore())
                    .keyFilesFound(keyFiles)
                    .primaryLanguage(primaryLanguage)
                    .framework(framework)
                    .repositoryId(cached.getRepository().getId())
                    .repositoryName(cached.getRepository().getFullName())
                    .build();
            })
            .collect(Collectors.toList());
    }

    /**
     * Cache detected stacks for future use.
     * Handles concurrent requests gracefully by catching constraint violations.
     */
    private void cacheDetectedStacks(GitHubRepository repository, List<DetectedStackDTO> detectedStacks) {
        List<RepositoryTechStack> entitiesToSave = detectedStacks.stream()
            .map(dto -> RepositoryTechStack.builder()
                .repository(repository)
                .stackType(dto.getStackType())
                .confidenceScore(dto.getConfidence())
                .detectedByFiles(dto.getKeyFilesFound() != null 
                    ? String.join(",", dto.getKeyFilesFound())
                    : null)
                .framework(dto.getFramework())
                .primaryLanguage(dto.getPrimaryLanguage())
                .build())
            .collect(Collectors.toList());
        
        if (!entitiesToSave.isEmpty()) {
            try {
                techStackRepository.saveAll(entitiesToSave);
                log.info("Cached {} tech stacks for repository: {}", 
                    entitiesToSave.size(), repository.getFullName());
            } catch (org.springframework.dao.DataIntegrityViolationException e) {
                // Race condition: another request already cached these stacks
                log.debug("Tech stacks already cached for repository {} (concurrent request): {}", 
                    repository.getFullName(), e.getMessage());
            }
        }
    }

    /**
     * Detect stacks from package.json content (for more accurate detection).
     */
    public List<DetectedStackDTO> detectStacksFromPackageJson(
            GitHubRepository repository, 
            String packageJsonContent) {
        
        List<DetectedStackDTO> stacks = new ArrayList<>();
        
        // Check for React
        if (packageJsonContent.contains("\"react\"") && 
            !packageJsonContent.contains("\"react-native\"")) {
            stacks.add(DetectedStackDTO.builder()
                .stackType(TechStackType.WEB_REACT)
                .confidence(90)
                .keyFilesFound(List.of("package.json"))
                .primaryLanguage("TypeScript/JavaScript")
                .framework(detectReactFramework(packageJsonContent))
                .repositoryId(repository.getId())
                .repositoryName(repository.getFullName())
                .build());
        }
        
        // Check for React Native
        if (packageJsonContent.contains("\"react-native\"")) {
            stacks.add(DetectedStackDTO.builder()
                .stackType(TechStackType.MOBILE_REACT_NATIVE)
                .confidence(95)
                .keyFilesFound(List.of("package.json"))
                .primaryLanguage("TypeScript/JavaScript")
                .framework("React Native")
                .repositoryId(repository.getId())
                .repositoryName(repository.getFullName())
                .build());
        }
        
        // Check for Angular
        if (packageJsonContent.contains("\"@angular/core\"")) {
            stacks.add(DetectedStackDTO.builder()
                .stackType(TechStackType.WEB_ANGULAR)
                .confidence(95)
                .keyFilesFound(List.of("package.json"))
                .primaryLanguage("TypeScript")
                .framework("Angular")
                .repositoryId(repository.getId())
                .repositoryName(repository.getFullName())
                .build());
        }
        
        // Check for Vue
        if (packageJsonContent.contains("\"vue\"")) {
            stacks.add(DetectedStackDTO.builder()
                .stackType(TechStackType.WEB_VUE)
                .confidence(90)
                .keyFilesFound(List.of("package.json"))
                .primaryLanguage("TypeScript/JavaScript")
                .framework(packageJsonContent.contains("\"nuxt\"") ? "Nuxt.js" : "Vue.js")
                .repositoryId(repository.getId())
                .repositoryName(repository.getFullName())
                .build());
        }
        
        // Check for Next.js
        if (packageJsonContent.contains("\"next\"")) {
            stacks.add(DetectedStackDTO.builder()
                .stackType(TechStackType.WEB_NEXTJS)
                .confidence(95)
                .keyFilesFound(List.of("package.json"))
                .primaryLanguage("TypeScript/JavaScript")
                .framework("Next.js")
                .repositoryId(repository.getId())
                .repositoryName(repository.getFullName())
                .build());
        }
        
        // Check for Node.js backend frameworks
        if (packageJsonContent.contains("\"express\"") || 
            packageJsonContent.contains("\"@nestjs/core\"") ||
            packageJsonContent.contains("\"fastify\"") ||
            packageJsonContent.contains("\"koa\"")) {
            stacks.add(DetectedStackDTO.builder()
                .stackType(TechStackType.BACKEND_NODE)
                .confidence(90)
                .keyFilesFound(List.of("package.json"))
                .primaryLanguage("TypeScript/JavaScript")
                .framework(detectNodeFramework(packageJsonContent))
                .repositoryId(repository.getId())
                .repositoryName(repository.getFullName())
                .build());
        }
        
        return stacks;
    }

    private int calculateConfidence(TechStackType stackType, List<String> matchedFiles, int totalFiles) {
        int baseConfidence = 0;
        
        // Key file detection gives higher confidence
        for (String file : matchedFiles) {
            if (isKeyFile(stackType, file)) {
                baseConfidence += 30;
            } else {
                baseConfidence += 5;
            }
        }
        
        // Cap at 100
        return Math.min(baseConfidence, 100);
    }

    private boolean isKeyFile(TechStackType stackType, String file) {
        return switch (stackType) {
            case MOBILE_KOTLIN -> file.contains("AndroidManifest.xml") || file.contains("build.gradle");
            case MOBILE_SWIFT -> file.contains("Package.swift") || file.contains(".xcodeproj");
            case MOBILE_REACT_NATIVE -> file.contains("metro.config") || file.contains("app.json");
            case MOBILE_FLUTTER -> file.contains("pubspec.yaml");
            case BACKEND_JAVA -> file.contains("pom.xml") || file.contains("build.gradle");
            case BACKEND_NODE -> file.contains("package.json");
            case BACKEND_PYTHON -> file.contains("requirements.txt") || file.contains("pyproject.toml");
            case BACKEND_GO -> file.contains("go.mod");
            case BACKEND_DOTNET -> file.contains(".csproj") || file.contains(".sln");
            case WEB_REACT -> file.contains("package.json") || file.contains("vite.config");
            case WEB_ANGULAR -> file.contains("angular.json");
            case WEB_VUE -> file.contains("vue.config") || file.contains("nuxt.config");
            case WEB_NEXTJS -> file.contains("next.config");
            default -> false;
        };
    }

    private String getPrimaryLanguage(TechStackType stackType) {
        return switch (stackType) {
            case MOBILE_KOTLIN -> "Kotlin";
            case MOBILE_SWIFT -> "Swift";
            case MOBILE_REACT_NATIVE, BACKEND_NODE, WEB_REACT, WEB_ANGULAR, WEB_VUE, WEB_NEXTJS -> "TypeScript/JavaScript";
            case MOBILE_FLUTTER -> "Dart";
            case BACKEND_JAVA -> "Java";
            case BACKEND_PYTHON -> "Python";
            case BACKEND_GO -> "Go";
            case BACKEND_DOTNET -> "C#";
            default -> "Unknown";
        };
    }

    private String detectFramework(TechStackType stackType, List<String> matchedFiles) {
        String filesStr = String.join(",", matchedFiles).toLowerCase();
        
        return switch (stackType) {
            case BACKEND_JAVA -> {
                if (filesStr.contains("spring") || filesStr.contains("application.properties") || 
                    filesStr.contains("application.yml")) {
                    yield "Spring Boot";
                }
                yield "Java";
            }
            case BACKEND_NODE -> {
                if (filesStr.contains("nest")) yield "NestJS";
                if (filesStr.contains("express")) yield "Express.js";
                yield "Node.js";
            }
            case BACKEND_PYTHON -> {
                if (filesStr.contains("django")) yield "Django";
                if (filesStr.contains("fastapi")) yield "FastAPI";
                if (filesStr.contains("flask")) yield "Flask";
                yield "Python";
            }
            case WEB_REACT -> "React";
            case WEB_ANGULAR -> "Angular";
            case WEB_VUE -> "Vue.js";
            case WEB_NEXTJS -> "Next.js";
            case MOBILE_KOTLIN -> "Android (Kotlin)";
            case MOBILE_SWIFT -> "iOS (Swift)";
            case MOBILE_REACT_NATIVE -> "React Native";
            case MOBILE_FLUTTER -> "Flutter";
            default -> stackType.getDisplayName();
        };
    }

    private String detectReactFramework(String packageJson) {
        if (packageJson.contains("\"next\"")) return "Next.js";
        if (packageJson.contains("\"vite\"")) return "React + Vite";
        if (packageJson.contains("\"react-scripts\"")) return "Create React App";
        return "React";
    }

    private String detectNodeFramework(String packageJson) {
        if (packageJson.contains("\"@nestjs/core\"")) return "NestJS";
        if (packageJson.contains("\"express\"")) return "Express.js";
        if (packageJson.contains("\"fastify\"")) return "Fastify";
        if (packageJson.contains("\"koa\"")) return "Koa";
        return "Node.js";
    }

    private List<DetectedStackDTO> filterOverlappingStacks(List<DetectedStackDTO> stacks) {
        // Group by category and keep highest confidence in each category
        Map<String, DetectedStackDTO> bestByCategory = new HashMap<>();
        
        for (DetectedStackDTO stack : stacks) {
            String category = stack.getStackType().getCategory();
            String key = category + "_" + stack.getRepositoryId();
            
            DetectedStackDTO existing = bestByCategory.get(key);
            if (existing == null || stack.getConfidence() > existing.getConfidence()) {
                // Special case: keep both backend and frontend for Node.js projects
                if (existing != null && 
                    !existing.getStackType().getCategory().equals(stack.getStackType().getCategory())) {
                    // Different categories, keep both
                    bestByCategory.put(key + "_" + stack.getStackType().name(), stack);
                } else {
                    bestByCategory.put(key, stack);
                }
            }
        }
        
        return new ArrayList<>(bestByCategory.values());
    }
}
