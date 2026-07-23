package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.abuscom.infisicalplugin.infisical.cache.Cache;
import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.process.ProcessOutputType;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemProcessingManager;
import com.intellij.spring.boot.run.SpringBootApplicationRunConfiguration;
import com.intellij.openapi.project.Project;

import com.intellij.openapi.options.SettingsEditor;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Sorgt nur dafür, dass Checkbox + Dropdown unter "Modify options" der Gradle-Run-Configuration
 * auftauchen und in {@link InjectSecretsSettings} persistiert werden. Die eigentliche
 * Secret-Injektion passiert NICHT hier (siehe patchCommandLine unten), sondern in
 * {@link InjectIntoGradleProcess}, das über {@link InjectSecretsRunConfigListener} erfährt, ob und
 * mit welcher Umgebung injiziert werden soll.
 */
public class InjectSecretsRunConfigurationExtension extends RunConfigurationExtension {

    @Override
    public boolean isApplicableFor(@NotNull RunConfigurationBase<?> configuration) {
        return configuration instanceof ExternalSystemRunConfiguration || configuration instanceof SpringBootApplicationRunConfiguration;
    }

    @Override
    public boolean isEnabledFor(@NotNull RunConfigurationBase applicableConfiguration, RunnerSettings runnerSettings) {
        return InjectSecretsSettings.getOrCreate(applicableConfiguration).enabled;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <P extends RunConfigurationBase<?>> SettingsEditor<P> createEditor(@NotNull P configuration) {
        return (SettingsEditor<P>) new InjectSecretsSettingsEditor();
    }

    @Override
    protected String getEditorTitle() {
        return "Infisical";
    }

    @Override
    protected void readExternal(@NotNull RunConfigurationBase<?> configuration, @NotNull Element element) {
        InjectSecretsSettings.readExternal(configuration, element);
    }

    @Override
    protected void writeExternal(@NotNull RunConfigurationBase<?> configuration, @NotNull Element element) {
        InjectSecretsSettings.writeExternal(configuration, element);
    }

    @Override
    protected void patchCommandLine(@NotNull RunConfigurationBase configuration,
                                     RunnerSettings runnerSettings,
                                     @NotNull GeneralCommandLine cmdLine,
                                     @NotNull String runnerId) throws ExecutionException {

    }

    @Override
    public <T extends RunConfigurationBase<?>> void updateJavaParameters(T configuration,
                                                                          JavaParameters javaParameters,
                                                                          RunnerSettings runnerSettings) throws ExecutionException {

        if (!TokenManager.getInstance().isTokenValid()) {
            NotificationGroupManager.getInstance()
                    .getNotificationGroup("Infisical Notifications")   // muss exakt der id aus plugin.xml entsprechen
                    .createNotification("Infisical", "Kein gültiger Token — bitte einloggen.", NotificationType.ERROR)
                    .notify(configuration.getProject());
            return;
        }

        try {
            Cache.getInstance().setCache(configuration.getProject());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        javaParameters.getEnv().putAll(Cache.getInstance().getSecrets());
    }
}
