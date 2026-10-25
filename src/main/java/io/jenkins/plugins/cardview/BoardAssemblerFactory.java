package io.jenkins.plugins.cardview;

import hudson.ExtensionList;
import io.jenkins.plugins.cardview.cause.CauseDescriber;
import io.jenkins.plugins.cardview.spi.CauseFormatter;
import io.jenkins.plugins.cardview.spi.ScheduleSource;
import io.jenkins.plugins.cardview.spi.StageSource;
import java.time.Duration;
import java.util.function.LongSupplier;

/**
 * Wires a {@link BoardAssembler} to the extensions installed in Jenkins. The only place that
 * looks extensions up, so the assemblers themselves take everything through their constructors.
 */
public final class BoardAssemblerFactory {

    /** How far back the "runs in 30 days" footer looks. */
    public static final Duration STATS_WINDOW = Duration.ofDays(30);

    private BoardAssemblerFactory() {}

    public static BoardAssembler fromExtensions() {
        LongSupplier clock = System::currentTimeMillis;
        CardAssembler cards = new CardAssembler(
                new StatusResolver(),
                new StageReader(ExtensionList.lookup(StageSource.class)),
                new ScheduleReader(ExtensionList.lookup(ScheduleSource.class)),
                new CauseDescriber(ExtensionList.lookup(CauseFormatter.class)),
                new RunStatsCalculator(STATS_WINDOW),
                clock);
        return new BoardAssembler(cards, new CardGrouper(), clock);
    }
}
