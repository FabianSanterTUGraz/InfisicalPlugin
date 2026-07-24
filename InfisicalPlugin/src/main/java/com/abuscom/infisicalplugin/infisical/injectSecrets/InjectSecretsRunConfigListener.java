package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.spring.boot.run.SpringBootApplicationRunConfiguration;

import org.jetbrains.annotations.NotNull;

import com.abuscom.infisicalplugin.infisical.cache.Cache;

/**
 * Läuft, bevor der Prozess startet - das ist der früheste Punkt an dem wir Zugriff auf die
 * konkrete RunConfiguration haben. Reicht Checkbox + Dropdown-Auswahl an {@link Cache} weiter, damit
 * {@link InjectIntoGradleProcess#configureSettings} (das selbst keinen Bezug zur RunConfiguration
 * hat) für Gradle-Configs weiß, ob und mit welcher Umgebung injiziert werden soll. Muss dieselben
 * Typen abdecken wie {@link InjectSecretsRunConfigurationExtension#isApplicableFor} - sonst bleibt
 * {@link Cache#isRunConfigInjectionEnabled()} für den nicht abgedeckten Typ auf dem alten Wert
 * stehen, obwohl die Checkbox in dessen RunConfiguration angehakt ist.
 */
public class InjectSecretsRunConfigListener implements ExecutionListener {
    @Override
    public void processStartScheduled(@NotNull String executorId, @NotNull ExecutionEnvironment env) {
        if (!(env.getRunProfile() instanceof RunConfigurationBase<?> config)
                || !(config instanceof ExternalSystemRunConfiguration || config instanceof SpringBootApplicationRunConfiguration)) {
            return;
        }

        InjectSecretsSettings settings = InjectSecretsSettings.getOrCreate(config);
        Cache.getInstance().setRunConfigSelection(settings.enabled, settings.selectedEnvironment);
    }
}
