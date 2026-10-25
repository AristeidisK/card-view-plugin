package io.jenkins.plugins.cardview.spi;

import hudson.ExtensionPoint;
import hudson.model.Job;
import java.util.List;

/**
 * Describes when a job runs on its own, in a few words each ("Daily, ~02:00", "On push").
 * Every source contributes; a card with no lines at all shows "On request".
 */
public interface ScheduleSource extends ExtensionPoint {

    /** @return short phrases, or an empty list when this source has nothing to say */
    List<String> describe(Job<?, ?> job);
}
