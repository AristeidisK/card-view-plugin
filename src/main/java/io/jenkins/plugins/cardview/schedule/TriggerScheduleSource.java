package io.jenkins.plugins.cardview.schedule;

import hudson.Extension;
import hudson.ExtensionList;
import hudson.model.Job;
import hudson.triggers.Trigger;
import io.jenkins.plugins.cardview.spi.ScheduleSource;
import io.jenkins.plugins.cardview.spi.TriggerDescriber;
import java.util.List;
import java.util.Optional;
import jenkins.model.ParameterizedJobMixIn;

/** Describes the triggers configured on a job, one phrase per trigger. */
@Extension
public class TriggerScheduleSource implements ScheduleSource {

    private final List<? extends TriggerDescriber> describers;

    /** Used by Jenkins: describes triggers with every installed {@link TriggerDescriber}. */
    public TriggerScheduleSource() {
        this(ExtensionList.lookup(TriggerDescriber.class));
    }

    public TriggerScheduleSource(List<? extends TriggerDescriber> describers) {
        this.describers = describers;
    }

    @Override
    public List<String> describe(Job<?, ?> job) {
        if (!(job instanceof ParameterizedJobMixIn.ParameterizedJob<?, ?> parameterized)) {
            return List.of();
        }
        return parameterized.getTriggers().values().stream().map(this::describe).toList();
    }

    private String describe(Trigger<?> trigger) {
        return describers.stream()
                .map(describer -> describer.describe(trigger))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseGet(() -> trigger.getDescriptor().getDisplayName());
    }
}
