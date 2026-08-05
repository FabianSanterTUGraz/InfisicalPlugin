package com.abuscom.infisicalplugin.infisical.injectSecrets.apacheMaven;

import com.abuscom.infisicalplugin.errorMessages.ErrorNotifier;
import com.abuscom.infisicalplugin.infisical.cache.Cache;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.abuscom.infisicalplugin.infisical.injectSecrets.InjectSecretsSettings;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;
import org.jetbrains.idea.maven.execution.MavenRunnerSettings;

import java.io.IOException;

public class InjectSecretsRunConfigListenerMaven implements ExecutionListener  {
    @Override
    public void processStartScheduled(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
        if (!(env.getRunProfile() instanceof MavenRunConfiguration config)) {
            return;
        }

        InjectSecretsSettings settings = InjectSecretsSettings.getOrCreate(config);
        if(!settings.enabled)
        {
            return;
        }

        Cache.getInstance().setRunConfigSelection(true, settings.selectedEnvironment);
        try {
            Cache.getInstance().setCache(config.getProject());
        } catch (IOException | InfisicalHttpException e) {
            ErrorNotifier.notify(config.getProject(), e);
            return;
        }

        MavenRunnerSettings mavenRunnerSettings = config.getRunnerSettings();
        if (mavenRunnerSettings == null) {
            mavenRunnerSettings = new MavenRunnerSettings();
            config.setRunnerSettings(mavenRunnerSettings);
        }
        mavenRunnerSettings.setEnvironmentProperties(Cache.getInstance().getSecrets());
    }
}
