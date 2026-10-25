package io.jenkins.plugins.cardview;

import hudson.model.ItemGroup;
import hudson.model.Job;
import io.jenkins.plugins.cardview.model.Board;
import io.jenkins.plugins.cardview.model.JobCard;
import java.util.List;
import java.util.function.LongSupplier;

/** Builds the whole board: one card per job, sorted into sections. */
public final class BoardAssembler {

    private final CardAssembler cards;
    private final CardGrouper grouper;
    private final LongSupplier clock;

    public BoardAssembler(CardAssembler cards, CardGrouper grouper, LongSupplier clock) {
        this.cards = cards;
        this.grouper = grouper;
        this.clock = clock;
    }

    public Board assemble(List<? extends Job<?, ?>> jobs, ItemGroup<?> context, CardOptions options) {
        List<JobCard> built = jobs.stream().map(job -> cards.assemble(job, context, options)).toList();
        return new Board(clock.getAsLong(), grouper.group(built, options.groupPattern()));
    }
}
