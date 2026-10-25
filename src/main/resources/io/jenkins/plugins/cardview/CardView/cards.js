/*
 * Card View: polls the view's "cards" endpoint and draws one card per job.
 * Text from Jenkins is only ever set through textContent, never as HTML.
 */
(function () {
  "use strict";

  var RESULT_TITLES = {
    SUCCESS: "Succeeded",
    UNSTABLE: "Unstable",
    FAILURE: "Failed",
    ABORTED: "Aborted",
    NOT_BUILT: "Not built",
  };

  var STATUS_LABELS = {
    RUNNING: "Running",
    QUEUED: "Queued",
    READY: "Ready",
    UNSTABLE: "Unstable",
    FAILED: "Failed",
    ABORTED: "Aborted",
    DISABLED: "Disabled",
    NEW: "New",
  };

  function el(tag, className, text) {
    var node = document.createElement(tag);
    if (className) {
      node.className = className;
    }
    if (text !== undefined && text !== null) {
      node.textContent = text;
    }
    return node;
  }

  function link(href, className, text) {
    var a = el("a", className, text);
    a.href = href;
    return a;
  }

  function duration(ms) {
    var s = Math.max(0, Math.round(ms / 1000));
    if (s < 60) {
      return s + " s";
    }
    var m = Math.floor(s / 60);
    if (m < 60) {
      return m + " m " + (s % 60) + " s";
    }
    var h = Math.floor(m / 60);
    return h + " h " + (m % 60) + " m";
  }

  function ago(timestamp, now) {
    var s = Math.max(0, Math.round((now - timestamp) / 1000));
    if (s < 45) {
      return "just now";
    }
    var m = Math.round(s / 60);
    if (m < 60) {
      return m + " min ago";
    }
    var h = Math.round(m / 60);
    if (h < 36) {
      return h + " h ago";
    }
    return Math.round(h / 24) + " d ago";
  }

  function plural(n, word) {
    return n + " " + word + (n === 1 ? "" : "s");
  }

  function lower(value) {
    return String(value).toLowerCase();
  }

  /* ---- one card ---- */

  function stageBar(build) {
    var bar = el("div", "cv-stages");
    var stages = build.stages || [];
    if (stages.length === 0) {
      var seg = el("span", "cv-seg cv-seg-" + (build.building ? "running" : resultClass(build.result)));
      seg.title = build.building ? "Running" : RESULT_TITLES[build.result] || build.result;
      bar.appendChild(seg);
      return bar;
    }
    stages.forEach(function (stage) {
      var seg = el("span", "cv-seg cv-seg-" + lower(stage.status));
      seg.title =
        stage.name + " · " + lower(stage.status) + (stage.durationMillis > 0 ? " · " + duration(stage.durationMillis) : "");
      bar.appendChild(seg);
    });
    return bar;
  }

  function resultClass(result) {
    return { SUCCESS: "success", UNSTABLE: "unstable", FAILURE: "failed", ABORTED: "aborted" }[result] || "pending";
  }

  function runPanel(build, rootUrl, now) {
    var panel = el("div", "cv-run");
    var line = el("div", "cv-run-line");
    line.appendChild(el("span", "cv-dot"));
    var title = build.building ? "Running" : RESULT_TITLES[build.result] || build.result || "Finished";
    var current = (build.stages || []).filter(function (s) {
      return s.status === "RUNNING";
    })[0];
    if (current) {
      title += " · " + current.name;
    }
    line.appendChild(link(rootUrl + build.url, "cv-run-title", title));
    line.appendChild(el("span", "cv-run-number", "#" + build.number));
    panel.appendChild(line);

    var meta = [];
    if (build.building) {
      var elapsed = now - build.timestamp;
      meta.push(
        build.estimatedDurationMillis > 0
          ? duration(elapsed) + " of ~" + duration(build.estimatedDurationMillis)
          : duration(elapsed) + " so far"
      );
    } else {
      meta.push(ago(build.timestamp + build.durationMillis, now));
      meta.push(duration(build.durationMillis));
    }
    if (build.cause) {
      meta.push(build.cause);
    }
    panel.appendChild(el("div", "cv-run-meta", meta.join(" · ")));
    panel.appendChild(stageBar(build));
    return panel;
  }

  function facts(card) {
    var dl = el("dl", "cv-facts");
    dl.appendChild(el("dt", null, "Schedule"));
    dl.appendChild(el("dd", null, card.schedule.join(" · ")));
    if (card.lastBuild && card.lastBuild.estimatedDurationMillis > 0) {
      dl.appendChild(el("dt", null, "Typical run"));
      dl.appendChild(el("dd", null, duration(card.lastBuild.estimatedDurationMillis)));
    }
    return dl;
  }

  function footer(card, windowDays, now) {
    var foot = el("footer", "cv-card-foot");
    var stats = card.stats;
    var left = plural(stats.runs, "run") + " in " + windowDays + " days";
    if (stats.successRate >= 0) {
      left += " · " + stats.successRate + "% succeeded";
    }
    foot.appendChild(el("span", null, left));
    foot.appendChild(
      el("span", "cv-foot-right", stats.lastSuccessTimestamp > 0 ? "Last success " + ago(stats.lastSuccessTimestamp, now) : "Never succeeded")
    );
    return foot;
  }

  function cardNode(card, rootUrl, windowDays, now) {
    var article = el("article", "cv-card cv-status-" + lower(card.status));
    var head = el("header", "cv-card-head");
    var title = link(rootUrl + card.url, "cv-title", card.title);
    title.title = card.fullName;
    head.appendChild(title);
    head.appendChild(el("span", "cv-badge", STATUS_LABELS[card.status] || card.status));
    article.appendChild(head);
    if (card.description) {
      article.appendChild(el("p", "cv-desc", card.description));
    }
    if (card.lastBuild) {
      article.appendChild(runPanel(card.lastBuild, rootUrl, now));
    } else {
      article.appendChild(el("div", "cv-run cv-run-empty", "No runs yet"));
    }
    article.appendChild(facts(card));
    article.appendChild(footer(card, windowDays, now));
    return article;
  }

  /* ---- one card per group ---- */

  var SEVERITY = ["RUNNING", "QUEUED", "FAILED", "UNSTABLE", "ABORTED", "READY"];

  /** The pipelines of a group that ran most recently, one each, oldest of them first. */
  function latestPipelines(cards, limit) {
    return cards
      .filter(function (card) {
        return card.lastBuild;
      })
      .sort(function (a, b) {
        return b.lastBuild.timestamp - a.lastBuild.timestamp;
      })
      .slice(0, limit)
      .reverse();
  }

  /** The most pressing status among the pipelines shown. */
  function groupStatus(lanes) {
    for (var i = 0; i < SEVERITY.length; i++) {
      for (var j = 0; j < lanes.length; j++) {
        if (lanes[j].status === SEVERITY[i]) {
          return SEVERITY[i];
        }
      }
    }
    return "NEW";
  }

  function groupStats(cards) {
    var stats = { runs: 0, succeeded: 0, lastSuccessTimestamp: 0 };
    cards.forEach(function (card) {
      stats.runs += card.stats.runs;
      stats.succeeded += card.stats.succeeded;
      stats.lastSuccessTimestamp = Math.max(stats.lastSuccessTimestamp, card.stats.lastSuccessTimestamp);
    });
    stats.successRate = stats.runs ? Math.round((stats.succeeded * 100) / stats.runs) : -1;
    return stats;
  }

  function laneNode(card, rootUrl, now) {
    var build = card.lastBuild;
    var lane = el("div", "cv-lane cv-status-" + lower(card.status));
    var title = link(rootUrl + card.url, "cv-lane-title", card.title);
    title.title = card.fullName + (card.description ? "\n" + card.description : "");
    lane.appendChild(title);

    var result = el("div", "cv-lane-result");
    result.appendChild(el("span", "cv-dot"));
    var word = build.building ? "Running" : RESULT_TITLES[build.result] || build.result || "Finished";
    result.appendChild(link(rootUrl + build.url, "cv-lane-word", word));
    lane.appendChild(result);

    var current = (build.stages || []).filter(function (s) {
      return s.status === "RUNNING";
    })[0];
    if (build.building) {
      lane.appendChild(el("div", "cv-lane-meta", "#" + build.number + " · " + (current ? current.name : "in progress")));
      lane.appendChild(
        el(
          "div",
          "cv-lane-meta",
          duration(now - build.timestamp) +
            (build.estimatedDurationMillis > 0 ? " of ~" + duration(build.estimatedDurationMillis) : "")
        )
      );
    } else {
      lane.appendChild(el("div", "cv-lane-meta", "#" + build.number + " · " + ago(build.timestamp + build.durationMillis, now)));
      lane.appendChild(el("div", "cv-lane-meta", duration(build.durationMillis)));
    }
    if (build.cause) {
      var cause = el("div", "cv-lane-cause", build.cause);
      cause.title = build.cause;
      lane.appendChild(cause);
    }
    lane.appendChild(stageBar(build));
    return lane;
  }

  function groupCardNode(name, cards, rootUrl, windowDays, limit, now) {
    var lanes = latestPipelines(cards, limit);
    var article = el("article", "cv-card cv-group-card cv-status-" + lower(groupStatus(lanes)));

    var head = el("header", "cv-card-head");
    head.appendChild(el("h2", "cv-title cv-group-card-title", name));
    head.appendChild(el("span", "cv-badge", STATUS_LABELS[groupStatus(lanes)] || "New"));
    article.appendChild(head);
    article.appendChild(el("p", "cv-desc cv-group-card-sub", tallyText(cards)));

    var grid = el("div", "cv-lanes");
    grid.style.setProperty("--cv-lanes", String(limit));
    if (lanes.length === 0) {
      grid.appendChild(el("div", "cv-run cv-run-empty", "Nothing has run yet"));
    }
    lanes.forEach(function (card) {
      grid.appendChild(laneNode(card, rootUrl, now));
    });
    article.appendChild(grid);
    article.appendChild(footer({ stats: groupStats(cards) }, windowDays, now));
    return article;
  }

  /* ---- the board ---- */

  function tally(cards) {
    var counts = { running: 0, failing: 0 };
    cards.forEach(function (card) {
      if (card.status === "RUNNING" || card.status === "QUEUED") {
        counts.running++;
      }
      if (card.status === "FAILED" || card.status === "UNSTABLE") {
        counts.failing++;
      }
    });
    return counts;
  }

  function tallyText(cards) {
    var counts = tally(cards);
    var parts = [plural(cards.length, "job")];
    if (counts.running) {
      parts.push(counts.running + " running");
    }
    parts.push(counts.failing ? counts.failing + " failing" : "all passing");
    return parts.join(" · ");
  }

  function render(root, board) {
    var rootUrl = root.dataset.rootUrl;
    var now = board.generatedAt;
    var all = [];
    board.groups.forEach(function (group) {
      all = all.concat(group.cards);
    });

    var fragment = document.createDocumentFragment();
    var summary = el("div", "cv-summary");
    summary.appendChild(el("span", null, tallyText(all)));
    summary.appendChild(el("span", "cv-updated", "Updated " + new Date(now).toLocaleTimeString()));
    fragment.appendChild(summary);

    if (all.length === 0) {
      fragment.appendChild(el("p", "cv-message", "No jobs in this view yet. Add some under Edit View."));
    }
    if (root.dataset.layout === "GROUPS") {
      var limit = Math.max(1, parseInt(root.dataset.pipelinesPerGroup, 10) || 5);
      var grid = el("div", "cv-grid cv-group-grid");
      board.groups.forEach(function (group) {
        if (group.cards.length) {
          grid.appendChild(
            groupCardNode(group.name || root.dataset.viewName, group.cards, rootUrl, board.statsWindowDays, limit, now)
          );
        }
      });
      fragment.appendChild(grid);
      root.replaceChildren(fragment);
      return;
    }
    board.groups.forEach(function (group) {
      var section = el("section", "cv-group");
      if (group.name) {
        var heading = el("h2", "cv-group-title", group.name);
        heading.appendChild(el("span", "cv-group-meta", tallyText(group.cards)));
        section.appendChild(heading);
      }
      var grid = el("div", "cv-grid");
      group.cards.forEach(function (card) {
        grid.appendChild(cardNode(card, rootUrl, board.statsWindowDays, now));
      });
      section.appendChild(grid);
      fragment.appendChild(section);
    });
    root.replaceChildren(fragment);
  }

  function showError(root, error) {
    var banner = root.querySelector(".cv-error");
    if (!banner) {
      banner = el("p", "cv-error");
      root.prepend(banner);
    }
    banner.textContent = "Could not refresh the cards (" + error.message + "). Retrying.";
  }

  function start(root) {
    var url = root.dataset.cardsUrl;
    var interval = Math.max(3, parseInt(root.dataset.refreshSeconds, 10) || 10) * 1000;
    var timer = null;

    function schedule() {
      clearTimeout(timer);
      timer = setTimeout(load, interval);
    }

    function load() {
      if (document.hidden) {
        schedule();
        return;
      }
      fetch(url, { credentials: "same-origin", headers: { Accept: "application/json" } })
        .then(function (response) {
          if (!response.ok) {
            throw new Error("HTTP " + response.status);
          }
          return response.json();
        })
        .then(function (board) {
          render(root, board);
        })
        .catch(function (error) {
          showError(root, error);
        })
        .finally(schedule);
    }

    document.addEventListener("visibilitychange", function () {
      if (!document.hidden) {
        load();
      }
    });
    load();
  }

  function init() {
    var root = document.getElementById("card-view-board");
    if (root) {
      start(root);
    }
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
