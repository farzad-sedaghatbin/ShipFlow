package com.github.farzadsedaghatbin.shipflow.dto.teams;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestTeamsNotificationRequest {
    private String message;
    private String channel;
}
