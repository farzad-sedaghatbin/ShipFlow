package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.CreateEvidenceRequest;
import com.github.farzadsedaghatbin.shipflow.dto.EvidenceDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Evidence;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.repository.EvidenceRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EvidenceService {

  private final EvidenceRepository evidenceRepository;
  private final PitchRepository pitchRepository;
  private final PersonRepository personRepository;
  private final ApplicationEventPublisher eventPublisher;

  public List<EvidenceDTO> getAllEvidences() {
    return evidenceRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
  }

  public List<EvidenceDTO> getEvidencesByPitchId(Long pitchId) {
    return evidenceRepository.findByPitchId(pitchId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  public List<EvidenceDTO> getEvidencesByPersonId(Long personId) {
    return evidenceRepository.findByPersonId(personId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  public EvidenceDTO getEvidenceById(Long id) {
    Evidence evidence = evidenceRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Evidence not found with id: " + id));
    return toDTO(evidence);
  }

  public EvidenceDTO createEvidence(CreateEvidenceRequest request) {
    Pitch pitch = pitchRepository.findById(request.getPitchId())
        .orElseThrow(() -> new IllegalArgumentException("Pitch not found with id: " + request.getPitchId()));

    Person person = personRepository.findById(request.getPersonId())
        .orElseThrow(() -> new IllegalArgumentException("Person not found with id: " + request.getPersonId()));

    Evidence evidence = Evidence.builder().pitch(pitch).person(person).date(request.getDate())
        .description(request.getDescription()).fileUrl(request.getFileUrl()).build();

    Evidence saved = evidenceRepository.save(evidence);

    // Publish event for knowledge ingestion
    eventPublisher.publishEvent(new KnowledgeEventListener.EvidenceKnowledgeEvent(saved.getId()));

    return toDTO(saved);
  }

  public EvidenceDTO updateEvidence(Long id, CreateEvidenceRequest request) {
    Evidence evidence = evidenceRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Evidence not found with id: " + id));

    evidence.setDate(request.getDate());
    evidence.setDescription(request.getDescription());
    evidence.setFileUrl(request.getFileUrl());

    Evidence saved = evidenceRepository.save(evidence);
    return toDTO(saved);
  }

  public void deleteEvidence(Long id) {
    evidenceRepository.deleteById(id);
  }

  private EvidenceDTO toDTO(Evidence evidence) {
    return EvidenceDTO.builder().id(evidence.getId()).pitchId(evidence.getPitch().getId())
        .pitchTitle(evidence.getPitch().getTitle())
        .cycleId(evidence.getPitch().getCycle() != null ? evidence.getPitch().getCycle().getId() : null)
        .cycleName(evidence.getPitch().getCycle() != null ? evidence.getPitch().getCycle().getName() : null)
        .projectId(evidence.getPitch().getCycle() != null && evidence.getPitch().getCycle().getProject() != null
            ? evidence.getPitch().getCycle().getProject().getId()
            : null)
        .projectName(
            evidence.getPitch().getCycle() != null && evidence.getPitch().getCycle().getProject() != null
                ? evidence.getPitch().getCycle().getProject().getName()
                : null)
        .projectKey(
            evidence.getPitch().getCycle() != null && evidence.getPitch().getCycle().getProject() != null
                ? evidence.getPitch().getCycle().getProject().getProjectKey()
                : null)
        .personId(evidence.getPerson().getId()).personName(evidence.getPerson().getName())
        .date(evidence.getDate()).description(evidence.getDescription()).fileUrl(evidence.getFileUrl()).build();
  }
}
