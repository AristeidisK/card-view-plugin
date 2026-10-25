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
