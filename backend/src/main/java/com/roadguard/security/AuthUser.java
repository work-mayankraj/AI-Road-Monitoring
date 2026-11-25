package com.roadguard.security;

import com.roadguard.entity.Role;

/** The logged-in person, available in controllers with @AuthenticationPrincipal. */
public record AuthUser(Long id, String email, String name, Role role) { }
