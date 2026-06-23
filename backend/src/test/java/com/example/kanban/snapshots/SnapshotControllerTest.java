package com.example.kanban.snapshots;

import com.example.kanban.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SnapshotControllerTest extends IntegrationTestSupport {
    @TempDir
    Path tempDir;

    @MockBean
    SnapshotScheduler snapshotScheduler;

    @Test
    void defaultSnapshotSettingsAreDisabledAndRetainThreeDays() throws Exception {
        MockHttpSession admin = loginAsAdmin();

        mvc.perform(get("/api/admin/snapshot-settings").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.cron").value("0 0 0 * * *"))
                .andExpect(jsonPath("$.retentionDays").value(3))
                .andExpect(jsonPath("$.outputPath").value("backup"));
    }

    @Test
    void superAdministratorUpdatesSettingsAndRunsSnapshot() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        String outputPath = tempDir.resolve("custom-backups").toString().replace("\\", "\\\\");

        mvc.perform(patch("/api/admin/snapshot-settings").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"cron\":\"0 30 1 * * *\",\"retentionDays\":5,\"outputPath\":\""
                                + outputPath + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.cron").value("0 30 1 * * *"))
                .andExpect(jsonPath("$.retentionDays").value(5));

        verify(snapshotScheduler).reschedule(argThat(settings ->
                settings.isEnabled()
                        && "0 30 1 * * *".equals(settings.getCron())
                        && settings.getRetentionDays() == 5
                        && outputPath.equals(settings.getOutputPath())));

        mvc.perform(post("/api/admin/snapshots/run").session(admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value(containsString("kanban-snapshot-")));

        try (Stream<Path> paths = Files.list(tempDir.resolve("custom-backups"))) {
            assertThat(paths
                    .filter(path -> path.getFileName().toString().endsWith(".sqlite3"))
                    .count()).isEqualTo(1);
        }
    }

    @Test
    void snapshotRunDeletesExpiredBackupFiles() throws Exception {
        MockHttpSession admin = loginAsAdmin();
        Path backupDir = Files.createDirectories(tempDir.resolve("cleanup-backups"));
        Path oldBackup = backupDir.resolve("kanban-snapshot-old.sqlite3");
        Files.write(oldBackup, Collections.singletonList("old"));
        Files.setLastModifiedTime(oldBackup, FileTime.from(Instant.now().minus(4, ChronoUnit.DAYS)));
        String outputPath = backupDir.toString().replace("\\", "\\\\");

        mvc.perform(patch("/api/admin/snapshot-settings").session(admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\":true,\"cron\":\"0 0 0 * * *\",\"retentionDays\":3,\"outputPath\":\""
                                + outputPath + "\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/admin/snapshots/run").session(admin))
                .andExpect(status().isOk());

        assertThat(Files.exists(oldBackup)).isFalse();
    }

    @Test
    void plainMembersCannotManageSnapshots() throws Exception {
        MockHttpSession member = createPlainMemberSession();

        mvc.perform(get("/api/admin/snapshot-settings").session(member))
                .andExpect(status().isForbidden());
    }
}
