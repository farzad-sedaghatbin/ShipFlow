package com.github.farzadsedaghatbin.shipflow.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.customdashboard.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomDashboardService Tests")
class CustomDashboardServiceTest {

  @Mock private CustomDashboardRepository customDashboardRepository;

  @Mock private DashboardWidgetConfigRepository widgetConfigRepository;

  @Mock private UserRepository userRepository;

  @Mock private CycleRepository cycleRepository;

  @Mock private PitchRepository pitchRepository;

  @Mock private TeamRepository teamRepository;

  @Mock private LocalizationService localizationService;

  @Mock private MessageService messageService;

  @InjectMocks private CustomDashboardService customDashboardService;

  private User testUser;
  private CustomDashboard testDashboard;
  private Cycle testCycle;
  private Pitch testPitch;
  private Team testTeam;

  @BeforeEach
  void setUp() {
    lenient()
        .when(localizationService.getMessage(anyString(), any(Object[].class)))
        .thenAnswer(
            i -> {
              String key = i.getArgument(0);
              if (key.contains("unauthorized")) return "Not authorized to access this dashboard";
              if (key.contains("dashboard.not.found")) return "Dashboard not found";
              if (key.contains("dashboard.name.duplicate")) return "Dashboard name already exists";
              return key;
            });
    lenient()
        .when(localizationService.getMessage(anyString()))
        .thenAnswer(
            i -> {
              String key = i.getArgument(0);
              if (key.contains("unauthorized")) return "Not authorized to access this dashboard";
              if (key.contains("dashboard.not.found")) return "Dashboard not found";
              if (key.contains("dashboard.name.duplicate")) return "Dashboard name already exists";
              return key;
            });
    lenient()
        .when(messageService.getMessage(anyString(), any(Object[].class)))
        .thenAnswer(
            i -> {
              String key = i.getArgument(0);
              if (key.contains("unauthorized")) return "Not authorized to access this dashboard";
              if (key.contains("dashboard.not.found")) return "Dashboard not found";
              return key;
            });
    lenient()
        .when(messageService.getMessage(anyString()))
        .thenAnswer(
            i -> {
              String key = i.getArgument(0);
              if (key.contains("unauthorized")) return "Not authorized to access this dashboard";
              if (key.contains("dashboard.not.found")) return "Dashboard not found";
              return key;
            });

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

    testDashboard =
        CustomDashboard.builder()
            .id(1L)
            .user(testUser)
            .name("Test Dashboard")
            .description("Test Description")
            .isDefault(false)
            .isTemplate(false)
            .layoutConfig("{}")
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
  }

  @Nested
  @DisplayName("Scope Feature Tests")
  class ScopeFeatureTests {

    @Test
    @DisplayName("Should create dashboard with cycle scope")
    void shouldCreateDashboardWithCycleScope() {
      // Arrange
      CreateDashboardRequest request =
          CreateDashboardRequest.builder()
              .name("Cycle Dashboard")
              .description("Dashboard for specific cycle")
              .cycleId(10L)
              .layoutConfig("{}")
              .build();

      when(customDashboardRepository.existsByUserIdAndName(1L, "Cycle Dashboard"))
          .thenReturn(false);
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(cycleRepository.findById(10L)).thenReturn(Optional.of(testCycle));
      when(customDashboardRepository.save(any(CustomDashboard.class)))
          .thenAnswer(
              invocation -> {
                CustomDashboard dashboard = invocation.getArgument(0);
                dashboard.setId(100L);
                dashboard.setCreatedAt(LocalDateTime.now());
                dashboard.setUpdatedAt(LocalDateTime.now());
                return dashboard;
              });
      when(widgetConfigRepository.countByDashboardId(100L)).thenReturn(0L);

      // Act
      CustomDashboardDTO result = customDashboardService.createDashboard(1L, request);

      // Assert
      assertNotNull(result);
      assertEquals("Cycle Dashboard", result.getName());
      assertEquals(10L, result.getCycleId());
      assertEquals("Q1 2026", result.getCycleName());
      verify(cycleRepository).findById(10L);
      verify(customDashboardRepository)
          .save(
              argThat(
                  dashboard ->
                      dashboard.getCycle() != null && dashboard.getCycle().getId().equals(10L)));
    }

