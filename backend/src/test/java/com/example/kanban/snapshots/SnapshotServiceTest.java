package com.example.kanban.snapshots;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.time.Clock;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SnapshotServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void runSnapshotSerializesConcurrentWriters() throws Exception {
        SnapshotSettingsRepository settingsRepository = mock(SnapshotSettingsRepository.class);
        when(settingsRepository.find()).thenReturn(new SnapshotSettings(
                true,
                SnapshotSettings.DEFAULT_CRON,
                3,
                tempDir.resolve("serialized").toString()));
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger active = new AtomicInteger();
        AtomicInteger maxActive = new AtomicInteger();
        doAnswer(invocation -> {
            int running = active.incrementAndGet();
            maxActive.updateAndGet(previous -> Math.max(previous, running));
            firstEntered.countDown();
            release.await(2, TimeUnit.SECONDS);
            active.decrementAndGet();
            return null;
        }).when(jdbc).execute(startsWith("vacuum into"));
        SnapshotService snapshotService = new SnapshotService(
                settingsRepository,
                jdbc,
                null,
                Clock.systemUTC());
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(snapshotService::runSnapshot);
            assertThat(firstEntered.await(1, TimeUnit.SECONDS)).isTrue();
            Future<?> second = executor.submit(snapshotService::runSnapshot);
            Thread.sleep(150);
            release.countDown();
            first.get(1, TimeUnit.SECONDS);
            second.get(1, TimeUnit.SECONDS);

            assertThat(maxActive.get()).isEqualTo(1);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void fallsBackToSqliteOnlineBackupWhenVacuumIntoFails() throws Exception {
        try (Connection sourceConnection = DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SingleConnectionDataSource dataSource = new SingleConnectionDataSource(sourceConnection, true);
            JdbcTemplate jdbc = new JdbcTemplate(dataSource);
            jdbc.execute("create table sample (id integer primary key, name text not null)");
            jdbc.update("insert into sample (name) values (?)", "snapshot-row");
            JdbcTemplate failingVacuumJdbc = org.mockito.Mockito.spy(jdbc);
            doThrow(new DataAccessResourceFailureException("vacuum failed"))
                    .when(failingVacuumJdbc)
                    .execute(startsWith("vacuum into"));
            SnapshotSettingsRepository settingsRepository = mock(SnapshotSettingsRepository.class);
            when(settingsRepository.find()).thenReturn(new SnapshotSettings(
                    true,
                    SnapshotSettings.DEFAULT_CRON,
                    3,
                    tempDir.toString()));
            SnapshotService snapshotService = new SnapshotService(
                    settingsRepository,
                    failingVacuumJdbc,
                    dataSource,
                    Clock.systemUTC());

            SnapshotService.SnapshotResult result = snapshotService.runSnapshot();
            Path snapshot = tempDir.resolve(result.getFileName());

            try (java.sql.Connection connection = DriverManager.getConnection("jdbc:sqlite:" + snapshot.toAbsolutePath())) {
                int count = connection.createStatement()
                        .executeQuery("select count(*) from sample where name = 'snapshot-row'")
                        .getInt(1);
                assertThat(count).isEqualTo(1);
            }
        }
    }
}
