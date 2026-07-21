package com.abuscom.infisicalplugin.infisical.login;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.credentialStore.Credentials;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import java.util.Base64;

public class TokenManager {
    private static final TokenManager INSTANCE = new TokenManager();
    private final List<TokenChangeListener> listeners = new ArrayList<>();

    private TokenManager() {
    }

    public static TokenManager getInstance() {
        return INSTANCE;
    }

    public String getTokenFromKeypass() {
        CredentialAttributes attributes = new CredentialAttributes(
                CredentialAttributesKt.generateServiceName("InfisicalPlugin","jwtToken")
        );

        Credentials credentials = PasswordSafe.getInstance().get(attributes);
        if(credentials != null) {
            return credentials.getPasswordAsString();
        }
        return null;
    }

    public void setTokenInKeypass(String token) {
        CredentialAttributes attributes = new CredentialAttributes(
                CredentialAttributesKt.generateServiceName("InfisicalPlugin","jwtToken")
        );

        PasswordSafe.getInstance().set(attributes, new Credentials("infisical", token));
        notifyListeners(token);
    }

    public void clearKeypass() {
        CredentialAttributes attributes = new CredentialAttributes(
                CredentialAttributesKt.generateServiceName("InfisicalPlugin","jwtToken")
        );

        PasswordSafe.getInstance().set(attributes, null);
        notifyListeners(null);
    }

    public void addTokenChangeListener(TokenChangeListener listener) {
        listeners.add(listener);
    }

    public void removeTokenChangeListener(TokenChangeListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(String token) {
        for (TokenChangeListener listener : listeners) {
            listener.onTokenChanged(token);
        }
    }

    public boolean isTokenValid()
    {
        if(TokenManager.getInstance().getTokenFromKeypass() == null)
        {
            return false;
        }
        String[] tokenParts = TokenManager.getInstance().getTokenFromKeypass().split("\\.");
        byte[] decoded= Base64.getUrlDecoder().decode(tokenParts[1]);
        String jsonRepresentation = new String(decoded, StandardCharsets.UTF_8);
        JsonObject payload = JsonParser.parseString(jsonRepresentation).getAsJsonObject();
        long exp = payload.get("exp").getAsLong();
        System.out.println("zeit1"+ exp + "unix zeit" + Instant.now().getEpochSecond());
        return exp >= Instant.now().getEpochSecond();
    }
}