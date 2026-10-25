package io.jenkins.plugins.cardview;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor.FormException;
import hudson.model.Item;
import hudson.model.Job;
import hudson.model.ListView;
import hudson.model.View;
import hudson.model.ViewGroup;
import hudson.util.FormValidation;
import io.jenkins.plugins.cardview.json.BoardJsonWriter;
import io.jenkins.plugins.cardview.model.Board;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;
import net.sf.json.JSONObject;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest2;
import org.kohsuke.stapler.StaplerResponse2;
import org.kohsuke.stapler.WebMethod;
import org.kohsuke.stapler.verb.GET;

/**
 * A list view that draws its jobs as cards. Job selection (ticked jobs, regex, recurse into folders)
 * is the list view's; this class adds the card settings and the {@code cards} JSON endpoint the page
 * polls.
 */
public class CardView extends ListView {

    static final int DEFAULT_CARDS_PER_ROW = 4;
    static final int MIN_CARDS_PER_ROW = 1;
    static final int MAX_CARDS_PER_ROW = 6;
    static final int DEFAULT_REFRESH_SECONDS = 10;
    static final int MIN_REFRESH_SECONDS = 3;

    private int cardsPerRow = DEFAULT_CARDS_PER_ROW;
    private int refreshSeconds = DEFAULT_REFRESH_SECONDS;
    private String groupPattern = "";
    private boolean showSyntheticStages;

    @DataBoundConstructor
    public CardView(String name) {
        super(name);
    }

    public CardView(String name, ViewGroup owner) {
        super(name, owner);
    }

    public int getCardsPerRow() {
        return cardsPerRow < MIN_CARDS_PER_ROW ? DEFAULT_CARDS_PER_ROW : Math.min(cardsPerRow, MAX_CARDS_PER_ROW);
    }

    @DataBoundSetter
    public void setCardsPerRow(int cardsPerRow) {
        this.cardsPerRow = Math.max(MIN_CARDS_PER_ROW, Math.min(cardsPerRow, MAX_CARDS_PER_ROW));
    }

    public int getRefreshSeconds() {
        return refreshSeconds < MIN_REFRESH_SECONDS ? DEFAULT_REFRESH_SECONDS : refreshSeconds;
    }

    @DataBoundSetter
    public void setRefreshSeconds(int refreshSeconds) {
        this.refreshSeconds = Math.max(MIN_REFRESH_SECONDS, refreshSeconds);
    }

    @NonNull
    public String getGroupPattern() {
        return groupPattern == null ? "" : groupPattern;
    }

    @DataBoundSetter
    public void setGroupPattern(String groupPattern) {
        this.groupPattern = Util.fixNull(groupPattern).trim();
    }

    public boolean isShowSyntheticStages() {
        return showSyntheticStages;
    }

    @DataBoundSetter
    public void setShowSyntheticStages(boolean showSyntheticStages) {
        this.showSyntheticStages = showSyntheticStages;
    }

    @Override
    protected void submit(StaplerRequest2 req) throws IOException, ServletException, FormException {
        super.submit(req);
        JSONObject form = req.getSubmittedForm();
        setCardsPerRow(form.optInt("cardsPerRow", DEFAULT_CARDS_PER_ROW));
        setRefreshSeconds(form.optInt("refreshSeconds", DEFAULT_REFRESH_SECONDS));
        setGroupPattern(form.optString("groupPattern", ""));
        setShowSyntheticStages(form.optBoolean("showSyntheticStages", false));
    }

    /** The cards as JSON, polled by the page every {@link #getRefreshSeconds()} seconds. */
    @GET
    @WebMethod(name = "cards")
    public void doCards(StaplerResponse2 rsp) throws IOException {
        checkPermission(View.READ);
        Board board = BoardAssemblerFactory.fromExtensions().assemble(jobs(), getOwner().getItemGroup(), options());
        rsp.setContentType("application/json;charset=UTF-8");
        rsp.setHeader("Cache-Control", "no-cache, no-store");
        rsp.getWriter().write(new BoardJsonWriter()
                .write(board, (int) BoardAssemblerFactory.STATS_WINDOW.toDays())
                .toString());
    }

    CardOptions options() {
        return new CardOptions(getGroupPattern(), isShowSyntheticStages());
    }

    /** Jobs in this view the current user may read. Folders and other containers get no card. */
    List<Job<?, ?>> jobs() {
        return getItems().stream()
                .<Job<?, ?>>flatMap(item -> item instanceof Job<?, ?> job && job.hasPermission(Item.READ)
                        ? Stream.of(job)
                        : Stream.empty())
                .toList();
    }

    @Extension
    @Symbol("cardView")
    public static final class DescriptorImpl extends ListView.DescriptorImpl {

        @NonNull
        @Override
        public String getDisplayName() {
            return "Card View";
        }

        public FormValidation doCheckGroupPattern(@QueryParameter String value) {
            if (Util.fixEmptyAndTrim(value) == null) {
                return FormValidation.ok();
            }
            try {
                Pattern.compile(value);
                return FormValidation.ok();
            } catch (PatternSyntaxException e) {
                return FormValidation.error(e.getDescription());
            }
        }

        public FormValidation doCheckCardsPerRow(@QueryParameter int value) {
            return value >= MIN_CARDS_PER_ROW && value <= MAX_CARDS_PER_ROW
                    ? FormValidation.ok()
                    : FormValidation.error("Between " + MIN_CARDS_PER_ROW + " and " + MAX_CARDS_PER_ROW);
        }
    }
}
