package com.abuscom.infisicalplugin.actions;

import com.abuscom.infisicalplugin.errorMessages.ErrorNotifier;
import com.abuscom.infisicalplugin.infisical.cache.Enviroments.CurrentEnviroments;
import com.abuscom.infisicalplugin.infisical.cache.Enviroments.EnviromentsAPICallResponse;
import com.abuscom.infisicalplugin.infisical.cache.Enviroments.EnvironmentEntry;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.abuscom.infisicalplugin.infisical.injectSecrets.InjectSecretsSettings;
import com.abuscom.infisicalplugin.infisical.login.LoginUser;
import com.abuscom.infisicalplugin.infisical.login.TokenChangeListener;
import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.Arrays;

public class Dialog extends DialogWrapper implements TokenChangeListener {

    private final Project project;
    private final ComboBox<String> environmentComboBox = new ComboBox<>(InjectSecretsSettings.ENVIRONMENTS);
    private final JTextField textField = new JTextField("");
    private final JButton loginButton = new JButton("Login");

    protected Dialog(@Nullable Project project) {
        super(project);
        this.project = project;
        setTitle("Enviroment Wählen");
        init();

        loginButton.addActionListener(e -> new LoginUser().login(project));
        TokenManager.getInstance().addTokenChangeListener(this);
        updateLoginButtonVisibility(TokenManager.getInstance().getTokenFromKeypass());
        loadEnvironments();
    }

    @Override
    public void onTokenChanged(String newToken) {
        updateLoginButtonVisibility(newToken);
        if (newToken != null) {
            loadEnvironments();
        }
    }

    @Override
    public void dispose() {
        TokenManager.getInstance().removeTokenChangeListener(this);
        super.dispose();
    }

    private void updateLoginButtonVisibility(String token) {
        loginButton.setVisible(token == null);
        loginButton.revalidate();
        loginButton.repaint();
    }

    private void loadEnvironments() {
        if (!TokenManager.getInstance().isTokenValid()) {
            return;
        }
        String token = TokenManager.getInstance().getTokenFromKeypass();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            InfisicalHttpClient httpClient = new InfisicalHttpClient(InfisicalHttpClient.DEFAULT_BASE_URL);
            CurrentEnviroments environmentsClient = new CurrentEnviroments(httpClient);

            EnviromentsAPICallResponse response;
            try {
                response = environmentsClient.enviroments(project, token);
            } catch (InfisicalHttpException | IOException e) {
                ApplicationManager.getApplication().invokeLater(
                        () -> ErrorNotifier.notify(project, e),
                        ModalityState.any());
                return;
            }
            String[] fetched = response.workspace().environments().stream()
                    .map(EnvironmentEntry::slug)
                    .toArray(String[]::new);

            ApplicationManager.getApplication().invokeLater(() -> {
                String previouslySelected = (String) environmentComboBox.getSelectedItem();
                DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(fetched);
                if (previouslySelected != null && Arrays.asList(fetched).contains(previouslySelected)) {
                    model.setSelectedItem(previouslySelected);
                }
                environmentComboBox.setModel(model);
            }, ModalityState.any());
        });
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Environment auswählen........."));
        panel.add(environmentComboBox);
        panel.add(textField);
        panel.add(loginButton);
        return panel;
    }

    public String getSelectedEnvironment() {
        return (String) environmentComboBox.getSelectedItem();
    }

    public String getPath() {
        return textField.getText();
    }
}
