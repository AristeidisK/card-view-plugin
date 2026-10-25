package io.jenkins.plugins.cardview;

import io.jenkins.plugins.cardview.model.CardGroup;
import io.jenkins.plugins.cardview.model.JobCard;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Sorts cards into named sections by a pattern on their titles. With {@code ^(\w+) - } the card
 * "D11b - Deploy" goes into section "D11b" and is retitled "Deploy". Cards that do not match go
 * into an unnamed section shown first.
 */
public final class CardGrouper {

    private static final Comparator<String> BY_NAME = String.CASE_INSENSITIVE_ORDER;

    public List<CardGroup> group(List<JobCard> cards, String groupPattern) {
        Pattern pattern = compile(groupPattern);
        Map<String, List<JobCard>> sections = new TreeMap<>(BY_NAME);
        for (JobCard card : cards) {
            Placement placement = place(card, pattern);
            sections.computeIfAbsent(placement.section, key -> new ArrayList<>()).add(placement.card);
        }
        List<CardGroup> groups = new ArrayList<>();
        sections.forEach((name, members) -> {
            members.sort(Comparator.comparing(JobCard::title, BY_NAME));
            groups.add(new CardGroup(name, members));
        });
        return groups;
    }

    /** @return null for an empty or invalid pattern */
    static Pattern compile(String groupPattern) {
        if (groupPattern == null || groupPattern.isBlank()) {
            return null;
        }
        try {
            return Pattern.compile(groupPattern);
        } catch (PatternSyntaxException e) {
            return null;
        }
    }

    private static Placement place(JobCard card, Pattern pattern) {
        if (pattern == null) {
            return new Placement("", card);
        }
        Matcher matcher = pattern.matcher(card.title());
        if (!matcher.find()) {
            return new Placement("", card);
        }
        String section = matcher.groupCount() >= 1 && matcher.group(1) != null ? matcher.group(1) : matcher.group();
        String rest = matcher.start() == 0 ? card.title().substring(matcher.end()).trim() : "";
        return new Placement(section.trim(), rest.isEmpty() ? card : card.withTitle(rest));
    }

    private record Placement(String section, JobCard card) {}
}
