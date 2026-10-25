package io.jenkins.plugins.cardview;

import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import io.jenkins.plugins.cardview.model.CardStatus;
import jenkins.model.ParameterizedJobMixIn;

/** Decides the badge for a job: what it is doing now, else how its last finished run went. */
public final class StatusResolver {

    public CardStatus resolve(Job<?, ?> job) {
        if (job instanceof ParameterizedJobMixIn.ParameterizedJob<?, ?> parameterized && parameterized.isDisabled()) {
            return CardStatus.DISABLED;
        }
        if (job.isBuilding()) {
            return CardStatus.RUNNING;
        }
        if (job.isInQueue()) {
            return CardStatus.QUEUED;
        }
        Run<?, ?> last = job.getLastCompletedBuild();
        return last == null ? CardStatus.NEW : statusOf(last.getResult());
    }

    private static CardStatus statusOf(Result result) {
        if (Result.SUCCESS.equals(result)) {
            return CardStatus.READY;
        }
        if (Result.UNSTABLE.equals(result)) {
            return CardStatus.UNSTABLE;
        }
        if (Result.ABORTED.equals(result)) {
            return CardStatus.ABORTED;
        }
        return Result.FAILURE.equals(result) ? CardStatus.FAILED : CardStatus.NEW;
    }
}
