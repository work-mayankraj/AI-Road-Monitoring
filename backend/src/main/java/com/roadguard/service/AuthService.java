package com.roadguard.service;

import com.roadguard.dto.AuthDto;
import com.roadguard.dto.Mappers;
import com.roadguard.entity.Role;
import com.roadguard.entity.User;
import com.roadguard.exception.ApiException;
import com.roadguard.repository.UserRepository;
import com.roadguard.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    /** Public sign-up always creates a normal USER, whatever the request says. */
    @Transactional
    public AuthDto.AuthResponse register(AuthDto.RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists.");
        }
        User u = new User();
        u.setName(r.name().trim());
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRole(Role.USER);
        users.save(u);
        return response(u);
    }

    @Transactional(readOnly = true)
    public AuthDto.AuthResponse login(AuthDto.LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase()).orElse(null);
        if (u == null || !encoder.matches(r.password(), u.getPasswordHash())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
        if (r.role() != null && !r.role().isBlank()) {
            Role wanted = "admin".equalsIgnoreCase(r.role()) ? Role.ADMIN : Role.USER;
            if (u.getRole() != wanted) {
                throw new ApiException(HttpStatus.FORBIDDEN, wanted == Role.ADMIN
                        ? "This account is not an administrator. Use the User tab."
                        : "This is an administrator account. Use the Administrator tab.");
            }
        }
        return response(u);
    }

    private AuthDto.AuthResponse response(User u) {
        return new AuthDto.AuthResponse(jwt.generate(u), Mappers.user(u));
    }
}
