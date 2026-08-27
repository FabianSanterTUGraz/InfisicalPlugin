package com.abuscom.infisicalplugin.infisical.injectSecrets.nx;

import com.intellij.execution.BeforeRunTask;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

public class InjectSecretsBeforeRunTaskNx extends BeforeRunTask<InjectSecretsBeforeRunTaskNx> {
    protected InjectSecretsBeforeRunTaskNx(@NotNull Key<InjectSecretsBeforeRunTaskNx> providerId) {
        super(providerId);
    }
}
