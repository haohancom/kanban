package com.example.kanban.snapshots;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SnapshotSettingsRepository {
    static final String KEY_ENABLED = "snapshot.enabled";
    static final String KEY_CRON = "snapshot.cron";
    static final String KEY_RETENTION_DAYS = "snapshot.retention_days";
    static final String KEY_OUTPUT_PATH = "snapshot.output_path";

    private final JdbcTemplate jdbc;

    public SnapshotSettingsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public SnapshotSettings find() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "select key, value from system_settings where key in (?, ?, ?, ?)",
                KEY_ENABLED,
                KEY_CRON,
                KEY_RETENTION_DAYS,
                KEY_OUTPUT_PATH);
        Map<String, String> values = new HashMap<>();
        for (Map<String, Object> row : rows) {
            values.put(String.valueOf(row.get("key")), String.valueOf(row.get("value")));
        }
        SnapshotSettings defaults = SnapshotSettings.defaults();
        return new SnapshotSettings(
                parseBoolean(values.get(KEY_ENABLED), defaults.isEnabled()),
                values.containsKey(KEY_CRON) ? values.get(KEY_CRON) : defaults.getCron(),
                parseInteger(values.get(KEY_RETENTION_DAYS), defaults.getRetentionDays()),
                values.containsKey(KEY_OUTPUT_PATH) ? values.get(KEY_OUTPUT_PATH) : defaults.getOutputPath());
    }

    @Transactional
    public void save(SnapshotSettings settings) {
        saveValue(KEY_ENABLED, Boolean.toString(settings.isEnabled()));
        saveValue(KEY_CRON, settings.getCron());
        saveValue(KEY_RETENTION_DAYS, Integer.toString(settings.getRetentionDays()));
        saveValue(KEY_OUTPUT_PATH, settings.getOutputPath());
    }

    private void saveValue(String key, String value) {
        jdbc.update(
                "insert into system_settings (key, value, updated_at) values (?, ?, current_timestamp) "
                        + "on conflict(key) do update set value = excluded.value, updated_at = current_timestamp",
                key,
                value);
    }

    private boolean parseBoolean(String value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private int parseInteger(String value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
}