    @Test
    @DisplayName("Should create dashboard with pitch scope")
    void shouldCreateDashboardWithPitchScope() {
      // Arrange
      CreateDashboardRequest request =
          CreateDashboardRequest.builder()
              .name("Pitch Dashboard")
              .description("Dashboard for specific pitch")
              .pitchId(20L)
              .layoutConfig("{}")
              .build();

      when(customDashboardRepository.existsByUserIdAndName(1L, "Pitch Dashboard"))
          .thenReturn(false);
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(pitchRepository.findById(20L)).thenReturn(Optional.of(testPitch));
      when(customDashboardRepository.save(any(CustomDashboard.class)))
          .thenAnswer(
              invocation -> {
                CustomDashboard dashboard = invocation.getArgument(0);
                dashboard.setId(101L);
                dashboard.setCreatedAt(LocalDateTime.now());
                dashboard.setUpdatedAt(LocalDateTime.now());
                return dashboard;
              });
      when(widgetConfigRepository.countByDashboardId(101L)).thenReturn(0L);

      // Act
      CustomDashboardDTO result = customDashboardService.createDashboard(1L, request);

      // Assert
      assertNotNull(result);
      assertEquals("Pitch Dashboard", result.getName());
      assertEquals(20L, result.getPitchId());
      assertEquals("Feature X", result.getPitchName());
      verify(pitchRepository).findById(20L);
      verify(customDashboardRepository)
          .save(
              argThat(
                  dashboard ->
                      dashboard.getPitch() != null && dashboard.getPitch().getId().equals(20L)));
    }

    @Test
    @DisplayName("Should create dashboard with team scope")
    void shouldCreateDashboardWithTeamScope() {
      // Arrange
      CreateDashboardRequest request =
          CreateDashboardRequest.builder()
              .name("Team Dashboard")
              .description("Dashboard for specific team")
              .teamId(30L)
              .layoutConfig("{}")
              .build();

      when(customDashboardRepository.existsByUserIdAndName(1L, "Team Dashboard")).thenReturn(false);
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(teamRepository.findById(30L)).thenReturn(Optional.of(testTeam));
      when(customDashboardRepository.save(any(CustomDashboard.class)))
          .thenAnswer(
              invocation -> {
                CustomDashboard dashboard = invocation.getArgument(0);
                dashboard.setId(102L);
                dashboard.setCreatedAt(LocalDateTime.now());
                dashboard.setUpdatedAt(LocalDateTime.now());
                return dashboard;
              });
      when(widgetConfigRepository.countByDashboardId(102L)).thenReturn(0L);

      // Act
      CustomDashboardDTO result = customDashboardService.createDashboard(1L, request);

      // Assert
      assertNotNull(result);
      assertEquals("Team Dashboard", result.getName());
      assertEquals(30L, result.getTeamId());
      assertEquals("Backend Team", result.getTeamName());
      verify(teamRepository).findById(30L);
      verify(customDashboardRepository)
          .save(
              argThat(
                  dashboard ->
                      dashboard.getTeam() != null && dashboard.getTeam().getId().equals(30L)));
    }

