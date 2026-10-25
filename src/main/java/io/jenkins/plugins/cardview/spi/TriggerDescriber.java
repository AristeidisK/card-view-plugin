package io.jenkins.plugins.cardview.spi;

import hudson.ExtensionPoint;
import hudson.triggers.Trigger;
import java.util.Optional;

/**
 * Describes one trigger in a few words. The first describer that returns a value wins;
 * otherwise the trigger's display name is used.
 */
public interface TriggerDescriber extends ExtensionPoint {

    /** @return a phrase, or empty when this describer does not handle this trigger */
    Optional<String> describe(Trigger<?> trigger);
}
