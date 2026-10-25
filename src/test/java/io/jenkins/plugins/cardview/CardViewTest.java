package io.jenkins.plugins.cardview;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hudson.model.FreeStyleProject;
import java.util.List;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.htmlunit.html.HtmlForm;
import org.htmlunit.html.HtmlPage;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class CardViewTest {

    @Test
    void servesCardsWithStagesAndGroups(JenkinsRule r) throws Exception {
        WorkflowJob deploy = r.createProject(WorkflowJob.class, "D11b - Deploy");
        deploy.setDescription("Deploys d11b.");
        deploy.setDefinition(new CpsFlowDefinition(
                "stage('Build') { echo 'built' }\nstage('Deploy') { echo 'deployed' }", true));
        r.buildAndAssertSuccess(deploy);
        r.createFreeStyleProject("D11grp - Backup");
        r.createFreeStyleProject("Unrelated");

        CardView view = new CardView("D11");
        view.setIncludeRegex("D11.*");
        view.setGroupPattern("^(\\w+) - ");
        r.jenkins.addView(view);

        JSONObject board = JSONObject.fromObject(
                r.createWebClient().goTo("view/D11/cards", "application/json").getWebResponse().getContentAsString());
        JSONArray groups = board.getJSONArray("groups");
        assertEquals(2, groups.size());

        JSONObject d11b = groups.getJSONObject(0);
        assertEquals("D11b", d11b.getString("name"));
        JSONObject card = d11b.getJSONArray("cards").getJSONObject(0);
        assertEquals("Deploy", card.getString("title"));
        assertEquals("READY", card.getString("status"));
        assertEquals("Deploys d11b.", card.getString("description"));
        assertEquals(List.of("On request"), card.getJSONArray("schedule"));
        JSONObject build = card.getJSONObject("lastBuild");
        assertEquals("SUCCESS", build.getString("result"));
        JSONArray stages = build.getJSONArray("stages");
        assertEquals(2, stages.size());
        assertEquals("Build", stages.getJSONObject(0).getString("name"));
        assertEquals("SUCCESS", stages.getJSONObject(1).getString("status"));
        assertEquals(1, card.getJSONObject("stats").getInt("runs"));
        assertEquals(100, card.getJSONObject("stats").getInt("successRate"));

        JSONObject backup = groups.getJSONObject(1).getJSONArray("cards").getJSONObject(0);
        assertEquals("NEW", backup.getString("status"));
        assertTrue(!backup.has("lastBuild"));
    }

    @Test
    void settingsSurviveTheConfigureForm(JenkinsRule r) throws Exception {
        FreeStyleProject project = r.createFreeStyleProject("job");
        CardView view = new CardView("cards");
        r.jenkins.addView(view);
        view.add(project);

        HtmlPage configure = r.createWebClient().goTo("view/cards/configure");
        HtmlForm form = configure.getFormByName("viewConfig");
        form.getInputByName("_.cardsPerRow").setValue("3");
        form.getInputByName("_.groupPattern").setValue("^(\\w+)-");
        form.getInputByName("_.showSyntheticStages").setChecked(true);
        r.submit(form);

        CardView saved = (CardView) r.jenkins.getView("cards");
        assertEquals(3, saved.getCardsPerRow());
        assertEquals("^(\\w+)-", saved.getGroupPattern());
        assertTrue(saved.isShowSyntheticStages());
        assertTrue(saved.contains(project), "list view job selection still applies");
    }

    @Test
    void pageRendersTheBoard(JenkinsRule r) throws Exception {
        r.jenkins.addView(new CardView("cards"));
        HtmlPage page = r.createWebClient().goTo("view/cards/");
        assertTrue(page.getElementById("card-view-board") != null);
    }
}
