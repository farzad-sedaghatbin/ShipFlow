package com.github.farzadsedaghatbin.shipflow.dto.slack;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestSlackNotificationRequest {
    private String message;
    private String channel;
}
