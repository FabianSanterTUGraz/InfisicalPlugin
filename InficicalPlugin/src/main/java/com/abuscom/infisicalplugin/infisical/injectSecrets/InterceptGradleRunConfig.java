package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;

import org.jetbrains.annotations.NotNull;

import com.abuscom.infisicalplugin.infisical.cache.Cache;

import java.util.HashMap;
import java.util.Map;


public class InterceptGradleRunConfig implements  ExecutionListener {
    @Override
    public void processStartScheduled(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
        {
            if (env.getRunProfile() instanceof ExternalSystemRunConfiguration) {
                ExternalSystemRunConfiguration gradleConfig = (ExternalSystemRunConfiguration) env.getRunProfile();

                if (!InjectSecretsSettings.getOrCreate(gradleConfig).enabled) {
                    return;
                }

                Map<String, String> currentEnvironment = new HashMap<>(gradleConfig.getSettings().getEnv());

                Cache cache = Cache.getInstance();
                currentEnvironment.putAll(cache.getSecrets());

                gradleConfig.getSettings().setEnv(currentEnvironment);
            }
        }
    }
}
