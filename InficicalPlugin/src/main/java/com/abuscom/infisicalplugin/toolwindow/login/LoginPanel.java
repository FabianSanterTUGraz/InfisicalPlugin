package com.abuscom.infisicalplugin.toolwindow.login;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.FlowLayout;

public final class LoginPanel extends JPanel implements TokenChangeListener {

    public static final int LOGIN_CALLBACK_PORT = 8010;
    private final TokenManager tokenManager;
    private final JLabel statusLabel;

    public LoginPanel() {
        super(new FlowLayout(FlowLayout.LEFT));
        tokenManager = TokenManager.getInstance();
        tokenManager.addTokenChangeListener(this);

        statusLabel = new JLabel();
        add(statusLabel);

        String token = tokenManager.getTokenFromKeypass();
        updateStatus(token);
    }

    @Override
    public void onTokenChanged(String newToken) {
        updateStatus(newToken);
    }
    private void updateStatus(String token) {
        if (token == null) {
            statusLabel.setText("Status: Nicht angemeldet");
        } else {
            statusLabel.setText("Status: Angemeldet");
        }
        revalidate();
        repaint();
    }
}