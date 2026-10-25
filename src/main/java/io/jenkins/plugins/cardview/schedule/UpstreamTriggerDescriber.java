package io.jenkins.plugins.cardview.schedule;

import hudson.Extension;
import hudson.triggers.Trigger;
import io.jenkins.plugins.cardview.spi.TriggerDescriber;
import java.util.Optional;
import jenkins.triggers.ReverseBuildTrigger;

/** "After D11grp - Branch Build". */
@Extension
public class UpstreamTriggerDescriber implements TriggerDescriber {

    @Override
    public Optional<String> describe(Trigger<?> trigger) {
        if (!(trigger instanceof ReverseBuildTrigger upstream)) {
            return Optional.empty();
        }
        return Optional.of("After " + upstream.getUpstreamProjects());
    }
}
