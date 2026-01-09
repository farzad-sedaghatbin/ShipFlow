package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.*;
import com.github.farzadsedaghatbin.shipflow.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/persons")
@RequiredArgsConstructor
@Tag(name = "Persons", description = "Person management endpoints")
@CrossOrigin(origins = "*")
public class PersonController {
    
    private final PersonService personService;
    
    @PostMapping
    @Operation(summary = "Create a new person")
    public ResponseEntity<PersonDTO> createPerson(@Valid @RequestBody CreatePersonRequest request) {
        PersonDTO created = personService.createPerson(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get person by ID")
    public ResponseEntity<PersonDTO> getPersonById(@PathVariable Long id) {
        PersonDTO person = personService.getPersonById(id);
        return ResponseEntity.ok(person);
    }
    
    @GetMapping
    @Operation(summary = "Get all persons")
    public ResponseEntity<List<PersonDTO>> getAllPersons(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search) {
        
        List<PersonDTO> persons;
        if (search != null && !search.isEmpty()) {
            persons = personService.searchPersons(search);
        } else if (Boolean.TRUE.equals(active)) {
            persons = personService.getActivePersons();
        } else {
            persons = personService.getAllPersons();
        }
        return ResponseEntity.ok(persons);
    }
    
    @GetMapping("/team/{teamId}")
    @Operation(summary = "Get persons assigned to a team")
    public ResponseEntity<List<PersonDTO>> getPersonsByTeam(@PathVariable Long teamId) {
        List<PersonDTO> persons = personService.getPersonsByTeam(teamId);
        return ResponseEntity.ok(persons);
    }
    
    @GetMapping("/cycle/{cycleId}")
    @Operation(summary = "Get persons assigned to teams in a cycle")
    public ResponseEntity<List<PersonDTO>> getPersonsByCycle(@PathVariable Long cycleId) {
        List<PersonDTO> persons = personService.getPersonsByCycle(cycleId);
        return ResponseEntity.ok(persons);
    }
    
    @PutMapping("/{id}")
    @Operation(summary = "Update a person")
    public ResponseEntity<PersonDTO> updatePerson(
            @PathVariable Long id,
            @Valid @RequestBody CreatePersonRequest request) {
        PersonDTO updated = personService.updatePerson(id, request);
        return ResponseEntity.ok(updated);
    }
    
    @PostMapping("/{id}/deactivate")
    @Operation(summary = "Deactivate a person (soft delete)")
    public ResponseEntity<PersonDTO> deactivatePerson(@PathVariable Long id) {
        PersonDTO deactivated = personService.deactivatePerson(id);
        return ResponseEntity.ok(deactivated);
    }
    
    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate a person")
    public ResponseEntity<PersonDTO> activatePerson(@PathVariable Long id) {
        PersonDTO activated = personService.activatePerson(id);
        return ResponseEntity.ok(activated);
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a person permanently")
    public ResponseEntity<Void> deletePerson(@PathVariable Long id) {
        personService.deletePerson(id);
        return ResponseEntity.noContent().build();
    }
    
    // Team Assignment endpoints
    
    @PostMapping("/assignments")
    @Operation(summary = "Assign a person to a team")
    public ResponseEntity<TeamAssignmentDTO> assignToTeam(
            @Valid @RequestBody CreateTeamAssignmentRequest request) {
        TeamAssignmentDTO assignment = personService.assignToTeam(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(assignment);
    }
    
    @GetMapping("/{personId}/assignments")
    @Operation(summary = "Get all assignments for a person")
    public ResponseEntity<List<TeamAssignmentDTO>> getPersonAssignments(@PathVariable Long personId) {
        List<TeamAssignmentDTO> assignments = personService.getPersonAssignments(personId);
        return ResponseEntity.ok(assignments);
    }
    
    @PutMapping("/assignments/{assignmentId}")
    @Operation(summary = "Update a team assignment")
    public ResponseEntity<TeamAssignmentDTO> updateAssignment(
            @PathVariable Long assignmentId,
            @Valid @RequestBody CreateTeamAssignmentRequest request) {
        TeamAssignmentDTO updated = personService.updateAssignment(assignmentId, request);
        return ResponseEntity.ok(updated);
    }
    
    @PostMapping("/assignments/{assignmentId}/end")
    @Operation(summary = "End a team assignment")
    public ResponseEntity<TeamAssignmentDTO> endAssignment(@PathVariable Long assignmentId) {
        TeamAssignmentDTO ended = personService.endAssignment(assignmentId);
        return ResponseEntity.ok(ended);
    }
    
    @GetMapping("/teams/{teamId}/assignments")
    @Operation(summary = "Get all active assignments for a team")
    public ResponseEntity<List<TeamAssignmentDTO>> getTeamAssignments(@PathVariable Long teamId) {
        List<TeamAssignmentDTO> assignments = personService.getTeamAssignments(teamId);
        return ResponseEntity.ok(assignments);
    }
}
