package com.github.farzadsedaghatbin.shipflow.dto.wiki;

import com.github.farzadsedaghatbin.shipflow.entity.enums.WikiGranteeType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.WikiPermissionLevel;

public record GrantPermissionRequest(
    WikiGranteeType granteeType,
    String granteeRef,
    WikiPermissionLevel level) {}
