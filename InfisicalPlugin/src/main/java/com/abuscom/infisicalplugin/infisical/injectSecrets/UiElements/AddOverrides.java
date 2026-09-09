package com.abuscom.infisicalplugin.infisical.injectSecrets.UiElements;

import com.abuscom.infisicalplugin.errorMessages.ErrorNotifier;
import com.abuscom.infisicalplugin.infisical.cache.Secrets.SecretClient;
import com.abuscom.infisicalplugin.infisical.cache.Secrets.SecretsAPICallResponse;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class AddOverrides extends DialogWrapper {
    private final Project project;
    private final String projectId;
    private final String environment;
    private final Map<String, String> currentValues;
    private final Map<String, JTextField> secretFields = new LinkedHashMap<>();

    public AddOverrides(@Nullable Project project, String projectId, String environment, Map<String, String> currentValues) {
        super(project);
        this.project = project;
        this.projectId = projectId;
        this.environment = environment;
        this.currentValues = currentValues;
        setTitle("Overrides");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        for (Map.Entry<String, String> entry : currentValues.entrySet()) {
            JTextField field = new JTextField(entry.getValue(), 20);
            secretFields.put(entry.getKey(), field);
            panel.add(new JLabel(entry.getKey() + "  :"));
            panel.add(field);
        }
        return panel;
    }

    @Override
    protected void doOKAction() {
        if (projectId == null || environment == null) {
            ErrorNotifier.notify(project, "Kein Projekt/Environment ausgewählt!");
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            InfisicalHttpClient httpClient = new InfisicalHttpClient(InfisicalHttpClient.DEFAULT_BASE_URL);
            SecretClient secretClient = new SecretClient(httpClient);
            String token = TokenManager.getInstance().getTokenFromKeypass();

            try {
                SecretsAPICallResponse existing = secretClient.secrets(projectId, environment, token);

                secretFields.forEach((secretName, field) -> {
                    String secretValue = field.getText();
                    if (secretValue == null || secretValue.isBlank()) {
                        return;
                    }

                    boolean hasPersonalOverride = existing.secrets().stream()
                            .anyMatch(s -> s.secretKey().equals(secretName) && "personal".equals(s.type()));

                    try {
                        if (hasPersonalOverride) {
                            secretClient.setOverride(projectId, secretName, environment, secretValue, token);
                        } else {
                            secretClient.createOverride(projectId, secretName, environment, secretValue, token);
                        }
                    } catch (InfisicalHttpException e) {
                        ApplicationManager.getApplication().invokeLater(
                                () -> ErrorNotifier.notify(project, e),
                                ModalityState.any());
                    }
                });
            } catch (InfisicalHttpException e) {
                ApplicationManager.getApplication().invokeLater(
                        () -> ErrorNotifier.notify(project, e),
                        ModalityState.any());
            }
        });

        super.doOKAction();
    }
}
