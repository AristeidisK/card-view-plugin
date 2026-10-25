package io.jenkins.plugins.cardview.cause;

import hudson.Extension;
import hudson.model.Cause;
import io.jenkins.plugins.cardview.spi.CauseFormatter;
import java.util.Optional;

/** "Started by Ana Smith". */
@Extension
public class UserCauseFormatter implements CauseFormatter {

    @Override
    public Optional<String> format(Cause cause) {
        if (!(cause instanceof Cause.UserIdCause user)) {
            return Optional.empty();
        }
        return Optional.of("Started by " + user.getUserName());
    }
}
