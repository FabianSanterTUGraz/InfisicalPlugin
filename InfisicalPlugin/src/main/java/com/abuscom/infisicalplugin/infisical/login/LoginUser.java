package com.abuscom.infisicalplugin.infisical.login;

import com.abuscom.infisicalplugin.errorMessages.ErrorNotifier;
import com.abuscom.infisicalplugin.infisical.cache.Cache;
import com.abuscom.infisicalplugin.infisical.login.LoginCallBackServer;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;

import java.io.IOException;

public class LoginUser {
    public static final int LOGIN_CALLBACK_PORT = 8010;

    public void login(Project project) {
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
            ErrorNotifier.notify(project,ex);
        }
    }

    private static String buildLoginUrl() {
        return Cache.resolveBaseUrl() + "/login?callback_port=" + LOGIN_CALLBACK_PORT;
    }
}
