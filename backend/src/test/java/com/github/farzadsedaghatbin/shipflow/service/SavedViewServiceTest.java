package com.github.farzadsedaghatbin.shipflow.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.savedview.CreateSavedViewRequest;
import com.github.farzadsedaghatbin.shipflow.dto.savedview.SavedViewDTO;
import com.github.farzadsedaghatbin.shipflow.dto.savedview.UpdateSavedViewRequest;
import com.github.farzadsedaghatbin.shipflow.entity.SavedView;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.SavedViewRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SavedViewService Tests")
class SavedViewServiceTest {

  @Mock
  private SavedViewRepository savedViewRepository;

  @Spy
  private ObjectMapper objectMapper;

  @InjectMocks
  private SavedViewService savedViewService;

  private static final Long USER_ID = 1L;
  private static final Long OTHER_USER_ID = 99L;
  private static final Long PROJECT_ID = 10L;
  private static final String FILTERS_JSON = "{\"statusFilter\":[\"BACKLOG\"],\"priorityFilter\":[\"URGENT\"]}";

  private SavedView buildView(Long id, String name, boolean isDefault) {
    SavedView view = new SavedView();
    view.setId(id);
    view.setUserId(USER_ID);
    view.setProjectId(PROJECT_ID);
    view.setName(name);
    view.setFilters(FILTERS_JSON);
    view.setDefault(isDefault);
    view.setCreatedAt(LocalDateTime.now());
    view.setUpdatedAt(LocalDateTime.now());
    return view;
  }

  // ── getSavedViews ────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("getSavedViews")
  class GetSavedViews {

    @Test
    @DisplayName("returns only views for the correct user+project")
    void returnsViewsForCorrectUserAndProject() {
      SavedView v1 = buildView(1L, "High Priority Bugs", false);
      SavedView v2 = buildView(2L, "My In Progress", true);
      when(savedViewRepository.findByUserIdAndProjectId(USER_ID, PROJECT_ID))
          .thenReturn(List.of(v1, v2));

      List<SavedViewDTO> result = savedViewService.getSavedViews(USER_ID, PROJECT_ID);

      assertEquals(2, result.size());
      assertEquals("High Priority Bugs", result.get(0).getName());
      assertEquals("My In Progress", result.get(1).getName());
      verify(savedViewRepository).findByUserIdAndProjectId(USER_ID, PROJECT_ID);
    }

    @Test
    @DisplayName("returns empty list when no views exist")
    void returnsEmptyListWhenNone() {
      when(savedViewRepository.findByUserIdAndProjectId(USER_ID, PROJECT_ID))
          .thenReturn(List.of());

      List<SavedViewDTO> result = savedViewService.getSavedViews(USER_ID, PROJECT_ID);

      assertTrue(result.isEmpty());
    }
  }

  // ── createSavedView ──────────────────────────────────────────────────────────

  @Nested
  @DisplayName("createSavedView")
  class CreateSavedView {

    @Test
    @DisplayName("creates view successfully (happy path)")
    void createsViewSuccessfully() {
      CreateSavedViewRequest request = new CreateSavedViewRequest("High Priority Bugs", FILTERS_JSON);
      SavedView saved = buildView(1L, "High Priority Bugs", false);

      when(savedViewRepository.existsByUserIdAndProjectIdAndName(USER_ID, PROJECT_ID, "High Priority Bugs"))
          .thenReturn(false);
      when(savedViewRepository.save(any(SavedView.class))).thenReturn(saved);

      SavedViewDTO result = savedViewService.createSavedView(USER_ID, PROJECT_ID, request);

      assertNotNull(result);
      assertEquals("High Priority Bugs", result.getName());
      assertEquals(FILTERS_JSON, result.getFilters());
      assertFalse(result.isDefault());
      verify(savedViewRepository).save(any(SavedView.class));
    }

    @Test
    @DisplayName("throws BadRequestException on duplicate name for same user+project")
    void throwsOnDuplicateName() {
      CreateSavedViewRequest request = new CreateSavedViewRequest("High Priority Bugs", FILTERS_JSON);

      when(savedViewRepository.existsByUserIdAndProjectIdAndName(USER_ID, PROJECT_ID, "High Priority Bugs"))
          .thenReturn(true);

      assertThrows(BadRequestException.class,
          () -> savedViewService.createSavedView(USER_ID, PROJECT_ID, request));
      verify(savedViewRepository, never()).save(any());
    }

    @Test
    @DisplayName("throws BadRequestException when filters is not valid JSON")
    void throwsOnInvalidFiltersJson() {
      CreateSavedViewRequest request = new CreateSavedViewRequest("My View", "not-valid-json{{");

      assertThrows(BadRequestException.class,
          () -> savedViewService.createSavedView(USER_ID, PROJECT_ID, request));
      verify(savedViewRepository, never()).save(any());
    }
  }

  // ── deleteSavedView ──────────────────────────────────────────────────────────

  @Nested
  @DisplayName("deleteSavedView")
  class DeleteSavedView {

