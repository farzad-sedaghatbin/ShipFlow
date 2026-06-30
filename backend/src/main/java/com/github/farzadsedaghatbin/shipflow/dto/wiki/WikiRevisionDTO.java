package com.github.farzadsedaghatbin.shipflow.dto.wiki;

import java.time.Instant;

public record WikiRevisionDTO(int revision, Instant timestamp, Long editorId, String title) {}
