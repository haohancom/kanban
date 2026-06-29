package com.example.kanban.users;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserService {
    private static final long MAX_AVATAR_BYTES = 2L * 1024L * 1024L;
    private static final Set<String> ALLOWED_AVATAR_TYPES = new HashSet<>(
            Arrays.asList("image/png", "image/jpeg", "image/webp", "image/gif"));

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedUsername;
    private final String seedPassword;
    private final String seedDisplayName;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${kanban.seed-admin.username:admin}") String seedUsername,
            @Value("${kanban.seed-admin.password:admin123}") String seedPassword,
            @Value("${kanban.seed-admin.display-name:超级管理员}") String seedDisplayName) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedUsername = seedUsername;
        this.seedPassword = seedPassword;
        this.seedDisplayName = seedDisplayName;
    }

    @PostConstruct
    public void seedAdminIfUsersTableIsEmpty() {
        if (userRepository.countUsers() == 0) {
            userRepository.create(seedUsername, seedDisplayName, passwordEncoder.encode(seedPassword), true);
        }
    }

    public List<UserRepository.UserRecord> listUsers() {
        return userRepository.listUsers();
    }

    public UserRepository.UserRecord createUser(String username, String displayName, String password, boolean superAdmin) {
        rejectDuplicateUsername(username);
        long id;
        try {
            id = userRepository.create(username, displayName, passwordEncoder.encode(password), superAdmin);
        } catch (DataAccessException ex) {
            rejectDuplicateUsername(username);
            throw ex;
        }
        return findUserOrThrow(id);
    }

    public UserRepository.UserRecord updateUser(long id, String displayName, Boolean superAdmin) {
        UserRepository.UserRecord current = findUserOrThrow(id);
        if (displayName != null && !StringUtils.hasText(displayName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        String updatedDisplayName = displayName == null ? current.getDisplayName() : displayName;
        boolean updatedSuperAdmin = superAdmin == null ? current.isSuperAdmin() : superAdmin;
        if (current.isSuperAdmin() && !updatedSuperAdmin) {
            if (userRepository.demoteSuperAdministratorIfAnotherExists(id) == 0) {
                throw new ResponseStatusException(HttpStatus.CONFLICT);
            }
            userRepository.update(id, updatedDisplayName, false);
            return findUserOrThrow(id);
        }
        userRepository.update(id, updatedDisplayName, updatedSuperAdmin);
        return findUserOrThrow(id);
    }

    public void resetPassword(long id, String password) {
        findUserOrThrow(id);
        userRepository.updatePasswordHash(id, passwordEncoder.encode(password));
    }

    public UserRepository.UserRecord updateAvatar(long id, MultipartFile file) {
        findUserOrThrow(id);
        validateAvatar(file);
        try {
            userRepository.updateAvatar(id, file.getBytes(), file.getContentType());
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Could not read avatar", ex);
        }
        return findUserOrThrow(id);
    }

    public UserRepository.AvatarRecord findAvatar(long id) {
        findUserOrThrow(id);
        return userRepository.findAvatarById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public UserRepository.UserRecord removeAvatar(long id) {
        findUserOrThrow(id);
        userRepository.removeAvatar(id);
        return findUserOrThrow(id);
    }

    private UserRepository.UserRecord findUserOrThrow(long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void rejectDuplicateUsername(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
    }

    private void validateAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        if (!ALLOWED_AVATAR_TYPES.contains(file.getContentType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }
}
