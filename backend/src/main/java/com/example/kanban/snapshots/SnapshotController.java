package com.example.kanban.snapshots;

import com.example.kanban.auth.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin")
public class SnapshotController {
    private final SnapshotService snapshotService;
    private final SnapshotScheduler snapshotScheduler;

    public SnapshotController(SnapshotService snapshotService, SnapshotScheduler snapshotScheduler) {
        this.snapshotService = snapshotService;
        this.snapshotScheduler = snapshotScheduler;
    }

    @GetMapping("/snapshot-settings")
    public SnapshotDtos.SnapshotSettingsResponse getSettings(Authentication authentication) {
        requireSuperAdministrator(authentication);
        return SnapshotDtos.SnapshotSettingsResponse.from(snapshotService.getSettings());
    }

    @PatchMapping("/snapshot-settings")
    public SnapshotDtos.SnapshotSettingsResponse updateSettings(
            Authentication authentication,
            @RequestBody SnapshotDtos.UpdateSnapshotSettingsRequest request) {
        requireSuperAdministrator(authentication);
        SnapshotSettings settings = snapshotService.updateSettings(
                request.getEnabled(),
                request.getCron(),
                request.getRetentionDays(),
                request.getOutputPath());
        snapshotScheduler.reschedule(settings);
        return SnapshotDtos.SnapshotSettingsResponse.from(settings);
    }

    @PostMapping("/snapshots/run")
    public SnapshotDtos.SnapshotRunResponse runSnapshot(Authentication authentication) {
        requireSuperAdministrator(authentication);
        SnapshotService.SnapshotResult result = snapshotService.runSnapshot();
        return new SnapshotDtos.SnapshotRunResponse(result.getFileName());
    }

    private void requireSuperAdministrator(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CurrentUser)
                || !((CurrentUser) authentication.getPrincipal()).isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
