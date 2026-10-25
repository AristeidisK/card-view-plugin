package io.jenkins.plugins.cardview;

/**
 * Per-view choices that change what goes on the cards.
 *
 * @param groupPattern regular expression matched against each job's name; its first capture group
 *     (or the whole match) names the card's section. Empty for one unnamed section.
 */
public record CardOptions(String groupPattern, boolean showSyntheticStages) {}
