# Card View

A Jenkins view that shows each job as a card, so you can see at a glance what is running, what
failed and when things last succeeded.

Each card shows:

- the job's name, a status badge (Running, Ready, Failed, Unstable, Queued, Disabled, New) and its
  description;
- the run in progress, or else the latest one: result, how long ago, how long it took, what started it,
  and a bar with one segment per Pipeline stage (stages still to come are taken from the previous run);
- when the job runs on its own ("Daily, ~02:00", "Weekly, Monday ~06:00", "On push", "After Build");
- runs and success rate over the last 30 days, and when it last succeeded.

The page refreshes itself and follows Jenkins' light and dark themes.

## Using it

1. **New View** → **Card View**.
2. Pick the jobs as for a list view: tick them, or use a regular expression, and tick
   **Recurse in subfolders** to include multibranch branches and jobs in folders.
3. Under **Cards**, choose how many cards fit on a row (1–6), how often the page refreshes, and
   optionally a **Group by** pattern. `^(\w+) - ` puts *D11b - Deploy* in a section called *D11b*
   with the title *Deploy*.

## Extending it

Three extension points let other plugins add to the cards without changing this one:

| Extension point | What it does |
|---|---|
| `StageSource` | Reads the stages of a kind of run (Pipeline is built in). |
| `ScheduleSource` | Adds a phrase saying when a job runs (triggers and multibranch are built in). |
| `TriggerDescriber` | Describes one kind of trigger (timer, SCM polling and upstream are built in). |
| `CauseFormatter` | Describes what started a run (user, upstream and timer are built in). |

## Building

Requires Java 17 or later.

```bash
./mvnw verify      # build and test
./mvnw hpi:run     # Jenkins with the plugin at http://localhost:8080/jenkins/
```

The plugin file is `target/card-view.hpi`; install it under **Manage Jenkins** → **Plugins** →
**Advanced settings** → **Deploy Plugin**.

## Licence

MIT, see [LICENSE](LICENSE).
