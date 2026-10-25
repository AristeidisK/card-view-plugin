package io.jenkins.plugins.cardview;

import hudson.model.ItemGroup;
import hudson.model.Job;
import hudson.model.Result;
import hudson.model.Run;
import io.jenkins.plugins.cardview.cause.CauseDescriber;
import io.jenkins.plugins.cardview.model.BuildSummary;
import io.jenkins.plugins.cardview.model.JobCard;
import java.util.function.LongSupplier;

/**
 * Builds the card for one job from its status, latest run, schedule and recent history. URLs are
 * relative to the Jenkins root, as {@link Job#getUrl()} returns them.
 */
public final class CardAssembler {

    private final StatusResolver statuses;
    private final StageReader stages;
    private final ScheduleReader schedules;
    private final CauseDescriber causes;
    private final RunStatsCalculator stats;
    private final LongSupplier clock;

    public CardAssembler(
            StatusResolver statuses,
            StageReader stages,
            ScheduleReader schedules,
            CauseDescriber causes,
            RunStatsCalculator stats,
            LongSupplier clock) {
        this.statuses = statuses;
        this.stages = stages;
        this.schedules = schedules;
        this.causes = causes;
        this.stats = stats;
        this.clock = clock;
    }

    /**
     * @param context the item group card titles are relative to, usually the view's owner
     */
    public JobCard assemble(Job<?, ?> job, ItemGroup<?> context, CardOptions options) {
        Run<?, ?> run = job.getLastBuild();
        return new JobCard(
                job.getRelativeDisplayNameFrom(context),
                job.getFullName(),
                job.getUrl(),
                description(job),
                statuses.resolve(job),
                schedules.read(job),
                run == null ? null : summarise(run, options),
                stats.calculate(job, clock.getAsLong()));
    }

    private BuildSummary summarise(Run<?, ?> run, CardOptions options) {
        Result result = run.getResult();
        return new BuildSummary(
                run.getNumber(),
                run.getUrl(),
                run.isBuilding() || result == null ? null : result.toString(),
                run.isBuilding(),
                run.getStartTimeInMillis(),
                run.isBuilding() ? clock.getAsLong() - run.getStartTimeInMillis() : run.getDuration(),
                run.getEstimatedDuration(),
                causes.describe(run),
                stages.read(run, options.showSyntheticStages()));
    }

    private static String description(Job<?, ?> job) {
        String description = job.getDescription();
        return description == null ? "" : description.trim();
    }
}
