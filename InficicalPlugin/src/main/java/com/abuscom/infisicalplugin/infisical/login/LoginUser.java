package com.abuscom.infisicalplugin.infisical.login;

import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.intellij.ide.BrowserUtil;

import java.io.IOException;

public class LoginUser {
    public static final int LOGIN_CALLBACK_PORT = 8010;

    public void login() {
        LoginCallBackServer server = new LoginCallBackServer(TokenManager.getInstance());
        TokenManager.getInstance().addTokenChangeListener(new TokenChangeListener() {
            @Override
            public void onTokenChanged(String token) {
                TokenManager.getInstance().removeTokenChangeListener(this);
            }
        });
        try {
            server.startServer();
            BrowserUtil.browse(buildLoginUrl());
        } catch (IOException ex) {
            System.err.println("Callback server exception: " + ex.getMessage());
        }
    }

    private static String buildLoginUrl() {
        return InfisicalHttpClient.DEFAULT_BASE_URL + "/login?callback_port=" + LOGIN_CALLBACK_PORT;
    }
}
