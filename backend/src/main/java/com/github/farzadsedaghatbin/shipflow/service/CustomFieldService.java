package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.customfield.*;
import com.github.farzadsedaghatbin.shipflow.entity.CustomFieldDefinition;
import com.github.farzadsedaghatbin.shipflow.entity.CustomFieldValue;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldEntityType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldType;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.repository.CustomFieldDefinitionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CustomFieldValueRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomFieldService {

  private final CustomFieldDefinitionRepository definitionRepository;
  private final CustomFieldValueRepository valueRepository;
  private final ProjectRepository projectRepository;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  // ── Definitions ────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<CustomFieldDefinitionDTO> getDefinitionsForEntity(
      CustomFieldEntityType entityType, Long projectId) {
    List<CustomFieldDefinition> defs =
        projectId != null
            ? definitionRepository.findApplicable(entityType, projectId)
            : definitionRepository.findByEntityTypeAndDeletedAtIsNullOrderBySortOrderAsc(entityType);
    return defs.stream().map(this::toDefinitionDTO).collect(Collectors.toList());
  }

  public CustomFieldDefinitionDTO createDefinition(CreateCustomFieldDefinitionRequest req) {
    String currentUsername = currentUsername();
    var user =
        userRepository
            .findByUsername(currentUsername)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

    boolean isAdmin = user.getRole() == UserRole.ADMIN;

    if (req.getProjectId() == null && !isAdmin) {
      throw new AccessDeniedException("Only ADMIN can create org-wide custom fields");
    }

    validateOptions(req.getFieldType(), req.getOptions());

    var def =
        CustomFieldDefinition.builder()
            .name(req.getName())
            .description(req.getDescription())
            .fieldType(req.getFieldType())
            .entityType(req.getEntityType())
            .required(Boolean.TRUE.equals(req.getRequired()))
            .sortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0)
            .options(encodeOptions(req.getOptions()))
            .build();

    if (req.getProjectId() != null) {
      var project =
          projectRepository
              .findById(req.getProjectId())
              .orElseThrow(
                  () -> new IllegalArgumentException("Project not found: " + req.getProjectId()));
      def.setProject(project);
    }

    return toDefinitionDTO(definitionRepository.save(def));
  }

  public CustomFieldDefinitionDTO updateDefinition(Long id, UpdateCustomFieldDefinitionRequest req) {
    var def =
        definitionRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new IllegalArgumentException("Custom field not found: " + id));

    if (req.getName() != null) def.setName(req.getName());
    if (req.getDescription() != null) def.setDescription(req.getDescription());
    if (req.getRequired() != null) def.setRequired(req.getRequired());
    if (req.getSortOrder() != null) def.setSortOrder(req.getSortOrder());
    if (req.getOptions() != null) {
      validateOptions(def.getFieldType(), req.getOptions());
      def.setOptions(encodeOptions(req.getOptions()));
    }

    return toDefinitionDTO(definitionRepository.save(def));
  }

  public void deleteDefinition(Long id) {
    String currentUsername = currentUsername();
    var def =
        definitionRepository
            .findByIdAndDeletedAtIsNull(id)
            .orElseThrow(() -> new IllegalArgumentException("Custom field not found: " + id));

    var user =
        userRepository
            .findByUsername(currentUsername)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

    // Cascade: hard-delete all values first to avoid orphaned rows
    valueRepository.deleteByDefinitionId(id);

    def.setDeletedAt(OffsetDateTime.now());
    def.setDeletedBy(user);
    definitionRepository.save(def);

    log.info("Custom field definition {} soft-deleted by {}", id, currentUsername);
  }

  // ── Values ─────────────────────────────────────────────────────────────────

  @Transactional(readOnly = true)
  public List<CustomFieldValueDTO> getValuesForEntity(
      CustomFieldEntityType entityType, Long entityId) {
    return valueRepository.findByEntityTypeAndEntityId(entityType, entityId).stream()
        .map(this::toValueDTO)
        .collect(Collectors.toList());
  }

  public CustomFieldValueDTO upsertValue(UpsertCustomFieldValueRequest req) {
    String currentUsername = currentUsername();
    var def =
        definitionRepository
            .findByIdAndDeletedAtIsNull(req.getDefinitionId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "Custom field not found: " + req.getDefinitionId()));

    if (req.getValue() != null) {
      validateValue(def, req.getValue());
    } else if (Boolean.TRUE.equals(def.getRequired())) {
      throw new BadRequestException(def.getName() + " is required and cannot be cleared");
    }

    var user =
        userRepository
            .findByUsername(currentUsername)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + currentUsername));

    var existing =
        valueRepository.findByDefinitionIdAndEntityTypeAndEntityId(
            req.getDefinitionId(), req.getEntityType(), req.getEntityId());

    CustomFieldValue cfv =
        existing.orElseGet(
            () ->
                CustomFieldValue.builder()
                    .definition(def)
                    .entityType(req.getEntityType())
                    .entityId(req.getEntityId())
                    .build());

    cfv.setValue(req.getValue());
    cfv.setUpdatedBy(user);

    return toValueDTO(valueRepository.save(cfv));
  }

  public List<CustomFieldValueDTO> bulkUpsertValues(BulkUpsertCustomFieldValuesRequest req) {
    if (req.getValues() == null || req.getValues().isEmpty()) {
      return List.of();
    }
    List<CustomFieldValueDTO> results = new ArrayList<>();
    for (Map.Entry<Long, String> entry : req.getValues().entrySet()) {
      var single =
          new UpsertCustomFieldValueRequest(
              entry.getKey(), req.getEntityType(), req.getEntityId(), entry.getValue());
      results.add(upsertValue(single));
    }
    return results;
  }

  // ── Validation helpers ──────────────────────────────────────────────────────

  private void validateOptions(CustomFieldType type, List<String> options) {
    if ((type == CustomFieldType.SELECT || type == CustomFieldType.MULTISELECT)
        && (options == null || options.isEmpty())) {
      throw new BadRequestException(type + " fields require at least one option");
    }
  }

  private void validateValue(CustomFieldDefinition def, String value) {
    if (Boolean.TRUE.equals(def.getRequired())
        && (value == null || value.isBlank())) {
      throw new BadRequestException(def.getName() + " is required");
    }
    switch (def.getFieldType()) {
      case NUMBER -> {
        try {
          Double.parseDouble(value);
        } catch (NumberFormatException e) {
          throw new BadRequestException("Invalid number value: " + value);
        }
      }
      case DATE -> {
        try {
          LocalDate.parse(value);
        } catch (Exception e) {
          throw new BadRequestException("Invalid date value (expected yyyy-MM-dd): " + value);
        }
      }
      case CHECKBOX -> {
        if (!"true".equals(value) && !"false".equals(value)) {
          throw new BadRequestException("Checkbox value must be 'true' or 'false'");
        }
      }
      case SELECT -> {
        Set<String> allowed = Set.copyOf(decodeOptions(def.getOptions()));
        if (!allowed.contains(value)) {
          throw new BadRequestException("Value '" + value + "' is not in allowed options");
        }
      }
      case MULTISELECT -> {
        Set<String> allowed = Set.copyOf(decodeOptions(def.getOptions()));
        List<String> chosen = decodeOptions(value);
        for (String v : chosen) {
          if (!allowed.contains(v)) {
            throw new BadRequestException("Value '" + v + "' is not in allowed options");
          }
        }
      }
      default -> { /* TEXT, URL, no further validation */ }
    }
  }

  // ── Mapping helpers ─────────────────────────────────────────────────────────

  private CustomFieldDefinitionDTO toDefinitionDTO(CustomFieldDefinition def) {
    return CustomFieldDefinitionDTO.builder()
        .id(def.getId())
        .name(def.getName())
        .description(def.getDescription())
        .fieldType(def.getFieldType())
        .entityType(def.getEntityType())
        .projectId(def.getProject() != null ? def.getProject().getId() : null)
        .projectName(def.getProject() != null ? def.getProject().getName() : null)
        .required(def.getRequired())
        .sortOrder(def.getSortOrder())
        .options(decodeOptions(def.getOptions()))
        .createdAt(def.getCreatedAt())
        .updatedAt(def.getUpdatedAt())
        .build();
  }

  private CustomFieldValueDTO toValueDTO(CustomFieldValue v) {
    return CustomFieldValueDTO.builder()
        .definitionId(v.getDefinition().getId())
        .definitionName(v.getDefinition().getName())
        .fieldType(v.getDefinition().getFieldType())
        .entityType(v.getEntityType())
        .entityId(v.getEntityId())
        .value(v.getValue())
        .updatedByUsername(v.getUpdatedBy() != null ? v.getUpdatedBy().getUsername() : null)
        .updatedAt(v.getUpdatedAt())
        .build();
  }

  private String encodeOptions(List<String> options) {
    if (options == null || options.isEmpty()) return null;
    try {
      return objectMapper.writeValueAsString(options);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to encode options", e);
    }
  }

  private List<String> decodeOptions(String json) {
    if (json == null || json.isBlank()) return List.of();
    try {
      return objectMapper.readValue(json, new TypeReference<List<String>>() {});
    } catch (JsonProcessingException e) {
      log.warn("Failed to decode options JSON: {}", json);
      return List.of();
    }
  }

  private String currentUsername() {
    return SecurityContextHolder.getContext().getAuthentication().getName();
  }
}
