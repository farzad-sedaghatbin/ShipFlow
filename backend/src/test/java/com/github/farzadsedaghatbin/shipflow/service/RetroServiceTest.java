package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PitchStatus;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroColumnType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import com.github.farzadsedaghatbin.shipflow.service.retro.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class RetroServiceTest {

  @Mock
  private RetroRepository retroRepository;

  @Mock
  private RetroItemRepository retroItemRepository;

  @Mock
  private CycleRepository cycleRepository;

  @Mock
  private ProjectRepository projectRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private RetroItemVoteRepository retroItemVoteRepository;

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private MessageService messageService;

  @Mock
  private LocalizationService localizationService;

  @Mock
  private RetroCrudService retroCrudService;

  @Mock
  private RetroItemService retroItemService;

  @Mock
  private RetroActionService retroActionService;

  @Mock
  private RetroConversionService retroConversionService;

  @Mock
  private RetroMapper retroMapper;

  @InjectMocks
  private RetroService retroService;

  private Project testProject;
  private Cycle testCycle;
  private Retrospective testRetro;
  private RetroItem testItem;
  private User testUser;

  @BeforeEach
  void setUp() {
    lenient().when(messageService.getMessage(anyString(), any(Object[].class))).thenAnswer(i -> {
      String key = i.getArgument(0);
      if (key.contains("retro.feature.disabled"))
        return "Retrospectives feature is disabled";
      return key;
    });
    lenient().when(messageService.getMessage(anyString())).thenAnswer(i -> {
      String key = i.getArgument(0);
      if (key.contains("retro.feature.disabled"))
        return "Retrospectives feature is disabled";
      return key;
    });

    lenient().when(localizationService.getMessage(anyString(), any(Object[].class))).thenAnswer(i -> {
      String key = i.getArgument(0);
      if (key.contains("retro.closed"))
        return "Retrospective is closed";
      if (key.contains("retro.not.found"))
        return "Retrospective not found";
      if (key.contains("retro.item.not.found"))
        return "Retrospective item not found";
      if (key.contains("retro.cannot.update.closed"))
        return "Cannot update a closed retrospective";
      if (key.contains("retro.cannot.open.closed"))
        return "Cannot open a closed retrospective";
      if (key.contains("retro.cannot.add.items.closed"))
        return "Cannot add items to a closed retrospective";
      if (key.contains("retro.cannot.update.items.closed"))
        return "Cannot update items in a closed retrospective";
      if (key.contains("retro.cannot.delete.items.closed"))
        return "Cannot delete items from a closed retrospective";
      return key;
    });
    lenient().when(localizationService.getMessage(anyString())).thenAnswer(i -> {
      String key = i.getArgument(0);
      if (key.contains("retro.closed"))
        return "Retrospective is closed";
      if (key.contains("retro.not.found"))
        return "Retrospective not found";
      if (key.contains("retro.item.not.found"))
        return "Retrospective item not found";
      if (key.contains("retro.cannot.update.closed"))
        return "Cannot update a closed retrospective";
      if (key.contains("retro.cannot.open.closed"))
        return "Cannot open a closed retrospective";
      if (key.contains("retro.cannot.add.items.closed"))
        return "Cannot add items to a closed retrospective";
      if (key.contains("retro.cannot.update.items.closed"))
        return "Cannot update items in a closed retrospective";
      if (key.contains("retro.cannot.delete.items.closed"))
        return "Cannot delete items from a closed retrospective";
      return key;
    });

    testProject = Project.builder().id(1L).name("Test Project").projectKey("TST").isActive(true)
        .enableRetrospectives(true).build();

    testCycle = Cycle.builder().id(1L).name("Test Cycle").project(testProject).startDate(LocalDate.now())
        .endDate(LocalDate.now().plusWeeks(6)).phase(CyclePhase.BUILD).isActive(true).build();

    testUser = User.builder().id(1L).username("testuser").build();

    testRetro = Retrospective.builder().id(1L).title("Test Retro").notes("Test notes").status(RetroStatus.DRAFT)
        .cycle(testCycle).project(testProject).createdBy(testUser).createdAt(LocalDateTime.now()).build();

    testItem = RetroItem.builder().id(1L).content("Test item").columnType(RetroColumnType.WENT_WELL)
        .retrospective(testRetro).author(testUser).createdAt(LocalDateTime.now()).build();

    // Mock SecurityContext
    SecurityContext securityContext = mock(SecurityContext.class);
    Authentication authentication = mock(Authentication.class);
    lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
    lenient().when(authentication.getName()).thenReturn("testuser");
    SecurityContextHolder.setContext(securityContext);
  }

  @Nested
  @DisplayName("Team Setting Tests")
  class TeamSettingTests {

    @Test
    @DisplayName("New project should have retrospectives enabled by default")
    void newProjectShouldHaveRetroEnabledByDefault() {
      when(retroCrudService.isRetrospectivesEnabled(1L)).thenReturn(true);

      boolean enabled = retroService.isRetrospectivesEnabled(1L);

      assertThat(enabled).isTrue();
    }

    @Test
    @DisplayName("Admin can toggle retrospectives off")
    void adminCanToggleRetroOff() {
      doNothing().when(retroCrudService).setRetrospectivesEnabled(1L, false);

      retroService.setRetrospectivesEnabled(1L, false);

      verify(retroCrudService).setRetrospectivesEnabled(1L, false);
    }

    @Test
    @DisplayName("Retro operations blocked when feature disabled")
    void retroOperationsBlockedWhenDisabled() {
      doThrow(new IllegalStateException("Retrospectives feature is disabled"))
          .when(retroCrudService).getAllRetrosByProject(1L);

      assertThatThrownBy(() -> retroService.getAllRetrosByProject(1L)).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Retrospectives feature is disabled");
    }
  }

  @Nested
  @DisplayName("Retro Lifecycle Tests")
  class RetroLifecycleTests {

    @Test
    @DisplayName("Create retro linked to cycle succeeds")
    void createRetroLinkedToCycleSucceeds() {
      CreateRetroRequest request = CreateRetroRequest.builder().title("Test Retro").notes("Notes").cycleId(1L)
          .projectId(1L).build();

      RetroDTO expectedDTO = RetroDTO.builder()
          .id(1L)
          .title("Test Retro")
          .notes("Notes")
          .status(RetroStatus.DRAFT)
          .itemCount(0)
          .build();

      when(retroCrudService.createRetro(any(CreateRetroRequest.class))).thenReturn(expectedDTO);

      RetroDTO result = retroService.createRetro(request);

      assertThat(result).isNotNull();
      assertThat(result.getTitle()).isEqualTo("Test Retro");
      verify(retroCrudService).createRetro(any(CreateRetroRequest.class));
    }

    @Test
    @DisplayName("Add items to each column succeeds")
    void addItemsToColumnsSucceeds() {
      CreateRetroItemRequest request = CreateRetroItemRequest.builder().content("Test item")
          .columnType(RetroColumnType.WENT_WELL).retrospectiveId(1L).build();

      RetroItemDTO expectedDTO = RetroItemDTO.builder()
          .id(1L)
          .content("Test item")
          .columnType(RetroColumnType.WENT_WELL)
          .voteCount(0)
          .build();

      when(retroItemService.createRetroItem(any(CreateRetroItemRequest.class))).thenReturn(expectedDTO);

      RetroItemDTO result = retroService.createRetroItem(request);

      assertThat(result).isNotNull();
      assertThat(result.getContent()).isEqualTo("Test item");
      verify(retroItemService).createRetroItem(any(CreateRetroItemRequest.class));
    }

    @Test
    @DisplayName("Close retro makes it read-only")
    void closeRetroMakesReadOnly() {
      RetroDTO expectedDTO = RetroDTO.builder()
          .id(1L)
          .title("Test Retro")
          .status(RetroStatus.CLOSED)
          .closedAt(LocalDateTime.now())
          .itemCount(3)
          .build();

      when(retroCrudService.closeRetro(1L)).thenReturn(expectedDTO);

      RetroDTO result = retroService.closeRetro(1L);

      assertThat(result.getStatus()).isEqualTo(RetroStatus.CLOSED);
      assertThat(result.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("Cannot add items to closed retro")
    void cannotAddItemsToClosedRetro() {
      CreateRetroItemRequest request = CreateRetroItemRequest.builder().content("New item")
          .columnType(RetroColumnType.TRY_NEXT).retrospectiveId(1L).build();

      doThrow(new IllegalStateException("Cannot add items to a closed retrospective"))
          .when(retroItemService).createRetroItem(any(CreateRetroItemRequest.class));

      assertThatThrownBy(() -> retroService.createRetroItem(request)).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot add items to a closed retrospective");
    }

    @Test
    @DisplayName("Delete retro removes it")
    void deleteRetroRemovesIt() {
      doNothing().when(retroCrudService).deleteRetro(1L);

      retroService.deleteRetro(1L);

      verify(retroCrudService).deleteRetro(1L);
    }
  }

  @Nested
  @DisplayName("Cycle Close Enforcement Tests")
  class CycleCloseEnforcementTests {

    @Test
    @DisplayName("Cycle with 0 closed retros cannot be closed")
    void cycleWithZeroClosedRetrosCannotBeClosed() {
      CycleRetroStatusDTO status = CycleRetroStatusDTO.builder()
          .cycleId(1L)
          .totalRetros(0)
          .closedRetros(0)
          .canCloseCycle(false)
          .message("Please create and close at least one Retro before closing this cycle")
          .build();

      when(retroActionService.getCycleRetroStatus(1L)).thenReturn(status);

      CycleRetroStatusDTO result = retroService.getCycleRetroStatus(1L);

      assertThat(result.getCanCloseCycle()).isFalse();
      assertThat(result.getMessage()).contains("create and close at least one Retro");
    }

    @Test
    @DisplayName("Cycle with 1 closed retro can be closed")
    void cycleWithOneClosedRetroCanBeClosed() {
      CycleRetroStatusDTO status = CycleRetroStatusDTO.builder()
          .cycleId(1L)
          .totalRetros(1)
          .closedRetros(1)
          .canCloseCycle(true)
          .message("Cycle can be closed - all retrospectives are complete")
          .build();

      when(retroActionService.getCycleRetroStatus(1L)).thenReturn(status);

      CycleRetroStatusDTO result = retroService.getCycleRetroStatus(1L);

      assertThat(result.getCanCloseCycle()).isTrue();
      assertThat(result.getMessage()).contains("can be closed");
    }

    @Test
    @DisplayName("Cycle with draft/open retro but not closed cannot close")
    void cycleWithDraftRetroCannotClose() {
      CycleRetroStatusDTO status = CycleRetroStatusDTO.builder()
          .cycleId(1L)
          .totalRetros(2)
          .closedRetros(0)
          .canCloseCycle(false)
          .message("Please close at least one retrospective before closing this cycle")
          .build();

      when(retroActionService.getCycleRetroStatus(1L)).thenReturn(status);

      CycleRetroStatusDTO result = retroService.getCycleRetroStatus(1L);

      assertThat(result.getCanCloseCycle()).isFalse();
      assertThat(result.getTotalRetros()).isEqualTo(2);
      assertThat(result.getClosedRetros()).isEqualTo(0);
    }

    @Test
    @DisplayName("Retro disabled bypasses cycle close gate")
    void retroDisabledBypassesCycleCloseGate() {
      CycleRetroStatusDTO status = CycleRetroStatusDTO.builder()
          .cycleId(1L)
          .totalRetros(0)
          .closedRetros(0)
          .canCloseCycle(true)
          .message("Retrospectives feature is disabled - cycle can be closed")
          .build();

      when(retroActionService.getCycleRetroStatus(1L)).thenReturn(status);

      CycleRetroStatusDTO result = retroService.getCycleRetroStatus(1L);

      assertThat(result.getCanCloseCycle()).isTrue();
      assertThat(result.getMessage()).contains("disabled");
    }

    @Test
    @DisplayName("canCloseCycle returns correct boolean")
    void canCloseCycleReturnsCorrectBoolean() {
      when(retroActionService.canCloseCycle(1L)).thenReturn(true);

      boolean canClose = retroService.canCloseCycle(1L);

      assertThat(canClose).isTrue();
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("Get non-existent retro throws ResourceNotFoundException")
    void getNonExistentRetroThrows() {
      doThrow(new ResourceNotFoundException("Retrospective not found with id: 999"))
          .when(retroCrudService).getRetroById(999L);

      assertThatThrownBy(() -> retroService.getRetroById(999L)).isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Retrospective not found");
    }

    @Test
    @DisplayName("Delete non-existent retro throws ResourceNotFoundException")
    void deleteNonExistentRetroThrows() {
      doThrow(new ResourceNotFoundException("Retrospective not found with id: 999"))
          .when(retroCrudService).deleteRetro(999L);

      assertThatThrownBy(() -> retroService.deleteRetro(999L)).isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Retrospective not found");
    }

    @Test
    @DisplayName("Cannot update closed retro")
    void cannotUpdateClosedRetro() {
      UpdateRetroRequest request = UpdateRetroRequest.builder().title("Updated title").build();

      doThrow(new IllegalStateException("Cannot update a closed retrospective"))
          .when(retroCrudService).updateRetro(eq(1L), any(UpdateRetroRequest.class));

      assertThatThrownBy(() -> retroService.updateRetro(1L, request)).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot update a closed retrospective");
    }

    @Test
    @DisplayName("Cannot open already closed retro")
    void cannotOpenClosedRetro() {
      doThrow(new IllegalStateException("Cannot open a closed retrospective"))
          .when(retroCrudService).openRetro(1L);

      assertThatThrownBy(() -> retroService.openRetro(1L)).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot open a closed retrospective");
    }
  }

  @Nested
  @DisplayName("Retro Item Tests")
  class RetroItemTests {

    @Test
    @DisplayName("Get items by retro returns all items")
    void getItemsByRetroReturnsAllItems() {
      RetroItemDTO itemDTO = RetroItemDTO.builder()
          .id(1L)
          .content("Test item")
          .columnType(RetroColumnType.WENT_WELL)
          .voteCount(0)
          .build();

      when(retroItemService.getItemsByRetro(1L)).thenReturn(Arrays.asList(itemDTO));

      List<RetroItemDTO> items = retroService.getItemsByRetro(1L);

      assertThat(items).hasSize(1);
      assertThat(items.get(0).getContent()).isEqualTo("Test item");
    }

    @Test
    @DisplayName("Update item changes content")
    void updateItemChangesContent() {
      RetroItemDTO itemDTO = RetroItemDTO.builder()
          .id(1L)
          .content("Updated content")
          .columnType(RetroColumnType.WENT_WELL)
          .voteCount(0)
          .build();

      when(retroItemService.updateRetroItem(1L, "Updated content")).thenReturn(itemDTO);

      RetroItemDTO result = retroService.updateRetroItem(1L, "Updated content");

      verify(retroItemService).updateRetroItem(1L, "Updated content");
    }

    @Test
    @DisplayName("Cannot update item in closed retro")
    void cannotUpdateItemInClosedRetro() {
      doThrow(new IllegalStateException("Cannot update items in a closed retrospective"))
          .when(retroItemService).updateRetroItem(1L, "New content");

      assertThatThrownBy(() -> retroService.updateRetroItem(1L, "New content"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot update items in a closed retrospective");
    }

    @Test
    @DisplayName("Delete item from open retro succeeds")
    void deleteItemFromOpenRetroSucceeds() {
      doNothing().when(retroItemService).deleteRetroItem(1L);

      retroService.deleteRetroItem(1L);

      verify(retroItemService).deleteRetroItem(1L);
    }

    @Test
    @DisplayName("Cannot delete item from closed retro")
    void cannotDeleteItemFromClosedRetro() {
      doThrow(new IllegalStateException("Cannot delete items from a closed retrospective"))
          .when(retroItemService).deleteRetroItem(1L);

      assertThatThrownBy(() -> retroService.deleteRetroItem(1L)).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot delete items from a closed retrospective");
    }
  }

  @Nested
  @DisplayName("Retro Action Tracking Tests (v0.5)")
  class RetroActionTrackingTests {

    @Test
    @DisplayName("markActedOn sets acted_on fields correctly")
    void markActedOnSetsFieldsCorrectly() {
      RetroItemDTO itemDTO = RetroItemDTO.builder()
          .id(1L)
          .content("Test item")
          .actedOn(true)
          .actedOnNotes("Completed during sprint planning")
          .actedOnAt(LocalDateTime.now())
          .build();

      when(retroActionService.markActedOn(1L, true, "Completed during sprint planning")).thenReturn(itemDTO);

      RetroItemDTO result = retroService.markActedOn(1L, true, "Completed during sprint planning");

      verify(retroActionService).markActedOn(1L, true, "Completed during sprint planning");
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("markActedOn with false clears acted_on fields")
    void markActedOnWithFalseClearsFields() {
      RetroItemDTO itemDTO = RetroItemDTO.builder()
          .id(1L)
          .content("Test item")
          .actedOn(false)
          .actedOnNotes(null)
          .actedOnAt(null)
          .build();

      when(retroActionService.markActedOn(1L, false, null)).thenReturn(itemDTO);

      RetroItemDTO result = retroService.markActedOn(1L, false, null);

      verify(retroActionService).markActedOn(1L, false, null);
      assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("markActedOn throws exception for non-existent item")
    void markActedOnThrowsExceptionForNonExistentItem() {
      doThrow(new ResourceNotFoundException("Retro item not found with id: 999"))
          .when(retroActionService).markActedOn(999L, true, "notes");

      assertThatThrownBy(() -> retroService.markActedOn(999L, true, "notes"))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Retro item not found");
    }

    @Test
    @DisplayName("convertToPitchDraft creates pitch from retro items")
    void convertToPitchDraftCreatesPitchFromRetroItems() {
      ConvertRetroToPitchRequest request = ConvertRetroToPitchRequest.builder()
          .retrospectiveId(1L)
          .retroItemIds(Arrays.asList(2L, 3L))
          .appetiteDays(1)
          .build();

      PitchDTO expectedPitch = PitchDTO.builder()
          .id(10L)
          .title("Improvements from: Test Retro")
          .description("Auto-generated from retrospective: Test Retro")
          .appetiteDays(1)
          .status(PitchStatus.PENDING)
          .build();

      when(retroConversionService.convertToPitchDraft(any(ConvertRetroToPitchRequest.class))).thenReturn(expectedPitch);

      PitchDTO result = retroService.convertToPitchDraft(request);

      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(10L);
      assertThat(result.getTitle()).contains("Test Retro");
      verify(retroConversionService).convertToPitchDraft(any(ConvertRetroToPitchRequest.class));
    }

    @Test
    @DisplayName("convertToPitchDraft throws exception for non-closed retro")
    void convertToPitchDraftThrowsExceptionForNonClosedRetro() {
      ConvertRetroToPitchRequest request = ConvertRetroToPitchRequest.builder()
          .retrospectiveId(1L)
          .build();

      doThrow(new IllegalStateException("Retrospective must be closed before converting to pitch draft"))
          .when(retroConversionService).convertToPitchDraft(any(ConvertRetroToPitchRequest.class));

      assertThatThrownBy(() -> retroService.convertToPitchDraft(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("must be closed");
    }

    @Test
    @DisplayName("convertToPitchDraft throws exception when no actionable items found")
    void convertToPitchDraftThrowsExceptionWhenNoActionableItems() {
      ConvertRetroToPitchRequest request = ConvertRetroToPitchRequest.builder()
          .retrospectiveId(1L)
          .build();

      doThrow(new IllegalStateException("No actionable items found to convert"))
          .when(retroConversionService).convertToPitchDraft(any(ConvertRetroToPitchRequest.class));

      assertThatThrownBy(() -> retroService.convertToPitchDraft(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("No actionable items");
    }

    @Test
    @DisplayName("convertToPitchDraft uses custom title when provided")
    void convertToPitchDraftUsesCustomTitle() {
      ConvertRetroToPitchRequest request = ConvertRetroToPitchRequest.builder()
          .retrospectiveId(1L)
          .customTitle("Custom Infrastructure Improvements")
          .appetiteDays(2)
          .build();

      PitchDTO expectedPitch = PitchDTO.builder()
          .id(11L)
          .title("Custom Infrastructure Improvements")
          .description("Auto-generated from retrospective: Test Retro")
          .appetiteDays(2)
          .status(PitchStatus.PENDING)
          .build();

      when(retroConversionService.convertToPitchDraft(any(ConvertRetroToPitchRequest.class))).thenReturn(expectedPitch);

      PitchDTO result = retroService.convertToPitchDraft(request);

      assertThat(result).isNotNull();
      assertThat(result.getTitle()).isEqualTo("Custom Infrastructure Improvements");
      assertThat(result.getAppetiteDays()).isEqualTo(2);
    }
  }
}
