package com.abuscom.infisicalplugin.infisical.injectSecrets.nx;

import com.abuscom.infisicalplugin.infisical.cache.Cache;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import dev.nx.console.run.NxCommandConfiguration;
import dev.nx.console.run.NxRunSettings;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class InjectSecretsRunConfigListenerNx implements ExecutionListener {
    @Override
    public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler) {
        if (!(env.getRunProfile() instanceof NxCommandConfiguration config)) {
            return;
        }

        NxRunSettings settings = config.getNxRunSettings();
        config.setNxRunSettings(settings);

        EnvironmentVariablesData current = settings.getEnvironmentVariables();
        Map<String, String> filtered = new HashMap<>(current.getEnvs());
        filtered.keySet().removeAll(Cache.getInstance().getSecrets().keySet());

        EnvironmentVariablesData filteredData = EnvironmentVariablesData.create(filtered, current.isPassParentEnvs());
        settings.setEnvironmentVariables(filteredData);
    }
}

