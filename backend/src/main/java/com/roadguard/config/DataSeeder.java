package com.roadguard.config;

import com.roadguard.entity.Role;
import com.roadguard.entity.User;
import com.roadguard.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Creates the administrator (and an optional demo user) on first start. Public sign-up can never create an admin. */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Value("${roadguard.admin.name}") private String adminName;
    @Value("${roadguard.admin.email}") private String adminEmail;
    @Value("${roadguard.admin.password}") private String adminPassword;
    @Value("${roadguard.seed-demo-user}") private boolean seedDemoUser;

    public DataSeeder(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        create(adminName, adminEmail, adminPassword, Role.ADMIN);
        if (seedDemoUser) {
            create("Demo User", "user@demo.com", "demo123", Role.USER);
        }
    }

    private void create(String name, String email, String password, Role role) {
        String e = email.trim().toLowerCase();
        if (users.existsByEmail(e)) return;
        User u = new User();
        u.setName(name);
        u.setEmail(e);
        u.setPasswordHash(encoder.encode(password));
        u.setRole(role);
        users.save(u);
        log.info("Created {} account {}", role, e);
    }
}
