package com.abuscom.infisicalplugin.toolwindow.login;

import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.HyperlinkLabel;

import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.io.IOException;

import java.lang.System;


public final class LoginPanel extends JPanel {

    public static final int LOGIN_CALLBACK_PORT = 8010;

    public LoginPanel() {
        super(new FlowLayout(FlowLayout.LEFT));

        HyperlinkLabel loginLink = new HyperlinkLabel("Mit Infisical einloggen");
        loginLink.addHyperlinkListener(e -> {
            LoginCallBackServer server = new LoginCallBackServer();
            try{
                server.startServer();
                BrowserUtil.browse(buildLoginUrl());
            }catch(IOException ex)
            {
                System.err.println("Callback server execption: " + ex.getMessage());
            }
        });
        add(loginLink);
    }

    private static String buildLoginUrl() {
        return InfisicalHttpClient.DEFAULT_BASE_URL + "/login?callback_port=" + LOGIN_CALLBACK_PORT;
    }
}