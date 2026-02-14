package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Pitch Soft Delete Tests")
class PitchSoftDeleteTest {

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private AICacheService cacheService;

  @InjectMocks
  private PitchService pitchService;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private Authentication authentication;

  private User testUser;
  private Pitch testPitch;
  private Cycle testCycle;

  @BeforeEach
  void setUp() {
    // Setup test user
    testUser = User.builder().id(1L).username("testuser").email("test@example.com").role(UserRole.MANAGER)
        .isActive(true).build();

    // Setup test cycle
    testCycle = Cycle.builder().id(1L).name("Test Cycle").build();

    // Setup test pitch
    testPitch = Pitch.builder().id(1L).title("Test Pitch").description("Test Description").appetiteDays(5)
        .cycle(testCycle).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

    // Mock security context
    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(authentication.getName()).thenReturn("testuser");
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
  }

  @Test
  @DisplayName("Should soft delete pitch successfully")
  void shouldSoftDeletePitchSuccessfully() {
    // Given
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
    when(pitchRepository.save(any(Pitch.class))).thenReturn(testPitch);

    // When
    pitchService.deletePitch(1L);

    // Then
    verify(pitchRepository).save(testPitch);
    assertThat(testPitch.getDeletedAt()).isNotNull();
    assertThat(testPitch.getDeletedBy()).isEqualTo(testUser);
    verify(cacheService).invalidateCycleRiskCache(testCycle.getId());
  }

  @Test
  @DisplayName("Should throw exception when pitch not found")
  void shouldThrowExceptionWhenPitchNotFound() {
    // Given
    when(pitchRepository.findById(999L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> pitchService.deletePitch(999L)).isInstanceOf(ResourceNotFoundException.class)
        .hasMessage("Pitch not found with id: 999");
  }

  @Test
  @DisplayName("Should throw exception when pitch already deleted")
  void shouldThrowExceptionWhenPitchAlreadyDeleted() {
    // Given
    testPitch.setDeletedAt(LocalDateTime.now());
    testPitch.setDeletedBy(testUser);
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));

    // When & Then
    assertThatThrownBy(() -> pitchService.deletePitch(1L)).isInstanceOf(IllegalStateException.class)
        .hasMessage("Pitch is already deleted");
  }

  @Test
  @DisplayName("Should not return deleted pitches in getAllPitches")
  void shouldNotReturnDeletedPitchesInGetAllPitches() {
    // Given
    when(pitchRepository.findAllNotDeleted()).thenReturn(java.util.List.of());

    // When
    var result = pitchService.getAllPitches();

    // Then
    assertThat(result).isEmpty();
    verify(pitchRepository).findAllNotDeleted();
    verify(pitchRepository, never()).findAll();
  }

  @Test
  @DisplayName("Should not return deleted pitch in getPitchById")
  void shouldNotReturnDeletedPitchInGetPitchById() {
    // Given
    when(pitchRepository.findByIdNotDeleted(1L)).thenReturn(Optional.empty());

    // When & Then
    assertThatThrownBy(() -> pitchService.getPitchById(1L)).isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Pitch not found with id: 1");
  }

  @Test
  @DisplayName("Should exclude deleted pitches from cycle queries")
  void shouldExcludeDeletedPitchesFromCycleQueries() {
    // Given
    when(pitchRepository.findByCycleIdNotDeleted(1L)).thenReturn(java.util.List.of());

    // When
    var result = pitchService.getPitchesByCycleId(1L);

    // Then
    assertThat(result).isEmpty();
    verify(pitchRepository).findByCycleIdNotDeleted(1L);
  }
}
