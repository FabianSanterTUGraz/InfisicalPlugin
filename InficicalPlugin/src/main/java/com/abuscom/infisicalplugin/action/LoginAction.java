package com.abuscom.infisicalplugin.action;

import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.toolwindow.login.LoginCallBackServer;
import com.abuscom.infisicalplugin.toolwindow.login.TokenManager;
import com.intellij.ide.BrowserUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class LoginAction extends AnAction {
    public static final int LOGIN_CALLBACK_PORT = 8010;

    public LoginAction() {
        super("Login", "Mit Infisical einloggen", AllIcons.Actions.Forward);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        LoginCallBackServer server = new LoginCallBackServer(TokenManager.getInstance());
        try {
            server.startServer();
            BrowserUtil.browse(buildLoginUrl());
        } catch (IOException ex) {
            System.err.println("Callback server exception: " + ex.getMessage());
        }
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    private static String buildLoginUrl() {
        return InfisicalHttpClient.DEFAULT_BASE_URL + "/login?callback_port=" + LOGIN_CALLBACK_PORT;
    }
}