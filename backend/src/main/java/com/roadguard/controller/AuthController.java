package com.roadguard.controller;

import com.roadguard.dto.AuthDto;
import com.roadguard.entity.Role;
import com.roadguard.security.AuthUser;
import com.roadguard.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthDto.AuthResponse register(@Valid @RequestBody AuthDto.RegisterRequest request) {
        return auth.register(request);
    }

    @PostMapping("/login")
    public AuthDto.AuthResponse login(@Valid @RequestBody AuthDto.LoginRequest request) {
        return auth.login(request);
    }

    @GetMapping("/me")
    public AuthDto.UserInfo me(@AuthenticationPrincipal AuthUser me) {
        return new AuthDto.UserInfo(me.name(), me.email(), me.role() == Role.ADMIN ? "admin" : "user");
    }
}