    @Test
    @DisplayName("deletes view when owned by the user")
    void deletesOwnedView() {
      SavedView view = buildView(1L, "High Priority Bugs", false);
      when(savedViewRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(view));

      savedViewService.deleteSavedView(1L, USER_ID, PROJECT_ID);

      verify(savedViewRepository).delete(view);
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when view belongs to a different user")
    void throwsWhenNotOwner() {
      when(savedViewRepository.findByIdAndUserId(1L, OTHER_USER_ID)).thenReturn(Optional.empty());

      assertThrows(ResourceNotFoundException.class,
          () -> savedViewService.deleteSavedView(1L, OTHER_USER_ID, PROJECT_ID));
      verify(savedViewRepository, never()).delete(any());
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when projectId does not match view's project")
    void throwsWhenProjectIdMismatch() {
      SavedView view = buildView(1L, "High Priority Bugs", false);
      when(savedViewRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(view));

      assertThrows(ResourceNotFoundException.class,
          () -> savedViewService.deleteSavedView(1L, USER_ID, 999L));
      verify(savedViewRepository, never()).delete(any());
    }
  }

  // ── setDefault ───────────────────────────────────────────────────────────────

  @Nested
  @DisplayName("setDefault")
  class SetDefault {

    @Test
    @DisplayName("unsets previous default before marking new one")
    void unsetsPreviousDefaultBeforeSettingNew() {
      SavedView previousDefault = buildView(1L, "Old Default", true);
      SavedView newDefault = buildView(2L, "New Default", false);

      when(savedViewRepository.findByIdAndUserId(2L, USER_ID))
          .thenReturn(Optional.of(newDefault));
      when(savedViewRepository.findByUserIdAndProjectIdAndIsDefaultTrue(USER_ID, PROJECT_ID))
          .thenReturn(Optional.of(previousDefault));
      when(savedViewRepository.save(any(SavedView.class))).thenAnswer(i -> i.getArgument(0));

      SavedViewDTO result = savedViewService.setDefault(2L, USER_ID, PROJECT_ID);

      assertFalse(previousDefault.isDefault(), "previous default should be unset");
      assertTrue(newDefault.isDefault(), "new view should be default");
      assertTrue(result.isDefault());
      // save called for both: unset old default + set new default
      verify(savedViewRepository, times(2)).save(any(SavedView.class));
    }

    @Test
    @DisplayName("sets default when no previous default exists")
    void setsDefaultWhenNoPreviousDefault() {
      SavedView view = buildView(1L, "My View", false);

      when(savedViewRepository.findByIdAndUserId(1L, USER_ID))
          .thenReturn(Optional.of(view));
      when(savedViewRepository.findByUserIdAndProjectIdAndIsDefaultTrue(USER_ID, PROJECT_ID))
          .thenReturn(Optional.empty());
      when(savedViewRepository.save(any(SavedView.class))).thenAnswer(i -> i.getArgument(0));

      SavedViewDTO result = savedViewService.setDefault(1L, USER_ID, PROJECT_ID);

      assertTrue(result.isDefault());
      verify(savedViewRepository, times(1)).save(any(SavedView.class));
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when view not owned by user")
    void throwsWhenNotOwner() {
      when(savedViewRepository.findByIdAndUserId(1L, OTHER_USER_ID)).thenReturn(Optional.empty());

      assertThrows(ResourceNotFoundException.class,
          () -> savedViewService.setDefault(1L, OTHER_USER_ID, PROJECT_ID));
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when projectId does not match view's project")
    void throwsWhenProjectIdMismatch() {
      SavedView view = buildView(1L, "My View", false);
      when(savedViewRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(view));

      assertThrows(ResourceNotFoundException.class,
          () -> savedViewService.setDefault(1L, USER_ID, 999L));
    }
  }

  // ── updateSavedView ──────────────────────────────────────────────────────────

  @Nested
  @DisplayName("updateSavedView")
  class UpdateSavedView {

    @Test
    @DisplayName("updates name and filters successfully")
    void updatesNameAndFilters() {
      SavedView view = buildView(1L, "Old Name", false);
      UpdateSavedViewRequest request = new UpdateSavedViewRequest("New Name",
          "{\"statusFilter\":[\"IN_PROGRESS\"]}");

      when(savedViewRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(view));
      when(savedViewRepository.existsByUserIdAndProjectIdAndName(USER_ID, PROJECT_ID, "New Name"))
          .thenReturn(false);
      when(savedViewRepository.save(any(SavedView.class))).thenAnswer(i -> i.getArgument(0));

      SavedViewDTO result = savedViewService.updateSavedView(1L, USER_ID, PROJECT_ID, request);

      assertEquals("New Name", result.getName());
      assertEquals("{\"statusFilter\":[\"IN_PROGRESS\"]}", result.getFilters());
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when view not found")
    void throwsWhenNotFound() {
      when(savedViewRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.empty());

      assertThrows(ResourceNotFoundException.class,
          () -> savedViewService.updateSavedView(1L, USER_ID, PROJECT_ID, new UpdateSavedViewRequest()));
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when projectId does not match view's project")
    void throwsWhenProjectIdMismatch() {
      SavedView view = buildView(1L, "Old Name", false);
      when(savedViewRepository.findByIdAndUserId(1L, USER_ID)).thenReturn(Optional.of(view));

      assertThrows(ResourceNotFoundException.class,
          () -> savedViewService.updateSavedView(1L, USER_ID, 999L, new UpdateSavedViewRequest()));
    }
  }
}
