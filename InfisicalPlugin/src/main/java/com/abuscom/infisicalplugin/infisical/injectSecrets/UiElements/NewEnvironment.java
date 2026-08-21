package com.abuscom.infisicalplugin.infisical.injectSecrets.UiElements;

import com.abuscom.infisicalplugin.errorMessages.ErrorNotifier;
import com.abuscom.infisicalplugin.infisical.cache.Cache;
import com.abuscom.infisicalplugin.infisical.cache.Secrets.SecretClient;
import com.abuscom.infisicalplugin.infisical.cache.Secrets.SecretEntry;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.http.HttpClient;

import static com.abuscom.infisicalplugin.infisical.injectSecrets.InjectSecretsSettingsEditor.redirectToInfisicalProject;

public class NewEnvironment extends DialogWrapper {

    private final JTextField txtEnviromentField = new JTextField(20);
    private final Project project;

    public NewEnvironment(@Nullable Project project) {
        super(project);
        this.project = project;
        setTitle("Enviroment hinzufügen");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Name:"), BorderLayout.WEST);
        panel.add(txtEnviromentField, BorderLayout.CENTER);
        return panel;
    }

    @Override
    protected void doOKAction() {
        String newEnviroment = txtEnviromentField.getText();
        if(newEnviroment == null || newEnviroment.isEmpty() || !isValidName(newEnviroment)) {
            ErrorNotifier.notify(project , "Keine gültige Eingabe!");
            return;
        }

        String projectID;
        try {
            projectID = Cache.readConfig(project,".infisical.json").get("workspaceId");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            InfisicalHttpClient httpClient = new InfisicalHttpClient(InfisicalHttpClient.DEFAULT_BASE_URL);
            SecretClient secretClient = new SecretClient(httpClient);
            try {
                secretClient.createEnvironment(projectID,newEnviroment, TokenManager.getInstance().getTokenFromKeypass());
            } catch (InfisicalHttpException e) {
                throw new RuntimeException(e);
            }
        });

        System.out.println("Enviroment is " + newEnviroment);
        redirectToInfisicalProject();
        super.doOKAction();
    }

    public boolean isValidName(String name)
    {
        return name != null && !name.contains(" ") && name.equals(name.toLowerCase());
    }


}
