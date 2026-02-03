package com.github.farzadsedaghatbin.shipflow.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for user information when searching for @mentions in comments.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentionUserDTO {

    private Long id;
    private String username;
    private String displayName;
}
