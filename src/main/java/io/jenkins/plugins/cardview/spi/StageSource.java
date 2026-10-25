package io.jenkins.plugins.cardview.spi;

import hudson.ExtensionPoint;
import hudson.model.Run;
import io.jenkins.plugins.cardview.model.StageSummary;
import java.util.List;
import java.util.Optional;

/**
 * Reads the stages of a run. Implement this to draw stage bars for a new kind of job.
 */
public interface StageSource extends ExtensionPoint {

    /**
     * @return the run's stages in the order they started, or empty when this source does not
     *     handle this kind of run
     */
    Optional<List<StageSummary>> stagesOf(Run<?, ?> run);
}