    @Test
    @DisplayName("Should create dashboard with multiple scopes")
    void shouldCreateDashboardWithMultipleScopes() {
      // Arrange
      CreateDashboardRequest request =
          CreateDashboardRequest.builder()
              .name("Multi-Scope Dashboard")
              .description("Dashboard with cycle, pitch, and team scope")
              .cycleId(10L)
              .pitchId(20L)
              .teamId(30L)
              .layoutConfig("{}")
              .build();

      when(customDashboardRepository.existsByUserIdAndName(1L, "Multi-Scope Dashboard"))
          .thenReturn(false);
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(cycleRepository.findById(10L)).thenReturn(Optional.of(testCycle));
      when(pitchRepository.findById(20L)).thenReturn(Optional.of(testPitch));
      when(teamRepository.findById(30L)).thenReturn(Optional.of(testTeam));
      when(customDashboardRepository.save(any(CustomDashboard.class)))
          .thenAnswer(
              invocation -> {
                CustomDashboard dashboard = invocation.getArgument(0);
                dashboard.setId(103L);
                dashboard.setCreatedAt(LocalDateTime.now());
                dashboard.setUpdatedAt(LocalDateTime.now());
                return dashboard;
              });
      when(widgetConfigRepository.countByDashboardId(103L)).thenReturn(0L);

      // Act
      CustomDashboardDTO result = customDashboardService.createDashboard(1L, request);

      // Assert
      assertNotNull(result);
      assertEquals("Multi-Scope Dashboard", result.getName());
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
      CreateDashboardRequest request =
          CreateDashboardRequest.builder()
              .name("Invalid Cycle Dashboard")
              .cycleId(999L)
              .layoutConfig("{}")
              .build();

      when(customDashboardRepository.existsByUserIdAndName(1L, "Invalid Cycle Dashboard"))
          .thenReturn(false);
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(cycleRepository.findById(999L)).thenReturn(Optional.empty());

      // Act & Assert
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> customDashboardService.createDashboard(1L, request));

