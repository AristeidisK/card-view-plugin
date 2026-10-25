package io.jenkins.plugins.cardview.spi;

import hudson.ExtensionPoint;
import hudson.model.Cause;
import java.util.Optional;

/**
 * Turns what started a run into a short phrase for the card ("Started by Ana", "After Build #17").
 * The first formatter that returns a value wins; otherwise the cause's own short description is used.
 */
public interface CauseFormatter extends ExtensionPoint {

    /** @return a phrase, or empty when this formatter does not handle this cause */
    Optional<String> format(Cause cause);
}
