package io.jenkins.plugins.cardview.schedule;

import hudson.Extension;
import hudson.model.Job;
import io.jenkins.plugins.cardview.spi.ScheduleSource;
import java.util.List;
import jenkins.branch.MultiBranchProject;

/** Branch jobs of a multibranch project build when their branch changes: "On push". */
@Extension(optional = true)
public class BranchScheduleSource implements ScheduleSource {

    @Override
    public List<String> describe(Job<?, ?> job) {
        return job.getParent() instanceof MultiBranchProject<?, ?> ? List.of("On push") : List.of();
    }
}
