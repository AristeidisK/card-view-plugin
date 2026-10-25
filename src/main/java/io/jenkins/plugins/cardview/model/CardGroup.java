package io.jenkins.plugins.cardview.model;

import java.util.List;

/**
 * A titled section of cards.
 *
 * @param name empty for cards that matched no group
 */
public record CardGroup(String name, List<JobCard> cards) {

    public CardGroup {
        cards = List.copyOf(cards);
    }
}
