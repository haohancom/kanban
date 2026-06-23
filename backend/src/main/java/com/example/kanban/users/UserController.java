package com.example.kanban.users;

import com.example.kanban.auth.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDtos.UserResponse> list(Authentication authentication) {
        requireSuperAdministrator(authentication);
        return userService.listUsers().stream()
                .map(UserDtos.UserResponse::from)
                .collect(Collectors.toList());
    }

    @PostMapping
    public UserDtos.UserResponse create(
            Authentication authentication,
            @Valid @RequestBody UserDtos.CreateUserRequest request) {
        requireSuperAdministrator(authentication);
        return UserDtos.UserResponse.from(userService.createUser(
                request.getUsername(),
                request.getDisplayName(),
                request.getPassword(),
                request.isSuperAdmin()));
    }

    @PatchMapping("/{id}")
    public UserDtos.UserResponse update(
            Authentication authentication,
            @PathVariable long id,
            @Valid @RequestBody UserDtos.UpdateUserRequest request) {
        requireSuperAdministrator(authentication);
        return UserDtos.UserResponse.from(userService.updateUser(
                id,
                request.getDisplayName(),
                request.getSuperAdmin()));
    }

    @PatchMapping("/{id}/password")
    public void resetPassword(
            Authentication authentication,
            @PathVariable long id,
            @Valid @RequestBody UserDtos.ResetPasswordRequest request) {
        requireSuperAdministrator(authentication);
        userService.resetPassword(id, request.getPassword());
    }

    private void requireSuperAdministrator(Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof CurrentUser)
                || !((CurrentUser) authentication.getPrincipal()).isSuperAdmin()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
