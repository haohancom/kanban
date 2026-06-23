package com.example.kanban.users;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

@Service
public class UserService {
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
}
