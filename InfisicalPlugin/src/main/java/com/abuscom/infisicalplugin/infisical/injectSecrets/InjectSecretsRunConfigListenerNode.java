package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.abuscom.infisicalplugin.infisical.cache.Cache;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.javascript.nodejs.execution.AbstractNodeTargetRunProfile;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.spring.boot.run.SpringBootApplicationRunConfiguration;
import org.jetbrains.annotations.NotNull;

public class InjectSecretsRunConfigListenerNode implements ExecutionListener {
    @Override
    public void processStartScheduled(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
        if (!(env.getRunProfile() instanceof RunConfigurationBase<?> config)
                || !( config instanceof AbstractNodeTargetRunProfile)) {
            return;
        }

        InjectSecretsSettings settings = InjectSecretsSettings.getOrCreate(config);
        Cache.getInstance().setRunConfigSelection(settings.enabled, settings.selectedEnvironment);
    }
}
