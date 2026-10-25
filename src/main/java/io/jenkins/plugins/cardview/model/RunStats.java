package io.jenkins.plugins.cardview.model;

/**
 * Finished runs inside the stats window.
 *
 * @param lastSuccessTimestamp 0 when the job has never succeeded
 */
public record RunStats(int runs, int succeeded, long lastSuccessTimestamp) {

    /** Percentage of runs that succeeded, or -1 when there were none. */
    public int successRate() {
        return runs == 0 ? -1 : Math.round(succeeded * 100f / runs);
    }
}
