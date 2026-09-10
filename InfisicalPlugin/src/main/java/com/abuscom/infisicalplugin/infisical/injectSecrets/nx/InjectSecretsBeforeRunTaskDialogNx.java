package com.abuscom.infisicalplugin.infisical.injectSecrets.nx;

import com.abuscom.infisicalplugin.errorMessages.ErrorNotifier;
import com.abuscom.infisicalplugin.infisical.cache.Cache;
import com.abuscom.infisicalplugin.infisical.cache.Enviroments.CurrentEnviroments;
import com.abuscom.infisicalplugin.infisical.cache.Enviroments.EnviromentsAPICallResponse;
import com.abuscom.infisicalplugin.infisical.cache.Enviroments.EnvironmentEntry;
import com.abuscom.infisicalplugin.infisical.cache.Secrets.ListProjects.ListProjectEntry;
import com.abuscom.infisicalplugin.infisical.cache.Secrets.ListProjects.ListProjectsResponse;
import com.abuscom.infisicalplugin.infisical.cache.Secrets.SecretClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.abuscom.infisicalplugin.infisical.login.LoginUser;
import com.abuscom.infisicalplugin.infisical.login.TokenChangeListener;
import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient.DEFAULT_BASE_URL;

/**
 * Minimaler Ersatz fuer {@link com.abuscom.infisicalplugin.infisical.injectSecrets.InjectSecretsSettingsEditor}
 * fuer Nx: NxCommandConfiguration bindet den "Modify options"-Extension-Mechanismus nicht ein, deshalb
 * wird die Projekt-/Environment-Auswahl hier ueber einen Dialog angeboten, den man per Doppelklick auf
 * die "Infisical: Secrets injizieren"-Zeile in der Before-Launch-Liste oeffnet
 * (siehe {@link InjectSecretsBeforeRunTaskProviderNx#configureTask}).
 */
public class InjectSecretsBeforeRunTaskDialogNx extends DialogWrapper implements TokenChangeListener {

    private final Project project;
    private final String initialEnvironment;

    private final ComboBox<String> projectComboBox = new ComboBox<>();
    private final ComboBox<String> environmentComboBox = new ComboBox<>();
    private final JButton loginButton = new JButton("Login");

    private final Map<String, String> projectNameToId = new HashMap<>();
    private volatile boolean suppressProjectSelectionEvents = false;

    public InjectSecretsBeforeRunTaskDialogNx(@NotNull Project project,
                                               @Nullable String currentProject,
                                               @Nullable String currentEnvironment) {
        super(project);
        this.project = project;
        this.initialEnvironment = currentEnvironment;
        setTitle("Infisical: Projekt/Environment wählen");

        projectComboBox.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXX");
        environmentComboBox.setPrototypeDisplayValue("XXXXXXXXXXXX");

        projectComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && !suppressProjectSelectionEvents) {
                onProjectSelected(null);
            }
        });

        loginButton.addActionListener(e -> new LoginUser().login(project));
        updateLoginButtonVisibility(TokenManager.getInstance().getTokenFromKeypass());
        TokenManager.getInstance().addTokenChangeListener(this);

        init();
        loadProjects(currentProject);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Projekt"));
        panel.add(projectComboBox);
        panel.add(new JLabel("Environment"));
        panel.add(environmentComboBox);
        panel.add(loginButton);
        return panel;
    }

    @Override
    public void onTokenChanged(String newToken) {
        updateLoginButtonVisibility(newToken);
        if (newToken != null) {
            loadProjects(getSelectedProject());
        }
    }

    private void updateLoginButtonVisibility(String token) {
        loginButton.setVisible(token == null);
        loginButton.revalidate();
        loginButton.repaint();
    }

    @Override
    public void dispose() {
        TokenManager.getInstance().removeTokenChangeListener(this);
        super.dispose();
    }

    private void loadProjects(@Nullable String preselect) {
        if (!TokenManager.getInstance().isTokenValid()) {
            ErrorNotifier.notify(project, "No valid jwt-Token given!(not logged in or expired)");
            return;
        }
        String token = TokenManager.getInstance().getTokenFromKeypass();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            InfisicalHttpClient httpClient = new InfisicalHttpClient(DEFAULT_BASE_URL);
            SecretClient client = new SecretClient(httpClient);

            ListProjectsResponse response;
            try {
                response = client.listProjects(token);
            } catch (InfisicalHttpException e) {
                ApplicationManager.getApplication().invokeLater(() -> ErrorNotifier.notify(project, e), ModalityState.any());
                return;
            }

            for (ListProjectEntry projectEntry : response.projects()) {
                projectNameToId.put(projectEntry.name(), projectEntry.id());
            }

            String[] fetched = response.projects().stream()
                    .filter(p -> "secret-manager".equals(p.type()))
                    .map(ListProjectEntry::name)
                    .toArray(String[]::new);

            ApplicationManager.getApplication().invokeLater(() -> {
                DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(fetched);
                if (preselect != null && Arrays.asList(fetched).contains(preselect)) {
                    model.setSelectedItem(preselect);
                }
                suppressProjectSelectionEvents = true;
                projectComboBox.setModel(model);
                suppressProjectSelectionEvents = false;

                onProjectSelected(initialEnvironment);
            }, ModalityState.any());
        });
    }

    private void onProjectSelected(@Nullable String preselectEnvironment) {
        String selectedName = (String) projectComboBox.getSelectedItem();
        if (selectedName == null) {
            return;
        }
        String projectId = projectNameToId.get(selectedName);
        if (projectId == null) {
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Cache.writeConfig(project, ".infisical.json", projectId);
            } catch (IOException e) {
                ApplicationManager.getApplication().invokeLater(() -> ErrorNotifier.notify(project, e), ModalityState.any());
                return;
            }
            loadEnvironments(preselectEnvironment);
        });
    }

    private void loadEnvironments(@Nullable String preselect) {
        if (!TokenManager.getInstance().isTokenValid()) {
            ErrorNotifier.notify(project, "No valid jwt-Token given!(not logged in or expired)");
            return;
        }
        String token = TokenManager.getInstance().getTokenFromKeypass();

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            InfisicalHttpClient httpClient = new InfisicalHttpClient(DEFAULT_BASE_URL);
            CurrentEnviroments environmentsClient = new CurrentEnviroments(httpClient);

            EnviromentsAPICallResponse response;
            try {
                response = environmentsClient.enviroments(project, token);
            } catch (InfisicalHttpException | IOException e) {
                ApplicationManager.getApplication().invokeLater(() -> ErrorNotifier.notify(project, e), ModalityState.any());
                return;
            }

            String[] fetched = response.workspace().environments().stream()
                    .map(EnvironmentEntry::slug)
                    .toArray(String[]::new);

            ApplicationManager.getApplication().invokeLater(() -> {
                DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(fetched);
                if (preselect != null && Arrays.asList(fetched).contains(preselect)) {
                    model.setSelectedItem(preselect);
                }
                environmentComboBox.setModel(model);
            }, ModalityState.any());
        });
    }

    public String getSelectedProject() {
        return (String) projectComboBox.getSelectedItem();
    }

    public String getSelectedProjectId() {
        return projectNameToId.get(getSelectedProject());
    }

    public String getSelectedEnvironment() {
        return (String) environmentComboBox.getSelectedItem();
    }
}
