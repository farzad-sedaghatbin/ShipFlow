package com.github.farzadsedaghatbin.shipflow.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.CreateHillChartPointRequest;
import com.github.farzadsedaghatbin.shipflow.dto.HillChartPointDTO;
import com.github.farzadsedaghatbin.shipflow.dto.UpdateHillChartPointRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.HillChartPoint;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.repository.HillChartPointRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HillChartServiceTest {

  @Mock private HillChartPointRepository hillChartPointRepository;

  @Mock private PitchRepository pitchRepository;

  @InjectMocks private HillChartService hillChartService;

  private Pitch testPitch;
  private HillChartPoint testPoint;
  private Cycle testCycle;

  @BeforeEach
  void setUp() {
    testCycle = new Cycle();
    testCycle.setId(1L);
    testCycle.setName("Cycle 1");

    testPitch = new Pitch();
    testPitch.setId(1L);
    testPitch.setTitle("Test Pitch");
    testPitch.setStatus(PitchStatus.IN_PROGRESS);
    testPitch.setCycle(testCycle);

    testPoint = new HillChartPoint();
    testPoint.setId(1L);
    testPoint.setPitch(testPitch);
    testPoint.setScope("Feature A");
    testPoint.setDescription("Implementing feature A");
    testPoint.setPosition(25);
    testPoint.setCreatedAt(LocalDateTime.now());
    testPoint.setUpdatedAt(LocalDateTime.now());
  }

  @Test
  void getAllHillChartPoints_ShouldReturnAllPoints() {
    // Arrange
    when(hillChartPointRepository.findAll()).thenReturn(Arrays.asList(testPoint));

    // Act
    List<HillChartPointDTO> result = hillChartService.getAllHillChartPoints();

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals("Feature A", result.get(0).getScope());
    verify(hillChartPointRepository, times(1)).findAll();
  }

  @Test
  void getHillChartPointById_WhenExists_ShouldReturnPoint() {
    // Arrange
    when(hillChartPointRepository.findById(1L)).thenReturn(Optional.of(testPoint));

    // Act
    HillChartPointDTO result = hillChartService.getHillChartPointById(1L);

    // Assert
    assertNotNull(result);
    assertEquals(1L, result.getId());
    assertEquals("Feature A", result.getScope());
    assertEquals(25, result.getPosition());
  }

  @Test
  void getHillChartPointById_WhenNotExists_ShouldThrowException() {
    // Arrange
    when(hillChartPointRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(RuntimeException.class, () -> hillChartService.getHillChartPointById(999L));
  }

  @Test
  void getHillChartPointsByPitch_ShouldReturnPointsForPitch() {
    // Arrange
    when(hillChartPointRepository.findByPitchIdOrderByUpdatedAtDesc(1L))
        .thenReturn(Arrays.asList(testPoint));

    // Act
    List<HillChartPointDTO> result = hillChartService.getHillChartPointsByPitch(1L);

    // Assert
    assertNotNull(result);
    assertEquals(1, result.size());
    assertEquals(1L, result.get(0).getPitchId());
    verify(hillChartPointRepository, times(1)).findByPitchIdOrderByUpdatedAtDesc(1L);
  }

  @Test
  void createHillChartPoint_ShouldSavePoint() {
    // Arrange
    CreateHillChartPointRequest request =
        CreateHillChartPointRequest.builder()
            .pitchId(1L)
            .scope("Feature B")
            .description("New feature")
            .position(50)
            .build();

    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
    when(hillChartPointRepository.save(any(HillChartPoint.class))).thenReturn(testPoint);

    // Act
    HillChartPointDTO result = hillChartService.createHillChartPoint(request);

    // Assert
    assertNotNull(result);
    verify(pitchRepository, times(1)).findById(1L);
    verify(hillChartPointRepository, times(1)).save(any(HillChartPoint.class));
  }

  @Test
  void createHillChartPoint_WhenPitchNotExists_ShouldThrowException() {
    // Arrange
    CreateHillChartPointRequest request =
        CreateHillChartPointRequest.builder()
            .pitchId(999L)
            .scope("Feature B")
            .description("New feature")
            .position(50)
            .build();

    when(pitchRepository.findById(999L)).thenReturn(Optional.empty());

    // Act & Assert
    assertThrows(RuntimeException.class, () -> hillChartService.createHillChartPoint(request));
  }

  @Test
  void updateHillChartPoint_WhenExists_ShouldUpdatePoint() {
    // Arrange
    UpdateHillChartPointRequest request =
        UpdateHillChartPointRequest.builder().position(75).build();

    when(hillChartPointRepository.findById(1L)).thenReturn(Optional.of(testPoint));
    when(hillChartPointRepository.save(any(HillChartPoint.class))).thenReturn(testPoint);

    // Act
    HillChartPointDTO result = hillChartService.updateHillChartPoint(1L, request);

    // Assert
    assertNotNull(result);
    verify(hillChartPointRepository, times(1)).save(testPoint);
  }

  @Test
  void deleteHillChartPoint_WhenExists_ShouldDelete() {
    // Arrange
    when(hillChartPointRepository.existsById(1L)).thenReturn(true);

    // Act
    hillChartService.deleteHillChartPoint(1L);

    // Assert
    verify(hillChartPointRepository, times(1)).deleteById(1L);
  }

  @Test
  void deleteHillChartPoint_WhenNotExists_ShouldThrowException() {
    // Arrange
    when(hillChartPointRepository.existsById(999L)).thenReturn(false);

    // Act & Assert
    assertThrows(RuntimeException.class, () -> hillChartService.deleteHillChartPoint(999L));
  }
}
