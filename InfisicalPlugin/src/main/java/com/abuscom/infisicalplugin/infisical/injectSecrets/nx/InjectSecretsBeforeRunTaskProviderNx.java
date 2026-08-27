package com.abuscom.infisicalplugin.infisical.injectSecrets.nx;

import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.util.Key;
import dev.nx.console.run.NxCommandConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public boolean executeTask(@NotNull DataContext context, @NotNull RunConfiguration configuration,
                                @NotNull ExecutionEnvironment env, @NotNull InjectSecretsBeforeRunTaskNx task) {
        return true;
    }
}
