package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroColumnType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.*;
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
  private MessageService messageService;

  @Mock
  private LocalizationService localizationService;

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
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

      boolean enabled = retroService.isRetrospectivesEnabled(1L);

      assertThat(enabled).isTrue();
    }

    @Test
    @DisplayName("Admin can toggle retrospectives off")
    void adminCanToggleRetroOff() {
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      when(projectRepository.save(any(Project.class))).thenReturn(testProject);

      retroService.setRetrospectivesEnabled(1L, false);

      verify(projectRepository).save(any(Project.class));
    }

    @Test
    @DisplayName("Retro operations blocked when feature disabled")
    void retroOperationsBlockedWhenDisabled() {
      testProject.setEnableRetrospectives(false);
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));

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
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      when(cycleRepository.findById(1L)).thenReturn(Optional.of(testCycle));
      when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
      when(retroRepository.save(any(Retrospective.class))).thenReturn(testRetro);
      when(retroItemRepository.countByRetrospectiveId(1L)).thenReturn(0L);

      CreateRetroRequest request = CreateRetroRequest.builder().title("Test Retro").notes("Notes").cycleId(1L)
          .projectId(1L).build();

      RetroDTO result = retroService.createRetro(request);

      assertThat(result).isNotNull();
      assertThat(result.getTitle()).isEqualTo("Test Retro");
      verify(retroRepository).save(any(Retrospective.class));
    }

    @Test
    @DisplayName("Add items to each column succeeds")
    void addItemsToColumnsSucceeds() {
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      when(retroRepository.findById(1L)).thenReturn(Optional.of(testRetro));
      when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
      when(retroItemRepository.save(any(RetroItem.class))).thenReturn(testItem);

      CreateRetroItemRequest request = CreateRetroItemRequest.builder().content("Test item")
          .columnType(RetroColumnType.WENT_WELL).retrospectiveId(1L).build();

      RetroItemDTO result = retroService.createRetroItem(request);

      assertThat(result).isNotNull();
      assertThat(result.getContent()).isEqualTo("Test item");
      verify(retroItemRepository).save(any(RetroItem.class));
    }

    @Test
    @DisplayName("Close retro makes it read-only")
    void closeRetroMakesReadOnly() {
      testRetro.setStatus(RetroStatus.OPEN);
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      when(retroRepository.findById(1L)).thenReturn(Optional.of(testRetro));
      when(retroRepository.save(any(Retrospective.class))).thenAnswer(invocation -> {
        Retrospective saved = invocation.getArgument(0);
        return saved;
      });
      when(retroItemRepository.countByRetrospectiveId(1L)).thenReturn(3L);

      RetroDTO result = retroService.closeRetro(1L);

      assertThat(result.getStatus()).isEqualTo(RetroStatus.CLOSED);
      assertThat(result.getClosedAt()).isNotNull();
    }

    @Test
    @DisplayName("Cannot add items to closed retro")
    void cannotAddItemsToClosedRetro() {
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      testRetro.setStatus(RetroStatus.CLOSED);
      when(retroRepository.findById(1L)).thenReturn(Optional.of(testRetro));

      CreateRetroItemRequest request = CreateRetroItemRequest.builder().content("New item")
          .columnType(RetroColumnType.TRY_NEXT).retrospectiveId(1L).build();

      assertThatThrownBy(() -> retroService.createRetroItem(request)).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot add items to a closed retrospective");
    }

    @Test
    @DisplayName("Delete retro removes it")
    void deleteRetroRemovesIt() {
      when(retroRepository.existsById(1L)).thenReturn(true);
      doNothing().when(retroRepository).deleteById(1L);

      retroService.deleteRetro(1L);

      verify(retroRepository).deleteById(1L);
    }
  }

  @Nested
  @DisplayName("Cycle Close Enforcement Tests")
  class CycleCloseEnforcementTests {

    @Test
    @DisplayName("Cycle with 0 closed retros cannot be closed")
    void cycleWithZeroClosedRetrosCannotBeClosed() {
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(retroRepository.countByCycleId(1L)).thenReturn(0L);
      when(retroRepository.countByCycleIdAndStatus(1L, RetroStatus.CLOSED)).thenReturn(0L);

      CycleRetroStatusDTO status = retroService.getCycleRetroStatus(1L);

      assertThat(status.getCanCloseCycle()).isFalse();
      assertThat(status.getMessage()).contains("create and close at least one Retro");
    }

    @Test
    @DisplayName("Cycle with 1 closed retro can be closed")
    void cycleWithOneClosedRetroCanBeClosed() {
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(retroRepository.countByCycleId(1L)).thenReturn(1L);
      when(retroRepository.countByCycleIdAndStatus(1L, RetroStatus.CLOSED)).thenReturn(1L);

      CycleRetroStatusDTO status = retroService.getCycleRetroStatus(1L);

      assertThat(status.getCanCloseCycle()).isTrue();
      assertThat(status.getMessage()).contains("can be closed");
    }

    @Test
    @DisplayName("Cycle with draft/open retro but not closed cannot close")
    void cycleWithDraftRetroCannotClose() {
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(retroRepository.countByCycleId(1L)).thenReturn(2L);
      when(retroRepository.countByCycleIdAndStatus(1L, RetroStatus.CLOSED)).thenReturn(0L);

      CycleRetroStatusDTO status = retroService.getCycleRetroStatus(1L);

      assertThat(status.getCanCloseCycle()).isFalse();
      assertThat(status.getTotalRetros()).isEqualTo(2);
      assertThat(status.getClosedRetros()).isEqualTo(0);
    }

    @Test
    @DisplayName("Retro disabled bypasses cycle close gate")
    void retroDisabledBypassesCycleCloseGate() {
      testProject.setEnableRetrospectives(false);
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(retroRepository.countByCycleId(1L)).thenReturn(0L);
      when(retroRepository.countByCycleIdAndStatus(1L, RetroStatus.CLOSED)).thenReturn(0L);

      CycleRetroStatusDTO status = retroService.getCycleRetroStatus(1L);

      assertThat(status.getCanCloseCycle()).isTrue();
      assertThat(status.getMessage()).contains("disabled");
    }

    @Test
    @DisplayName("canCloseCycle returns correct boolean")
    void canCloseCycleReturnsCorrectBoolean() {
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(retroRepository.countByCycleId(1L)).thenReturn(1L);
      when(retroRepository.countByCycleIdAndStatus(1L, RetroStatus.CLOSED)).thenReturn(1L);

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
      when(retroRepository.findByIdWithDetails(999L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> retroService.getRetroById(999L)).isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Retrospective not found");
    }

    @Test
    @DisplayName("Delete non-existent retro throws ResourceNotFoundException")
    void deleteNonExistentRetroThrows() {
      when(retroRepository.existsById(999L)).thenReturn(false);

      assertThatThrownBy(() -> retroService.deleteRetro(999L)).isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Retrospective not found");
    }

    @Test
    @DisplayName("Cannot update closed retro")
    void cannotUpdateClosedRetro() {
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      testRetro.setStatus(RetroStatus.CLOSED);
      when(retroRepository.findById(1L)).thenReturn(Optional.of(testRetro));

      UpdateRetroRequest request = UpdateRetroRequest.builder().title("Updated title").build();

      assertThatThrownBy(() -> retroService.updateRetro(1L, request)).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot update a closed retrospective");
    }

    @Test
    @DisplayName("Cannot open already closed retro")
    void cannotOpenClosedRetro() {
      testRetro.setStatus(RetroStatus.CLOSED);
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      when(retroRepository.findById(1L)).thenReturn(Optional.of(testRetro));

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
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      when(retroRepository.findById(1L)).thenReturn(Optional.of(testRetro));
      when(retroItemRepository.findByRetrospectiveIdOrderByCreatedAtAsc(1L)).thenReturn(Arrays.asList(testItem));

      List<RetroItemDTO> items = retroService.getItemsByRetro(1L);

      assertThat(items).hasSize(1);
      assertThat(items.get(0).getContent()).isEqualTo("Test item");
    }

    @Test
    @DisplayName("Update item changes content")
    void updateItemChangesContent() {
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(testItem));
      when(retroItemRepository.save(any(RetroItem.class))).thenReturn(testItem);

      RetroItemDTO result = retroService.updateRetroItem(1L, "Updated content");

      verify(retroItemRepository).save(any(RetroItem.class));
    }

    @Test
    @DisplayName("Cannot update item in closed retro")
    void cannotUpdateItemInClosedRetro() {
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      testRetro.setStatus(RetroStatus.CLOSED);
      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(testItem));

      assertThatThrownBy(() -> retroService.updateRetroItem(1L, "New content"))
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot update items in a closed retrospective");
    }

    @Test
    @DisplayName("Delete item from open retro succeeds")
    void deleteItemFromOpenRetroSucceeds() {
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      testRetro.setStatus(RetroStatus.OPEN);
      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(testItem));
      doNothing().when(retroItemRepository).deleteById(1L);

      retroService.deleteRetroItem(1L);

      verify(retroItemRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Cannot delete item from closed retro")
    void cannotDeleteItemFromClosedRetro() {
      when(projectRepository.findById(1L)).thenReturn(Optional.of(testProject));
      testRetro.setStatus(RetroStatus.CLOSED);
      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(testItem));

      assertThatThrownBy(() -> retroService.deleteRetroItem(1L)).isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("Cannot delete items from a closed retrospective");
    }
  }
}
