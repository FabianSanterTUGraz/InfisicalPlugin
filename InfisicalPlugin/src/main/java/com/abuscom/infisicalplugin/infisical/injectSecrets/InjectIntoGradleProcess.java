package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionContext;
import org.jetbrains.plugins.gradle.service.project.GradleExecutionHelperExtension;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;

import com.abuscom.infisicalplugin.infisical.cache.Cache;

import java.io.IOException;
import java.util.Map;

/**
 * Hookt sich über den Erweiterungspunkt {@code org.jetbrains.plugins.gradle.executionHelperExtension}
 * in {@link org.jetbrains.plugins.gradle.service.execution.GradleExecutionHelper} ein.
 * <p>
 * {@link #configureSettings} wird von dort für jede Gradle-Ausführung aufgerufen, bevor die
 * {@code settings.getEnv()}-Map in die eigentliche Gradle-Tooling-API-Operation übernommen wird
 * (Methode {@code setupEnvironment} in {@code GradleExecutionHelper}). Env-Variablen, die hier per
 * {@code addEnvironmentVariable(...)} ergänzt werden, landen dadurch garantiert im gestarteten
 * Gradle-Prozess — im Gegensatz zu {@code configureOperation}, das erst danach läuft.
 */
public class InjectIntoGradleProcess implements GradleExecutionHelperExtension{
    @Override
    public void configureSettings(@NotNull GradleExecutionSettings settings, @NotNull GradleExecutionContext context)
    {
        if (!Cache.getInstance().isRunConfigInjectionEnabled() || !TokenManager.getInstance().isTokenValid()) {
            return;
        }

        try {
            Cache.getInstance().setCache(context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for(Map.Entry<String,String> environmentVar : Cache.getInstance().getSecrets().entrySet())
        {
            settings.addEnvironmentVariable(environmentVar.getKey(), environmentVar.getValue());
        }
    }
}
