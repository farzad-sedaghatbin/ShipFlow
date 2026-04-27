package com.github.farzadsedaghatbin.shipflow.service.wisearchitecture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.DetectedStackDTO;
import com.github.farzadsedaghatbin.shipflow.dto.wisearchitecture.TechStackType;
import com.github.farzadsedaghatbin.shipflow.entity.RepositoryTechStack;
import com.github.farzadsedaghatbin.shipflow.entity.github.GitHubRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RepositoryTechStackRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TechStackDetectorService")
class TechStackDetectorServiceTest {

    @Mock
    private RepositoryTechStackRepository techStackRepository;

    @InjectMocks
    private TechStackDetectorService service;

    private GitHubRepository testRepo;

    @BeforeEach
    void setUp() {
        testRepo = GitHubRepository.builder()
            .id(1L)
            .owner("test")
            .name("test-repo")
            .fullName("test/test-repo")
            .build();
    }

    @Nested
    @DisplayName("detectStacks")
    class DetectStacks {

        @Test
        @DisplayName("should detect Java/Spring backend stack")
        void shouldDetectJavaSpringStack() {
            when(techStackRepository.findByRepository(testRepo)).thenReturn(List.of());
            
            List<String> files = List.of(
                "pom.xml",
                "src/main/java/com/example/Application.java",
                "src/main/resources/application.properties",
                "src/main/java/com/example/service/UserService.java"
            );

            List<DetectedStackDTO> stacks = service.detectStacks(testRepo, files);

            assertThat(stacks).isNotEmpty();
            assertThat(stacks).anyMatch(s -> s.getStackType() == TechStackType.BACKEND_JAVA);
            
            DetectedStackDTO javaStack = stacks.stream()
                .filter(s -> s.getStackType() == TechStackType.BACKEND_JAVA)
                .findFirst()
                .orElseThrow();
            
            assertThat(javaStack.getConfidence()).isGreaterThanOrEqualTo(30);
            assertThat(javaStack.getPrimaryLanguage()).isEqualTo("Java");
            assertThat(javaStack.getRepositoryId()).isEqualTo(1L);
            
            verify(techStackRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("should detect React web stack")
        void shouldDetectReactStack() {
            when(techStackRepository.findByRepository(testRepo)).thenReturn(List.of());
            
            List<String> files = List.of(
                "package.json",
                "src/App.tsx",
                "src/index.tsx",
                "vite.config.ts",
                "src/components/Button.tsx"
            );

            List<DetectedStackDTO> stacks = service.detectStacks(testRepo, files);

            assertThat(stacks).isNotEmpty();
            assertThat(stacks).anyMatch(s -> s.getStackType() == TechStackType.WEB_REACT);
        }

        @Test
        @DisplayName("should detect Kotlin/Android mobile stack")
        void shouldDetectKotlinAndroidStack() {
            when(techStackRepository.findByRepository(testRepo)).thenReturn(List.of());
            
            List<String> files = List.of(
                "build.gradle.kts",
                "app/src/main/AndroidManifest.xml",
                "app/src/main/java/com/example/MainActivity.kt",
                "app/src/main/java/com/example/viewmodel/UserViewModel.kt"
            );

            List<DetectedStackDTO> stacks = service.detectStacks(testRepo, files);

            assertThat(stacks).isNotEmpty();
            assertThat(stacks).anyMatch(s -> s.getStackType() == TechStackType.MOBILE_KOTLIN);
            
            DetectedStackDTO kotlinStack = stacks.stream()
                .filter(s -> s.getStackType() == TechStackType.MOBILE_KOTLIN)
                .findFirst()
                .orElseThrow();
            
            assertThat(kotlinStack.getPrimaryLanguage()).isEqualTo("Kotlin");
        }

        @Test
        @DisplayName("should detect Swift/iOS mobile stack")
        void shouldDetectSwiftIOSStack() {
            when(techStackRepository.findByRepository(testRepo)).thenReturn(List.of());
            
            List<String> files = List.of(
                "Package.swift",
                "Sources/App/ContentView.swift",
                "Sources/App/AppDelegate.swift"
            );

            List<DetectedStackDTO> stacks = service.detectStacks(testRepo, files);

            assertThat(stacks).isNotEmpty();
            assertThat(stacks).anyMatch(s -> s.getStackType() == TechStackType.MOBILE_SWIFT);
        }

        @Test
        @DisplayName("should detect Node.js backend stack")
        void shouldDetectNodeStack() {
            when(techStackRepository.findByRepository(testRepo)).thenReturn(List.of());
            
            List<String> files = List.of(
                "package.json",
                "server.js",
                "src/routes/users.js",
                "src/middleware/auth.js"
            );

            List<DetectedStackDTO> stacks = service.detectStacks(testRepo, files);

            assertThat(stacks).isNotEmpty();
            assertThat(stacks).anyMatch(s -> s.getStackType() == TechStackType.BACKEND_NODE);
        }

        @Test
        @DisplayName("should detect Python backend stack")
        void shouldDetectPythonStack() {
            when(techStackRepository.findByRepository(testRepo)).thenReturn(List.of());
            
            List<String> files = List.of(
                "requirements.txt",
                "app.py",
                "src/models/user.py",
                "src/services/auth_service.py"
            );

            List<DetectedStackDTO> stacks = service.detectStacks(testRepo, files);

            assertThat(stacks).isNotEmpty();
            assertThat(stacks).anyMatch(s -> s.getStackType() == TechStackType.BACKEND_PYTHON);
        }

        @Test
        @DisplayName("should detect Angular web stack")
        void shouldDetectAngularStack() {
            when(techStackRepository.findByRepository(testRepo)).thenReturn(List.of());
            
            List<String> files = List.of(
                "angular.json",
                "package.json",
                "src/app/app.component.ts",
                "src/app/app.module.ts"
            );

            List<DetectedStackDTO> stacks = service.detectStacks(testRepo, files);

            assertThat(stacks).isNotEmpty();
            assertThat(stacks).anyMatch(s -> s.getStackType() == TechStackType.WEB_ANGULAR);
        }

        @Test
        @DisplayName("should detect Flutter mobile stack")
        void shouldDetectFlutterStack() {
            when(techStackRepository.findByRepository(testRepo)).thenReturn(List.of());
            
            List<String> files = List.of(
                "pubspec.yaml",
                "lib/main.dart",
                "lib/screens/home_screen.dart"
            );

            List<DetectedStackDTO> stacks = service.detectStacks(testRepo, files);

            assertThat(stacks).isNotEmpty();
            assertThat(stacks).anyMatch(s -> s.getStackType() == TechStackType.MOBILE_FLUTTER);
        }

        @Test
        @DisplayName("should detect multiple stacks in full-stack project")
        void shouldDetectMultipleStacks() {
            when(techStackRepository.findByRepository(testRepo)).thenReturn(List.of());
            
            List<String> files = List.of(
                // Backend (Java)
                "backend/pom.xml",
                "backend/src/main/java/Application.java",
                "backend/src/main/resources/application.properties",
                // Frontend (React)
                "frontend/package.json",
                "frontend/src/App.tsx",
                "frontend/vite.config.ts"
            );

            List<DetectedStackDTO> stacks = service.detectStacks(testRepo, files);

            assertThat(stacks.size()).isGreaterThanOrEqualTo(2);
            assertThat(stacks).anyMatch(s -> s.getStackType() == TechStackType.BACKEND_JAVA);
            assertThat(stacks).anyMatch(s -> s.getStackType() == TechStackType.WEB_REACT);
        }

        @Test
        @DisplayName("should return empty list for unknown project structure")
        void shouldReturnEmptyForUnknownStructure() {
            when(techStackRepository.findByRepository(testRepo)).thenReturn(List.of());
            
            List<String> files = List.of(
                "README.md",
                "LICENSE",
                "docs/guide.txt"
            );

            List<DetectedStackDTO> stacks = service.detectStacks(testRepo, files);

            assertThat(stacks).isEmpty();
        }

        @Test
        @DisplayName("should include key files in result")
        void shouldIncludeKeyFilesInResult() {
            when(techStackRepository.findByRepository(testRepo)).thenReturn(List.of());
            
            List<String> files = List.of(
                "pom.xml",
                "src/main/java/Application.java"
            );

            List<DetectedStackDTO> stacks = service.detectStacks(testRepo, files);

            assertThat(stacks).isNotEmpty();
            DetectedStackDTO stack = stacks.get(0);
            assertThat(stack.getKeyFilesFound()).isNotEmpty();
        }

        @Test
        @DisplayName("should use cached stacks when available")
        void shouldUseCachedStacks() {
            RepositoryTechStack cachedStack = RepositoryTechStack.builder()
                .id(1L)
                .repository(testRepo)
                .stackType(TechStackType.BACKEND_JAVA)
                .confidenceScore(95)
                .detectedByFiles("pom.xml,src/main/java/Application.java")
                .build();
            
            when(techStackRepository.findByRepository(testRepo)).thenReturn(List.of(cachedStack));

            List<String> files = List.of("pom.xml", "src/main/java/Application.java");
            List<DetectedStackDTO> stacks = service.detectStacks(testRepo, files);

            assertThat(stacks).hasSize(1);
            assertThat(stacks.get(0).getStackType()).isEqualTo(TechStackType.BACKEND_JAVA);
            assertThat(stacks.get(0).getConfidence()).isEqualTo(95);
            
            verify(techStackRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("should cache detected stacks for future use")
        void shouldCacheDetectedStacks() {
            when(techStackRepository.findByRepository(testRepo)).thenReturn(List.of());
            
            List<String> files = List.of("pom.xml", "src/main/java/Application.java");
            service.detectStacks(testRepo, files);

            verify(techStackRepository).saveAll(argThat(list -> {
                if (list instanceof List) {
                    List<?> l = (List<?>) list;
                    return !l.isEmpty() && 
                        l.get(0) instanceof RepositoryTechStack &&
                        ((RepositoryTechStack) l.get(0)).getStackType() == TechStackType.BACKEND_JAVA;
                }
                return false;
            }));
        }
    }

    @Nested
    @DisplayName("clearCache")
    class ClearCache {

        @Test
        @DisplayName("should clear cache for repository")
        void shouldClearCacheForRepository() {
            service.clearCache(testRepo);

            verify(techStackRepository).deleteByRepository(testRepo);
        }
    }

    @Nested
    @DisplayName("detectStacksFromPackageJson")
    class DetectStacksFromPackageJson {

        @Test
        @DisplayName("should detect React from package.json")
        void shouldDetectReactFromPackageJson() {
            String packageJson = """
                {
                    "dependencies": {
                        "react": "^18.2.0",
                        "react-dom": "^18.2.0"
                    }
                }
                """;

            List<DetectedStackDTO> stacks = service.detectStacksFromPackageJson(testRepo, packageJson);

            assertThat(stacks).anyMatch(s -> s.getStackType() == TechStackType.WEB_REACT);
        }

        @Test
        @DisplayName("should detect React Native from package.json")
        void shouldDetectReactNativeFromPackageJson() {
            String packageJson = """
                {
                    "dependencies": {
                        "react-native": "^0.72.0",
                        "react": "18.2.0"
                    }
                }
                """;

            List<DetectedStackDTO> stacks = service.detectStacksFromPackageJson(testRepo, packageJson);

            assertThat(stacks).anyMatch(s -> s.getStackType() == TechStackType.MOBILE_REACT_NATIVE);
            // Should not detect WEB_REACT when react-native is present
            assertThat(stacks).noneMatch(s -> s.getStackType() == TechStackType.WEB_REACT);
        }

        @Test
        @DisplayName("should detect Next.js from package.json")
        void shouldDetectNextJsFromPackageJson() {
            String packageJson = """
                {
                    "dependencies": {
                        "next": "^14.0.0",
                        "react": "^18.2.0"
                    }
                }
                """;

            List<DetectedStackDTO> stacks = service.detectStacksFromPackageJson(testRepo, packageJson);

            assertThat(stacks).anyMatch(s -> s.getStackType() == TechStackType.WEB_NEXTJS);
        }

        @Test
        @DisplayName("should detect Angular from package.json")
        void shouldDetectAngularFromPackageJson() {
            String packageJson = """
                {
                    "dependencies": {
                        "@angular/core": "^17.0.0",
                        "@angular/common": "^17.0.0"
                    }
                }
                """;

            List<DetectedStackDTO> stacks = service.detectStacksFromPackageJson(testRepo, packageJson);

            assertThat(stacks).anyMatch(s -> s.getStackType() == TechStackType.WEB_ANGULAR);
        }

        @Test
        @DisplayName("should detect Express.js backend from package.json")
        void shouldDetectExpressFromPackageJson() {
            String packageJson = """
                {
                    "dependencies": {
                        "express": "^4.18.0"
                    }
                }
                """;

            List<DetectedStackDTO> stacks = service.detectStacksFromPackageJson(testRepo, packageJson);

            assertThat(stacks).anyMatch(s -> s.getStackType() == TechStackType.BACKEND_NODE);
            
            DetectedStackDTO nodeStack = stacks.stream()
                .filter(s -> s.getStackType() == TechStackType.BACKEND_NODE)
                .findFirst()
                .orElseThrow();
            
            assertThat(nodeStack.getFramework()).isEqualTo("Express.js");
        }

        @Test
        @DisplayName("should detect NestJS backend from package.json")
        void shouldDetectNestJsFromPackageJson() {
            String packageJson = """
                {
                    "dependencies": {
                        "@nestjs/core": "^10.0.0",
                        "@nestjs/common": "^10.0.0"
                    }
                }
                """;

            List<DetectedStackDTO> stacks = service.detectStacksFromPackageJson(testRepo, packageJson);

            assertThat(stacks).anyMatch(s -> s.getStackType() == TechStackType.BACKEND_NODE);
            
            DetectedStackDTO nodeStack = stacks.stream()
                .filter(s -> s.getStackType() == TechStackType.BACKEND_NODE)
                .findFirst()
                .orElseThrow();
            
            assertThat(nodeStack.getFramework()).isEqualTo("NestJS");
        }

        @Test
        @DisplayName("should detect Vue.js from package.json")
        void shouldDetectVueFromPackageJson() {
            String packageJson = """
                {
                    "dependencies": {
                        "vue": "^3.3.0"
                    }
                }
                """;

            List<DetectedStackDTO> stacks = service.detectStacksFromPackageJson(testRepo, packageJson);

            assertThat(stacks).anyMatch(s -> s.getStackType() == TechStackType.WEB_VUE);
        }
    }
}
