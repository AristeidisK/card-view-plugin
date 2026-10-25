package io.jenkins.plugins.cardview.json;

import io.jenkins.plugins.cardview.model.Board;
import io.jenkins.plugins.cardview.model.BuildSummary;
import io.jenkins.plugins.cardview.model.CardGroup;
import io.jenkins.plugins.cardview.model.JobCard;
import io.jenkins.plugins.cardview.model.RunStats;
import io.jenkins.plugins.cardview.model.StageSummary;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/** Serialises a {@link Board} into the JSON the card page reads. */
public final class BoardJsonWriter {

    public JSONObject write(Board board, int statsWindowDays) {
        JSONArray groups = new JSONArray();
        board.groups().forEach(group -> groups.add(group(group)));
        JSONObject json = new JSONObject();
        json.put("generatedAt", board.generatedAt());
        json.put("statsWindowDays", statsWindowDays);
        json.put("groups", groups);
        return json;
    }

    private static JSONObject group(CardGroup group) {
        JSONArray cards = new JSONArray();
        group.cards().forEach(card -> cards.add(card(card)));
        JSONObject json = new JSONObject();
        json.put("name", group.name());
        json.put("cards", cards);
        return json;
    }

    private static JSONObject card(JobCard card) {
        JSONObject json = new JSONObject();
        json.put("title", card.title());
        json.put("fullName", card.fullName());
        json.put("url", card.url());
        json.put("description", card.description());
        json.put("status", card.status().name());
        json.put("schedule", JSONArray.fromObject(card.schedule()));
        BuildSummary lastBuild = card.lastBuild();
        if (lastBuild != null) {
            json.put("lastBuild", build(lastBuild));
        }
        json.put("stats", stats(card.stats()));
        return json;
    }

    private static JSONObject build(BuildSummary build) {
        JSONArray stages = new JSONArray();
        build.stages().forEach(stage -> stages.add(stage(stage)));
        JSONObject json = new JSONObject();
        json.put("number", build.number());
        json.put("url", build.url());
        json.put("result", build.result() == null ? "" : build.result());
        json.put("building", build.building());
        json.put("timestamp", build.timestamp());
        json.put("durationMillis", build.durationMillis());
        json.put("estimatedDurationMillis", build.estimatedDurationMillis());
        json.put("cause", build.cause());
        json.put("stages", stages);
        return json;
    }

    private static JSONObject stage(StageSummary stage) {
        JSONObject json = new JSONObject();
        json.put("name", stage.name());
        json.put("status", stage.status().name());
        json.put("durationMillis", stage.durationMillis());
        return json;
    }

    private static JSONObject stats(RunStats stats) {
        JSONObject json = new JSONObject();
        json.put("runs", stats.runs());
        json.put("succeeded", stats.succeeded());
        json.put("successRate", stats.successRate());
        json.put("lastSuccessTimestamp", stats.lastSuccessTimestamp());
        return json;
    }
}
