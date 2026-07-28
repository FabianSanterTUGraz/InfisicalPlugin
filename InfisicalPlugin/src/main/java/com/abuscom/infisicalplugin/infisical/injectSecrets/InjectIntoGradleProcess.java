package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.intellij.execution.process.ProcessOutputType;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTask;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.service.internal.ExternalSystemProcessingManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionContext;
import org.jetbrains.plugins.gradle.service.project.GradleExecutionHelperExtension;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;

import com.abuscom.infisicalplugin.errorMessages.ErrorNotifier;
import com.abuscom.infisicalplugin.infisical.cache.Cache;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * Hookt sich über den Erweiterungspunkt {@code org.jetbrains.plugins.gradle.executionHelperExtension}
 *
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

        if(!Cache.getInstance().infisicalJsonExists(context.getProject()))
        {
            ErrorNotifier.notify(context.getProject(),"No json file given in the root!");
            return;
        }
        else if (!TokenManager.getInstance().isTokenValid()) {
            ExternalSystemTaskId id = context.getTaskId();
            ExternalSystemTask task = ExternalSystemProcessingManager.getInstance().findTask(id);
            if(task != null) {
                task.cancel();
            }
            ErrorNotifier.notify(context.getProject(),"No valid jwt-Token given!(not logged in or expired)");
            return;
        }

        try {
            Cache.getInstance().setCache(context.getProject());
        } catch (IOException | InfisicalHttpException e) {
            ErrorNotifier.notify(context.getProject(), e);
            return;
        }

        for(Map.Entry<String,String> environmentVar : Cache.getInstance().getSecrets().entrySet())
        {
            settings.addEnvironmentVariable(environmentVar.getKey(), environmentVar.getValue());
        }
    }
}
