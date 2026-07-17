package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import com.intellij.openapi.options.SettingsEditor;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import com.abuscom.infisicalplugin.infisical.cache.Cache;

public class InjectSecretsRunConfigurationExtension extends RunConfigurationExtension {

    private static final String ELEMENT_NAME = "InfisicalSecretsInjection";
    private static final String ATTR_ENABLED = "enabled";

    @Override
    public boolean isApplicableFor(@NotNull RunConfigurationBase<?> configuration) {
        return configuration instanceof ExternalSystemRunConfiguration;
    }

    @Override
    public boolean isEnabledFor(@NotNull RunConfigurationBase applicableConfiguration, RunnerSettings runnerSettings) {
        return InjectSecretsSettings.getOrCreate(applicableConfiguration).enabled;
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
    protected void readExternal(@NotNull RunConfigurationBase<?> configuration, @NotNull Element element) {
        Element child = element.getChild(ELEMENT_NAME);
        InjectSecretsSettings settings = InjectSecretsSettings.getOrCreate(configuration);
        settings.enabled = child != null && Boolean.parseBoolean(child.getAttributeValue(ATTR_ENABLED, "false"));
    }

    @Override
    protected void writeExternal(@NotNull RunConfigurationBase<?> configuration, @NotNull Element element) {
        Element child = new Element(ELEMENT_NAME);
        child.setAttribute(ATTR_ENABLED, String.valueOf(InjectSecretsSettings.getOrCreate(configuration).enabled));
        element.addContent(child);
    }

    @Override
    protected void patchCommandLine(@NotNull RunConfigurationBase configuration,
                                     RunnerSettings runnerSettings,
                                     @NotNull GeneralCommandLine cmdLine,
                                     @NotNull String runnerId) throws ExecutionException {
        cmdLine.getEnvironment().putAll(Cache.getInstance().getSecrets());
    }

    @Override
    public <T extends RunConfigurationBase<?>> void updateJavaParameters(T configuration,
                                                                          JavaParameters javaParameters,
                                                                          RunnerSettings runnerSettings) throws ExecutionException {
        // empty just to supress error warning
    }
}