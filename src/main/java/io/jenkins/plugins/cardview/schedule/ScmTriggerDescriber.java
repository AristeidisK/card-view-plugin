package io.jenkins.plugins.cardview.schedule;

import hudson.Extension;
import hudson.triggers.SCMTrigger;
import hudson.triggers.Trigger;
import io.jenkins.plugins.cardview.spi.TriggerDescriber;
import java.util.Optional;

/** "Polls SCM, every 15 min". */
@Extension
public class ScmTriggerDescriber implements TriggerDescriber {

    @Override
    public Optional<String> describe(Trigger<?> trigger) {
        if (!(trigger instanceof SCMTrigger)) {
            return Optional.empty();
        }
        String spec = trigger.getSpec();
        return Optional.of(spec == null || spec.isBlank() ? "On push" : "Polls SCM, " + CronDescriber.describe(spec));
    }
}
