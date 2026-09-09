package com.abuscom.infisicalplugin.infisical.injectSecrets.UiElements;

import com.abuscom.infisicalplugin.errorMessages.ErrorNotifier;
import com.abuscom.infisicalplugin.infisical.cache.Secrets.SecretClient;
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
import java.util.List;

public class AddOverrides extends DialogWrapper {
    private final Project project;
    private final String projectId;
    private final String environment;

    public AddOverrides(@Nullable Project project, String projectId, String environment) {
        super(project);
        this.project = project;
        this.projectId = projectId;
        this.environment = environment;
        setTitle("Overrides");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
        List<String> test = List.of("test1", "test2", "test3");
        for (String name : test) {
            JTextField field = new JTextField(20);
            panel.add(new JLabel(name + "  :"));
            panel.add(field);
        }
        return panel;
    }

    @Override
    protected void doOKAction() {
        String secretName = "test123";
        String secretValue = "dasWurdevomPlugingeändert";

        if (projectId == null || environment == null) {
            ErrorNotifier.notify(project, "Kein Projekt/Environment ausgewählt!");
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            InfisicalHttpClient httpClient = new InfisicalHttpClient(InfisicalHttpClient.DEFAULT_BASE_URL);
            SecretClient secretClient = new SecretClient(httpClient);
            try {
                secretClient.setOverride(projectId, secretName, environment, secretValue, TokenManager.getInstance().getTokenFromKeypass());
            } catch (InfisicalHttpException e) {
                ApplicationManager.getApplication().invokeLater(
                        () -> ErrorNotifier.notify(project, e),
                        ModalityState.any());
            }
        });

        super.doOKAction();
    }
}
