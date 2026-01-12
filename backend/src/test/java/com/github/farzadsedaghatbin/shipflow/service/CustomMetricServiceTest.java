package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.metrics.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import com.github.farzadsedaghatbin.shipflow.service.MetricFormulaParser.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomMetricService Tests")
class CustomMetricServiceTest {

    @Mock
    private CustomMetricRepository customMetricRepository;
    
    @Mock
    private MetricDataPointRepository metricDataPointRepository;
    
    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PitchRepository pitchRepository;
    
    @Mock
    private CycleRepository cycleRepository;
    
    @Mock
    private WorkLogRepository workLogRepository;
    
    @Mock
    private TaskRepository taskRepository;
    
    @Mock
    private MetricFormulaParser formulaParser;
    
    @Mock
    private TeamRepository teamRepository;

    @InjectMocks
    private CustomMetricService customMetricService;

    private User testUser;
    private CustomMetric testMetric;
    private Cycle testCycle;
    private Pitch testPitch;
    private Team testTeam;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        testCycle = new Cycle();
        testCycle.setId(10L);
        testCycle.setName("Q1 2026");

        testPitch = new Pitch();
        testPitch.setId(20L);
        testPitch.setTitle("Feature X");

        testTeam = new Team();
        testTeam.setId(30L);
        testTeam.setName("Backend Team");

        testMetric = CustomMetric.builder()
                .id(1L)
                .user(testUser)
                .name("Test Metric")
                .description("Test Description")
                .formula("SUM(hours)")
                .dataSource(CustomMetric.DataSource.WORK_LOG)
                .aggregationType(CustomMetric.AggregationType.SUM)
                .displayFormat(CustomMetric.DisplayFormat.NUMBER)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Create Metric Tests")
    class CreateMetricTests {

        @Test
        @DisplayName("Should successfully create a new metric")
        void shouldCreateMetric() {
            // Arrange
            CreateCustomMetricRequest request = CreateCustomMetricRequest.builder()
                    .name("New Metric")
                    .description("Description")
                    .formula("10 + 20")
                    .dataSource(CustomMetric.DataSource.CUSTOM)
                    .aggregationType(CustomMetric.AggregationType.SUM)
                    .displayFormat(CustomMetric.DisplayFormat.NUMBER)
                    .isActive(true)
                    .build();

            when(formulaParser.validateFormula(anyString())).thenReturn(ValidationResult.success());
            when(customMetricRepository.existsByUserIdAndName(1L, "New Metric")).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(customMetricRepository.save(any(CustomMetric.class))).thenAnswer(invocation -> {
                CustomMetric metric = invocation.getArgument(0);
                metric.setId(2L);
                metric.setCreatedAt(LocalDateTime.now());
                metric.setUpdatedAt(LocalDateTime.now());
                return metric;
            });

            // Act
            CustomMetricDTO result = customMetricService.createMetric(1L, request);

            // Assert
            assertNotNull(result);
            assertEquals(2L, result.getId());
            assertEquals("New Metric", result.getName());
            assertEquals("Description", result.getDescription());
            verify(customMetricRepository).save(any(CustomMetric.class));
        }

        @Test
        @DisplayName("Should throw exception for invalid formula")
        void shouldThrowExceptionForInvalidFormula() {
            // Arrange
            CreateCustomMetricRequest request = CreateCustomMetricRequest.builder()
                    .name("Invalid Metric")
                    .formula("INVALID(formula")
                    .dataSource(CustomMetric.DataSource.CUSTOM)
                    .build();

            when(formulaParser.validateFormula("INVALID(formula"))
                    .thenReturn(ValidationResult.error("Unbalanced parentheses"));

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> customMetricService.createMetric(1L, request));
            
