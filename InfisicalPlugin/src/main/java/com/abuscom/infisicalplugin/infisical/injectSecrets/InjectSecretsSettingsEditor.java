package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.abuscom.infisicalplugin.errorMessages.ErrorNotifier;
import com.abuscom.infisicalplugin.infisical.cache.Enviroments.CurrentEnviroments;
import com.abuscom.infisicalplugin.infisical.cache.Enviroments.EnviromentsAPICallResponse;
import com.abuscom.infisicalplugin.infisical.cache.Enviroments.EnvironmentEntry;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.abuscom.infisicalplugin.infisical.login.TokenChangeListener;
import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.ComboBox;

import org.jetbrains.annotations.NotNull;

import com.abuscom.infisicalplugin.infisical.login.LoginUser;

import javax.swing.*;
import java.awt.FlowLayout;
import java.io.IOException;

public class InjectSecretsSettingsEditor extends SettingsEditor<RunConfigurationBase<?>> implements TokenChangeListener {

    private final ComboBox<String> environmentComboBox = new ComboBox<>(InjectSecretsSettings.ENVIRONMENTS);
    private final JButton loginButton = new JButton("Login");
    private RunConfigurationBase<?> configuration;
    private JPanel rootPanel;
    private volatile boolean environmentsLoaded = false;

    public InjectSecretsSettingsEditor() {
        loginButton.addActionListener(e -> new LoginUser().login(configuration.getProject()));
        TokenManager.getInstance().addTokenChangeListener(this);
        updateLoginButtonVisibility(TokenManager.getInstance().getTokenFromKeypass());
    }

    @Override
    protected void disposeEditor() {
        TokenManager.getInstance().removeTokenChangeListener(this);
        super.disposeEditor();
    }

    @Override
    public void onTokenChanged(String newToken) {
        updateLoginButtonVisibility(newToken);
        if (newToken != null) {
            loadEnvironments();
        }
    }

    private void updateLoginButtonVisibility(String token) {
        loginButton.setVisible(token == null);
        loginButton.revalidate();
        loginButton.repaint();
    }

    @Override
    protected void resetEditorFrom(@NotNull RunConfigurationBase<?> configuration) {
        this.configuration = configuration;

        InjectSecretsSettings settings = InjectSecretsSettings.getOrCreate(configuration);
        environmentComboBox.setSelectedItem(settings.selectedEnvironment);

        loadEnvironments();
    }

    private void loadEnvironments() {
        if (configuration == null) {
            return;
        }
        if (!TokenManager.getInstance().isTokenValid()) {
            return;
        }
        String token = TokenManager.getInstance().getTokenFromKeypass();
        InjectSecretsSettings settings = InjectSecretsSettings.getOrCreate(configuration);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            InfisicalHttpClient httpClient = new InfisicalHttpClient(InfisicalHttpClient.DEFAULT_BASE_URL);
            CurrentEnviroments environmentsClient = new CurrentEnviroments(httpClient);

            EnviromentsAPICallResponse response;
            try {
                response = environmentsClient.enviroments(configuration.getProject(), token);
            } catch (InfisicalHttpException | IOException e) {
                ApplicationManager.getApplication().invokeLater(
                        () -> ErrorNotifier.notify(configuration.getProject(), e),
                        ModalityState.any());
                return;
            }
            String[] fetched = response.workspace().environments().stream()
                    .map(EnvironmentEntry::slug)
                    .toArray(String[]::new);

            ApplicationManager.getApplication().invokeLater(() -> {
                DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(fetched);
                if (settings.selectedEnvironment != null
                        && java.util.Arrays.asList(fetched).contains(settings.selectedEnvironment)) {
                    model.setSelectedItem(settings.selectedEnvironment);
                }
                environmentComboBox.setModel(model);
                environmentsLoaded = true;
            }, ModalityState.any());
        });
    }

    @Override
    protected void applyEditorTo(@NotNull RunConfigurationBase<?> configuration) {
        InjectSecretsSettings settings = InjectSecretsSettings.getOrCreate(configuration);
        settings.enabled = rootPanel.isVisible();
        if(environmentsLoaded) {
            settings.selectedEnvironment = (String) environmentComboBox.getSelectedItem();
        }
    }

    @Override
    protected @NotNull JComponent createEditor() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Environment auswählen........."));
        panel.add(environmentComboBox);
        panel.add(loginButton);
        rootPanel = panel;
        return panel;
    }
}
