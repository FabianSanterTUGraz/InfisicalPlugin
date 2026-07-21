package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;

import org.jetbrains.annotations.NotNull;

import com.abuscom.infisicalplugin.infisical.cache.Cache;

/**
 * Läuft, bevor der Gradle-Prozess startet - das ist der früheste Punkt an dem wir Zugriff auf die
 * konkrete RunConfiguration haben. Reicht Checkbox + Dropdown-Auswahl an {@link Cache} weiter, damit
 * {@link InjectIntoGradleProcess#configureSettings} (das selbst keinen Bezug zur RunConfiguration
 * hat) weiß, ob und mit welcher Umgebung injiziert werden soll.
 */
public class InjectSecretsRunConfigListener implements ExecutionListener {
    @Override
    public void processStartScheduled(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
        if (!(env.getRunProfile() instanceof ExternalSystemRunConfiguration gradleConfig)) {
            return;
        }

        InjectSecretsSettings settings = InjectSecretsSettings.getOrCreate(gradleConfig);
        Cache.getInstance().setRunConfigSelection(settings.enabled, settings.selectedEnvironment);
    }
}