            assertTrue(exception.getMessage().contains("Invalid formula"));
            verify(customMetricRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception for duplicate metric name")
        void shouldThrowExceptionForDuplicateName() {
            // Arrange
            CreateCustomMetricRequest request = CreateCustomMetricRequest.builder()
                    .name("Duplicate Metric")
                    .formula("10 + 20")
                    .dataSource(CustomMetric.DataSource.CUSTOM)
                    .build();

            when(formulaParser.validateFormula(anyString())).thenReturn(ValidationResult.success());
            when(customMetricRepository.existsByUserIdAndName(1L, "Duplicate Metric")).thenReturn(true);

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> customMetricService.createMetric(1L, request));
            
            assertTrue(exception.getMessage().contains("already exists"));
            verify(customMetricRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Arrange
            CreateCustomMetricRequest request = CreateCustomMetricRequest.builder()
                    .name("Metric")
                    .formula("10")
                    .dataSource(CustomMetric.DataSource.CUSTOM)
                    .build();

            when(formulaParser.validateFormula(anyString())).thenReturn(ValidationResult.success());
            when(customMetricRepository.existsByUserIdAndName(anyLong(), anyString())).thenReturn(false);
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> customMetricService.createMetric(999L, request));
        }
    }

    @Nested
    @DisplayName("Update Metric Tests")
    class UpdateMetricTests {

        @Test
        @DisplayName("Should successfully update metric")
        void shouldUpdateMetric() {
            // Arrange
            UpdateCustomMetricRequest request = UpdateCustomMetricRequest.builder()
                    .name("Updated Name")
                    .description("Updated Description")
                    .build();

            when(customMetricRepository.findById(1L)).thenReturn(Optional.of(testMetric));
            when(customMetricRepository.existsByUserIdAndName(1L, "Updated Name")).thenReturn(false);
            when(customMetricRepository.save(any(CustomMetric.class))).thenReturn(testMetric);

            // Act
            CustomMetricDTO result = customMetricService.updateMetric(1L, 1L, request);

            // Assert
            assertNotNull(result);
            assertEquals("Updated Name", testMetric.getName());
            assertEquals("Updated Description", testMetric.getDescription());
            verify(customMetricRepository).save(testMetric);
        }

        @Test
        @DisplayName("Should throw exception when metric not found")
        void shouldThrowExceptionWhenMetricNotFound() {
            // Arrange
            UpdateCustomMetricRequest request = UpdateCustomMetricRequest.builder()
                    .name("Name")
                    .build();

            when(customMetricRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> customMetricService.updateMetric(1L, 999L, request));
        }

        @Test
        @DisplayName("Should throw exception when user not authorized")
        void shouldThrowExceptionWhenNotAuthorized() {
            // Arrange
            UpdateCustomMetricRequest request = UpdateCustomMetricRequest.builder()
                    .name("Name")
                    .build();

            when(customMetricRepository.findById(1L)).thenReturn(Optional.of(testMetric));

            // Act & Assert
            SecurityException exception = assertThrows(SecurityException.class,
                    () -> customMetricService.updateMetric(999L, 1L, request));
            
            assertTrue(exception.getMessage().contains("Not authorized"));
        }
    }

    @Nested
    @DisplayName("Delete Metric Tests")
    class DeleteMetricTests {

        @Test
        @DisplayName("Should successfully delete metric")
        void shouldDeleteMetric() {
            // Arrange
            when(customMetricRepository.findById(1L)).thenReturn(Optional.of(testMetric));
            doNothing().when(customMetricRepository).delete(testMetric);

            // Act
            customMetricService.deleteMetric(1L, 1L);

            // Assert
            verify(customMetricRepository).delete(testMetric);
        }

        @Test
        @DisplayName("Should throw exception when deleting non-existent metric")
        void shouldThrowExceptionWhenDeletingNonExistentMetric() {
            // Arrange
            when(customMetricRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> customMetricService.deleteMetric(1L, 999L));
        }

        @Test
        @DisplayName("Should throw exception when user not authorized to delete")
        void shouldThrowExceptionWhenNotAuthorizedToDelete() {
            // Arrange
            when(customMetricRepository.findById(1L)).thenReturn(Optional.of(testMetric));

            // Act & Assert
            assertThrows(SecurityException.class,
                    () -> customMetricService.deleteMetric(999L, 1L));
        }
    }

    @Nested
    @DisplayName("Get Metric Tests")
    class GetMetricTests {

