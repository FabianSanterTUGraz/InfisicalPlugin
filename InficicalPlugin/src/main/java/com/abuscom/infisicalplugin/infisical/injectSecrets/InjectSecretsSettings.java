package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.util.Key;

import org.jetbrains.annotations.NotNull;

public class InjectSecretsSettings {

    public static final Key<InjectSecretsSettings> KEY =
            Key.create("com.abuscom.infisicalplugin.InjectSecretsSettings");

    public boolean enabled = false;

    public static InjectSecretsSettings getOrCreate(@NotNull RunConfigurationBase<?> config) {
        InjectSecretsSettings settings = config.getCopyableUserData(KEY);
        if (settings == null) {
            settings = new InjectSecretsSettings();
            config.putCopyableUserData(KEY, settings);
        }
        return settings;
    }
}