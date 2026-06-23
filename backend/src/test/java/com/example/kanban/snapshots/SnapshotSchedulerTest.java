package com.example.kanban.snapshots;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SnapshotSchedulerTest {
    @Test
    void rescheduleCancelsExistingTaskBeforeSchedulingUpdatedCron() {
        SnapshotSettingsRepository settingsRepository = mock(SnapshotSettingsRepository.class);
        SnapshotService snapshotService = mock(SnapshotService.class);
        RecordingTaskScheduler taskScheduler = new RecordingTaskScheduler();
        SnapshotScheduler scheduler = new SnapshotScheduler(settingsRepository, snapshotService, taskScheduler);

        scheduler.reschedule(new SnapshotSettings(false, "0 0 0 * * *", 3, "backup"));
        scheduler.reschedule(new SnapshotSettings(true, "0 30 1 * * *", 5, "backup"));

        assertThat(taskScheduler.futures).hasSize(2);
        assertThat(taskScheduler.futures.get(0).cancelled).isTrue();
        assertThat(taskScheduler.futures.get(1).cancelled).isFalse();
    }

    private static class RecordingTaskScheduler implements TaskScheduler {
        private final List<RecordingScheduledFuture> futures = new ArrayList<>();

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Trigger trigger) {
            RecordingScheduledFuture future = new RecordingScheduledFuture();
            futures.add(future);
            return future;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Date startTime) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Date startTime, long period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, long period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Date startTime, long delay) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long delay) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
            throw new UnsupportedOperationException();
        }
    }

    private static class RecordingScheduledFuture implements ScheduledFuture<Object> {
        private boolean cancelled;

        @Override
        public long getDelay(TimeUnit unit) {
            return 0;
        }

        @Override
        public int compareTo(java.util.concurrent.Delayed other) {
            return 0;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            cancelled = true;
            return true;
        }

        @Override
        public boolean isCancelled() {
            return cancelled;
        }

        @Override
        public boolean isDone() {
            return cancelled;
        }

        @Override
        public Object get() {
            return null;
        }

        @Override
        public Object get(long timeout, TimeUnit unit) {
            return null;
        }
    }
}