        @Test
        @DisplayName("Should get all metrics for user")
        void shouldGetAllMetricsForUser() {
            // Arrange
            CustomMetric metric2 = CustomMetric.builder()
                    .id(2L)
                    .user(testUser)
                    .name("Metric 2")
                    .formula("20")
                    .dataSource(CustomMetric.DataSource.CUSTOM)
                    .aggregationType(CustomMetric.AggregationType.AVG)
                    .displayFormat(CustomMetric.DisplayFormat.NUMBER)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(customMetricRepository.findByUserIdOrderByNameAsc(1L)).thenReturn(Arrays.asList(testMetric, metric2));

            // Act
            List<CustomMetricDTO> results = customMetricService.getUserMetrics(1L);

            // Assert
            assertEquals(2, results.size());
            assertEquals("Test Metric", results.get(0).getName());
            assertEquals("Metric 2", results.get(1).getName());
        }

        @Test
        @DisplayName("Should get specific metric by ID")
        void shouldGetMetricById() {
            // Arrange
            when(customMetricRepository.findById(1L)).thenReturn(Optional.of(testMetric));

            // Act
            CustomMetricDTO result = customMetricService.getMetric(1L, 1L);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getId());
            assertEquals("Test Metric", result.getName());
        }

        @Test
        @DisplayName("Should throw exception when getting non-existent metric")
        void shouldThrowExceptionWhenGettingNonExistentMetric() {
            // Arrange
            when(customMetricRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> customMetricService.getMetric(1L, 999L));
        }

