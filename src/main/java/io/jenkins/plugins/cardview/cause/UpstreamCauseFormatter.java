package io.jenkins.plugins.cardview.cause;

import hudson.Extension;
import hudson.model.Cause;
import io.jenkins.plugins.cardview.spi.CauseFormatter;
import java.util.Optional;

/** "After D11grp - Branch Build/main #17". */
@Extension
public class UpstreamCauseFormatter implements CauseFormatter {

    @Override
    public Optional<String> format(Cause cause) {
        if (!(cause instanceof Cause.UpstreamCause upstream)) {
            return Optional.empty();
        }
        return Optional.of("After " + upstream.getUpstreamProject() + " #" + upstream.getUpstreamBuild());
    }
}
