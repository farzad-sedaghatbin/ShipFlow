package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.agreement.ProjectAgreementDTO;
import com.github.farzadsedaghatbin.shipflow.dto.agreement.ProjectAgreementRequest;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Meeting;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Project;
import com.github.farzadsedaghatbin.shipflow.entity.ProjectAgreement;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.MeetingRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectAgreementRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProjectAgreementServiceTest {

  @Mock
  private ProjectAgreementRepository projectAgreementRepository;

  @Mock
  private ProjectRepository projectRepository;

  @Mock
  private MeetingRepository meetingRepository;

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private ProjectAgreementService projectAgreementService;

  private Project project;
  private Project otherProject;
  private User currentUser;

  @BeforeEach
  void setUp() {
    project = Project.builder().id(1L).name("Mobile Banking App").build();
    otherProject = Project.builder().id(2L).name("DevOps Platform").build();
    currentUser = User.builder().id(10L).username("sara").build();

    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("sara", "pwd", List.of()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void listByProject_returnsMappedDTOs() {
    ProjectAgreement agreement = ProjectAgreement.builder()
        .id(1L)
        .project(project)
        .title("Sprint review cadence")
        .content("Every other Friday at 2pm.")
        .agreedDate(LocalDate.of(2026, 4, 1))
        .createdBy(currentUser)
        .createdAt(LocalDateTime.of(2026, 4, 1, 9, 0))
        .build();

    when(projectAgreementRepository.findByProjectIdNotDeleted(1L)).thenReturn(List.of(agreement));

    List<ProjectAgreementDTO> result = projectAgreementService.listByProject(1L);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getTitle()).isEqualTo("Sprint review cadence");
    assertThat(result.get(0).getProjectId()).isEqualTo(1L);
    assertThat(result.get(0).getCreatedByName()).isEqualTo("sara");
  }

  @Test
  void create_withoutMeeting_defaultsAgreedDateToToday() {
    when(userRepository.findByUsername("sara")).thenReturn(Optional.of(currentUser));
    when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
    when(projectAgreementRepository.save(any(ProjectAgreement.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    ProjectAgreementRequest request = ProjectAgreementRequest.builder()
        .title("On-call rotation ownership")
        .content("Payments team owns payments on-call this cycle.")
        .build();

    ProjectAgreementDTO result = projectAgreementService.create(1L, request);

    assertThat(result.getAgreedDate()).isEqualTo(LocalDate.now());
    assertThat(result.getMeetingId()).isNull();
    assertThat(result.getCreatedByName()).isEqualTo("sara");
    verify(projectAgreementRepository).save(any(ProjectAgreement.class));
  }

  @Test
  void create_withExplicitAgreedDate_usesProvidedDate() {
    when(userRepository.findByUsername("sara")).thenReturn(Optional.of(currentUser));
    when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
    when(projectAgreementRepository.save(any(ProjectAgreement.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    ProjectAgreementRequest request = ProjectAgreementRequest.builder()
        .title("Sprint review cadence")
        .content("Every other Friday at 2pm.")
        .agreedDate(LocalDate.of(2026, 4, 1))
        .build();

    ProjectAgreementDTO result = projectAgreementService.create(1L, request);

    assertThat(result.getAgreedDate()).isEqualTo(LocalDate.of(2026, 4, 1));
  }

  @Test
  void create_withMeetingInSameProject_linksMeeting() {
    Cycle cycle = Cycle.builder().id(5L).project(project).build();
    Pitch pitch = Pitch.builder().id(7L).cycle(cycle).build();
    Meeting meeting = Meeting.builder().id(3L).pitch(pitch).build();

    when(userRepository.findByUsername("sara")).thenReturn(Optional.of(currentUser));
    when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
    when(meetingRepository.findById(3L)).thenReturn(Optional.of(meeting));
    when(projectAgreementRepository.save(any(ProjectAgreement.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    ProjectAgreementRequest request = ProjectAgreementRequest.builder()
        .title("Kickoff agreement")
        .content("Agreed scope at kickoff.")
        .meetingId(3L)
        .build();

    ProjectAgreementDTO result = projectAgreementService.create(1L, request);

    assertThat(result.getMeetingId()).isEqualTo(3L);
  }

  @Test
  void create_withMeetingFromDifferentProject_throwsBadRequest() {
    Cycle otherCycle = Cycle.builder().id(6L).project(otherProject).build();
    Pitch otherPitch = Pitch.builder().id(8L).cycle(otherCycle).build();
    Meeting meetingFromOtherProject = Meeting.builder().id(4L).pitch(otherPitch).build();

    when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
    when(meetingRepository.findById(4L)).thenReturn(Optional.of(meetingFromOtherProject));

    ProjectAgreementRequest request = ProjectAgreementRequest.builder()
        .title("Cross-project attempt")
        .content("Should be rejected.")
        .meetingId(4L)
        .build();

    assertThatThrownBy(() -> projectAgreementService.create(1L, request))
        .isInstanceOf(BadRequestException.class)
        .hasMessageContaining("does not belong to project");

    verify(projectAgreementRepository, never()).save(any(ProjectAgreement.class));
  }

  @Test
  void create_projectNotFound_throwsResourceNotFound() {
    when(projectRepository.findById(99L)).thenReturn(Optional.empty());

    ProjectAgreementRequest request = ProjectAgreementRequest.builder()
        .title("Doesn't matter")
        .content("Doesn't matter")
        .build();

    assertThatThrownBy(() -> projectAgreementService.create(99L, request))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void update_existingAgreement_updatesFields() {
    ProjectAgreement existing = ProjectAgreement.builder()
        .id(1L)
        .project(project)
        .title("Old title")
        .content("Old content")
        .agreedDate(LocalDate.of(2026, 1, 1))
        .build();

    when(projectAgreementRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(existing));
    when(projectAgreementRepository.save(any(ProjectAgreement.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    ProjectAgreementRequest request = ProjectAgreementRequest.builder()
        .title("New title")
        .content("New content")
        .agreedDate(LocalDate.of(2026, 5, 1))
        .build();

    ProjectAgreementDTO result = projectAgreementService.update(project.getId(), 1L, request);

    assertThat(result.getTitle()).isEqualTo("New title");
    assertThat(result.getContent()).isEqualTo("New content");
    assertThat(result.getAgreedDate()).isEqualTo(LocalDate.of(2026, 5, 1));
  }

  @Test
  void update_notFound_throwsResourceNotFound() {
    when(projectAgreementRepository.findByIdNotDeleted(404L)).thenReturn(Optional.empty());

    ProjectAgreementRequest request = ProjectAgreementRequest.builder()
        .title("x")
        .content("y")
        .build();

    assertThatThrownBy(() -> projectAgreementService.update(project.getId(), 404L, request))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void update_agreementBelongsToDifferentProject_throwsResourceNotFound() {
    ProjectAgreement existing = ProjectAgreement.builder()
        .id(1L)
        .project(project)
        .title("Old title")
        .content("Old content")
        .agreedDate(LocalDate.of(2026, 1, 1))
        .build();

    when(projectAgreementRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(existing));

    ProjectAgreementRequest request = ProjectAgreementRequest.builder()
        .title("x")
        .content("y")
        .build();

    // otherProject.id (2L) does not match the agreement's own project (1L) — must not leak
    // or allow mutation across projects just because the id was guessed/incremented.
    assertThatThrownBy(() -> projectAgreementService.update(otherProject.getId(), 1L, request))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(projectAgreementRepository, never()).save(any());
  }

  @Test
  void delete_softDeletesAgreement() {
    ProjectAgreement existing = ProjectAgreement.builder()
        .id(1L)
        .project(project)
        .title("To delete")
        .content("Content")
        .agreedDate(LocalDate.now())
        .build();

    when(projectAgreementRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(existing));
    when(userRepository.findByUsername("sara")).thenReturn(Optional.of(currentUser));
    when(projectAgreementRepository.save(any(ProjectAgreement.class)))
        .thenAnswer(inv -> inv.getArgument(0));

    projectAgreementService.delete(project.getId(), 1L);

    assertThat(existing.getDeletedAt()).isNotNull();
    assertThat(existing.getDeletedBy()).isEqualTo(currentUser);
    verify(projectAgreementRepository, never()).deleteById(any());
  }

  @Test
  void delete_notFound_throwsResourceNotFound() {
    when(projectAgreementRepository.findByIdNotDeleted(404L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> projectAgreementService.delete(project.getId(), 404L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void delete_agreementBelongsToDifferentProject_throwsResourceNotFound() {
    ProjectAgreement existing = ProjectAgreement.builder()
        .id(1L)
        .project(project)
        .title("To delete")
        .content("Content")
        .agreedDate(LocalDate.now())
        .build();

    when(projectAgreementRepository.findByIdNotDeleted(1L)).thenReturn(Optional.of(existing));

    assertThatThrownBy(() -> projectAgreementService.delete(otherProject.getId(), 1L))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(projectAgreementRepository, never()).save(any());
  }
}
