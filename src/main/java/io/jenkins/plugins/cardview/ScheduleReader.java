package io.jenkins.plugins.cardview;

import hudson.model.Job;
import io.jenkins.plugins.cardview.spi.ScheduleSource;
import java.util.List;

/** Collects every {@link ScheduleSource}'s phrases for a job; "On request" when there are none. */
public final class ScheduleReader {

    static final String ON_REQUEST = "On request";

    private final List<? extends ScheduleSource> sources;

    public ScheduleReader(List<? extends ScheduleSource> sources) {
        this.sources = List.copyOf(sources);
    }

    public List<String> read(Job<?, ?> job) {
        List<String> phrases = sources.stream()
                .flatMap(source -> source.describe(job).stream())
                .distinct()
                .toList();
        return phrases.isEmpty() ? List.of(ON_REQUEST) : phrases;
    }
}
