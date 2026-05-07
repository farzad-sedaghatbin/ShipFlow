package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.CreateEvidenceRequest;
import com.github.farzadsedaghatbin.shipflow.dto.EvidenceDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Evidence;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.repository.EvidenceRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import java.time.LocalDate;
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
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class EvidenceServiceTest {

  @Mock
  private EvidenceRepository evidenceRepository;

  @Mock
  private PitchRepository pitchRepository;

  @Mock
  private PersonRepository personRepository;

  @Mock
  private ApplicationEventPublisher eventPublisher;

  @InjectMocks
  private EvidenceService evidenceService;

  private Evidence testEvidence;
  private Pitch testPitch;
  private Person testPerson;
  private CreateEvidenceRequest testRequest;

  @BeforeEach
  void setUp() {
    testPitch = Pitch.builder().id(1L).title("Test Pitch").build();

    testPerson = Person.builder().id(1L).name("John Doe").email("john@example.com").isActive(true)
        .createdAt(LocalDateTime.now()).build();

    testEvidence = Evidence.builder().id(1L).pitch(testPitch).person(testPerson).date(LocalDate.now())
        .description("Test evidence description").fileUrl("https://example.com/file.pdf").build();

    testRequest = new CreateEvidenceRequest();
    testRequest.setPitchId(1L);
    testRequest.setPersonId(1L);
    testRequest.setDate(LocalDate.now());
    testRequest.setDescription("Test evidence description");
    testRequest.setFileUrl("https://example.com/file.pdf");
  }

  @Test
  void getAllEvidences_ShouldReturnAllEvidences() {
    when(evidenceRepository.findAll()).thenReturn(Arrays.asList(testEvidence));

    List<EvidenceDTO> result = evidenceService.getAllEvidences();

    assertThat(result).hasSize(1);
  }

  @Test
  void getEvidenceById_WhenExists_ShouldReturnEvidence() {
    when(evidenceRepository.findById(1L)).thenReturn(Optional.of(testEvidence));

    EvidenceDTO result = evidenceService.getEvidenceById(1L);

    assertThat(result).isNotNull();
  }

  @Test
  void getEvidenceById_WhenNotExists_ShouldThrowException() {
    when(evidenceRepository.findById(999L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> evidenceService.getEvidenceById(999L)).isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Evidence not found");
  }

  @Test
  void createEvidence_ShouldSaveEvidence() {
    when(pitchRepository.findById(1L)).thenReturn(Optional.of(testPitch));
    when(personRepository.findById(1L)).thenReturn(Optional.of(testPerson));
    when(evidenceRepository.save(any(Evidence.class))).thenReturn(testEvidence);

    EvidenceDTO result = evidenceService.createEvidence(testRequest);

    assertThat(result).isNotNull();
    verify(evidenceRepository).save(any(Evidence.class));
  }

  @Test
  void updateEvidence_WhenExists_ShouldUpdateEvidence() {
    when(evidenceRepository.findById(1L)).thenReturn(Optional.of(testEvidence));
    when(evidenceRepository.save(any(Evidence.class))).thenReturn(testEvidence);

    testRequest.setDescription("Updated description");
    EvidenceDTO result = evidenceService.updateEvidence(1L, testRequest);

    assertThat(result).isNotNull();
    verify(evidenceRepository).save(any(Evidence.class));
  }

  @Test
  void deleteEvidence_ShouldCallRepository() {
    doNothing().when(evidenceRepository).deleteById(1L);

    evidenceService.deleteEvidence(1L);

    verify(evidenceRepository).deleteById(1L);
  }

  @Test
  void getEvidencesByPitchId_ShouldReturnEvidences() {
    when(evidenceRepository.findByPitchId(1L)).thenReturn(Arrays.asList(testEvidence));

    List<EvidenceDTO> result = evidenceService.getEvidencesByPitchId(1L);

    assertThat(result).hasSize(1);
    verify(evidenceRepository).findByPitchId(1L);
  }

  @Test
  void getEvidencesByPersonId_ShouldReturnEvidences() {
    when(evidenceRepository.findByPersonId(1L)).thenReturn(Arrays.asList(testEvidence));

    List<EvidenceDTO> result = evidenceService.getEvidencesByPersonId(1L);

    assertThat(result).hasSize(1);
    verify(evidenceRepository).findByPersonId(1L);
  }
}
