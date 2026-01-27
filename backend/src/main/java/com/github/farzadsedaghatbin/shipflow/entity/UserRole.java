package com.github.farzadsedaghatbin.shipflow.entity;

public enum UserRole {
    ADMIN,      // Full system access - can manage users, settings, permissions
    MANAGER,    // Can manage cycles, pitches, teams, approve bets
    MEMBER,     // Can create/update own work, contribute to pitches/tasks
    READONLY    // Read-only access across all resources
}
