package com.example.kanban.snapshots;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.sqlite.SQLiteConnection;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import javax.sql.DataSource;

@Service
public class SnapshotService {
    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final SnapshotSettingsRepository settingsRepository;
    private final JdbcTemplate jdbc;
    private final DataSource dataSource;
    private final Clock clock;

    @Autowired
    public SnapshotService(
            SnapshotSettingsRepository settingsRepository,
            JdbcTemplate jdbc,
            DataSource dataSource) {
        this(settingsRepository, jdbc, dataSource, Clock.systemDefaultZone());
    }

    SnapshotService(
            SnapshotSettingsRepository settingsRepository,
            JdbcTemplate jdbc,
            DataSource dataSource,
            Clock clock) {
        this.settingsRepository = settingsRepository;
        this.jdbc = jdbc;
        this.dataSource = dataSource;
        this.clock = clock;
    }

    public SnapshotSettings getSettings() {
        return settingsRepository.find();
    }

    public SnapshotSettings updateSettings(Boolean enabled, String cron, Integer retentionDays, String outputPath) {
        SnapshotSettings current = settingsRepository.find();
        SnapshotSettings updated = new SnapshotSettings(
                enabled == null ? current.isEnabled() : enabled,
                cron == null ? current.getCron() : validateCron(cron),
                retentionDays == null ? current.getRetentionDays() : validateRetentionDays(retentionDays),
                outputPath == null ? current.getOutputPath() : validateOutputPath(outputPath));
        settingsRepository.save(updated);
        return updated;
    }

    public synchronized SnapshotResult runSnapshot() {
        SnapshotSettings settings = settingsRepository.find();
        Path outputDirectory = Paths.get(settings.getOutputPath());
        try {
            Files.createDirectories(outputDirectory);
            Path snapshotPath = nextSnapshotPath(outputDirectory);
            writeSnapshot(snapshotPath);
            deleteExpiredSnapshots(outputDirectory, settings.getRetentionDays());
            return new SnapshotResult(snapshotPath.getFileName().toString());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not create snapshot", ex);
        }
    }

    private String validateCron(String cron) {
        if (!StringUtils.hasText(cron)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        try {
            new CronTrigger(cron);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return cron;
    }

    private int validateRetentionDays(int retentionDays) {
        if (retentionDays < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return retentionDays;
    }

    private String validateOutputPath(String outputPath) {
        if (!StringUtils.hasText(outputPath)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return outputPath;
    }

    private Path nextSnapshotPath(Path outputDirectory) {
        String baseName = "kanban-snapshot-" + LocalDateTime.now(clock).format(FILE_TIMESTAMP);
        Path snapshotPath = outputDirectory.resolve(baseName + ".sqlite3");
        int suffix = 1;
        while (Files.exists(snapshotPath)) {
            snapshotPath = outputDirectory.resolve(baseName + "-" + suffix + ".sqlite3");
            suffix++;
        }
        return snapshotPath;
    }

    private void writeSnapshot(Path snapshotPath) throws IOException {
        try {
            jdbc.execute("vacuum into '" + sqlLiteral(snapshotPath.toAbsolutePath().toString()) + "'");
        } catch (DataAccessException ex) {
            Files.deleteIfExists(snapshotPath);
            backupDatabase(snapshotPath);
        }
    }

    private String sqlLiteral(String value) {
        return value.replace("'", "''");
    }

    private void backupDatabase(Path snapshotPath) throws IOException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try {
            SQLiteConnection sqliteConnection = connection.unwrap(SQLiteConnection.class);
            sqliteConnection.getDatabase().backup("main", snapshotPath.toAbsolutePath().toString(), null);
        } catch (SQLException ex) {
            throw new IOException("Could not create SQLite backup", ex);
        } finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
        }
    }

    private void deleteExpiredSnapshots(Path outputDirectory, int retentionDays) throws IOException {
        Instant cutoff = Instant.now(clock).minus(retentionDays, ChronoUnit.DAYS);
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(outputDirectory, "kanban-snapshot-*.sqlite3")) {
            for (Path path : paths) {
                if (Files.getLastModifiedTime(path).toInstant().isBefore(cutoff)) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    public static class SnapshotResult {
        private final String fileName;

        public SnapshotResult(String fileName) {
            this.fileName = fileName;
        }

        public String getFileName() {
            return fileName;
        }
    }
}
