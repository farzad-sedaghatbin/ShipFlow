package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.agent.AgentActionProposalDTO;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import com.github.farzadsedaghatbin.shipflow.service.agent.AgentActionService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Confirm or cancel a change the ShipFlow Agent proposed in a comment reply. Only the person who
 * asked the agent can do either — see {@link AgentActionService}.
 */
@RestController
@RequestMapping("/api/agent/actions")
@RequiredArgsConstructor
public class AgentActionController {

  private final AgentActionService agentActionService;
  private final UserService userService;

  @PostMapping("/{id}/confirm")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Confirm an agent proposal", description = "Applies the proposed change as the signed-in requester")
  public ResponseEntity<AgentActionProposalDTO> confirm(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    return ResponseEntity.ok(agentActionService.confirm(id, auth, userIdOf(userDetails)));
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Cancel an agent proposal", description = "Declines the proposed change")
  public ResponseEntity<AgentActionProposalDTO> cancel(
      @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
    return ResponseEntity.ok(agentActionService.cancel(id, userIdOf(userDetails)));
  }

  private Long userIdOf(UserDetails userDetails) {
    return userService.findByUsername(userDetails.getUsername()).getId();
  }
}
