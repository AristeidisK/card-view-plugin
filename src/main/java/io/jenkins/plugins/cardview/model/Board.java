package io.jenkins.plugins.cardview.model;

import java.util.List;

/** The whole page of cards, as of {@code generatedAt}. */
public record Board(long generatedAt, List<CardGroup> groups) {

    public Board {
        groups = List.copyOf(groups);
    }
}
