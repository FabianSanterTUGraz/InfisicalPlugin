package com.abuscom.infisicalplugin.infisical.injectSecrets.python;

import com.abuscom.infisicalplugin.infisical.injectSecrets.InjectSecretsSettings;
import com.abuscom.infisicalplugin.infisical.injectSecrets.InjectSecretsSettingsEditor;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.openapi.options.SettingsEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.jetbrains.python.run.AbstractPythonRunConfiguration;
import com.jetbrains.python.run.PythonRunConfigurationExtension;

public class InjectSecretsRunConfigurationsExtensionPython extends PythonRunConfigurationExtension {
    @Override
    protected String getEditorTitle() {
        return "Infisical";
    }

    @Override
    public boolean isApplicableFor(@NotNull AbstractPythonRunConfiguration<?> abstractPythonRunConfiguration) {
        return abstractPythonRunConfiguration instanceof AbstractPythonRunConfiguration;
    }

    @Override
    public boolean isEnabledFor(@NotNull AbstractPythonRunConfiguration<?> abstractPythonRunConfiguration, @Nullable RunnerSettings runnerSettings) {
        return InjectSecretsSettings.getOrCreate(abstractPythonRunConfiguration).enabled;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <P extends AbstractPythonRunConfiguration<?>> SettingsEditor<P> createEditor(@NotNull P configuration) {
        return (SettingsEditor<P>) (SettingsEditor<?>) new InjectSecretsSettingsEditor();
    }

    @Override
    protected void patchCommandLine(@NotNull AbstractPythonRunConfiguration<?> abstractPythonRunConfiguration, @Nullable RunnerSettings runnerSettings, @NotNull GeneralCommandLine generalCommandLine, @NotNull String s) throws ExecutionException {

    }

}
