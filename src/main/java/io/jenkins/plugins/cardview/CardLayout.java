package io.jenkins.plugins.cardview;

/** How a Card View lays out its jobs. */
public enum CardLayout {

    /** One card for every job, as large as it needs to be. */
    JOBS("One card per job"),

    /**
     * One card for every group, showing the latest run of the pipelines in that group that ran most
     * recently, one narrow column each.
     */
    GROUPS("One card per group");

    private final String displayName;

    CardLayout(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** @return the layout with this name, or {@link #JOBS} for anything unknown */
    public static CardLayout fromName(String name) {
        for (CardLayout layout : values()) {
            if (layout.name().equals(name)) {
                return layout;
            }
        }
        return JOBS;
    }
}
