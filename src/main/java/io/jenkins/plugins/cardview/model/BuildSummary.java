package io.jenkins.plugins.cardview.model;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.List;

/**
 * The run a card shows: the one in progress, otherwise the latest finished one.
 *
 * @param result {@code null} while the run is building
 */
public record BuildSummary(
        int number,
        String url,
        @CheckForNull String result,
        boolean building,
        long timestamp,
        long durationMillis,
        long estimatedDurationMillis,
        String cause,
        List<StageSummary> stages) {

    public BuildSummary {
        stages = List.copyOf(stages);
    }
}
