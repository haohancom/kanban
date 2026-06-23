package com.example.kanban.snapshots;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;

@Component
public class SnapshotScheduler {
    private final SnapshotSettingsRepository settingsRepository;
    private final SnapshotService snapshotService;
    private final TaskScheduler taskScheduler;
    private ScheduledFuture<?> scheduledSnapshot;

    public SnapshotScheduler(
            SnapshotSettingsRepository settingsRepository,
            SnapshotService snapshotService,
            TaskScheduler taskScheduler) {
        this.settingsRepository = settingsRepository;
        this.snapshotService = snapshotService;
        this.taskScheduler = taskScheduler;
    }

    @PostConstruct
    public void scheduleInitialSnapshotTask() {
        reschedule(settingsRepository.find());
    }

    public synchronized void reschedule(SnapshotSettings settings) {
        if (scheduledSnapshot != null) {
            scheduledSnapshot.cancel(false);
        }
        scheduledSnapshot = taskScheduler.schedule(this::runIfEnabled, new CronTrigger(settings.getCron()));
    }

    private void runIfEnabled() {
        if (settingsRepository.find().isEnabled()) {
            snapshotService.runSnapshot();
        }
    }
}
