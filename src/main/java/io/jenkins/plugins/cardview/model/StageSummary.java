package io.jenkins.plugins.cardview.model;

/**
 * One stage of a run.
 *
 * @param synthetic true for stages Jenkins adds itself, such as "Declarative: Checkout SCM"
 */
public record StageSummary(String name, StageStatus status, long durationMillis, boolean synthetic) {

    public StageSummary withStatus(StageStatus newStatus) {
        return new StageSummary(name, newStatus, durationMillis, synthetic);
    }
}
