package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.IconLoader;

import org.jetbrains.plugins.gradle.service.execution.GradleRunConfiguration;

import javax.swing.Icon;
import java.util.LinkedHashMap;
import java.util.Map;

public final class InjectBeforeRunTask extends BeforeRunTaskProvider<InjectBeforeRunTask.Task> {

    public static final Key<Task> ID = Key.create("InfisicalPlugin.InjectSecrets");

    @Override
    public Key<Task> getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "[CUSTOM]Infisical: Get ENV-variables.";
    }

    @Override
    public Icon getIcon() {
        return IconLoader.getIcon("META-INF/pluginIcon.svg", InjectBeforeRunTask.class);
    }

    @Override
    public Task createTask(RunConfiguration runConfiguration) {
        return runConfiguration instanceof GradleRunConfiguration ? new Task() : null;
    }

    @Override
    public boolean executeTask(DataContext context, RunConfiguration configuration,
                                ExecutionEnvironment environment, Task task) {
        if (!(configuration instanceof GradleRunConfiguration gradleConfig)) {
            return true;
        }

        Map<String, String> secrets = resolveSecrets(gradleConfig);
        if (secrets.isEmpty()) {
            return true;
        }

        ExternalSystemTaskExecutionSettings settings = gradleConfig.getSettings();
        Map<String, String> env = new LinkedHashMap<>(settings.getEnv());
        env.putAll(secrets);
        settings.setEnv(env);
        settings.setPassParentEnvs(true);
        return true;
    }

    // TODO(#5): an den echten Infisical-Secrets-Client anbinden, sobald der Secret-Abruf implementiert ist.
    private Map<String, String> resolveSecrets(GradleRunConfiguration configuration) {
        return Map.of();
    }

    public static final class Task extends BeforeRunTask<Task> {
        Task() {
            super(ID);
        }
    }
}