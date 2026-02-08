package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.entity.Permission;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.enums.PermissionType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ResourceType;
import com.github.farzadsedaghatbin.shipflow.repository.PermissionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

  @Mock
  private PermissionRepository permissionRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private SecurityContext securityContext;

  @Mock
  private LocalizationService localizationService;

  @InjectMocks
  private PermissionService permissionService;

  private User testUser;
  private Permission testPermission;

  @BeforeEach
  void setUp() {
    lenient().when(localizationService.getMessage(anyString(), any(Object[].class))).thenAnswer(i -> {
      String key = i.getArgument(0);
      if (key.contains("permission.already.exists"))
        return "Permission already exists";
      if (key.contains("permission.not.found"))
        return "Permission not found";
      return key;
    });
    lenient().when(localizationService.getMessage(anyString())).thenAnswer(i -> {
      String key = i.getArgument(0);
      if (key.contains("permission.already.exists"))
        return "Permission already exists";
      if (key.contains("permission.not.found"))
        return "Permission not found";
      return key;
    });

    testUser = User.builder().id(1L).username("testuser").role(UserRole.MEMBER).isActive(true).build();

    testPermission = Permission.builder().id(1L).role(UserRole.MEMBER).resourceType(ResourceType.BUG)
        .permissionType(PermissionType.CREATE).description("Developer can create bugs").build();
  }

  @Test
  void hasPermission_WithValidUserAndPermission_ShouldReturnTrue() {
    // Given
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(permissionRepository.existsByRoleAndResourceTypeAndPermissionType(UserRole.MEMBER, ResourceType.BUG,
        PermissionType.CREATE)).thenReturn(true);

    // When
    boolean result = permissionService.hasPermission("testuser", ResourceType.BUG, PermissionType.CREATE);

    // Then
    assertThat(result).isTrue();
    verify(userRepository).findByUsername("testuser");
    verify(permissionRepository).existsByRoleAndResourceTypeAndPermissionType(UserRole.MEMBER, ResourceType.BUG,
        PermissionType.CREATE);
  }

  @Test
  void hasPermission_WithInvalidUser_ShouldReturnFalse() {
    // Given
    when(userRepository.findByUsername("unknownuser")).thenReturn(Optional.empty());

    // When
    boolean result = permissionService.hasPermission("unknownuser", ResourceType.BUG, PermissionType.CREATE);

    // Then
    assertThat(result).isFalse();
    verify(userRepository).findByUsername("unknownuser");
    verify(permissionRepository, never()).existsByRoleAndResourceTypeAndPermissionType(any(), any(), any());
  }

  @Test
  void hasPermission_WithInactiveUser_ShouldReturnFalse() {
    // Given
    testUser.setIsActive(false);
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

    // When
    boolean result = permissionService.hasPermission("testuser", ResourceType.BUG, PermissionType.CREATE);

    // Then
    assertThat(result).isFalse();
    verify(userRepository).findByUsername("testuser");
    verify(permissionRepository, never()).existsByRoleAndResourceTypeAndPermissionType(any(), any(), any());
  }

  @Test
  void hasPermission_WithNoPermission_ShouldReturnFalse() {
    // Given
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(permissionRepository.existsByRoleAndResourceTypeAndPermissionType(UserRole.MEMBER, ResourceType.CYCLE,
        PermissionType.DELETE)).thenReturn(false);

    // When
    boolean result = permissionService.hasPermission("testuser", ResourceType.CYCLE, PermissionType.DELETE);

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void hasPermission_WithRole_ShouldReturnTrue() {
    // Given
    when(permissionRepository.existsByRoleAndResourceTypeAndPermissionType(UserRole.ADMIN, ResourceType.USER,
        PermissionType.MANAGE)).thenReturn(true);

    // When
    boolean result = permissionService.hasPermission(UserRole.ADMIN, ResourceType.USER, PermissionType.MANAGE);

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void hasPermission_WithCurrentUser_ShouldReturnTrue() {
    // Given
    Authentication authentication = new UsernamePasswordAuthenticationToken("testuser", "password",
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DEVELOPER")));
    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(permissionRepository.existsByRoleAndResourceTypeAndPermissionType(UserRole.MEMBER, ResourceType.BUG,
        PermissionType.READ)).thenReturn(true);

    // When
    boolean result = permissionService.hasPermission(ResourceType.BUG, PermissionType.READ);

    // Then
    assertThat(result).isTrue();
    SecurityContextHolder.clearContext();
  }

  @Test
  void requirePermission_WithPermission_ShouldNotThrowException() {
    // Given
    Authentication authentication = new UsernamePasswordAuthenticationToken("testuser", "password",
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DEVELOPER")));
    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(permissionRepository.existsByRoleAndResourceTypeAndPermissionType(UserRole.MEMBER, ResourceType.BUG,
        PermissionType.READ)).thenReturn(true);

    // When/Then - should not throw
    permissionService.requirePermission(ResourceType.BUG, PermissionType.READ);

    SecurityContextHolder.clearContext();
  }

  @Test
  void requirePermission_WithoutPermission_ShouldThrowAccessDeniedException() {
    // Given
    Authentication authentication = new UsernamePasswordAuthenticationToken("testuser", "password",
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DEVELOPER")));
    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(permissionRepository.existsByRoleAndResourceTypeAndPermissionType(UserRole.MEMBER, ResourceType.USER,
        PermissionType.DELETE)).thenReturn(false);

    // When/Then
    assertThatThrownBy(() -> permissionService.requirePermission(ResourceType.USER, PermissionType.DELETE))
        .isInstanceOf(AccessDeniedException.class).hasMessageContaining("Access denied");

    SecurityContextHolder.clearContext();
  }

  @Test
  void getPermissionsForRole_ShouldReturnPermissions() {
    // Given
    List<Permission> permissions = List.of(testPermission);
    when(permissionRepository.findByRole(UserRole.MEMBER)).thenReturn(permissions);

    // When
    List<Permission> result = permissionService.getPermissionsForRole(UserRole.MEMBER);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testPermission);
  }

  @Test
  void getPermissionsForResource_ShouldReturnPermissions() {
    // Given
    List<Permission> permissions = List.of(testPermission);
    when(permissionRepository.findByResourceType(ResourceType.BUG)).thenReturn(permissions);

    // When
    List<Permission> result = permissionService.getPermissionsForResource(ResourceType.BUG);

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testPermission);
  }

  @Test
  void getCurrentUserPermissions_WithAuthenticatedUser_ShouldReturnPermissions() {
    // Given
    Authentication authentication = new UsernamePasswordAuthenticationToken("testuser", "password",
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_DEVELOPER")));
    SecurityContextHolder.setContext(securityContext);
    when(securityContext.getAuthentication()).thenReturn(authentication);
    when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
    when(permissionRepository.findByRole(UserRole.MEMBER)).thenReturn(List.of(testPermission));

    // When
    List<Permission> result = permissionService.getCurrentUserPermissions();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testPermission);

    SecurityContextHolder.clearContext();
  }

  @Test
  void createPermission_WhenNew_ShouldCreatePermission() {
    // Given
    when(permissionRepository.findByRoleAndResourceTypeAndPermissionType(UserRole.MEMBER, ResourceType.REPORT,
        PermissionType.CREATE)).thenReturn(Optional.empty());
    when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> {
      Permission p = invocation.getArgument(0);
      p.setId(2L);
      return p;
    });

    // When
    Permission result = permissionService.createPermission(UserRole.MEMBER, ResourceType.REPORT,
        PermissionType.CREATE, "QA can create reports");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getId()).isEqualTo(2L);
    assertThat(result.getRole()).isEqualTo(UserRole.MEMBER);
    assertThat(result.getResourceType()).isEqualTo(ResourceType.REPORT);
    assertThat(result.getPermissionType()).isEqualTo(PermissionType.CREATE);
    verify(permissionRepository).save(any(Permission.class));
  }

  @Test
  void createPermission_WhenExists_ShouldThrowException() {
    // Given
    when(permissionRepository.findByRoleAndResourceTypeAndPermissionType(UserRole.MEMBER, ResourceType.BUG,
        PermissionType.CREATE)).thenReturn(Optional.of(testPermission));

    // When/Then
    assertThatThrownBy(() -> permissionService.createPermission(UserRole.MEMBER, ResourceType.BUG,
        PermissionType.CREATE, "Already exists")).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");

    verify(permissionRepository, never()).save(any());
  }

  @Test
  void deletePermission_ShouldDeletePermission() {
    // Given
    when(permissionRepository.existsById(1L)).thenReturn(true);

    // When
    permissionService.deletePermission(1L);

    // Then
    verify(permissionRepository).existsById(1L);
    verify(permissionRepository).deleteById(1L);
  }

  @Test
  void getAllPermissions_ShouldReturnAllPermissions() {
    // Given
    List<Permission> permissions = List.of(testPermission);
    when(permissionRepository.findAll()).thenReturn(permissions);

    // When
    List<Permission> result = permissionService.getAllPermissions();

    // Then
    assertThat(result).hasSize(1);
    assertThat(result.get(0)).isEqualTo(testPermission);
    verify(permissionRepository).findAll();
  }

  @Test
  void updatePermission_WithValidId_ShouldUpdateDescription() {
    // Given
    when(permissionRepository.findById(1L)).thenReturn(Optional.of(testPermission));
    when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // When
    Permission result = permissionService.updatePermission(1L, "Updated description");

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getDescription()).isEqualTo("Updated description");
    verify(permissionRepository).findById(1L);
    verify(permissionRepository).save(testPermission);
  }

  @Test
  void updatePermission_WithInvalidId_ShouldThrowException() {
    // Given
    when(permissionRepository.findById(99L)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> permissionService.updatePermission(99L, "New description"))
        .isInstanceOf(RuntimeException.class).hasMessageContaining("Permission not found");

    verify(permissionRepository).findById(99L);
    verify(permissionRepository, never()).save(any());
  }

  @Test
  void createPermission_WhenAlreadyExists_ShouldThrowException() {
    // Given
    when(permissionRepository.findByRoleAndResourceTypeAndPermissionType(UserRole.MEMBER, ResourceType.BUG,
        PermissionType.CREATE)).thenReturn(Optional.of(testPermission));

    // When/Then
    assertThatThrownBy(() -> permissionService.createPermission(UserRole.MEMBER, ResourceType.BUG,
        PermissionType.CREATE, "Duplicate")).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");

    verify(permissionRepository, never()).save(any());
  }

  @Test
  void deletePermission_WithInvalidId_ShouldThrowException() {
    // Given
    when(permissionRepository.existsById(99L)).thenReturn(false);

    // When/Then
    assertThatThrownBy(() -> permissionService.deletePermission(99L)).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Permission not found");

    verify(permissionRepository).existsById(99L);
    verify(permissionRepository, never()).deleteById(any());
  }

  @Test
  void createBulkPermissions_ShouldCreateMultiple() {
    // Given
    when(permissionRepository.findByRoleAndResourceTypeAndPermissionType(UserRole.MEMBER, ResourceType.BUG,
        PermissionType.READ)).thenReturn(Optional.empty());
    when(permissionRepository.findByRoleAndResourceTypeAndPermissionType(UserRole.MEMBER, ResourceType.BUG,
        PermissionType.UPDATE)).thenReturn(Optional.empty());
    when(permissionRepository.save(any(Permission.class))).thenAnswer(invocation -> {
      Permission p = invocation.getArgument(0);
      p.setId(1L);
      return p;
    });

    List<PermissionService.PermissionRequest> requests = List.of(
        new PermissionService.PermissionRequest(UserRole.MEMBER, ResourceType.BUG, PermissionType.READ,
            "QA read bugs"),
        new PermissionService.PermissionRequest(UserRole.MEMBER, ResourceType.BUG, PermissionType.UPDATE,
            "QA update bugs"));

    // When
    List<Permission> result = permissionService.createBulkPermissions(requests);

    // Then
    assertThat(result).hasSize(2);
    verify(permissionRepository, times(2)).save(any());
  }

  @Test
  void deleteBulkPermissions_ShouldDeleteMultiple() {
    // Given
    List<Long> ids = List.of(1L, 2L, 3L);
    when(permissionRepository.existsById(any())).thenReturn(true);

    // When
    permissionService.deleteBulkPermissions(ids);

    // Then
    verify(permissionRepository, times(3)).existsById(any());
    verify(permissionRepository, times(3)).deleteById(any());
  }
}
