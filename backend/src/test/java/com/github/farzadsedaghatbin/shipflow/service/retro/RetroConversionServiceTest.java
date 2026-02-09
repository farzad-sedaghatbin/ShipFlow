package com.github.farzadsedaghatbin.shipflow.service.retro;

import static com.github.farzadsedaghatbin.shipflow.service.retro.RetroTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.ConvertRetroToPitchRequest;
import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroColumnType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RetroItemRepository;
import com.github.farzadsedaghatbin.shipflow.service.LocalizationService;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("RetroConversionService")
class RetroConversionServiceTest {

  @Mock private RetroItemRepository retroItemRepository;
  @Mock private CycleRepository cycleRepository;
  @Mock private PitchRepository pitchRepository;
  @Mock private LocalizationService localizationService;
  @Mock private RetroCrudService retroCrudService;
  @Mock private RetroMapper retroMapper;

  @InjectMocks private RetroConversionService service;

  private Project testProject;
  private Cycle testCycle;
  private Cycle nextCycle;
  private User testUser;
  private Retrospective testRetro;

  @BeforeEach
  void setUp() {
    testProject = aProject().build();
    testCycle = aCycle().withId(1L).withName("Current Cycle").withProject(testProject).build();
    nextCycle = aCycle().withId(2L).withName("Next Cycle").withProject(testProject)
        .withPhase(CyclePhase.SHAPING).startingIn(7).build();
    testUser = aUser().build();
    
    setupMocks();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setupMocks() {
    lenient().when(retroCrudService.getCurrentUser()).thenReturn(testUser);
    lenient().when(localizationService.getMessageWithDefault(anyString(), anyString()))
        .thenAnswer(i -> i.getArgument(1)); // Return default message
  }

  private Retrospective buildClosedRetroWithItems(RetroItem... items) {
    List<RetroItem> itemList = Arrays.asList(items);
    testRetro = aRetro()
        .closed()
        .withCycle(testCycle)
        .withProject(testProject)
        .createdBy(testUser)
        .withItems(itemList)
        .build();
    return testRetro;
  }

  // ==================== INVARIANT: RETRO MUST BE CLOSED ====================

  @Nested
  @DisplayName("Invariant: Retro Must Be Closed")
  class RetroMustBeClosedTests {

    @ParameterizedTest
    @EnumSource(value = RetroStatus.class, names = {"DRAFT", "OPEN"})
    @DisplayName("throws for non-closed retro status")
    void convertToPitchDraft_WhenNotClosed_Throws(RetroStatus status) {
      testRetro = aRetro()
          .withStatus(status)
          .withCycle(testCycle)
          .withProject(testProject)
          .build();

      when(retroCrudService.getRetroEntityWithItems(1L)).thenReturn(testRetro);

      ConvertRetroToPitchRequest request = ConvertRetroToPitchRequest.builder()
          .retrospectiveId(1L)
          .build();

      assertThatThrownBy(() -> service.convertToPitchDraft(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("must be closed");
    }
  }

  // ==================== INVARIANT: ITEMS MUST BE ACTIONABLE ====================

  @Nested
  @DisplayName("Invariant: Items Must Be Actionable")
  class ItemsMustBeActionableTests {

    @Test
    @DisplayName("accepts ACTIONS column items")
    void convertToPitchDraft_WithActionsItems_Succeeds() {
      RetroItem actionItem = aRetroItem().withId(1L).action()
          .withContent("Action item").build();
      testRetro = buildClosedRetroWithItems(actionItem);

      Pitch savedPitch = aPitch().withId(10L).withCycle(nextCycle).build();
      PitchDTO expectedDto = PitchDTO.builder().id(10L).build();

      when(retroCrudService.getRetroEntityWithItems(1L)).thenReturn(testRetro);
      when(cycleRepository.findFirstByProjectIdAndStartDateAfterOrderByStartDateAsc(eq(testProject.getId()), any(LocalDate.class)))
          .thenReturn(Optional.of(nextCycle));
      when(pitchRepository.save(any())).thenReturn(savedPitch);
      when(retroMapper.toPitchDTO(savedPitch)).thenReturn(expectedDto);

      ConvertRetroToPitchRequest request = ConvertRetroToPitchRequest.builder()
          .retrospectiveId(1L)
          .build();

      PitchDTO result = service.convertToPitchDraft(request);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("accepts TRY_NEXT column items")
    void convertToPitchDraft_WithTryNextItems_Succeeds() {
      RetroItem tryNextItem = aRetroItem().withId(1L).tryNext()
          .withContent("Try next item").build();
      testRetro = buildClosedRetroWithItems(tryNextItem);

      Pitch savedPitch = aPitch().withId(10L).withCycle(nextCycle).build();
      PitchDTO expectedDto = PitchDTO.builder().id(10L).build();

      when(retroCrudService.getRetroEntityWithItems(1L)).thenReturn(testRetro);
      when(cycleRepository.findFirstByProjectIdAndStartDateAfterOrderByStartDateAsc(eq(testProject.getId()), any(LocalDate.class)))
          .thenReturn(Optional.of(nextCycle));
      when(pitchRepository.save(any())).thenReturn(savedPitch);
      when(retroMapper.toPitchDTO(savedPitch)).thenReturn(expectedDto);

      ConvertRetroToPitchRequest request = ConvertRetroToPitchRequest.builder()
          .retrospectiveId(1L)
          .build();

      PitchDTO result = service.convertToPitchDraft(request);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("rejects non-actionable items when explicitly selected")
    void convertToPitchDraft_WithNonActionableSelected_Throws() {
      RetroItem wentWellItem = aRetroItem().withId(1L).wentWell()
          .withContent("Went well item").build();
      testRetro = buildClosedRetroWithItems(wentWellItem);

      when(retroCrudService.getRetroEntityWithItems(1L)).thenReturn(testRetro);

      ConvertRetroToPitchRequest request = ConvertRetroToPitchRequest.builder()
          .retrospectiveId(1L)
          .retroItemIds(Collections.singletonList(1L))
          .build();

      assertThatThrownBy(() -> service.convertToPitchDraft(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("not from actionable columns");
    }

    @Test
    @DisplayName("throws when no actionable items exist")
    void convertToPitchDraft_WithNoActionableItems_Throws() {
      RetroItem wentWellItem = aRetroItem().withId(1L).wentWell().build();
      testRetro = buildClosedRetroWithItems(wentWellItem);

      when(retroCrudService.getRetroEntityWithItems(1L)).thenReturn(testRetro);

      ConvertRetroToPitchRequest request = ConvertRetroToPitchRequest.builder()
          .retrospectiveId(1L)
          .build();

      assertThatThrownBy(() -> service.convertToPitchDraft(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("No actionable items");
    }

    @Test
    @DisplayName("filters out merged items")
    void convertToPitchDraft_FiltersMergedItems() {
      RetroItem targetItem = aRetroItem().withId(1L).action()
          .withContent("Target").build();
      RetroItem mergedItem = aRetroItem().withId(2L).action()
          .withContent("Merged").mergedInto(targetItem).build();
      testRetro = buildClosedRetroWithItems(targetItem, mergedItem);

      Pitch savedPitch = aPitch().withId(10L).withCycle(nextCycle).build();
      PitchDTO expectedDto = PitchDTO.builder().id(10L).build();

      when(retroCrudService.getRetroEntityWithItems(1L)).thenReturn(testRetro);
      when(cycleRepository.findFirstByProjectIdAndStartDateAfterOrderByStartDateAsc(eq(testProject.getId()), any(LocalDate.class)))
          .thenReturn(Optional.of(nextCycle));
      when(pitchRepository.save(any())).thenReturn(savedPitch);
      when(retroMapper.toPitchDTO(savedPitch)).thenReturn(expectedDto);

      ConvertRetroToPitchRequest request = ConvertRetroToPitchRequest.builder()
          .retrospectiveId(1L)
          .build();

      service.convertToPitchDraft(request);

      // Verify only non-merged item is marked as acted on
      ArgumentCaptor<List<RetroItem>> captor = ArgumentCaptor.forClass(List.class);
      verify(retroItemRepository).saveAll(captor.capture());
      assertThat(captor.getValue()).hasSize(1);
      assertThat(captor.getValue().get(0).getId()).isEqualTo(1L);
    }
  }

  // ==================== INVARIANT: TARGET CYCLE SAME PROJECT ====================

  @Nested
  @DisplayName("Invariant: Target Cycle Must Belong To Same Project")
  class TargetCycleSameProjectTests {

    @Test
    @DisplayName("throws when target cycle belongs to different project")
    void convertToPitchDraft_WhenCycleDifferentProject_Throws() {
      Project otherProject = aProject().withId(2L).withName("Other Project").build();
      Cycle otherCycle = aCycle().withId(3L).withProject(otherProject).build();

      RetroItem actionItem = aRetroItem().withId(1L).action().build();
      testRetro = buildClosedRetroWithItems(actionItem);

      when(retroCrudService.getRetroEntityWithItems(1L)).thenReturn(testRetro);
      when(cycleRepository.findById(3L)).thenReturn(Optional.of(otherCycle));

      ConvertRetroToPitchRequest request = ConvertRetroToPitchRequest.builder()
          .retrospectiveId(1L)
          .targetCycleId(3L)
          .build();

      assertThatThrownBy(() -> service.convertToPitchDraft(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("same project");
    }

    @Test
    @DisplayName("succeeds when target cycle belongs to same project")
    void convertToPitchDraft_WhenCycleSameProject_Succeeds() {
      RetroItem actionItem = aRetroItem().withId(1L).action().build();
      testRetro = buildClosedRetroWithItems(actionItem);

      Pitch savedPitch = aPitch().withId(10L).withCycle(nextCycle).build();
      PitchDTO expectedDto = PitchDTO.builder().id(10L).build();

      when(retroCrudService.getRetroEntityWithItems(1L)).thenReturn(testRetro);
      when(cycleRepository.findById(2L)).thenReturn(Optional.of(nextCycle));
      when(pitchRepository.save(any())).thenReturn(savedPitch);
      when(retroMapper.toPitchDTO(savedPitch)).thenReturn(expectedDto);

      ConvertRetroToPitchRequest request = ConvertRetroToPitchRequest.builder()
          .retrospectiveId(1L)
          .targetCycleId(2L)
          .build();

      PitchDTO result = service.convertToPitchDraft(request);

      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("throws when target cycle not found")
    void convertToPitchDraft_WhenCycleNotFound_Throws() {
      RetroItem actionItem = aRetroItem().withId(1L).action().build();
      testRetro = buildClosedRetroWithItems(actionItem);

      when(retroCrudService.getRetroEntityWithItems(1L)).thenReturn(testRetro);
      when(cycleRepository.findById(999L)).thenReturn(Optional.empty());

      ConvertRetroToPitchRequest request = ConvertRetroToPitchRequest.builder()
          .retrospectiveId(1L)
          .targetCycleId(999L)
          .build();

      assertThatThrownBy(() -> service.convertToPitchDraft(request))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("finds next upcoming cycle when not specified")
    void convertToPitchDraft_FindsNextCycleWhenNotSpecified() {
      RetroItem actionItem = aRetroItem().withId(1L).action().build();
      testRetro = buildClosedRetroWithItems(actionItem);

      Pitch savedPitch = aPitch().withId(10L).withCycle(nextCycle).build();
      PitchDTO expectedDto = PitchDTO.builder().id(10L).cycleId(2L).build();

      when(retroCrudService.getRetroEntityWithItems(1L)).thenReturn(testRetro);
      when(cycleRepository.findFirstByProjectIdAndStartDateAfterOrderByStartDateAsc(eq(testProject.getId()), any(LocalDate.class)))
          .thenReturn(Optional.of(nextCycle));
      when(pitchRepository.save(any())).thenReturn(savedPitch);
      when(retroMapper.toPitchDTO(savedPitch)).thenReturn(expectedDto);

      ConvertRetroToPitchRequest request = ConvertRetroToPitchRequest.builder()
          .retrospectiveId(1L)
          .build();

      PitchDTO result = service.convertToPitchDraft(request);

      assertThat(result.getCycleId()).isEqualTo(2L);
    }
  }

  // ==================== CURRENT USER FETCHED ONCE ====================

  @Nested
  @DisplayName("Invariant: Current User Fetched Once")
  class CurrentUserFetchedOnceTests {

    @Test
    @DisplayName("fetches current user only once for multiple items")
    void convertToPitchDraft_FetchesUserOnce() {
      RetroItem item1 = aRetroItem().withId(1L).action().build();
      RetroItem item2 = aRetroItem().withId(2L).action().build();
      RetroItem item3 = aRetroItem().withId(3L).tryNext().build();
      testRetro = buildClosedRetroWithItems(item1, item2, item3);

      Pitch savedPitch = aPitch().withId(10L).withCycle(nextCycle).build();
      PitchDTO expectedDto = PitchDTO.builder().id(10L).build();

      when(retroCrudService.getRetroEntityWithItems(1L)).thenReturn(testRetro);
      when(cycleRepository.findFirstByProjectIdAndStartDateAfterOrderByStartDateAsc(eq(testProject.getId()), any(LocalDate.class)))
          .thenReturn(Optional.of(nextCycle));
      when(pitchRepository.save(any())).thenReturn(savedPitch);
      when(retroMapper.toPitchDTO(savedPitch)).thenReturn(expectedDto);

      ConvertRetroToPitchRequest request = ConvertRetroToPitchRequest.builder()
          .retrospectiveId(1L)
          .build();

      service.convertToPitchDraft(request);

      // Verify getCurrentUser called exactly once (not 3 times for 3 items)
      verify(retroCrudService, times(1)).getCurrentUser();
    }
  }

  // ==================== PITCH CREATION TESTS ====================

  @Nested
  @DisplayName("Pitch Creation")
  class PitchCreationTests {

    @Test
    @DisplayName("uses custom title when provided")
    void convertToPitchDraft_UsesCustomTitle() {
      RetroItem actionItem = aRetroItem().withId(1L).action().build();
      testRetro = buildClosedRetroWithItems(actionItem);

      Pitch savedPitch = aPitch().withId(10L).withTitle("Custom Title").withCycle(nextCycle).build();
      PitchDTO expectedDto = PitchDTO.builder().id(10L).title("Custom Title").build();

      when(retroCrudService.getRetroEntityWithItems(1L)).thenReturn(testRetro);
      when(cycleRepository.findFirstByProjectIdAndStartDateAfterOrderByStartDateAsc(eq(testProject.getId()), any(LocalDate.class)))
          .thenReturn(Optional.of(nextCycle));
      when(pitchRepository.save(any())).thenReturn(savedPitch);
      when(retroMapper.toPitchDTO(savedPitch)).thenReturn(expectedDto);

      ConvertRetroToPitchRequest request = ConvertRetroToPitchRequest.builder()
          .retrospectiveId(1L)
          .customTitle("Custom Title")
          .build();

      PitchDTO result = service.convertToPitchDraft(request);

      ArgumentCaptor<Pitch> captor = ArgumentCaptor.forClass(Pitch.class);
      verify(pitchRepository).save(captor.capture());
      assertThat(captor.getValue().getTitle()).isEqualTo("Custom Title");
    }

    @Test
    @DisplayName("generates title from retro when not provided")
    void convertToPitchDraft_GeneratesTitle() {
      RetroItem actionItem = aRetroItem().withId(1L).action().build();
      testRetro = buildClosedRetroWithItems(actionItem);

      Pitch savedPitch = aPitch().withId(10L).withCycle(nextCycle).build();
      PitchDTO expectedDto = PitchDTO.builder().id(10L).build();

      when(retroCrudService.getRetroEntityWithItems(1L)).thenReturn(testRetro);
      when(cycleRepository.findFirstByProjectIdAndStartDateAfterOrderByStartDateAsc(eq(testProject.getId()), any(LocalDate.class)))
          .thenReturn(Optional.of(nextCycle));
      when(pitchRepository.save(any())).thenReturn(savedPitch);
      when(retroMapper.toPitchDTO(savedPitch)).thenReturn(expectedDto);

      ConvertRetroToPitchRequest request = ConvertRetroToPitchRequest.builder()
          .retrospectiveId(1L)
          .build();

      service.convertToPitchDraft(request);

      ArgumentCaptor<Pitch> captor = ArgumentCaptor.forClass(Pitch.class);
      verify(pitchRepository).save(captor.capture());
      assertThat(captor.getValue().getTitle()).contains("Improvements from:");
    }

    @Test
    @DisplayName("marks converted items as acted on")
    void convertToPitchDraft_MarksItemsAsActedOn() {
      RetroItem item1 = aRetroItem().withId(1L).action().build();
      RetroItem item2 = aRetroItem().withId(2L).tryNext().build();
      testRetro = buildClosedRetroWithItems(item1, item2);

      Pitch savedPitch = aPitch().withId(10L).withCycle(nextCycle).build();
      PitchDTO expectedDto = PitchDTO.builder().id(10L).build();

      when(retroCrudService.getRetroEntityWithItems(1L)).thenReturn(testRetro);
      when(cycleRepository.findFirstByProjectIdAndStartDateAfterOrderByStartDateAsc(eq(testProject.getId()), any(LocalDate.class)))
          .thenReturn(Optional.of(nextCycle));
      when(pitchRepository.save(any())).thenReturn(savedPitch);
      when(retroMapper.toPitchDTO(savedPitch)).thenReturn(expectedDto);

      ConvertRetroToPitchRequest request = ConvertRetroToPitchRequest.builder()
          .retrospectiveId(1L)
          .build();

      service.convertToPitchDraft(request);

      verify(retroItemRepository).saveAll(anyList());
      assertThat(item1.getActedOn()).isTrue();
      assertThat(item2.getActedOn()).isTrue();
      assertThat(item1.getActedOnBy()).isEqualTo(testUser);
    }
  }
}
