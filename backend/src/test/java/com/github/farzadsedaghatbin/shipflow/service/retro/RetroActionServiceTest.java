package com.github.farzadsedaghatbin.shipflow.service.retro;

import static com.github.farzadsedaghatbin.shipflow.service.retro.RetroTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.CycleRetroStatusDTO;
import com.github.farzadsedaghatbin.shipflow.dto.RetroActionStatsDTO;
import com.github.farzadsedaghatbin.shipflow.dto.RetroItemDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroColumnType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RetroItemRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RetroRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("RetroActionService")
class RetroActionServiceTest {

  @Mock private RetroRepository retroRepository;
  @Mock private RetroItemRepository retroItemRepository;
  @Mock private CycleRepository cycleRepository;
  @Mock private RetroCrudService retroCrudService;
  @Mock private RetroMapper retroMapper;

  @InjectMocks private RetroActionService service;

  private Project testProject;
  private Cycle testCycle;
  private User testUser;

  @BeforeEach
  void setUp() {
    testProject = aProject().build();
    testCycle = aCycle().withProject(testProject).build();
    testUser = aUser().build();
    
    lenient().when(retroCrudService.getCurrentUser()).thenReturn(testUser);
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // ==================== ACTION TRACKING TESTS ====================

  @Nested
  @DisplayName("Mark Acted On")
  class MarkActedOnTests {

    @Test
    @DisplayName("markActedOn sets fields when true")
    void markActedOn_WhenTrue_SetsFields() {
      RetroItem item = aRetroItem().withId(1L).build();
      RetroItemDTO dto = RetroItemDTO.builder()
          .id(1L)
          .actedOn(true)
          .actedOnNotes("Done in sprint")
          .build();

      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(item));
      when(retroItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
      when(retroMapper.toItemDTOWithLookup(any(), any())).thenReturn(dto);

      RetroItemDTO result = service.markActedOn(1L, true, "Done in sprint");

      assertThat(item.getActedOn()).isTrue();
      assertThat(item.getActedOnNotes()).isEqualTo("Done in sprint");
      assertThat(item.getActedOnAt()).isNotNull();
      assertThat(item.getActedOnBy()).isEqualTo(testUser);
    }

    @Test
    @DisplayName("markActedOn clears fields when false")
    void markActedOn_WhenFalse_ClearsFields() {
      RetroItem item = aRetroItem()
          .withId(1L)
          .actedOn("Previous notes", testUser)
          .build();
      RetroItemDTO dto = RetroItemDTO.builder().id(1L).actedOn(false).build();

      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(item));
      when(retroItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
      when(retroMapper.toItemDTOWithLookup(any(), any())).thenReturn(dto);

      service.markActedOn(1L, false, null);

      assertThat(item.getActedOn()).isFalse();
      assertThat(item.getActedOnNotes()).isNull();
      assertThat(item.getActedOnAt()).isNull();
      assertThat(item.getActedOnBy()).isNull();
    }

    @Test
    @DisplayName("markActedOn throws when item not found")
    void markActedOn_WhenNotFound_Throws() {
      when(retroItemRepository.findById(999L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.markActedOn(999L, true, "notes"))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  // ==================== ACTION STATS TESTS ====================

  @Nested
  @DisplayName("Action Stats")
  class ActionStatsTests {

    @ParameterizedTest
    @CsvSource({
        "4, 2, 50.0",   // 50% follow-through
        "5, 5, 100.0",  // 100% follow-through
        "3, 0, 0.0",    // 0% follow-through
        "0, 0, 0.0"     // No actions
    })
    @DisplayName("getActionStats calculates follow-through rate correctly")
    void getActionStats_CalculatesRateCorrectly(
        int totalActions, long actedOnCount, double expectedRate) {
      
      List<RetroItem> actionItems = Arrays.asList(
          new RetroItem[totalActions]
      );

      when(retroRepository.existsById(1L)).thenReturn(true);
      when(retroItemRepository.findByRetrospectiveIdAndColumnType(1L, RetroColumnType.ACTIONS))
          .thenReturn(actionItems);
      when(retroItemRepository.countActedOnByRetrospectiveId(1L)).thenReturn(actedOnCount);

      RetroActionStatsDTO result = service.getActionStats(1L);

      assertThat(result.getTotalActionItems()).isEqualTo(totalActions);
      assertThat(result.getActedOnCount()).isEqualTo(actedOnCount);
      assertThat(result.getPendingCount()).isEqualTo(totalActions - actedOnCount);
      assertThat(result.getFollowThroughRate()).isEqualTo(expectedRate);
    }

    @Test
    @DisplayName("getActionStats throws when retro not found")
    void getActionStats_WhenNotFound_Throws() {
      when(retroRepository.existsById(999L)).thenReturn(false);

      assertThatThrownBy(() -> service.getActionStats(999L))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  // ==================== PENDING ACTIONS TESTS ====================

  @Nested
  @DisplayName("Pending Action Items")
  class PendingActionItemsTests {

    @Test
    @DisplayName("getPendingActionItems returns unacted items")
    void getPendingActionItems_ReturnsUnactedItems() {
      RetroItem item1 = aRetroItem().withId(1L).action().build();
      RetroItem item2 = aRetroItem().withId(2L).action().build();
      List<RetroItem> items = Arrays.asList(item1, item2);

      RetroItemDTO dto1 = RetroItemDTO.builder().id(1L).build();
      RetroItemDTO dto2 = RetroItemDTO.builder().id(2L).build();

      doNothing().when(retroCrudService).validateRetrospectivesEnabled(1L);
      when(retroItemRepository.findUnactedActionItemsByProjectId(1L)).thenReturn(items);
      when(retroMapper.toItemDTOBatch(items, testUser)).thenReturn(Arrays.asList(dto1, dto2));

      List<RetroItemDTO> result = service.getPendingActionItems(1L);

      assertThat(result).hasSize(2);
    }
  }

  // ==================== CYCLE RETRO STATUS TESTS ====================

  @Nested
  @DisplayName("Cycle Retro Status")
  class CycleRetroStatusTests {

    @Test
    @DisplayName("getCycleRetroStatus returns cannot close with 0 closed retros")
    void getCycleRetroStatus_WithZeroClosedRetros_CannotClose() {
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(retroRepository.countByCycleId(1L)).thenReturn(2L);
      when(retroRepository.countByCycleIdAndStatus(1L, RetroStatus.CLOSED)).thenReturn(0L);

      CycleRetroStatusDTO result = service.getCycleRetroStatus(1L);

      assertThat(result.getCanCloseCycle()).isFalse();
      assertThat(result.getTotalRetros()).isEqualTo(2);
      assertThat(result.getClosedRetros()).isEqualTo(0);
      assertThat(result.getMessage()).contains("create and close");
    }

    @Test
    @DisplayName("getCycleRetroStatus returns can close with 1+ closed retros")
    void getCycleRetroStatus_WithClosedRetro_CanClose() {
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(retroRepository.countByCycleId(1L)).thenReturn(2L);
      when(retroRepository.countByCycleIdAndStatus(1L, RetroStatus.CLOSED)).thenReturn(1L);

      CycleRetroStatusDTO result = service.getCycleRetroStatus(1L);

      assertThat(result.getCanCloseCycle()).isTrue();
      assertThat(result.getMessage()).contains("can be closed");
    }

    @Test
    @DisplayName("getCycleRetroStatus bypasses check when retros disabled")
    void getCycleRetroStatus_WhenRetrosDisabled_BypassesCheck() {
      testProject.setEnableRetrospectives(false);
      
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(retroRepository.countByCycleId(1L)).thenReturn(0L);
      when(retroRepository.countByCycleIdAndStatus(1L, RetroStatus.CLOSED)).thenReturn(0L);

      CycleRetroStatusDTO result = service.getCycleRetroStatus(1L);

      assertThat(result.getCanCloseCycle()).isTrue();
      assertThat(result.getMessage()).contains("disabled");
    }

    @Test
    @DisplayName("canCloseCycle returns boolean")
    void canCloseCycle_ReturnsBoolean() {
      when(cycleRepository.findByIdWithProject(1L)).thenReturn(Optional.of(testCycle));
      when(retroRepository.countByCycleId(1L)).thenReturn(1L);
      when(retroRepository.countByCycleIdAndStatus(1L, RetroStatus.CLOSED)).thenReturn(1L);

      boolean result = service.canCloseCycle(1L);

      assertThat(result).isTrue();
    }

    @Test
    @DisplayName("getCycleRetroStatus throws when cycle not found")
    void getCycleRetroStatus_WhenCycleNotFound_Throws() {
      when(cycleRepository.findByIdWithProject(999L)).thenReturn(Optional.empty());

      assertThatThrownBy(() -> service.getCycleRetroStatus(999L))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }
}