        @Test
        @DisplayName("Should throw exception when user not authorized to view")
        void shouldThrowExceptionWhenNotAuthorizedToView() {
            // Arrange
            when(customMetricRepository.findById(1L)).thenReturn(Optional.of(testMetric));

            // Act & Assert
            assertThrows(SecurityException.class,
                    () -> customMetricService.getMetric(999L, 1L));
        }
    }

    @Nested
    @DisplayName("Calculate Metric Tests")
    class CalculateMetricTests {

        @Test
        @DisplayName("Should calculate metric value successfully")
        void shouldCalculateMetricValue() {
            // Arrange
            when(customMetricRepository.findById(1L)).thenReturn(Optional.of(testMetric));
            when(workLogRepository.findAll()).thenReturn(createMockWorkLogs());
            when(formulaParser.evaluateFormula(anyString(), anyMap())).thenReturn(new BigDecimal("100"));
            when(metricDataPointRepository.save(any(MetricDataPoint.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            MetricValueDTO result = customMetricService.calculateMetricValue(1L, 1L);

            // Assert
            assertNotNull(result);
            assertEquals(1L, result.getMetricId());
            assertEquals("Test Metric", result.getMetricName());
            assertEquals(new BigDecimal("100"), result.getValue());
            assertNotNull(result.getDisplayValue());
            verify(metricDataPointRepository).save(any(MetricDataPoint.class));
        }

        @Test
        @DisplayName("Should throw exception when calculating non-existent metric")
        void shouldThrowExceptionWhenCalculatingNonExistentMetric() {
            // Arrange
            when(customMetricRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(IllegalArgumentException.class,
                    () -> customMetricService.calculateMetricValue(1L, 999L));
        }

        @Test
        @DisplayName("Should throw exception when user not authorized to calculate")
        void shouldThrowExceptionWhenNotAuthorizedToCalculate() {
            // Arrange
            when(customMetricRepository.findById(1L)).thenReturn(Optional.of(testMetric));

            // Act & Assert
            assertThrows(SecurityException.class,
                    () -> customMetricService.calculateMetricValue(999L, 1L));
        }
    }

    @Nested
    @DisplayName("Metric History Tests")
    class MetricHistoryTests {

        @Test
        @DisplayName("Should get metric history")
        void shouldGetMetricHistory() {
            // Arrange
            List<MetricDataPoint> dataPoints = Arrays.asList(
                    MetricDataPoint.builder()
                            .metric(testMetric)
                            .calculatedValue(new BigDecimal("100"))
                            .calculationDate(LocalDateTime.now())
                            .metadata("{}")
                            .build(),
                    MetricDataPoint.builder()
                            .metric(testMetric)
                            .calculatedValue(new BigDecimal("150"))
                            .calculationDate(LocalDateTime.now().minusDays(1))
                            .metadata("{}")
                            .build()
            );

            when(customMetricRepository.findById(1L)).thenReturn(Optional.of(testMetric));
            when(metricDataPointRepository.findTopNByMetricId(1L, 30)).thenReturn(dataPoints);

            // Act
            List<MetricValueDTO> results = customMetricService.getMetricHistory(1L, 1L, 30);

            // Assert
            assertEquals(2, results.size());
            assertEquals(new BigDecimal("100"), results.get(0).getValue());
            assertEquals(new BigDecimal("150"), results.get(1).getValue());
        }
    }

    @Nested
    @DisplayName("Formula Validation Tests")
    class FormulaValidationTests {

        @Test
        @DisplayName("Should validate valid formula")
        void shouldValidateValidFormula() {
            // Arrange
            when(formulaParser.validateFormula("10 + 20")).thenReturn(ValidationResult.success());

            // Act
            ValidationResult result = customMetricService.validateFormula("10 + 20");

            // Assert
            assertTrue(result.isValid());
            assertNull(result.getErrorMessage());
        }

        @Test
        @DisplayName("Should return error for invalid formula")
        void shouldReturnErrorForInvalidFormula() {
            // Arrange
            when(formulaParser.validateFormula("invalid(")).thenReturn(ValidationResult.error("Unbalanced parentheses"));

            // Act
            ValidationResult result = customMetricService.validateFormula("invalid(");

            // Assert
            assertFalse(result.isValid());
            assertEquals("Unbalanced parentheses", result.getErrorMessage());
        }
    }

    @Nested
    @DisplayName("Scope Feature Tests")
    class ScopeFeatureTests {

        @Test
        @DisplayName("Should create metric with cycle scope")
        void shouldCreateMetricWithCycleScope() {
            // Arrange
            CreateCustomMetricRequest request = CreateCustomMetricRequest.builder()
                    .name("Cycle Metric")
                    .description("Metric for specific cycle")
                    .formula("10 + 20")
                    .dataSource(CustomMetric.DataSource.CUSTOM)
                    .aggregationType(CustomMetric.AggregationType.SUM)
                    .displayFormat(CustomMetric.DisplayFormat.NUMBER)
                    .cycleId(10L)
                    .isActive(true)
                    .build();

            when(formulaParser.validateFormula(anyString())).thenReturn(ValidationResult.success());
            when(customMetricRepository.existsByUserIdAndName(1L, "Cycle Metric")).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(cycleRepository.findById(10L)).thenReturn(Optional.of(testCycle));
            when(customMetricRepository.save(any(CustomMetric.class))).thenAnswer(invocation -> {
                CustomMetric metric = invocation.getArgument(0);
                metric.setId(100L);
                metric.setCreatedAt(LocalDateTime.now());
                metric.setUpdatedAt(LocalDateTime.now());
                return metric;
            });

            // Act
            CustomMetricDTO result = customMetricService.createMetric(1L, request);

            // Assert
            assertNotNull(result);
            assertEquals("Cycle Metric", result.getName());
            assertEquals(10L, result.getCycleId());
            assertEquals("Q1 2026", result.getCycleName());
            verify(cycleRepository).findById(10L);
            verify(customMetricRepository).save(argThat(metric -> 
                metric.getCycle() != null && metric.getCycle().getId().equals(10L)
            ));
        }

        @Test
        @DisplayName("Should create metric with pitch scope")
        void shouldCreateMetricWithPitchScope() {
            // Arrange
            CreateCustomMetricRequest request = CreateCustomMetricRequest.builder()
                    .name("Pitch Metric")
                    .description("Metric for specific pitch")
                    .formula("COUNT(tasks)")
                    .dataSource(CustomMetric.DataSource.TASK)
                    .aggregationType(CustomMetric.AggregationType.COUNT)
                    .displayFormat(CustomMetric.DisplayFormat.NUMBER)
                    .pitchId(20L)
                    .isActive(true)
                    .build();

            when(formulaParser.validateFormula(anyString())).thenReturn(ValidationResult.success());
            when(customMetricRepository.existsByUserIdAndName(1L, "Pitch Metric")).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(pitchRepository.findById(20L)).thenReturn(Optional.of(testPitch));
            when(customMetricRepository.save(any(CustomMetric.class))).thenAnswer(invocation -> {
                CustomMetric metric = invocation.getArgument(0);
                metric.setId(101L);
                metric.setCreatedAt(LocalDateTime.now());
                metric.setUpdatedAt(LocalDateTime.now());
                return metric;
            });

            // Act
            CustomMetricDTO result = customMetricService.createMetric(1L, request);

            // Assert
            assertNotNull(result);
            assertEquals("Pitch Metric", result.getName());
            assertEquals(20L, result.getPitchId());
            assertEquals("Feature X", result.getPitchName());
            verify(pitchRepository).findById(20L);
            verify(customMetricRepository).save(argThat(metric -> 
                metric.getPitch() != null && metric.getPitch().getId().equals(20L)
            ));
        }

        @Test
        @DisplayName("Should create metric with team scope")
        void shouldCreateMetricWithTeamScope() {
            // Arrange
            CreateCustomMetricRequest request = CreateCustomMetricRequest.builder()
                    .name("Team Metric")
                    .description("Metric for specific team")
                    .formula("AVG(velocity)")
                    .dataSource(CustomMetric.DataSource.CUSTOM)
                    .aggregationType(CustomMetric.AggregationType.AVG)
                    .displayFormat(CustomMetric.DisplayFormat.DECIMAL)
                    .teamId(30L)
                    .isActive(true)
                    .build();

            when(formulaParser.validateFormula(anyString())).thenReturn(ValidationResult.success());
            when(customMetricRepository.existsByUserIdAndName(1L, "Team Metric")).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(teamRepository.findById(30L)).thenReturn(Optional.of(testTeam));
            when(customMetricRepository.save(any(CustomMetric.class))).thenAnswer(invocation -> {
                CustomMetric metric = invocation.getArgument(0);
                metric.setId(102L);
                metric.setCreatedAt(LocalDateTime.now());
                metric.setUpdatedAt(LocalDateTime.now());
                return metric;
            });

            // Act
            CustomMetricDTO result = customMetricService.createMetric(1L, request);

            // Assert
            assertNotNull(result);
            assertEquals("Team Metric", result.getName());
            assertEquals(30L, result.getTeamId());
            assertEquals("Backend Team", result.getTeamName());
            verify(teamRepository).findById(30L);
            verify(customMetricRepository).save(argThat(metric -> 
                metric.getTeam() != null && metric.getTeam().getId().equals(30L)
            ));
        }

        @Test
        @DisplayName("Should create metric with multiple scopes")
        void shouldCreateMetricWithMultipleScopes() {
            // Arrange
            CreateCustomMetricRequest request = CreateCustomMetricRequest.builder()
                    .name("Multi-Scope Metric")
                    .description("Metric with cycle, pitch, and team scope")
                    .formula("SUM(hours)")
                    .dataSource(CustomMetric.DataSource.WORK_LOG)
                    .aggregationType(CustomMetric.AggregationType.SUM)
                    .displayFormat(CustomMetric.DisplayFormat.NUMBER)
                    .cycleId(10L)
                    .pitchId(20L)
                    .teamId(30L)
                    .isActive(true)
                    .build();

            when(formulaParser.validateFormula(anyString())).thenReturn(ValidationResult.success());
            when(customMetricRepository.existsByUserIdAndName(1L, "Multi-Scope Metric")).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(cycleRepository.findById(10L)).thenReturn(Optional.of(testCycle));
            when(pitchRepository.findById(20L)).thenReturn(Optional.of(testPitch));
            when(teamRepository.findById(30L)).thenReturn(Optional.of(testTeam));
            when(customMetricRepository.save(any(CustomMetric.class))).thenAnswer(invocation -> {
                CustomMetric metric = invocation.getArgument(0);
                metric.setId(103L);
                metric.setCreatedAt(LocalDateTime.now());
                metric.setUpdatedAt(LocalDateTime.now());
                return metric;
            });

            // Act
            CustomMetricDTO result = customMetricService.createMetric(1L, request);

            // Assert
            assertNotNull(result);
            assertEquals("Multi-Scope Metric", result.getName());
            assertEquals(10L, result.getCycleId());
            assertEquals("Q1 2026", result.getCycleName());
            assertEquals(20L, result.getPitchId());
            assertEquals("Feature X", result.getPitchName());
            assertEquals(30L, result.getTeamId());
            assertEquals("Backend Team", result.getTeamName());
            verify(cycleRepository).findById(10L);
            verify(pitchRepository).findById(20L);
            verify(teamRepository).findById(30L);
        }

        @Test
        @DisplayName("Should throw exception for invalid cycle ID")
        void shouldThrowExceptionForInvalidCycleId() {
            // Arrange
            CreateCustomMetricRequest request = CreateCustomMetricRequest.builder()
                    .name("Invalid Cycle Metric")
                    .formula("10 + 20")
                    .dataSource(CustomMetric.DataSource.CUSTOM)
                    .cycleId(999L)
                    .isActive(true)
                    .build();

            when(formulaParser.validateFormula(anyString())).thenReturn(ValidationResult.success());
            when(customMetricRepository.existsByUserIdAndName(1L, "Invalid Cycle Metric")).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(cycleRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> customMetricService.createMetric(1L, request));
            
            assertTrue(exception.getMessage().contains("Cycle not found"));
            verify(customMetricRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception for invalid pitch ID")
        void shouldThrowExceptionForInvalidPitchId() {
            // Arrange
            CreateCustomMetricRequest request = CreateCustomMetricRequest.builder()
                    .name("Invalid Pitch Metric")
                    .formula("COUNT(*)")
                    .dataSource(CustomMetric.DataSource.PITCH)
                    .pitchId(999L)
                    .isActive(true)
                    .build();

            when(formulaParser.validateFormula(anyString())).thenReturn(ValidationResult.success());
            when(customMetricRepository.existsByUserIdAndName(1L, "Invalid Pitch Metric")).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(pitchRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> customMetricService.createMetric(1L, request));
            
            assertTrue(exception.getMessage().contains("Pitch not found"));
            verify(customMetricRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception for invalid team ID")
        void shouldThrowExceptionForInvalidTeamId() {
            // Arrange
            CreateCustomMetricRequest request = CreateCustomMetricRequest.builder()
                    .name("Invalid Team Metric")
                    .formula("AVG(velocity)")
                    .dataSource(CustomMetric.DataSource.CUSTOM)
                    .teamId(999L)
                    .isActive(true)
                    .build();

            when(formulaParser.validateFormula(anyString())).thenReturn(ValidationResult.success());
            when(customMetricRepository.existsByUserIdAndName(1L, "Invalid Team Metric")).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(teamRepository.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> customMetricService.createMetric(1L, request));
            
            assertTrue(exception.getMessage().contains("Team not found"));
            verify(customMetricRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should create metric without scope (global metric)")
        void shouldCreateGlobalMetric() {
            // Arrange
            CreateCustomMetricRequest request = CreateCustomMetricRequest.builder()
                    .name("Global Metric")
                    .description("Organization-wide metric")
                    .formula("SUM(all_hours)")
                    .dataSource(CustomMetric.DataSource.WORK_LOG)
                    .aggregationType(CustomMetric.AggregationType.SUM)
                    .displayFormat(CustomMetric.DisplayFormat.NUMBER)
                    .isActive(true)
                    .build();

            when(formulaParser.validateFormula(anyString())).thenReturn(ValidationResult.success());
            when(customMetricRepository.existsByUserIdAndName(1L, "Global Metric")).thenReturn(false);
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(customMetricRepository.save(any(CustomMetric.class))).thenAnswer(invocation -> {
                CustomMetric metric = invocation.getArgument(0);
                metric.setId(104L);
                metric.setCreatedAt(LocalDateTime.now());
                metric.setUpdatedAt(LocalDateTime.now());
                return metric;
            });

            // Act
            CustomMetricDTO result = customMetricService.createMetric(1L, request);

            // Assert
            assertNotNull(result);
            assertEquals("Global Metric", result.getName());
            assertNull(result.getCycleId());
            assertNull(result.getCycleName());
            assertNull(result.getPitchId());
            assertNull(result.getPitchName());
            assertNull(result.getTeamId());
            assertNull(result.getTeamName());
            verify(cycleRepository, never()).findById(anyLong());
            verify(pitchRepository, never()).findById(anyLong());
            verify(teamRepository, never()).findById(anyLong());
        }
    }

    private List<WorkLog> createMockWorkLogs() {
        WorkLog wl1 = new WorkLog();
        wl1.setHoursSpent(BigDecimal.valueOf(8.0));
        
        WorkLog wl2 = new WorkLog();
        wl2.setHoursSpent(BigDecimal.valueOf(6.5));
        
        return Arrays.asList(wl1, wl2);
    }
}
