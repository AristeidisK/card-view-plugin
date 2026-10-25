package io.jenkins.plugins.cardview.cause;

import hudson.Extension;
import hudson.model.Cause;
import hudson.triggers.TimerTrigger;
import io.jenkins.plugins.cardview.spi.CauseFormatter;
import java.util.Optional;

/** "On schedule". */
@Extension
public class TimerCauseFormatter implements CauseFormatter {

    @Override
    public Optional<String> format(Cause cause) {
        return cause instanceof TimerTrigger.TimerTriggerCause ? Optional.of("On schedule") : Optional.empty();
    }
}
