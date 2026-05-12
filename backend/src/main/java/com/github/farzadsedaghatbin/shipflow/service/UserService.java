package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.*;
import com.github.farzadsedaghatbin.shipflow.entity.PasswordResetToken;
import com.github.farzadsedaghatbin.shipflow.entity.Person;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.entity.NotificationUserMapping;
import com.github.farzadsedaghatbin.shipflow.repository.NotificationUserMappingRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PasswordResetTokenRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PersonRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

  private final UserRepository userRepository;
  private final PersonRepository personRepository;
  private final NotificationUserMappingRepository notificationUserMappingRepository;
  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final MessageService messageService;

  @Transactional(readOnly = true)
  public List<UserDTO> findAll() {
    return userRepository.findByDeletedAtIsNull().stream().map(this::toDTO).collect(Collectors.toList());
  }

  @Cacheable(value = "users", key = "'detail:' + #id")
  @Transactional(readOnly = true)
  public UserDTO findById(Long id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    if (user.getDeletedAt() != null) {
      throw new ResourceNotFoundException("User not found with id: " + id);
    }
    return toDTO(user);
  }

  @Transactional(readOnly = true)
  public UserDTO findByUsername(String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    return toDTO(user);
  }

  @Transactional(readOnly = true)
  public User findUserByUsername(String username) {
    return userRepository.findByUsernameWithPerson(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
  }

  @Transactional
  public UserDTO createUser(RegisterRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new IllegalArgumentException(
          messageService.getMessage("error.user.username.exists", request.getUsername()));
    }

    User user = User.builder().username(request.getUsername())
        .password(passwordEncoder.encode(request.getPassword())).role(request.getRole()).isActive(true).build();

    if (request.getPersonId() != null) {
      Person person = personRepository.findById(request.getPersonId()).orElseThrow(
          () -> new ResourceNotFoundException("Person not found with id: " + request.getPersonId()));
      user.setPerson(person);
    }

    user = userRepository.save(user);
    return toDTO(user);
  }

  @CacheEvict(value = "users", allEntries = true)
  @Transactional
  public UserDTO updateRole(Long id, UserRole role) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    user.setRole(role);
    user = userRepository.save(user);
    return toDTO(user);
  }

  @CacheEvict(value = "users", allEntries = true)
  @Transactional
  public UserDTO linkToPerson(Long userId, Long personId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    Person person = personRepository.findById(personId)
        .orElseThrow(() -> new ResourceNotFoundException("Person not found with id: " + personId));
    user.setPerson(person);
    user = userRepository.save(user);
    return toDTO(user);
  }

  @CacheEvict(value = "users", allEntries = true)
  @Transactional
  public void deleteUser(Long id) {
    String currentUsername = SecurityContextHolder.getContext().getAuthentication() != null
        ? SecurityContextHolder.getContext().getAuthentication().getName()
        : null;

    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

    if (user.getDeletedAt() != null) {
      throw new ResourceNotFoundException("User not found with id: " + id);
    }

    if (Objects.equals(user.getUsername(), currentUsername)) {
      throw new IllegalArgumentException("Cannot delete your own account");
    }

    user.setDeletedAt(LocalDateTime.now());
    user.setIsActive(false);
    userRepository.save(user);
    log.info("Admin '{}' soft-deleted user: {}", currentUsername, user.getUsername());
  }

  @CacheEvict(value = "users", allEntries = true)
  @Transactional
  public UserDTO deactivate(Long id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    if (user.getDeletedAt() != null) {
      throw new ResourceNotFoundException("User not found with id: " + id);
    }
    user.setIsActive(false);
    user = userRepository.save(user);
    return toDTO(user);
  }

  @CacheEvict(value = "users", allEntries = true)
  @Transactional
  public UserDTO activate(Long id) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    if (user.getDeletedAt() != null) {
      throw new ResourceNotFoundException("User not found with id: " + id);
    }
    user.setIsActive(true);
    user = userRepository.save(user);
    return toDTO(user);
  }

  @Transactional
  public void changePassword(Long id, String newPassword) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
  }

  @Transactional
  public void changePassword(Long id, ChangePasswordRequest request) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
      throw new IllegalArgumentException(messageService.getMessage("error.user.password.incorrect"));
    }

    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);
  }

  @Transactional
  public void adminResetPassword(Long id, String newPassword) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

    String adminUsername = SecurityContextHolder.getContext().getAuthentication() != null
        ? SecurityContextHolder.getContext().getAuthentication().getName()
        : "unknown";

    user.setPassword(passwordEncoder.encode(newPassword));
    userRepository.save(user);
    log.info("Admin '{}' reset password for user: {}", adminUsername, user.getUsername());
  }

  @Transactional
  public String createPasswordResetToken(String email) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));

    // Invalidate any existing tokens for this user
    passwordResetTokenRepository.invalidateAllTokensForUser(user);

    // Create new token
    String token = UUID.randomUUID().toString();
    PasswordResetToken resetToken = PasswordResetToken.builder().token(token).user(user)
        .expiryDate(LocalDateTime.now().plusHours(24)) // Token valid for 24 hours
        .used(false).build();

    passwordResetTokenRepository.save(resetToken);

    log.info("Password reset token created for user: {}", user.getUsername());
    return token;
  }

  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsedFalse(request.getToken())
        .orElseThrow(() -> new IllegalArgumentException("Invalid or expired password reset token"));

    if (!resetToken.isValid()) {
      throw new IllegalArgumentException(messageService.getMessage("error.user.reset.token.expired"));
    }

    User user = resetToken.getUser();
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);

    // Mark token as used
    resetToken.setUsed(true);
    passwordResetTokenRepository.save(resetToken);

    log.info("Password reset successful for user: {}", user.getUsername());
  }

  @Transactional(readOnly = true)
  public boolean validateResetToken(String token) {
    return passwordResetTokenRepository.findByTokenAndUsedFalse(token).map(PasswordResetToken::isValid)
        .orElse(false);
  }

  @Cacheable(value = "users", key = "'profile:' + #userId")
  @Transactional(readOnly = true)
  public UserProfileDTO getProfile(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
    return toProfileDTO(user);
  }

  @Cacheable(value = "users", key = "'profileByUsername:' + #username")
  @Transactional(readOnly = true)
  public UserProfileDTO getProfileByUsername(String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    return toProfileDTO(user);
  }

  @CacheEvict(value = "users", allEntries = true)
  @Transactional
  public UserProfileDTO updateProfile(Long userId, UpdateProfileRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

    // Update email if provided
    if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
      if (userRepository.existsByEmail(request.getEmail())) {
        throw new IllegalArgumentException(messageService.getMessage("error.user.email.in.use"));
      }
      user.setEmail(request.getEmail());
    }

    // Update person details if user has a linked person
    if (user.getPerson() != null) {
      Person person = user.getPerson();
      if (request.getAvatarUrl() != null) {
        person.setAvatarUrl(request.getAvatarUrl());
      }
      if (request.getBio() != null) {
        person.setBio(request.getBio());
      }
      if (request.getSkills() != null) {
        person.setSkills(request.getSkills());
      }
      if (request.getDepartment() != null) {
        person.setDepartment(request.getDepartment());
      }
      personRepository.save(person);
    }

    userRepository.save(user);
    return toProfileDTO(user);
  }

  public boolean existsByUsername(String username) {
    return userRepository.existsByUsername(username);
  }

  public boolean existsByEmail(String email) {
    return userRepository.existsByEmail(email);
  }

  private UserProfileDTO toProfileDTO(User user) {
    UserProfileDTO.UserProfileDTOBuilder builder = UserProfileDTO.builder().id(user.getId())
        .username(user.getUsername()).email(user.getEmail()).role(user.getRole()).isActive(user.getIsActive())
        .createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt());

    if (user.getPerson() != null) {
      Person person = user.getPerson();
      builder.personId(person.getId()).personName(person.getName()).avatarUrl(person.getAvatarUrl())
          .department(person.getDepartment()).bio(person.getBio()).skills(person.getSkills());
    }

    return builder.build();
  }

  private UserDTO toDTO(User user) {
    return UserDTO.builder().id(user.getId()).username(user.getUsername()).role(user.getRole())
        .personId(user.getPerson() != null ? user.getPerson().getId() : null)
        .personName(user.getPerson() != null ? user.getPerson().getName() : null).isActive(user.getIsActive())
        .createdAt(user.getCreatedAt()).updatedAt(user.getUpdatedAt()).build();
  }

  // ========== Notification User Mapping ==========

  @Transactional(readOnly = true)
  public List<NotificationUserMappingDTO> getNotificationMappings(String username) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    if (user.getPerson() == null) {
      return List.of();
    }
    return notificationUserMappingRepository.findByPersonId(user.getPerson().getId()).stream()
        .map(this::toMappingDTO).collect(Collectors.toList());
  }

  @Transactional
  public NotificationUserMappingDTO upsertNotificationMapping(String username,
      UpsertNotificationMappingRequest request) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    if (user.getPerson() == null) {
      throw new IllegalArgumentException("Cannot upsert notification mapping: user has no linked person profile");
    }
    Person person = user.getPerson();

    String normalizedProvider = request.getProviderName().trim().toLowerCase();

    String normalizedExternalUserId = request.getExternalUserId().trim();
    if (normalizedExternalUserId.isEmpty()) {
      throw new IllegalArgumentException(
          "Cannot upsert notification mapping: external user id must not be blank");
    }

    NotificationUserMapping mapping = notificationUserMappingRepository
        .findByPersonIdAndProviderName(person.getId(), normalizedProvider)
        .orElse(NotificationUserMapping.builder().person(person).providerName(normalizedProvider).build());

    mapping.setExternalUserId(normalizedExternalUserId);
    mapping = notificationUserMappingRepository.save(mapping);
    log.info("Upserted notification mapping for person {} provider {}", person.getId(),
        request.getProviderName());
    return toMappingDTO(mapping);
  }

  @Transactional
  public void deleteNotificationMapping(String username, String providerName) {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + username));
    if (user.getPerson() == null) {
      return;
    }
    String normalizedProvider = providerName.trim().toLowerCase();
    notificationUserMappingRepository.deleteByPersonIdAndProviderName(user.getPerson().getId(),
        normalizedProvider);
    log.info("Deleted notification mapping for person {} provider {}", user.getPerson().getId(), providerName);
  }

  private NotificationUserMappingDTO toMappingDTO(NotificationUserMapping mapping) {
    return NotificationUserMappingDTO.builder().id(mapping.getId()).personId(mapping.getPerson().getId())
        .providerName(mapping.getProviderName()).externalUserId(mapping.getExternalUserId()).build();
  }
}
