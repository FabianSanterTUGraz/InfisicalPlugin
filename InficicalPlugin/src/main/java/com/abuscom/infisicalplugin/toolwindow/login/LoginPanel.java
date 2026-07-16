package com.abuscom.infisicalplugin.toolwindow.login;

import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.intellij.ide.BrowserUtil;
import com.intellij.ui.HyperlinkLabel;

import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.io.IOException;

public final class LoginPanel extends JPanel implements TokenChangeListener {

    public static final int LOGIN_CALLBACK_PORT = 8010;
    private final TokenManager tokenManager;
    private HyperlinkLabel loginLink;

    public LoginPanel() {
        super(new FlowLayout(FlowLayout.LEFT));
        tokenManager = TokenManager.getInstance();
        tokenManager.addTokenChangeListener(this);
        tokenManager.clearKeypass();
        String token = tokenManager.getTokenFromKeypass();
        System.out.println("der token" + " " + token);

        if(token == null) {
            createLoginButton();
        }
    }

    @Override
    public void onTokenChanged(String newToken) {
        if(newToken == null) {
            createLoginButton();
        } else {
            removeLoginButton();
        }
    }

    private void createLoginButton() {
        if(loginLink == null) {
            loginLink = new HyperlinkLabel("Mit Infisical einloggen");
            loginLink.addHyperlinkListener(e -> {
                LoginCallBackServer server = new LoginCallBackServer(tokenManager);
                try {
                    server.startServer();
                    BrowserUtil.browse(buildLoginUrl());
                } catch (IOException ex) {
                    System.err.println("Callback server execption: " + ex.getMessage());
                }
            });
            add(loginLink);
            revalidate();
            repaint();
        }
    }

    private void removeLoginButton() {
        if(loginLink != null) {
            remove(loginLink);
            loginLink = null;
            revalidate();
            repaint();
        }
    }

    private static String buildLoginUrl() {
        return InfisicalHttpClient.DEFAULT_BASE_URL + "/login?callback_port=" + LOGIN_CALLBACK_PORT;
    }
}