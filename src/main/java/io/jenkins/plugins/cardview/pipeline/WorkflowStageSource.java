package io.jenkins.plugins.cardview.pipeline;

import hudson.Extension;
import hudson.model.Result;
import hudson.model.Run;
import io.jenkins.plugins.cardview.model.StageStatus;
import io.jenkins.plugins.cardview.model.StageSummary;
import io.jenkins.plugins.cardview.spi.StageSource;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jenkinsci.plugins.workflow.actions.ErrorAction;
import org.jenkinsci.plugins.workflow.actions.LabelAction;
import org.jenkinsci.plugins.workflow.actions.TagsAction;
import org.jenkinsci.plugins.workflow.actions.ThreadNameAction;
import org.jenkinsci.plugins.workflow.actions.TimingAction;
import org.jenkinsci.plugins.workflow.actions.WarningAction;
import org.jenkinsci.plugins.workflow.flow.FlowExecution;
import org.jenkinsci.plugins.workflow.graph.BlockEndNode;
import org.jenkinsci.plugins.workflow.graph.BlockStartNode;
import org.jenkinsci.plugins.workflow.graph.FlowNode;
import org.jenkinsci.plugins.workflow.graphanalysis.DepthFirstScanner;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.jenkinsci.plugins.workflow.steps.FlowInterruptedException;

/**
 * Top-level stages of a Pipeline run, read from its flow graph. Nested stages count towards
 * their parent; parallel branches are not stages.
 */
@Extension(optional = true)
public class WorkflowStageSource implements StageSource {

    /** Tag Declarative Pipeline puts on stages it adds itself ("Declarative: Checkout SCM"). */
    static final String SYNTHETIC_TAG = "SYNTHETIC_STAGE";
    /** Tag Declarative Pipeline puts on stages skipped by a {@code when} or an earlier failure. */
    static final String STATUS_TAG = "STAGE_STATUS";

    @Override
    public Optional<List<StageSummary>> stagesOf(Run<?, ?> run) {
        if (!(run instanceof WorkflowRun workflowRun)) {
            return Optional.empty();
        }
        FlowExecution execution = workflowRun.getExecution();
        if (execution == null) {
            return Optional.of(List.of());
        }
        return Optional.of(new GraphReading(run).read(execution));
    }

    static boolean isStageStart(FlowNode node) {
        return node instanceof BlockStartNode
                && node.getPersistentAction(LabelAction.class) != null
                && node.getPersistentAction(ThreadNameAction.class) == null;
    }

    /** One pass over a run's flow graph. */
    private static final class GraphReading {

        private final Run<?, ?> run;
        private final long now = System.currentTimeMillis();
        private final Map<String, FlowNode> starts = new LinkedHashMap<>();
        private final Map<String, BlockEndNode<?>> ends = new HashMap<>();
        private final Set<String> warned = new HashSet<>();

        GraphReading(Run<?, ?> run) {
            this.run = run;
        }

        List<StageSummary> read(FlowExecution execution) {
            List<FlowNode> warnings = new ArrayList<>();
            for (FlowNode node : new DepthFirstScanner().allNodes(execution)) {
                if (isStageStart(node) && outermostStage(node) == null) {
                    starts.put(node.getId(), node);
                }
                if (node instanceof BlockEndNode<?> end) {
                    ends.put(end.getStartNode().getId(), end);
                }
                if (node.getPersistentAction(WarningAction.class) != null) {
                    warnings.add(node);
                }
            }
            for (FlowNode node : warnings) {
                FlowNode stage = starts.containsKey(node.getId()) ? node : outermostStage(node);
                if (stage != null) {
                    warned.add(stage.getId());
                }
            }
            return starts.values().stream()
                    .sorted(Comparator.comparingLong((FlowNode node) -> TimingAction.getStartTime(node))
                            .thenComparingInt(node -> Integer.parseInt(node.getId())))
                    .map(this::summarise)
                    .toList();
        }

        private StageSummary summarise(FlowNode start) {
            BlockEndNode<?> end = ends.get(start.getId());
            StageStatus status = statusOf(start, end);
            long begin = TimingAction.getStartTime(start);
            long finish = end != null ? TimingAction.getStartTime(end) : run.isBuilding() ? now : begin;
            long duration = status == StageStatus.SKIPPED ? 0 : Math.max(0, finish - begin);
            LabelAction label = start.getPersistentAction(LabelAction.class);
            String name = label == null ? start.getDisplayName() : label.getDisplayName();
            return new StageSummary(name, status, duration, tag(start, SYNTHETIC_TAG) != null);
        }

        private StageStatus statusOf(FlowNode start, BlockEndNode<?> end) {
            String tagged = tag(start, STATUS_TAG);
            if (tagged != null && tagged.startsWith("SKIPPED")) {
                return StageStatus.SKIPPED;
            }
            if (end == null) {
                return run.isBuilding() ? StageStatus.RUNNING : statusOf(run.getResult());
            }
            ErrorAction error = end.getPersistentAction(ErrorAction.class);
            if (error != null) {
                return error.getError() instanceof FlowInterruptedException ? StageStatus.ABORTED : StageStatus.FAILED;
            }
            return warned.contains(start.getId()) ? StageStatus.UNSTABLE : StageStatus.SUCCESS;
        }

        private static StageStatus statusOf(Result result) {
            if (result == null || result.equals(Result.SUCCESS)) {
                return StageStatus.SUCCESS;
            }
            if (result.equals(Result.UNSTABLE)) {
                return StageStatus.UNSTABLE;
            }
            return result.equals(Result.ABORTED) ? StageStatus.ABORTED : StageStatus.FAILED;
        }

        private static String tag(FlowNode node, String name) {
            TagsAction tags = node.getPersistentAction(TagsAction.class);
            return tags == null ? null : tags.getTagValue(name);
        }

        /** The outermost stage enclosing this node, or null when it sits outside every stage. */
        private static FlowNode outermostStage(FlowNode node) {
            FlowNode outermost = null;
            for (BlockStartNode block : node.iterateEnclosingBlocks()) {
                if (isStageStart(block)) {
                    outermost = block;
                }
            }
            return outermost;
        }
    }
}
