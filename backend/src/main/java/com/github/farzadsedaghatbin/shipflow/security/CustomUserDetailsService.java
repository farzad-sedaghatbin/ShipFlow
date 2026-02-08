package com.github.farzadsedaghatbin.shipflow.security;

import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userRepository.findByUsername(username)
        .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

    if (!user.getIsActive()) {
      throw new UsernameNotFoundException("User account is disabled: " + username);
    }

    return new org.springframework.security.core.userdetails.User(user.getUsername(), user.getPassword(),
        user.getIsActive(), true, // accountNonExpired
        true, // credentialsNonExpired
        true, // accountNonLocked
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
  }
}
