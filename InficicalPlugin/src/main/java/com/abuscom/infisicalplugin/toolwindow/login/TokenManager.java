package com.abuscom.infisicalplugin.toolwindow.login;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.credentialStore.Credentials;
import java.util.ArrayList;
import java.util.List;

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
}
