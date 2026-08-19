package com.abuscom.infisicalplugin.infisical.injectSecrets;

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
import com.abuscom.infisicalplugin.infisical.login.TokenChangeListener;
import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;

import org.jetbrains.annotations.NotNull;

import com.abuscom.infisicalplugin.infisical.login.LoginUser;

import javax.swing.*;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InjectSecretsSettingsEditor extends SettingsEditor<RunConfigurationBase<?>> implements TokenChangeListener {

    private final ComboBox<String> environmentComboBox = new ComboBox<>(InjectSecretsSettings.ENVIRONMENTS);
    private final ComboBox<String> projectComboBox = new ComboBox<>(InjectSecretsSettings.PROJECTS);
    private final JButton loginButton = new JButton("Login");
    private RunConfigurationBase<?> configuration;
    private JPanel rootPanel;
    private volatile boolean environmentsLoaded = false;
    private volatile boolean projectsLoaded = false;
    private volatile boolean suppressProjectSelectionEvents = false;
    private  Map<String,String> projectNameToId = new HashMap<>();

    public InjectSecretsSettingsEditor() {
        loginButton.addActionListener(e -> new LoginUser().login(configuration.getProject()));
        TokenManager.getInstance().addTokenChangeListener(this);
        projectComboBox.addItemListener(e -> {
            if (e.getStateChange() == ItemEvent.SELECTED && !suppressProjectSelectionEvents) {
                onProjectSelected();
            }
        });

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
            loadProjects();
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
        projectComboBox.setSelectedItem(settings.selectedProject);

        loadEnvironments();
        loadProjects();
    }

    private void loadProjects()
    {
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
            SecretClient client = new SecretClient(httpClient);

            ListProjectsResponse response;
            try {
                response = client.listProjects(token);
            } catch (InfisicalHttpException e) {
                ApplicationManager.getApplication().invokeLater(
                        () -> ErrorNotifier.notify(configuration.getProject(), e),
                        ModalityState.any());
                return;
            }

            for(ListProjectEntry projectEntry : response.projects())
            {
                projectNameToId.put(projectEntry.name(), projectEntry.id());
            }

            String resolvedProjectId = resolveInfisicalJsonValue("workspaceId");
            String selectedProjectName = resolveProjectName(resolvedProjectId);

            String[] fetched = response.projects().stream()
                    .filter(p -> "secret-manager".equals(p.type()))
                    .map(ListProjectEntry::name)
                    .toArray(String[]::new);

            ApplicationManager.getApplication().invokeLater(() -> {
                DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(fetched);
                String toSelect = pickPreselection(selectedProjectName, settings.selectedProject, fetched);
                if (toSelect != null) {
                    model.setSelectedItem(toSelect);
                }
                suppressProjectSelectionEvents = true;
                projectComboBox.setModel(model);
                suppressProjectSelectionEvents = false;
                projectsLoaded = true;
            }, ModalityState.any());
        });
    }

    /**
     * Liest einen einzelnen Wert (z.B. "workspaceId", "defaultEnvironment") aus der
     * .infisical.json. Fehlt die Datei schlicht noch (kein Projekt/Environment je gewaehlt),
     * wird das still ignoriert; existiert die Datei aber und laesst sich trotzdem nicht lesen,
     * bekommt der User eine Fehlermeldung.
     */
    private String resolveInfisicalJsonValue(String key) {
        Project project = configuration.getProject();
        try {
            return Cache.readConfig(project, ".infisical.json").get(key);
        } catch (IOException e) {
            if (Cache.getInstance().infisicalJsonExists(project)) {
                ApplicationManager.getApplication().invokeLater(
                        () -> ErrorNotifier.notify(project, e),
                        ModalityState.any());
            }
            return null;
        }
    }

    private String resolveProjectName(String workspaceId) {
        return projectNameToId.entrySet().stream()
                .filter(e -> e.getValue().equals(workspaceId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * .infisical.json gewinnt immer, wenn der Wert in den aktuell verfuegbaren Optionen vorkommt.
     * Nur wenn das nicht aufloesbar ist (Datei fehlt/Wert veraltet), zaehlt die zuletzt
     * gespeicherte Auswahl dieser Run-Configuration.
     */
    private static String pickPreselection(String fromInfisicalJson, String fromRunConfig, String[] options) {
        List<String> available = Arrays.asList(options);
        if (fromInfisicalJson != null && available.contains(fromInfisicalJson)) {
            return fromInfisicalJson;
        }
        if (fromRunConfig != null && available.contains(fromRunConfig)) {
            return fromRunConfig;
        }
        return null;
    }

    private void onProjectSelected() {
        if (configuration == null) {
            return;
        }
        String selectedName = (String) projectComboBox.getSelectedItem();
        if (selectedName == null) {
            return;
        }
        String id = projectNameToId.get(selectedName);
        if (id == null) {
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                Cache.writeConfig(configuration.getProject(), ".infisical.json", id);
            } catch (IOException e) {
                ApplicationManager.getApplication().invokeLater(
                        () -> ErrorNotifier.notify(configuration.getProject(), e),
                        ModalityState.any());
                return;
            }
            loadEnvironments();
        });
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

            String defaultEnvironment = resolveInfisicalJsonValue("defaultEnvironment");

            ApplicationManager.getApplication().invokeLater(() -> {
                DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(fetched);
                String toSelect = pickPreselection(defaultEnvironment, settings.selectedEnvironment, fetched);
                if (toSelect != null) {
                    model.setSelectedItem(toSelect);
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
        if(projectsLoaded) {
            settings.selectedProject = (String) projectComboBox.getSelectedItem();
        }
    }

    @Override
    protected @NotNull JComponent createEditor() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(new JLabel("Projekt/Environment auswählen"));
        panel.add(projectComboBox);
        panel.add(environmentComboBox);
        panel.add(loginButton);
        rootPanel = panel;
        return panel;
    }
}
