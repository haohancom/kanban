package com.example.kanban.snapshots;

public class SnapshotSettings {
    public static final String DEFAULT_CRON = "0 0 0 * * *";
    public static final int DEFAULT_RETENTION_DAYS = 3;
    public static final String DEFAULT_OUTPUT_PATH = "backup";

    private final boolean enabled;
    private final String cron;
    private final int retentionDays;
    private final String outputPath;

    public SnapshotSettings(boolean enabled, String cron, int retentionDays, String outputPath) {
        this.enabled = enabled;
        this.cron = cron;
        this.retentionDays = retentionDays;
        this.outputPath = outputPath;
    }

    public static SnapshotSettings defaults() {
        return new SnapshotSettings(false, DEFAULT_CRON, DEFAULT_RETENTION_DAYS, DEFAULT_OUTPUT_PATH);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getCron() {
        return cron;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public String getOutputPath() {
        return outputPath;
    }
}
