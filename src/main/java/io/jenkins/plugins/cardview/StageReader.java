package io.jenkins.plugins.cardview;

import hudson.model.Run;
import io.jenkins.plugins.cardview.model.StageStatus;
import io.jenkins.plugins.cardview.model.StageSummary;
import io.jenkins.plugins.cardview.spi.StageSource;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Stages to draw for a run: asks each {@link StageSource} in turn, hides synthetic stages unless
 * asked not to, and for a running build appends the stages still to come, taken from the last
 * finished build.
 */
public final class StageReader {

    private final List<? extends StageSource> sources;

    public StageReader(List<? extends StageSource> sources) {
        this.sources = List.copyOf(sources);
    }

    public List<StageSummary> read(Run<?, ?> run, boolean showSynthetic) {
        List<StageSummary> stages = new ArrayList<>(visible(stagesOf(run), showSynthetic));
        if (!run.isBuilding()) {
            return stages;
        }
        Run<?, ?> previous = run.getPreviousCompletedBuild();
        if (previous == null) {
            return stages;
        }
        Set<String> seen = new HashSet<>();
        stages.forEach(stage -> seen.add(stage.name()));
        for (StageSummary stage : visible(stagesOf(previous), showSynthetic)) {
            if (seen.add(stage.name())) {
                stages.add(stage.withStatus(StageStatus.PENDING));
            }
        }
        return stages;
    }

    private List<StageSummary> stagesOf(Run<?, ?> run) {
        return sources.stream()
                .map(source -> source.stagesOf(run))
                .flatMap(Optional::stream)
                .findFirst()
                .orElse(List.of());
    }

    private static List<StageSummary> visible(List<StageSummary> stages, boolean showSynthetic) {
        return showSynthetic ? stages : stages.stream().filter(stage -> !stage.synthetic()).toList();
    }
}
