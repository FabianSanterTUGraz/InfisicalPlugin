package com.abuscom.infisicalplugin.infisical.injectSecrets;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionContext;
import org.jetbrains.plugins.gradle.service.project.GradleExecutionHelperExtension;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;

public class InjectIntoGradleProcess implements GradleExecutionHelperExtension{
    @Override
    public void configureSettings(GradleExecutionSettings settings, @NotNull GradleExecutionContext context)
    {
        settings.addEnvironmentVariable("GITLAB_TOKEN","123");
        System.out.println("Methode-Wird-Aufgerufen!");
    }
}
