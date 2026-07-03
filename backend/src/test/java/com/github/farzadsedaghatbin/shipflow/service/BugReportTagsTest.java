package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.dto.qa.BugReportDTO;
import com.github.farzadsedaghatbin.shipflow.dto.qa.CreateBugReportRequest;
import com.github.farzadsedaghatbin.shipflow.dto.qa.UpdateBugReportRequest;
import com.github.farzadsedaghatbin.shipflow.entity.BugReport;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity;
import com.github.farzadsedaghatbin.shipflow.repository.BugReportRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Regression tests for the bug-report tags round trip: creating/updating with tags, and
 * re-reading them back via {@code toDTO}. A QA team reported tags entered on create not
 * showing up when viewing the bug afterward.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("BugReport Tags Tests")
class BugReportTagsTest {

  @Mock
  private BugReportRepository bugReportRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private BugReportService bugReportService;

  private User user;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(bugReportService, "testManagementEnabled", true);
    user = User.builder().id(1L).username("qa.tester").build();
    lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    lenient().when(bugReportRepository.save(any(BugReport.class))).thenAnswer(inv -> inv.getArgument(0));
  }

  @Test
  @DisplayName("Tags entered on create round-trip through toDTO as both tags and tagList")
  void createBugReport_WithTags_RoundTripsCorrectly() {
    CreateBugReportRequest request = CreateBugReportRequest.builder().title("Login fails").description("...")
        .severity(BugSeverity.MAJOR).tags(List.of("auth", "login", "regression")).build();

    BugReportDTO result = bugReportService.createBugReport(request, 1L);

    assertThat(result.getTags()).isEqualTo("auth,login,regression");
    assertThat(result.getTagList()).containsExactly("auth", "login", "regression");
  }

  @Test
  @DisplayName("Empty tags list is stored as null, not an empty-string tag")
  void createBugReport_WithEmptyTagsList_StoresNull() {
    CreateBugReportRequest request = CreateBugReportRequest.builder().title("Login fails").description("...")
        .severity(BugSeverity.MAJOR).tags(List.of()).build();

    BugReportDTO result = bugReportService.createBugReport(request, 1L);

    // Before the fix, String.join(",", List.of()) produced "" (non-null), which toDTO then
    // split into a single-element list [""] — rendering one blank tag badge in the UI instead
    // of hiding the Tags section entirely.
    assertThat(result.getTags()).isNull();
    assertThat(result.getTagList()).isNull();
  }

  @Test
  @DisplayName("Blank-only tag entries are dropped rather than stored as empty tags")
  void createBugReport_WithBlankTagEntries_DropsBlanks() {
    CreateBugReportRequest request = CreateBugReportRequest.builder().title("Login fails").description("...")
        .severity(BugSeverity.MAJOR).tags(List.of("auth", "  ", "")).build();

    BugReportDTO result = bugReportService.createBugReport(request, 1L);

    assertThat(result.getTags()).isEqualTo("auth");
    assertThat(result.getTagList()).containsExactly("auth");
  }

  @Test
  @DisplayName("Removing all tags on update clears them to null instead of an empty string")
  void updateBugReport_ClearingAllTags_StoresNull() {
    BugReport existing = BugReport.builder().id(5L).title("Login fails").description("...")
        .severity(BugSeverity.MAJOR).tags("auth,login").build();
    when(bugReportRepository.findById(5L)).thenReturn(Optional.of(existing));

    UpdateBugReportRequest request = UpdateBugReportRequest.builder().title("Login fails").description("...")
        .severity(BugSeverity.MAJOR).tags(List.of()).build();

    BugReportDTO result = bugReportService.updateBugReport(5L, request, 1L);

    assertThat(result.getTags()).isNull();
    assertThat(result.getTagList()).isNull();
  }
}
