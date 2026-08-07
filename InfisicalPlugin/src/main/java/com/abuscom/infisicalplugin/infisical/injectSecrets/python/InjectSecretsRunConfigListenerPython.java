package com.abuscom.infisicalplugin.infisical.injectSecrets.python;

import com.abuscom.infisicalplugin.errorMessages.ErrorNotifier;
import com.abuscom.infisicalplugin.infisical.cache.Cache;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.abuscom.infisicalplugin.infisical.injectSecrets.InjectSecretsSettings;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import org.jetbrains.annotations.NotNull;

import com.jetbrains.python.run.AbstractPythonRunConfiguration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InjectSecretsRunConfigListenerPython implements ExecutionListener {
    @Override
    public void processStartScheduled(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
        if (!(env.getRunProfile() instanceof AbstractPythonRunConfiguration config)) {
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

        Map<String, String> envs = new HashMap<>(config.getEnvs());
        for (Map.Entry<String, String> secret : Cache.getInstance().getSecrets().entrySet()) {
            envs.putIfAbsent(secret.getKey(), secret.getValue());
        }
        config.setEnvs(envs);
    }
}
