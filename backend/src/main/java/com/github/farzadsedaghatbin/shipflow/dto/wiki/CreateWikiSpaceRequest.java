package com.github.farzadsedaghatbin.shipflow.dto.wiki;

public record CreateWikiSpaceRequest(String name, String spaceKey, String description, Long projectId) {}
