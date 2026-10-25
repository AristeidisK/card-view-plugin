package io.jenkins.plugins.cardview.model;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import java.util.List;

/**
 * Everything one card displays.
 *
 * @param lastBuild {@code null} when the job has never run
 */
public record JobCard(
        String title,
        String fullName,
        String url,
        String description,
        CardStatus status,
        List<String> schedule,
        @CheckForNull BuildSummary lastBuild,
        RunStats stats) {

    public JobCard {
        schedule = List.copyOf(schedule);
    }

    public JobCard withTitle(String newTitle) {
        return new JobCard(newTitle, fullName, url, description, status, schedule, lastBuild, stats);
    }
}
