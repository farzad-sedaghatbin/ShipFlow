package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.customfield.*;
import com.github.farzadsedaghatbin.shipflow.entity.CustomFieldDefinition;
import com.github.farzadsedaghatbin.shipflow.entity.CustomFieldValue;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldEntityType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CustomFieldType;
import com.github.farzadsedaghatbin.shipflow.exception.BadRequestException;
import com.github.farzadsedaghatbin.shipflow.repository.CustomFieldDefinitionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.CustomFieldValueRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ProjectRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CustomFieldServiceTest {

  @Mock private CustomFieldDefinitionRepository definitionRepository;
  @Mock private CustomFieldValueRepository valueRepository;
  @Mock private ProjectRepository projectRepository;
  @Mock private UserRepository userRepository;
  @Spy private ObjectMapper objectMapper = new ObjectMapper();

  @InjectMocks private CustomFieldService service;

  private User adminUser;
  private User memberUser;

  @BeforeEach
  void setUp() {
    adminUser = User.builder().id(1L).username("admin").role(UserRole.ADMIN).build();
    memberUser = User.builder().id(2L).username("member").role(UserRole.MEMBER).build();
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("admin", "pwd", List.of()));
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  // ── Definition validation ─────────────────────────────────────────────────

  @Nested
  class DefinitionValidation {

    @Test
    void createDefinition_SELECT_withoutOptions_throws() {
      when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

      var req = new CreateCustomFieldDefinitionRequest();
      req.setName("Priority");
      req.setFieldType(CustomFieldType.SELECT);
      req.setEntityType(CustomFieldEntityType.TASK);
      req.setProjectId(null);
      req.setOptions(List.of()); // empty options

      assertThatThrownBy(() -> service.createDefinition(req))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("option");
    }

    @Test
    void createDefinition_orgWide_asMember_throws() {
      SecurityContextHolder.getContext()
          .setAuthentication(
              new UsernamePasswordAuthenticationToken("member", "pwd", List.of()));
      when(userRepository.findByUsername("member")).thenReturn(Optional.of(memberUser));

      var req = new CreateCustomFieldDefinitionRequest();
      req.setName("Sprint Notes");
      req.setFieldType(CustomFieldType.TEXT);
      req.setEntityType(CustomFieldEntityType.TASK);
      req.setProjectId(null); // org-wide

      assertThatThrownBy(() -> service.createDefinition(req))
          .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteDefinition_setsDeletedAt() {
      var def =
          CustomFieldDefinition.builder()
              .id(10L)
              .name("Field")
              .fieldType(CustomFieldType.TEXT)
              .entityType(CustomFieldEntityType.TASK)
              .required(false)
              .sortOrder(0)
              .build();
      when(definitionRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(def));
      when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
      when(definitionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

      service.deleteDefinition(10L);

      verify(valueRepository).deleteByDefinitionId(10L);
      assertThat(def.getDeletedAt()).isNotNull();
      assertThat(def.getDeletedBy()).isEqualTo(adminUser);
    }
  }

  // ── Value validation ────────────────────────────────────────────────────────

  @Nested
  class ValueValidation {

    @Test
    void upsertValue_NUMBER_invalid_throws() {
      var def =
          CustomFieldDefinition.builder()
              .id(1L)
              .name("Score")
              .fieldType(CustomFieldType.NUMBER)
              .entityType(CustomFieldEntityType.TASK)
              .required(false)
              .sortOrder(0)
              .build();
      when(definitionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(def));
      when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

      var req = new UpsertCustomFieldValueRequest(1L, CustomFieldEntityType.TASK, 99L, "not-a-number");
      assertThatThrownBy(() -> service.upsertValue(req))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("number");
    }

    @Test
    void upsertValue_required_blank_throws() {
      var def =
          CustomFieldDefinition.builder()
              .id(2L)
              .name("Required Field")
              .fieldType(CustomFieldType.TEXT)
              .entityType(CustomFieldEntityType.TASK)
              .required(true)
              .sortOrder(0)
              .build();
      when(definitionRepository.findByIdAndDeletedAtIsNull(2L)).thenReturn(Optional.of(def));
      when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

      var req = new UpsertCustomFieldValueRequest(2L, CustomFieldEntityType.TASK, 99L, "  ");
      assertThatThrownBy(() -> service.upsertValue(req))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("required");
    }

    @Test
    void upsertValue_SELECT_unknownOption_throws() throws Exception {
      var def =
          CustomFieldDefinition.builder()
              .id(3L)
              .name("Priority")
              .fieldType(CustomFieldType.SELECT)
              .entityType(CustomFieldEntityType.TASK)
              .options(new ObjectMapper().writeValueAsString(List.of("Low", "Medium", "High")))
              .required(false)
              .sortOrder(0)
              .build();
      when(definitionRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(def));
      when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

      var req = new UpsertCustomFieldValueRequest(3L, CustomFieldEntityType.TASK, 99L, "Critical");
      assertThatThrownBy(() -> service.upsertValue(req))
          .isInstanceOf(BadRequestException.class)
          .hasMessageContaining("allowed options");
    }

    @Test
    void bulkUpsert_callsUpsertForEachEntry() {
      var def =
          CustomFieldDefinition.builder()
              .id(1L)
              .name("Notes")
              .fieldType(CustomFieldType.TEXT)
              .entityType(CustomFieldEntityType.TASK)
              .required(false)
              .sortOrder(0)
              .build();
      when(definitionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(def));
      when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

      var cfv =
          CustomFieldValue.builder()
              .definition(def)
              .entityType(CustomFieldEntityType.TASK)
              .entityId(99L)
              .value("hello")
              .updatedBy(adminUser)
              .updatedAt(OffsetDateTime.now())
              .build();
      when(valueRepository.findByDefinitionIdAndEntityTypeAndEntityId(1L, CustomFieldEntityType.TASK, 99L))
          .thenReturn(Optional.empty());
      when(valueRepository.save(any())).thenReturn(cfv);

      var req = new BulkUpsertCustomFieldValuesRequest(CustomFieldEntityType.TASK, 99L, Map.of(1L, "hello"));
      var results = service.bulkUpsertValues(req);

      assertThat(results).hasSize(1);
      verify(valueRepository, times(1)).save(any());
    }
  }
}
