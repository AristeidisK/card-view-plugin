package io.jenkins.plugins.cardview.model;

/** Outcome of one stage, as drawn on a card's stage bar. */
public enum StageStatus {
    SUCCESS,
    RUNNING,
    PENDING,
    FAILED,
    UNSTABLE,
    ABORTED,
    SKIPPED
}
