package com.abuscom.infisicalplugin.infisical.injectSecrets.apacheMaven;

import com.abuscom.infisicalplugin.infisical.cache.Cache;
import com.abuscom.infisicalplugin.infisical.injectSecrets.InjectSecretsSettings;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;
import org.jetbrains.idea.maven.execution.MavenRunnerSettings;

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
        MavenRunnerSettings mavenRunnerSettings = config.getRunnerSettings();
        //Cache.getInstance().setRunConfigSelection(settings.enabled, settings.selectedEnvironment);
    }
}
