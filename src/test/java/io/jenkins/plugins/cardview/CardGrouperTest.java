package io.jenkins.plugins.cardview;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.jenkins.plugins.cardview.model.CardGroup;
import io.jenkins.plugins.cardview.model.CardStatus;
import io.jenkins.plugins.cardview.model.JobCard;
import io.jenkins.plugins.cardview.model.RunStats;
import java.util.List;
import org.junit.jupiter.api.Test;

class CardGrouperTest {

    private static JobCard card(String title) {
        return new JobCard(title, title, "job/x/", "", CardStatus.READY, List.of(), null, new RunStats(0, 0, 0));
    }

    private static List<String> titles(CardGroup group) {
        return group.cards().stream().map(JobCard::title).toList();
    }

    @Test
    void groupsByCaptureAndStripsPrefix() {
        List<CardGroup> groups = new CardGrouper().group(
                List.of(card("D11grp - Deploy"), card("D11b - Deploy"), card("D11b - Branch Build » main"), card("Other")),
                "^(\\w+) - ");
        assertEquals(List.of("", "D11b", "D11grp"), groups.stream().map(CardGroup::name).toList());
        assertEquals(List.of("Other"), titles(groups.get(0)));
        assertEquals(List.of("Branch Build » main", "Deploy"), titles(groups.get(1)));
        assertEquals(List.of("Deploy"), titles(groups.get(2)));
    }

    @Test
    void emptyOrInvalidPatternMeansOneSection() {
        List<JobCard> cards = List.of(card("b"), card("a"));
        assertEquals(List.of("a", "b"), titles(new CardGrouper().group(cards, "").get(0)));
        assertEquals(1, new CardGrouper().group(cards, "(unclosed").size());
    }
}
