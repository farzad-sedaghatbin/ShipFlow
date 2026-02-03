package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.qa.CreateNoteRequest;
import com.github.farzadsedaghatbin.shipflow.dto.qa.NoteDTO;
import com.github.farzadsedaghatbin.shipflow.entity.ManualNote;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing manual notes. */
@Service
@Slf4j
public class NoteService {

  private final ManualNoteRepository noteRepository;
  private final PitchRepository pitchRepository;
  private final MeetingRepository meetingRepository;
  private final TeamRepository teamRepository;
  private final CycleRepository cycleRepository;
  private final UserRepository userRepository;
  private final KnowledgeIngestionService knowledgeIngestionService;
  private final LocalizationService localizationService;

  @Autowired
  public NoteService(ManualNoteRepository noteRepository, PitchRepository pitchRepository,
      MeetingRepository meetingRepository, TeamRepository teamRepository, CycleRepository cycleRepository,
      UserRepository userRepository,
      @Autowired(required = false) KnowledgeIngestionService knowledgeIngestionService,
      LocalizationService localizationService) {
    this.noteRepository = noteRepository;
    this.pitchRepository = pitchRepository;
    this.meetingRepository = meetingRepository;
    this.teamRepository = teamRepository;
    this.cycleRepository = cycleRepository;
    this.userRepository = userRepository;
    this.knowledgeIngestionService = knowledgeIngestionService;
    this.localizationService = localizationService;
  }

  /** Create a new note. */
  @Transactional
  public NoteDTO createNote(CreateNoteRequest request, Long authorId) {
    ManualNote note = ManualNote.builder().title(request.getTitle()).content(request.getContent())
        .contextType(request.getContextType().toLowerCase()).contextId(request.getContextId())
        .authorId(authorId).includeInKnowledge(request.getIncludeInKnowledge()).build();

    // Set related IDs based on context
    setRelatedIds(note, request.getContextType(), request.getContextId());

    note = noteRepository.save(note);

    // Ingest into knowledge base if applicable
    if (note.getIncludeInKnowledge() && knowledgeIngestionService != null) {
      final Long noteId = note.getId();
      knowledgeIngestionService.ingestManualNote(noteId);
    }

    log.info("Created note: {} by user {}", note.getTitle(), authorId);
    return toDTO(note);
  }

  /** Update an existing note. */
  @Transactional
  public NoteDTO updateNote(Long noteId, CreateNoteRequest request, Long userId) {
    ManualNote note = noteRepository.findById(noteId)
        .orElseThrow(() -> new RuntimeException("Note not found: " + noteId));

    // Verify ownership
    if (!note.getAuthorId().equals(userId)) {
      throw new RuntimeException(localizationService.getMessage("note.unauthorized.edit"));
    }

    note.setTitle(request.getTitle());
    note.setContent(request.getContent());
    note.setIncludeInKnowledge(request.getIncludeInKnowledge());

    note = noteRepository.save(note);

    // Re-ingest into knowledge base
    if (note.getIncludeInKnowledge() && knowledgeIngestionService != null) {
      knowledgeIngestionService.ingestManualNote(note.getId());
    }

    return toDTO(note);
  }

  /** Delete a note. */
  @Transactional
  public void deleteNote(Long noteId, Long userId) {
    ManualNote note = noteRepository.findById(noteId)
        .orElseThrow(() -> new RuntimeException("Note not found: " + noteId));

    // Verify ownership
    if (!note.getAuthorId().equals(userId)) {
      throw new RuntimeException(localizationService.getMessage("note.unauthorized.delete"));
    }

    noteRepository.delete(note);
    log.info("Deleted note: {}", noteId);
  }

  /** Get a note by ID. */
  public NoteDTO getNoteById(Long noteId) {
    ManualNote note = noteRepository.findById(noteId)
        .orElseThrow(() -> new RuntimeException("Note not found: " + noteId));
    return toDTO(note);
  }

  /** Get notes by context. */
  public List<NoteDTO> getNotesByContext(String contextType, Long contextId) {
    return noteRepository.findByContextTypeAndContextId(contextType.toLowerCase(), contextId).stream()
        .map(this::toDTO).collect(Collectors.toList());
  }

  /** Get notes by author. */
  public List<NoteDTO> getNotesByAuthor(Long authorId) {
    return noteRepository.findByAuthorId(authorId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Get notes by pitch. */
  public List<NoteDTO> getNotesByPitch(Long pitchId) {
    return noteRepository.findByPitchId(pitchId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Get notes by cycle. */
  public List<NoteDTO> getNotesByCycle(Long cycleId) {
    return noteRepository.findByCycleId(cycleId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  /** Get notes by team. */
  public List<NoteDTO> getNotesByTeam(Long teamId) {
    return noteRepository.findByTeamId(teamId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  // ===== Private helper methods =====

  private void setRelatedIds(ManualNote note, String contextType, Long contextId) {
    switch (contextType.toLowerCase()) {
      case "pitch" :
        note.setPitchId(contextId);
        pitchRepository.findById(contextId).ifPresent(pitch -> {
          note.setCycleId(pitch.getCycle().getId());
          if (pitch.getTeam() != null) {
            note.setTeamId(pitch.getTeam().getId());
          }
        });
        break;
      case "meeting" :
        meetingRepository.findById(contextId).ifPresent(meeting -> {
          if (meeting.getPitch() != null) {
            note.setPitchId(meeting.getPitch().getId());
            note.setCycleId(meeting.getPitch().getCycle().getId());
            if (meeting.getPitch().getTeam() != null) {
              note.setTeamId(meeting.getPitch().getTeam().getId());
            }
          }
        });
        break;
      case "team" :
        note.setTeamId(contextId);
        teamRepository.findById(contextId).ifPresent(team -> {
          if (team.getCycle() != null) {
            note.setCycleId(team.getCycle().getId());
          }
        });
        break;
      case "cycle" :
        note.setCycleId(contextId);
        break;
    }
  }

  private NoteDTO toDTO(ManualNote note) {
    String authorName = null;
    if (note.getAuthorId() != null) {
      authorName = userRepository.findById(note.getAuthorId()).map(User::getUsername).orElse(null);
    }

    return NoteDTO.builder().id(note.getId()).title(note.getTitle()).content(note.getContent())
        .contextType(note.getContextType()).contextId(note.getContextId()).cycleId(note.getCycleId())
        .teamId(note.getTeamId()).pitchId(note.getPitchId()).authorId(note.getAuthorId()).authorName(authorName)
        .includeInKnowledge(note.getIncludeInKnowledge()).createdAt(note.getCreatedAt())
        .updatedAt(note.getUpdatedAt()).build();
  }
}
