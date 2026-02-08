package com.github.farzadsedaghatbin.shipflow.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.dashboard.CreateDashboardWidgetRequest;
import com.github.farzadsedaghatbin.shipflow.dto.dashboard.DashboardWidgetDTO;
import com.github.farzadsedaghatbin.shipflow.dto.dashboard.UpdateDashboardWidgetRequest;
import com.github.farzadsedaghatbin.shipflow.entity.DashboardWidget;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.DashboardWidgetRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardWidgetServiceTest {

  @Mock
  private DashboardWidgetRepository dashboardWidgetRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private DashboardWidgetService dashboardWidgetService;

  private User testUser;
  private DashboardWidget testWidget;

  @BeforeEach
  void setUp() {
    testUser = User.builder().id(1L).username("testuser").build();

    testWidget = DashboardWidget.builder().id(1L).user(testUser).widgetType("STATS_CARDS").isVisible(true)
        .displayOrder(0).build();
  }

  @Test
  void getUserWidgets_WhenWidgetsExist_ShouldReturnWidgets() {
    // Arrange
    when(dashboardWidgetRepository.findByUserIdOrderByDisplayOrderAsc(1L)).thenReturn(Arrays.asList(testWidget));

    // Act
    List<DashboardWidgetDTO> result = dashboardWidgetService.getUserWidgets(1L);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("STATS_CARDS", result.get(0).getWidgetType());
    verify(dashboardWidgetRepository, times(1)).findByUserIdOrderByDisplayOrderAsc(1L);
  }

  @Test
  void getUserWidgets_WhenNoWidgetsExist_ShouldCreateDefaults() {
    // Arrange
    when(dashboardWidgetRepository.findByUserIdOrderByDisplayOrderAsc(1L)).thenReturn(Collections.emptyList());
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(dashboardWidgetRepository.saveAll(anyList())).thenAnswer(invocation -> {
      List<DashboardWidget> widgets = invocation.getArgument(0);
      for (int i = 0; i < widgets.size(); i++) {
        widgets.get(i).setId((long) (i + 1));
      }
      return widgets;
    });

    // Act
    List<DashboardWidgetDTO> result = dashboardWidgetService.getUserWidgets(1L);

    // Assert
    assertNotNull(result);
    assertTrue(result.size() > 0);
    verify(dashboardWidgetRepository, times(1)).saveAll(anyList());
  }

  @Test
  void getVisibleWidgets_ShouldReturnOnlyVisibleWidgets() {
    // Arrange
    when(dashboardWidgetRepository.findByUserIdAndIsVisibleOrderByDisplayOrderAsc(1L, true))
        .thenReturn(Arrays.asList(testWidget));

    // Act
    List<DashboardWidgetDTO> result = dashboardWidgetService.getVisibleWidgets(1L);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertTrue(result.get(0).getIsVisible());
  }

  @Test
  void createWidget_WithValidData_ShouldCreateWidget() {
    // Arrange
    CreateDashboardWidgetRequest request = CreateDashboardWidgetRequest.builder().widgetType("NEW_WIDGET")
        .isVisible(true).displayOrder(5).build();

    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(dashboardWidgetRepository.save(any(DashboardWidget.class))).thenAnswer(invocation -> {
      DashboardWidget widget = invocation.getArgument(0);
      widget.setId(2L);
      return widget;
    });

    // Act
    DashboardWidgetDTO result = dashboardWidgetService.createWidget(1L, request);

    // Assert
    assertNotNull(result);
    assertEquals("NEW_WIDGET", result.getWidgetType());
    assertEquals(5, result.getDisplayOrder());
    verify(dashboardWidgetRepository, times(1)).save(any(DashboardWidget.class));
  }

  @Test
  void updateWidget_WhenExists_ShouldUpdateWidget() {
    // Arrange
    UpdateDashboardWidgetRequest request = UpdateDashboardWidgetRequest.builder().isVisible(false).displayOrder(10)
        .build();

    when(dashboardWidgetRepository.findById(1L)).thenReturn(Optional.of(testWidget));
    when(dashboardWidgetRepository.save(any(DashboardWidget.class))).thenReturn(testWidget);

    // Act
    DashboardWidgetDTO result = dashboardWidgetService.updateWidget(1L, request);

    // Assert
    assertNotNull(result);
    assertFalse(result.getIsVisible());
    assertEquals(10, result.getDisplayOrder());
    verify(dashboardWidgetRepository, times(1)).save(any(DashboardWidget.class));
  }

  @Test
  void updateWidget_WhenNotExists_ShouldThrowException() {
    // Arrange
    UpdateDashboardWidgetRequest request = UpdateDashboardWidgetRequest.builder().isVisible(false).build();

    when(dashboardWidgetRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(RuntimeException.class, () -> dashboardWidgetService.updateWidget(999L, request));
  }

  @Test
  void bulkUpdateWidgets_ShouldUpdateMultipleWidgets() {
    // Arrange
    DashboardWidget widget2 = DashboardWidget.builder().id(2L).user(testUser).widgetType("WIDGET_2").isVisible(true)
        .displayOrder(1).build();

    DashboardWidgetDTO dto1 = DashboardWidgetDTO.builder().id(1L).isVisible(false).displayOrder(5).build();

    DashboardWidgetDTO dto2 = DashboardWidgetDTO.builder().id(2L).isVisible(true).displayOrder(1).build();

    when(dashboardWidgetRepository.findByUserIdOrderByDisplayOrderAsc(1L))
        .thenReturn(Arrays.asList(testWidget, widget2));
    when(dashboardWidgetRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

    // Act
    List<DashboardWidgetDTO> result = dashboardWidgetService.bulkUpdateWidgets(1L, Arrays.asList(dto1, dto2));

    // Assert
    assertNotNull(result);
    assertEquals(2, result.size());
    verify(dashboardWidgetRepository, times(1)).saveAll(anyList());
  }

  @Test
  void deleteWidget_ShouldDeleteWidget() {
    // Act
    dashboardWidgetService.deleteWidget(1L);

    // Assert
    verify(dashboardWidgetRepository, times(1)).deleteById(1L);
  }

  @Test
  void resetToDefaults_ShouldDeleteExistingAndCreateDefaults() {
    // Arrange
    when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
    when(dashboardWidgetRepository.saveAll(anyList())).thenAnswer(invocation -> {
      List<DashboardWidget> widgets = invocation.getArgument(0);
      for (int i = 0; i < widgets.size(); i++) {
        widgets.get(i).setId((long) (i + 1));
      }
      return widgets;
    });

    // Act
    List<DashboardWidgetDTO> result = dashboardWidgetService.resetToDefaults(1L);

    // Assert
    assertNotNull(result);
    assertTrue(result.size() > 0);
    verify(dashboardWidgetRepository, times(1)).deleteByUserId(1L);
    verify(dashboardWidgetRepository, times(1)).saveAll(anyList());
  }
}
