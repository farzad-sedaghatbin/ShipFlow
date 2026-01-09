package com.github.farzadsedaghatbin.shipflow.entity.enums;

public enum RetroStatus {
    DRAFT,      // Retro created but not started
    OPEN,       // Retro in progress, team members can add items
    CLOSED      // Retro completed, read-only
}
