package com.abuscom.infisicalplugin.infisical.auth;

import java.time.Instant;

public record AccessToken(String value, Instant expiresAt) {

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
