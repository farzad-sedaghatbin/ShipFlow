package com.github.farzadsedaghatbin.shipflow.service.retro;

import static com.github.farzadsedaghatbin.shipflow.service.retro.RetroTestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.CreateRetroItemRequest;
import com.github.farzadsedaghatbin.shipflow.dto.RetroItemDTO;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroColumnType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroStatus;
import org.springframework.security.access.AccessDeniedException;
import com.github.farzadsedaghatbin.shipflow.repository.RetroItemRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RetroItemVoteRepository;
import com.github.farzadsedaghatbin.shipflow.repository.RetroRepository;
import com.github.farzadsedaghatbin.shipflow.service.LocalizationService;
import com.github.farzadsedaghatbin.shipflow.service.MessageService;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@DisplayName("RetroItemService")
class RetroItemServiceTest {

  @Mock private RetroRepository retroRepository;
  @Mock private RetroItemRepository retroItemRepository;
  @Mock private RetroItemVoteRepository retroItemVoteRepository;
  @Mock private LocalizationService localizationService;
  @Mock private MessageService messageService;
  @Mock private RetroMapper retroMapper;
  @Mock private RetroCrudService retroCrudService;
  @Mock private RetroSseService retroSseService;

  @InjectMocks private RetroItemService service;

  private Project testProject;
  private Cycle testCycle;
  private User testUser;
  private Retrospective testRetro;

