package com.github.farzadsedaghatbin.shipflow.entity.enums;

public enum PitchStatus {
    PENDING,
    SHAPED,      // Ready for betting - shaped and estimated
    STARTED,
    IN_PROGRESS,
    TESTING,
    DONE,
    COOLDOWN,
    CANCELLED,
    CIRCUIT_BREAKER  // Flagged for exceeding time budget - Shape Up safety valve
}
