package com.github.farzadsedaghatbin.shipflow.service.email;

import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Looks up "who do we email, and what do we call them" for a user id.
 *
 * <p>The address usually lives on the user's linked {@code Person}, a lazy association, and several
 * callers (automation actions, the settings endpoint) run outside a database session — reading
 * {@code user.getPerson()} on an entity they were handed throws {@code LazyInitializationException}.
 * Loading by id inside this transaction avoids that for every caller.
 */
@Service
@RequiredArgsConstructor
public class EmailRecipientService {

  /** An address that can be emailed, and the name to greet the person by. */
  public record Recipient(String address, String name) {
  }

  private final UserRepository userRepository;

  /** The user's emailable address and greeting name; empty when there is no usable address. */
  @Transactional(readOnly = true)
  public Optional<Recipient> forUserId(Long userId) {
    if (userId == null) {
      return Optional.empty();
    }
    return userRepository.findById(userId).flatMap(user -> {
      String address = EmailAddressResolver.of(user);
      if (address == null) {
        return Optional.empty();
      }
      String name = user.getPerson() != null && user.getPerson().getName() != null ? user.getPerson().getName()
          : user.getUsername();
      return Optional.of(new Recipient(address, name));
    });
  }
}
