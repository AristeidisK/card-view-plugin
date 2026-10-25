package io.jenkins.plugins.cardview.model;

/** What a card's badge says about its job right now. */
public enum CardStatus {
    RUNNING,
    QUEUED,
    READY,
    UNSTABLE,
    FAILED,
    ABORTED,
    DISABLED,
    NEW
}
