package com.abuscom.infisicalplugin.infisical.injectSecrets.nx;

import com.abuscom.infisicalplugin.errorMessages.ErrorNotifier;
import com.abuscom.infisicalplugin.infisical.cache.Cache;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.util.Key;
import dev.nx.console.run.NxCommandConfiguration;
import dev.nx.console.run.NxRunSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.Promise;
import org.jetbrains.concurrency.Promises;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class InjectSecretsBeforeRunTaskProviderNx extends BeforeRunTaskProvider<InjectSecretsBeforeRunTaskNx> {

    public static final Key<InjectSecretsBeforeRunTaskNx> ID = Key.create("Infisical.InjectSecrets.Nx");

    @Override
    public Key<InjectSecretsBeforeRunTaskNx> getId() {
        return ID;
    }

    @Override
    public String getName() {
        return "Infisical: Secrets injizieren";
    }

    @Override
    public @Nullable InjectSecretsBeforeRunTaskNx createTask(@NotNull RunConfiguration runConfiguration) {
        if (!(runConfiguration instanceof NxCommandConfiguration)) {
            return null;
        }
        return new InjectSecretsBeforeRunTaskNx(ID);
    }

    @Override
    public boolean isConfigurable() {
        return true;
    }

    @Override
    public Promise<Boolean> configureTask(@NotNull DataContext context,
                                           @NotNull RunConfiguration configuration,
                                           @NotNull InjectSecretsBeforeRunTaskNx task) {
        InjectSecretsBeforeRunTaskDialogNx dialog = new InjectSecretsBeforeRunTaskDialogNx(
                configuration.getProject(), task.project, task.environment);

        boolean confirmed = dialog.showAndGet();
        if (confirmed) {
            task.project = dialog.getSelectedProject();
            task.projectId = dialog.getSelectedProjectId();
            task.environment = dialog.getSelectedEnvironment();
        }
        return Promises.resolvedPromise(confirmed);
    }

    /**
     * Reicht nur die Auswahl (enabled + Environment) an {@link Cache} weiter, analog zu
     * {@link com.abuscom.infisicalplugin.infisical.injectSecrets.gradle.InjectSecretsRunConfigListenerJava}.
     * Der eigentliche Guard-Check + API-Call passiert erst an der Injection-Stelle selbst
     * ({@link InjectSecretsRunConfigurationsExtensionNx#patchCommandLine}), sonst wuerden die
     * Secrets zweimal geholt.
     */
    @Override
    public boolean executeTask(@NotNull DataContext context, @NotNull RunConfiguration configuration,
                                @NotNull ExecutionEnvironment env, @NotNull InjectSecretsBeforeRunTaskNx task) {
        Cache.getInstance().setRunConfigSelection(true, task.projectId, task.environment);
        NxCommandConfiguration config = (NxCommandConfiguration) configuration;
        NxRunSettings currentSettings = config.getNxRunSettings();

        EnvironmentVariablesData environmentVariablesData = currentSettings.getEnvironmentVariables();
        Map<String,String> merging = new HashMap<>(environmentVariablesData.getEnvs());

        try {
            Cache.getInstance().setCache(configuration.getProject());
        } catch (IOException | InfisicalHttpException e) {
            ErrorNotifier.notify(configuration.getProject(), e);
        }

        for (Map.Entry<String, String> secret : Cache.getInstance().getSecrets().entrySet()) {
            merging.putIfAbsent(secret.getKey(), secret.getValue());
        }

        EnvironmentVariablesData newData = EnvironmentVariablesData.create(merging,environmentVariablesData.isPassParentEnvs());

        currentSettings.setEnvironmentVariables(newData);

        return true;
    }
}
