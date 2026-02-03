package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.*;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.Team;
import com.github.farzadsedaghatbin.shipflow.entity.TeamAssignment;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TeamAssignmentRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TeamRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonService {

  private final PersonRepository personRepository;
  private final TeamAssignmentRepository teamAssignmentRepository;
  private final TeamRepository teamRepository;
  private final MessageService messageService;

  public PersonDTO createPerson(CreatePersonRequest request) {
    if (personRepository.existsByEmail(request.getEmail())) {
      throw new IllegalArgumentException(
          messageService.getMessage("error.person.email.exists", request.getEmail()));
    }

    Person person = Person.builder().name(request.getName()).email(request.getEmail()).skills(request.getSkills())
        .avatarUrl(request.getAvatarUrl()).isActive(true).createdAt(LocalDateTime.now()).build();

    Person saved = personRepository.save(person);
    return mapToDTO(saved);
  }

  @Transactional(readOnly = true)
  public PersonDTO getPersonById(Long id) {
    Person person = personRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Person not found with id: " + id));
    return mapToDTO(person);
  }

  @Transactional(readOnly = true)
  public PersonDTO getPersonByEmail(String email) {
    Person person = personRepository.findByEmail(email)
        .orElseThrow(() -> new EntityNotFoundException("Person not found with email: " + email));
    return mapToDTO(person);
  }

  @Transactional(readOnly = true)
  public List<PersonDTO> getAllPersons() {
    return personRepository.findAll().stream().map(this::mapToDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<PersonDTO> getActivePersons() {
    return personRepository.findByIsActiveTrue().stream().map(this::mapToDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<PersonDTO> searchPersons(String query) {
    return personRepository.searchByNameOrEmail(query).stream().map(this::mapToDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<PersonDTO> getPersonsByTeam(Long teamId) {
    return personRepository.findByCurrentTeam(teamId).stream().map(this::mapToDTO).collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<PersonDTO> getPersonsByCycle(Long cycleId) {
    return personRepository.findByCurrentCycle(cycleId).stream().map(this::mapToDTO).collect(Collectors.toList());
  }

  public PersonDTO updatePerson(Long id, CreatePersonRequest request) {
    Person person = personRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Person not found with id: " + id));

    // Check if email is being changed and if new email already exists
    if (!person.getEmail().equals(request.getEmail()) && personRepository.existsByEmail(request.getEmail())) {
      throw new IllegalArgumentException(
          messageService.getMessage("error.person.email.exists", request.getEmail()));
    }

    person.setName(request.getName());
    person.setEmail(request.getEmail());
    person.setSkills(request.getSkills());
    person.setAvatarUrl(request.getAvatarUrl());

    Person saved = personRepository.save(person);
    return mapToDTO(saved);
  }

  public PersonDTO deactivatePerson(Long id) {
    Person person = personRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Person not found with id: " + id));

    person.setIsActive(false);

    // End all active assignments
    List<TeamAssignment> activeAssignments = teamAssignmentRepository.findByPersonIdAndIsActiveTrue(id);
    for (TeamAssignment assignment : activeAssignments) {
      assignment.setIsActive(false);
      assignment.setEndDate(LocalDate.now());
      teamAssignmentRepository.save(assignment);
    }

    Person saved = personRepository.save(person);
    return mapToDTO(saved);
  }

  public PersonDTO activatePerson(Long id) {
    Person person = personRepository.findById(id)
        .orElseThrow(() -> new EntityNotFoundException("Person not found with id: " + id));

    person.setIsActive(true);
    Person saved = personRepository.save(person);
    return mapToDTO(saved);
  }

  public void deletePerson(Long id) {
    if (!personRepository.existsById(id)) {
      throw new EntityNotFoundException("Person not found with id: " + id);
    }
    personRepository.deleteById(id);
  }

  // Team Assignment methods

  public TeamAssignmentDTO assignToTeam(CreateTeamAssignmentRequest request) {
    Person person = personRepository.findById(request.getPersonId())
        .orElseThrow(() -> new EntityNotFoundException("Person not found with id: " + request.getPersonId()));

    Team team = teamRepository.findById(request.getTeamId())
        .orElseThrow(() -> new EntityNotFoundException("Team not found with id: " + request.getTeamId()));

    // Check if already assigned to this team
    if (teamAssignmentRepository.existsActiveAssignment(request.getPersonId(), request.getTeamId())) {
      throw new IllegalArgumentException(messageService.getMessage("error.person.already.assigned"));
    }

    TeamAssignment assignment = TeamAssignment.builder().person(person).team(team).role(request.getRole())
        .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now())
        .endDate(request.getEndDate()).isActive(true).build();

    TeamAssignment saved = teamAssignmentRepository.save(assignment);
    return mapToAssignmentDTO(saved);
  }

  public TeamAssignmentDTO updateAssignment(Long assignmentId, CreateTeamAssignmentRequest request) {
    TeamAssignment assignment = teamAssignmentRepository.findById(assignmentId)
        .orElseThrow(() -> new EntityNotFoundException("Assignment not found with id: " + assignmentId));

    assignment.setRole(request.getRole());
    assignment.setStartDate(request.getStartDate());
    assignment.setEndDate(request.getEndDate());

    TeamAssignment saved = teamAssignmentRepository.save(assignment);
    return mapToAssignmentDTO(saved);
  }

  public TeamAssignmentDTO endAssignment(Long assignmentId) {
    TeamAssignment assignment = teamAssignmentRepository.findById(assignmentId)
        .orElseThrow(() -> new EntityNotFoundException("Assignment not found with id: " + assignmentId));

    assignment.setIsActive(false);
    assignment.setEndDate(LocalDate.now());

    TeamAssignment saved = teamAssignmentRepository.save(assignment);
    return mapToAssignmentDTO(saved);
  }

  @Transactional(readOnly = true)
  public List<TeamAssignmentDTO> getPersonAssignments(Long personId) {
    return teamAssignmentRepository.findByPersonIdWithTeamAndCycle(personId).stream().map(this::mapToAssignmentDTO)
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<TeamAssignmentDTO> getTeamAssignments(Long teamId) {
    return teamAssignmentRepository.findByTeamIdAndIsActiveTrue(teamId).stream().map(this::mapToAssignmentDTO)
        .collect(Collectors.toList());
  }

  // Mapping methods

  private PersonDTO mapToDTO(Person person) {
    List<TeamAssignment> assignments = teamAssignmentRepository.findByPersonIdWithTeamAndCycle(person.getId());

    List<TeamAssignmentDTO> currentAssignments = assignments.stream().filter(TeamAssignment::getIsActive)
        .map(this::mapToAssignmentDTO).collect(Collectors.toList());

    List<TeamAssignmentDTO> pastAssignments = assignments.stream().filter(a -> !a.getIsActive())
        .map(this::mapToAssignmentDTO).collect(Collectors.toList());

    return PersonDTO.builder().id(person.getId()).name(person.getName()).email(person.getEmail())
        .skills(person.getSkills()).avatarUrl(person.getAvatarUrl()).isActive(person.getIsActive())
        .createdAt(person.getCreatedAt()).currentAssignments(currentAssignments)
        .pastAssignments(pastAssignments).build();
  }

  private TeamAssignmentDTO mapToAssignmentDTO(TeamAssignment assignment) {
    Team team = assignment.getTeam();
    Cycle cycle = team.getCycle();

    return TeamAssignmentDTO.builder().id(assignment.getId()).personId(assignment.getPerson().getId())
        .personName(assignment.getPerson().getName()).teamId(team.getId()).teamName(team.getName())
        .cycleId(cycle != null ? cycle.getId() : null).cycleName(cycle != null ? cycle.getName() : null)
        .role(assignment.getRole()).startDate(assignment.getStartDate()).endDate(assignment.getEndDate())
        .isActive(assignment.getIsActive()).build();
  }
}
