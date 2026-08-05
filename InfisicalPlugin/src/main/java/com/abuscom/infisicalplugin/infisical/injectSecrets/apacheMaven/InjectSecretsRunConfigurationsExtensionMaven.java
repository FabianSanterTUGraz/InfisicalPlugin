package com.abuscom.infisicalplugin.infisical.injectSecrets.apacheMaven;

import com.abuscom.infisicalplugin.errorMessages.ErrorNotifier;
import com.abuscom.infisicalplugin.infisical.injectSecrets.InjectSecretsSettings;
import com.abuscom.infisicalplugin.infisical.injectSecrets.InjectSecretsSettingsEditor;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.spring.boot.application.yaml.SpringBootApplicationYamlReplacementTokenCompletionContributor;
import org.jetbrains.idea.maven.execution.MavenRunConfiguration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class InjectSecretsRunConfigurationsExtensionMaven extends RunConfigurationExtension {
    @Override
    public boolean isApplicableFor(@NotNull RunConfigurationBase<?> runConfigurationBase) {
        return runConfigurationBase instanceof MavenRunConfiguration;
    }

    @Override
    public <T extends RunConfigurationBase<?>> void updateJavaParameters(@NotNull T t, @NotNull JavaParameters javaParameters, @Nullable RunnerSettings runnerSettings) throws ExecutionException {
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <P extends RunConfigurationBase<?>> SettingsEditor<P> createEditor(@NotNull P configuration) {
        return (SettingsEditor<P>) new InjectSecretsSettingsEditor();
    }

    @Override
    protected String getEditorTitle() {
        return "Infisical";
    }

    @Override
    public boolean isEnabledFor(@NotNull RunConfigurationBase applicableConfiguration, RunnerSettings runnerSettings) {
        return InjectSecretsSettings.getOrCreate(applicableConfiguration).enabled;
    }
}
