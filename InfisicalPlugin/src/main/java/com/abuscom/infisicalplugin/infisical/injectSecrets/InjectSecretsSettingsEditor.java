package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.abuscom.infisicalplugin.infisical.cache.Cache;
import com.abuscom.infisicalplugin.infisical.cache.CurrentEnviroments;
import com.abuscom.infisicalplugin.infisical.cache.EnviromentsAPICallResponse;
import com.abuscom.infisicalplugin.infisical.cache.EnvironmentEntry;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.abuscom.infisicalplugin.infisical.login.TokenChangeListener;
import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.ComboBox;

import org.jetbrains.annotations.NotNull;

import com.abuscom.infisicalplugin.infisical.login.LoginUser;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import java.io.IOException;

public class InjectSecretsSettingsEditor extends SettingsEditor<RunConfigurationBase<?>> implements TokenChangeListener {

    private final JCheckBox enabledCheckBox = new JCheckBox("Infisical Secrets in diese Run Configuration injizieren");
    private final ComboBox<String> environmentComboBox = new ComboBox<>(InjectSecretsSettings.ENVIRONMENTS);
    private final JButton loginButton = new JButton("Login");
    private final JButton refreshButton = new JButton("Refresh", AllIcons.Actions.BuildLoadChanges);
    private RunConfigurationBase<?> configuration;

    public InjectSecretsSettingsEditor() {
        loginButton.addActionListener(e -> new LoginUser().login());
        refreshButton.addActionListener(e-> Cache.getInstance().clearCache());
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

        refreshButton.setVisible(token != null);
        refreshButton.revalidate();
        refreshButton.repaint();
    }

    @Override
    protected void resetEditorFrom(@NotNull RunConfigurationBase<?> configuration) {
        this.configuration = configuration;

        InjectSecretsSettings settings = InjectSecretsSettings.getOrCreate(configuration);
        enabledCheckBox.setSelected(settings.enabled);
        environmentComboBox.setSelectedItem(settings.selectedEnvironment);

        loadEnvironments();
    }

    private void loadEnvironments() {
        if (configuration == null) {
            return;
        }
        String token = TokenManager.getInstance().getTokenFromKeypass();
        if (token == null) {
            return;
        }
        InjectSecretsSettings settings = InjectSecretsSettings.getOrCreate(configuration);

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            InfisicalHttpClient httpClient = new InfisicalHttpClient(InfisicalHttpClient.DEFAULT_BASE_URL);
            CurrentEnviroments environmentsClient = new CurrentEnviroments(httpClient);

            EnviromentsAPICallResponse response;
            try {
                response = environmentsClient.enviroments(configuration.getProject(), token);
            } catch (InfisicalHttpException | IOException e) {
                throw new RuntimeException(e);
            }
            String[] fetched = response.workspace().environments().stream()
                    .map(EnvironmentEntry::slug)
                    .toArray(String[]::new);

            ApplicationManager.getApplication().invokeLater(() -> {
                environmentComboBox.removeAllItems();
                for (String env : fetched) {
                    environmentComboBox.addItem(env);
                }
                environmentComboBox.setSelectedItem(settings.selectedEnvironment);
            }, ModalityState.any());
        });
    }

    @Override
    protected void applyEditorTo(@NotNull RunConfigurationBase<?> configuration) {
        InjectSecretsSettings settings = InjectSecretsSettings.getOrCreate(configuration);
        settings.enabled = enabledCheckBox.isSelected();
        settings.selectedEnvironment = (String) environmentComboBox.getSelectedItem();
    }

    @Override
    protected @NotNull JComponent createEditor() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(enabledCheckBox);
        panel.add(environmentComboBox);
        panel.add(loginButton);
        panel.add(refreshButton);
        return panel;
    }
}
