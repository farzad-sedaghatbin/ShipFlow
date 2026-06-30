package com.github.farzadsedaghatbin.shipflow.dto.wiki;

import java.util.List;

public record WikiTreeNodeDTO(
    Long id, String title, String slug, int position, List<WikiTreeNodeDTO> children) {}
