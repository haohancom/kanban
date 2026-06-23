package com.example.kanban.snapshots;

public final class SnapshotDtos {
    private SnapshotDtos() {
    }

    public static class SnapshotSettingsResponse {
        private final boolean enabled;
        private final String cron;
        private final int retentionDays;
        private final String outputPath;

        public SnapshotSettingsResponse(boolean enabled, String cron, int retentionDays, String outputPath) {
            this.enabled = enabled;
            this.cron = cron;
            this.retentionDays = retentionDays;
            this.outputPath = outputPath;
        }

        public static SnapshotSettingsResponse from(SnapshotSettings settings) {
            return new SnapshotSettingsResponse(
                    settings.isEnabled(),
                    settings.getCron(),
                    settings.getRetentionDays(),
                    settings.getOutputPath());
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

    public static class UpdateSnapshotSettingsRequest {
        private Boolean enabled;
        private String cron;
        private Integer retentionDays;
        private String outputPath;

        public Boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(Boolean enabled) {
            this.enabled = enabled;
        }

        public String getCron() {
            return cron;
        }

        public void setCron(String cron) {
            this.cron = cron;
        }

        public Integer getRetentionDays() {
            return retentionDays;
        }

        public void setRetentionDays(Integer retentionDays) {
            this.retentionDays = retentionDays;
        }

        public String getOutputPath() {
            return outputPath;
        }

        public void setOutputPath(String outputPath) {
            this.outputPath = outputPath;
        }
    }

    public static class SnapshotRunResponse {
        private final String fileName;

        public SnapshotRunResponse(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }
}
