package io.jenkins.plugins.cardview;

import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import io.jenkins.plugins.cardview.model.RunStats;
import java.time.Duration;

/** Counts a job's finished runs inside a window, newest first, stopping at the window's edge. */
public final class RunStatsCalculator {

    private final Duration window;

    public RunStatsCalculator(Duration window) {
        this.window = window;
    }

    public Duration window() {
        return window;
    }

    public RunStats calculate(Job<?, ?> job, long now) {
        long since = now - window.toMillis();
        int runs = 0;
        int succeeded = 0;
        for (Run<?, ?> run : job.getBuilds()) {
            if (run.getTimeInMillis() < since) {
                break;
            }
            if (run.isBuilding()) {
                continue;
            }
            runs++;
            if (Result.SUCCESS.equals(run.getResult())) {
                succeeded++;
            }
        }
        Run<?, ?> lastSuccess = job.getLastSuccessfulBuild();
        return new RunStats(runs, succeeded, lastSuccess == null ? 0 : lastSuccess.getTimeInMillis());
    }
}
