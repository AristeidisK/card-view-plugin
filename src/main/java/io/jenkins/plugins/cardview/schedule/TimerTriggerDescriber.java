package io.jenkins.plugins.cardview.schedule;

import hudson.Extension;
import hudson.triggers.TimerTrigger;
import hudson.triggers.Trigger;
import io.jenkins.plugins.cardview.spi.TriggerDescriber;
import java.util.Optional;

/** "Daily, ~02:00". */
@Extension
public class TimerTriggerDescriber implements TriggerDescriber {

    @Override
    public Optional<String> describe(Trigger<?> trigger) {
        return trigger instanceof TimerTrigger ? Optional.of(CronDescriber.describe(trigger.getSpec())) : Optional.empty();
    }
}
