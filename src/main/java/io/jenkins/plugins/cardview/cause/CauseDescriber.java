package io.jenkins.plugins.cardview.cause;

import hudson.model.Cause;
import hudson.model.Run;
import io.jenkins.plugins.cardview.spi.CauseFormatter;
import java.util.List;
import java.util.Optional;

/** Picks the phrase for what started a run, asking each formatter in turn. */
public final class CauseDescriber {

    private final List<? extends CauseFormatter> formatters;

    public CauseDescriber(List<? extends CauseFormatter> formatters) {
        this.formatters = List.copyOf(formatters);
    }

    /** @return a phrase for the run's first cause, or an empty string when it has none */
    public String describe(Run<?, ?> run) {
        List<Cause> causes = run.getCauses();
        if (causes.isEmpty()) {
            return "";
        }
        Cause cause = causes.get(0);
        return formatters.stream()
                .map(formatter -> formatter.format(cause))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseGet(cause::getShortDescription);
    }
}