  @BeforeEach
  void setUp() {
    testProject = aProject().build();
    testCycle = aCycle().withProject(testProject).build();
    testUser = aUser().build();
    testRetro = aRetro()
        .open()
        .withCycle(testCycle)
        .withProject(testProject)
        .createdBy(testUser)
        .build();
    
    setupCommonMocks();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void setupCommonMocks() {
    lenient().when(messageService.getMessage(any(String.class)))
        .thenAnswer(i -> i.getArgument(0));
    lenient().when(localizationService.getMessage(any(String.class)))
        .thenAnswer(i -> i.getArgument(0));
    lenient().when(retroCrudService.getCurrentUser()).thenReturn(testUser);
  }

  // ==================== ITEM CRUD TESTS ====================

  @Nested
  @DisplayName("Item CRUD Operations")
  class ItemCrudTests {

    @Test
    @DisplayName("getItemsByRetro returns items with batch mapping")
    void getItemsByRetro_ReturnsItemsWithBatchMapping() {
      RetroItem item1 = aRetroItem().withId(1L).withRetrospective(testRetro).build();
      RetroItem item2 = aRetroItem().withId(2L).withRetrospective(testRetro).build();
      List<RetroItem> items = Arrays.asList(item1, item2);

      RetroItemDTO dto1 = RetroItemDTO.builder().id(1L).build();
      RetroItemDTO dto2 = RetroItemDTO.builder().id(2L).build();

      when(retroRepository.findById(1L)).thenReturn(Optional.of(testRetro));
      doNothing().when(retroCrudService).validateRetrospectivesEnabled(testProject.getId());
      when(retroItemRepository.findByRetrospectiveIdOrderByCreatedAtAsc(1L)).thenReturn(items);
      when(retroMapper.toItemDTOBatch(items, testUser)).thenReturn(Arrays.asList(dto1, dto2));

      List<RetroItemDTO> result = service.getItemsByRetro(1L);

      assertThat(result).hasSize(2);
      verify(retroMapper).toItemDTOBatch(items, testUser);
    }

    @ParameterizedTest
    @EnumSource(RetroColumnType.class)
    @DisplayName("createRetroItem succeeds for all column types")
    void createRetroItem_SucceedsForAllColumnTypes(RetroColumnType columnType) {
      RetroItem savedItem = aRetroItem()
          .withColumnType(columnType)
          .withRetrospective(testRetro)
          .withAuthor(testUser)
          .build();
      RetroItemDTO dto = RetroItemDTO.builder().id(1L).columnType(columnType).build();

      when(retroRepository.findById(1L)).thenReturn(Optional.of(testRetro));
      doNothing().when(retroCrudService).validateRetrospectivesEnabled(testProject.getId());
      when(retroItemRepository.save(any())).thenReturn(savedItem);
      when(retroMapper.toItemDTOWithLookup(any(), any())).thenReturn(dto);

      CreateRetroItemRequest request = CreateRetroItemRequest.builder()
          .content("Test")
          .columnType(columnType)
          .retrospectiveId(1L)
          .build();

      RetroItemDTO result = service.createRetroItem(request);

      assertThat(result.getColumnType()).isEqualTo(columnType);
    }

    @Test
    @DisplayName("createRetroItem handles anonymous items")
    void createRetroItem_HandlesAnonymousItems() {
      RetroItem savedItem = aRetroItem()
          .anonymous()
          .withRetrospective(testRetro)
          .build();
      RetroItemDTO dto = RetroItemDTO.builder().id(1L).isAnonymous(true).build();

      when(retroRepository.findById(1L)).thenReturn(Optional.of(testRetro));
      doNothing().when(retroCrudService).validateRetrospectivesEnabled(testProject.getId());
      when(retroItemRepository.save(any(RetroItem.class))).thenReturn(savedItem);
      when(retroMapper.toItemDTOWithLookup(any(), any())).thenReturn(dto);

      CreateRetroItemRequest request = CreateRetroItemRequest.builder()
          .content("Anonymous feedback")
          .columnType(RetroColumnType.WENT_WELL)
          .retrospectiveId(1L)
          .isAnonymous(true)
          .build();

      RetroItemDTO result = service.createRetroItem(request);

      assertThat(result.getIsAnonymous()).isTrue();
    }

    @Test
    @DisplayName("createRetroItem throws for closed retro")
    void createRetroItem_WhenRetroClosed_Throws() {
      testRetro.setStatus(RetroStatus.CLOSED);
      
      when(retroRepository.findById(1L)).thenReturn(Optional.of(testRetro));
      doNothing().when(retroCrudService).validateRetrospectivesEnabled(testProject.getId());

      CreateRetroItemRequest request = CreateRetroItemRequest.builder()
          .content("Test")
          .columnType(RetroColumnType.WENT_WELL)
          .retrospectiveId(1L)
          .build();

      assertThatThrownBy(() -> service.createRetroItem(request))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("updateRetroItem changes content when user is author")
    void updateRetroItem_ChangesContent() {
      RetroItem item = aRetroItem().withRetrospective(testRetro).withAuthor(testUser).build();
      RetroItemDTO dto = RetroItemDTO.builder().id(1L).content("Updated").build();

      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(item));
      doNothing().when(retroCrudService).validateRetrospectivesEnabled(testProject.getId());
      when(retroItemRepository.save(any())).thenReturn(item);
      when(retroMapper.toItemDTOWithLookup(any(), any())).thenReturn(dto);

      RetroItemDTO result = service.updateRetroItem(1L, "Updated");

      assertThat(result.getContent()).isEqualTo("Updated");
      verify(retroItemRepository).save(item);
    }

    @Test
    @DisplayName("updateRetroItem throws when non-owner non-admin tries to edit")
    void updateRetroItem_WhenNonOwner_Throws() {
      User otherUser = aUser().withId(99L).withUsername("other").build();
      RetroItem item = aRetroItem().withRetrospective(testRetro).withAuthor(otherUser).build();

      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(item));
      doNothing().when(retroCrudService).validateRetrospectivesEnabled(testProject.getId());

      assertThatThrownBy(() -> service.updateRetroItem(1L, "Hacked"))
          .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("updateRetroItem succeeds for ADMIN user regardless of authorship")
    void updateRetroItem_AdminCanEditAnyItem() {
      User adminUser = User.builder().id(2L).username("admin").role(UserRole.ADMIN).build();
      when(retroCrudService.getCurrentUser()).thenReturn(adminUser);

      User otherUser = aUser().withId(99L).withUsername("other").build();
      RetroItem item = aRetroItem().withRetrospective(testRetro).withAuthor(otherUser).build();
      RetroItemDTO dto = RetroItemDTO.builder().id(1L).content("Updated by admin").build();

      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(item));
      doNothing().when(retroCrudService).validateRetrospectivesEnabled(testProject.getId());
      when(retroItemRepository.save(any())).thenReturn(item);
      when(retroMapper.toItemDTOWithLookup(any(), any())).thenReturn(dto);

      RetroItemDTO result = service.updateRetroItem(1L, "Updated by admin");

      assertThat(result.getContent()).isEqualTo("Updated by admin");
    }

    @Test
    @DisplayName("deleteRetroItem removes item when user is author")
    void deleteRetroItem_RemovesItem() {
      RetroItem item = aRetroItem().withRetrospective(testRetro).withAuthor(testUser).build();

      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(item));
      doNothing().when(retroCrudService).validateRetrospectivesEnabled(testProject.getId());
      doNothing().when(retroItemRepository).deleteById(1L);

      service.deleteRetroItem(1L);

      verify(retroItemRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteRetroItem throws when non-owner non-admin tries to delete")
    void deleteRetroItem_WhenNonOwner_Throws() {
      User otherUser = aUser().withId(99L).withUsername("other").build();
      RetroItem item = aRetroItem().withRetrospective(testRetro).withAuthor(otherUser).build();

      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(item));
      doNothing().when(retroCrudService).validateRetrospectivesEnabled(testProject.getId());

      assertThatThrownBy(() -> service.deleteRetroItem(1L))
          .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("deleteRetroItem throws for closed retro")
    void deleteRetroItem_WhenRetroClosed_Throws() {
      testRetro.setStatus(RetroStatus.CLOSED);
      RetroItem item = aRetroItem().withRetrospective(testRetro).withAuthor(testUser).build();

      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(item));
      doNothing().when(retroCrudService).validateRetrospectivesEnabled(testProject.getId());

      assertThatThrownBy(() -> service.deleteRetroItem(1L))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  // ==================== VOTING TESTS ====================

  @Nested
  @DisplayName("Voting Operations")
  class VotingTests {

    @Test
    @DisplayName("toggleVote adds vote when not voted")
    void toggleVote_WhenNotVoted_AddsVote() {
      RetroItem item = aRetroItem().withRetrospective(testRetro).withVotes(0).build();
      RetroItemDTO dto = RetroItemDTO.builder().id(1L).voteCount(1).hasVoted(true).build();

      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(item));
      doNothing().when(retroCrudService).validateRetrospectivesEnabled(testProject.getId());
      when(retroItemVoteRepository.existsByRetroItemIdAndUserId(1L, testUser.getId()))
          .thenReturn(false);
      when(retroItemRepository.save(any())).thenReturn(item);
      when(retroMapper.toItemDTOWithLookup(any(), any())).thenReturn(dto);

      RetroItemDTO result = service.toggleVote(1L);

      assertThat(result.getVoteCount()).isEqualTo(1);
      verify(retroItemVoteRepository).save(any(RetroItemVote.class));
    }

    @Test
    @DisplayName("toggleVote removes vote when already voted")
    void toggleVote_WhenAlreadyVoted_RemovesVote() {
      RetroItem item = aRetroItem().withRetrospective(testRetro).withVotes(1).build();
      RetroItemDTO dto = RetroItemDTO.builder().id(1L).voteCount(0).hasVoted(false).build();

      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(item));
      doNothing().when(retroCrudService).validateRetrospectivesEnabled(testProject.getId());
      when(retroItemVoteRepository.existsByRetroItemIdAndUserId(1L, testUser.getId()))
          .thenReturn(true);
      when(retroItemRepository.save(any())).thenReturn(item);
      when(retroMapper.toItemDTOWithLookup(any(), any())).thenReturn(dto);

      service.toggleVote(1L);

      verify(retroItemVoteRepository).deleteByRetroItemIdAndUserId(1L, testUser.getId());
    }

    @Test
    @DisplayName("toggleVote throws for merged items")
    void toggleVote_WhenItemMerged_Throws() {
      RetroItem targetItem = aRetroItem().withId(2L).build();
      RetroItem mergedItem = aRetroItem()
          .withId(1L)
          .withRetrospective(testRetro)
          .mergedInto(targetItem)
          .build();

      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(mergedItem));
      doNothing().when(retroCrudService).validateRetrospectivesEnabled(testProject.getId());

      assertThatThrownBy(() -> service.toggleVote(1L))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("toggleVote throws when user is null")
    void toggleVote_WhenUserNull_Throws() {
      RetroItem item = aRetroItem().withRetrospective(testRetro).build();

      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(item));
      doNothing().when(retroCrudService).validateRetrospectivesEnabled(testProject.getId());
      when(retroCrudService.getCurrentUser()).thenReturn(null);

      assertThatThrownBy(() -> service.toggleVote(1L))
          .isInstanceOf(IllegalStateException.class);
    }
  }

  // ==================== MERGING TESTS ====================

  @Nested
  @DisplayName("Merging Operations")
  class MergingTests {

    @Test
    @DisplayName("mergeItems transfers votes and marks source as merged")
    void mergeItems_TransfersVotesAndMarksAsMerged() {
      RetroItem targetItem = aRetroItem()
          .withId(1L)
          .withRetrospective(testRetro)
          .wentWell()
          .withVotes(2)
          .build();
      RetroItem sourceItem = aRetroItem()
          .withId(2L)
          .withRetrospective(testRetro)
          .wentWell()
          .withVotes(3)
          .build();
      
      RetroItemVote sourceVote = RetroItemVote.builder()
          .retroItem(sourceItem)
          .user(aUser().withId(2L).build())
          .build();
      
      RetroItemDTO dto = RetroItemDTO.builder().id(1L).voteCount(3).build();

      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(targetItem));
      when(retroItemRepository.findById(2L)).thenReturn(Optional.of(sourceItem));
      doNothing().when(retroCrudService).validateRetrospectivesEnabled(testProject.getId());
      when(retroItemVoteRepository.findByRetroItemId(2L)).thenReturn(Collections.singletonList(sourceVote));
      when(retroItemVoteRepository.existsByRetroItemIdAndUserId(1L, 2L)).thenReturn(false);
      when(retroItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
      when(retroMapper.toItemDTOWithLookup(any(), any())).thenReturn(dto);

      service.mergeItems(1L, 2L);

      assertThat(sourceItem.getMergedInto()).isEqualTo(targetItem);
      assertThat(sourceItem.getVoteCount()).isEqualTo(0);
      verify(retroItemVoteRepository).save(any(RetroItemVote.class));
    }

    @Test
    @DisplayName("mergeItems throws for different retros")
    void mergeItems_WhenDifferentRetros_Throws() {
      Retrospective otherRetro = aRetro().withId(2L).build();
      RetroItem targetItem = aRetroItem().withId(1L).withRetrospective(testRetro).build();
      RetroItem sourceItem = aRetroItem().withId(2L).withRetrospective(otherRetro).build();

      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(targetItem));
      when(retroItemRepository.findById(2L)).thenReturn(Optional.of(sourceItem));

      assertThatThrownBy(() -> service.mergeItems(1L, 2L))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("mergeItems throws for different columns")
    void mergeItems_WhenDifferentColumns_Throws() {
      RetroItem targetItem = aRetroItem()
          .withId(1L)
          .withRetrospective(testRetro)
          .wentWell()
          .build();
      RetroItem sourceItem = aRetroItem()
          .withId(2L)
          .withRetrospective(testRetro)
          .didntGoWell()
          .build();

      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(targetItem));
      when(retroItemRepository.findById(2L)).thenReturn(Optional.of(sourceItem));

      assertThatThrownBy(() -> service.mergeItems(1L, 2L))
          .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("unmergeItem clears mergedInto")
    void unmergeItem_ClearsMergedInto() {
      RetroItem targetItem = aRetroItem().withId(2L).build();
      RetroItem item = aRetroItem()
          .withId(1L)
          .withRetrospective(testRetro)
          .mergedInto(targetItem)
          .build();
      RetroItemDTO dto = RetroItemDTO.builder().id(1L).build();

      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(item));
      doNothing().when(retroCrudService).validateRetrospectivesEnabled(testProject.getId());
      when(retroItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
      when(retroMapper.toItemDTOWithLookup(any(), any())).thenReturn(dto);

      service.unmergeItem(1L);

      assertThat(item.getMergedInto()).isNull();
    }

    @Test
    @DisplayName("unmergeItem throws when not merged")
    void unmergeItem_WhenNotMerged_Throws() {
      RetroItem item = aRetroItem().withRetrospective(testRetro).build();

      when(retroItemRepository.findById(1L)).thenReturn(Optional.of(item));

      assertThatThrownBy(() -> service.unmergeItem(1L))
          .isInstanceOf(IllegalStateException.class);
    }
  }
}
