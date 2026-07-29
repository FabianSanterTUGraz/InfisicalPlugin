package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.spring.boot.run.SpringBootApplicationRunConfiguration;

import org.jetbrains.annotations.NotNull;

import com.abuscom.infisicalplugin.infisical.cache.Cache;

/**
 * Pendant zu {@link InjectSecretsRunConfigListenerJava}, nur für Spring-Boot-Run-Configs -
 * getrennt, weil com.intellij.spring.boot ein eigenes optionales Plugin ist und nicht in jeder
 * Java-faehigen IDE vorhanden ist (siehe withSpringBoot.xml).
 */
public class InjectSecretsRunConfigListenerSpringBoot implements ExecutionListener {
    @Override
    public void processStartScheduled(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
        if (!(env.getRunProfile() instanceof RunConfigurationBase<?> config)
                || !(config instanceof SpringBootApplicationRunConfiguration)) {
            return;
        }

        InjectSecretsSettings settings = InjectSecretsSettings.getOrCreate(config);
        Cache.getInstance().setRunConfigSelection(settings.enabled, settings.selectedEnvironment);
    }
}