      assertTrue(exception.getMessage().contains("Cycle not found"));
      verify(customDashboardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception for invalid pitch ID")
    void shouldThrowExceptionForInvalidPitchId() {
      // Arrange
      CreateDashboardRequest request =
          CreateDashboardRequest.builder()
              .name("Invalid Pitch Dashboard")
              .pitchId(999L)
              .layoutConfig("{}")
              .build();

      when(customDashboardRepository.existsByUserIdAndName(1L, "Invalid Pitch Dashboard"))
          .thenReturn(false);
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(pitchRepository.findById(999L)).thenReturn(Optional.empty());

      // Act & Assert
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> customDashboardService.createDashboard(1L, request));

      assertTrue(exception.getMessage().contains("Pitch not found"));
      verify(customDashboardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception for invalid team ID")
    void shouldThrowExceptionForInvalidTeamId() {
      // Arrange
      CreateDashboardRequest request =
          CreateDashboardRequest.builder()
              .name("Invalid Team Dashboard")
              .teamId(999L)
              .layoutConfig("{}")
              .build();

      when(customDashboardRepository.existsByUserIdAndName(1L, "Invalid Team Dashboard"))
          .thenReturn(false);
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(teamRepository.findById(999L)).thenReturn(Optional.empty());

      // Act & Assert
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> customDashboardService.createDashboard(1L, request));

      assertTrue(exception.getMessage().contains("Team not found"));
      verify(customDashboardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should create dashboard without scope (global dashboard)")
    void shouldCreateGlobalDashboard() {
      // Arrange
      CreateDashboardRequest request =
          CreateDashboardRequest.builder()
              .name("Global Dashboard")
              .description("Organization-wide dashboard")
              .layoutConfig("{}")
              .build();

      when(customDashboardRepository.existsByUserIdAndName(1L, "Global Dashboard"))
          .thenReturn(false);
      when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
      when(customDashboardRepository.save(any(CustomDashboard.class)))
          .thenAnswer(
              invocation -> {
                CustomDashboard dashboard = invocation.getArgument(0);
                dashboard.setId(104L);
                dashboard.setCreatedAt(LocalDateTime.now());
                dashboard.setUpdatedAt(LocalDateTime.now());
                return dashboard;
              });
      when(widgetConfigRepository.countByDashboardId(104L)).thenReturn(0L);

      // Act
      CustomDashboardDTO result = customDashboardService.createDashboard(1L, request);

      // Assert
      assertNotNull(result);
      assertEquals("Global Dashboard", result.getName());
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

  @Nested
  @DisplayName("User Context Filter Tests")
  class UserContextFilterTests {

    @Test
    @DisplayName("Should toggle user context filter from false to true")
    void shouldToggleFilterFromFalseToTrue() {
      // Arrange
      testDashboard.setUserContextFilter(false);
      when(customDashboardRepository.findById(1L)).thenReturn(Optional.of(testDashboard));
      when(customDashboardRepository.save(any(CustomDashboard.class)))
          .thenAnswer(
              invocation -> {
                CustomDashboard dashboard = invocation.getArgument(0);
                return dashboard;
              });
      when(widgetConfigRepository.countByDashboardId(1L)).thenReturn(3L);

      // Act
      CustomDashboardDTO result = customDashboardService.toggleUserContextFilter(1L, 1L);

      // Assert
      assertNotNull(result);
      assertTrue(result.getUserContextFilter());
      verify(customDashboardRepository)
          .save(
              argThat(
                  dashboard ->
                      dashboard.getUserContextFilter() != null
                          && dashboard.getUserContextFilter()));
    }

    @Test
    @DisplayName("Should toggle user context filter from true to false")
    void shouldToggleFilterFromTrueToFalse() {
      // Arrange
      testDashboard.setUserContextFilter(true);
      when(customDashboardRepository.findById(1L)).thenReturn(Optional.of(testDashboard));
      when(customDashboardRepository.save(any(CustomDashboard.class)))
          .thenAnswer(
              invocation -> {
                CustomDashboard dashboard = invocation.getArgument(0);
                return dashboard;
              });
      when(widgetConfigRepository.countByDashboardId(1L)).thenReturn(3L);

      // Act
      CustomDashboardDTO result = customDashboardService.toggleUserContextFilter(1L, 1L);

      // Assert
      assertNotNull(result);
      assertFalse(result.getUserContextFilter());
      verify(customDashboardRepository)
          .save(
              argThat(
                  dashboard ->
                      dashboard.getUserContextFilter() != null
                          && !dashboard.getUserContextFilter()));
    }

    @Test
    @DisplayName("Should toggle filter from null to true")
    void shouldToggleFilterFromNullToTrue() {
      // Arrange
      testDashboard.setUserContextFilter(null);
      when(customDashboardRepository.findById(1L)).thenReturn(Optional.of(testDashboard));
      when(customDashboardRepository.save(any(CustomDashboard.class)))
          .thenAnswer(
              invocation -> {
                CustomDashboard dashboard = invocation.getArgument(0);
                return dashboard;
              });
      when(widgetConfigRepository.countByDashboardId(1L)).thenReturn(3L);

      // Act
      CustomDashboardDTO result = customDashboardService.toggleUserContextFilter(1L, 1L);

      // Assert
      assertNotNull(result);
      assertTrue(result.getUserContextFilter());
    }

    @Test
    @DisplayName("Should throw exception when dashboard not found")
    void shouldThrowExceptionWhenDashboardNotFound() {
      // Arrange
      when(customDashboardRepository.findById(999L)).thenReturn(Optional.empty());

      // Act & Assert
      IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> customDashboardService.toggleUserContextFilter(1L, 999L));

      assertTrue(exception.getMessage().contains("Dashboard not found"));
      verify(customDashboardRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user is not authorized")
    void shouldThrowExceptionWhenUserNotAuthorized() {
      // Arrange
      User otherUser = User.builder().id(2L).username("otheruser").build();
      testDashboard.setUser(otherUser);
      when(customDashboardRepository.findById(1L)).thenReturn(Optional.of(testDashboard));

      // Act & Assert
      SecurityException exception =
          assertThrows(
              SecurityException.class,
              () -> customDashboardService.toggleUserContextFilter(1L, 1L));

      assertTrue(exception.getMessage().contains("Not authorized"));
      verify(customDashboardRepository, never()).save(any());
    }
  }
}
