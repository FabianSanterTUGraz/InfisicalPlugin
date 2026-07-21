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

/**
 * Sorgt nur dafür, dass Checkbox + Dropdown unter "Modify options" der Gradle-Run-Configuration
 * auftauchen und in {@link InjectSecretsSettings} persistiert werden. Die eigentliche
 * Secret-Injektion passiert NICHT hier (siehe patchCommandLine unten), sondern in
 * {@link InjectIntoGradleProcess}, das über {@link InjectSecretsRunConfigListener} erfährt, ob und
 * mit welcher Umgebung injiziert werden soll.
 */
public class InjectSecretsRunConfigurationExtension extends RunConfigurationExtension {

    private static final String ELEMENT_NAME = "InfisicalSecretsInjection";
    private static final String ATTR_ENABLED = "enabled";
    private static final String ATTR_ENVIRONMENT = "environment";

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
        settings.selectedEnvironment = child != null
                ? child.getAttributeValue(ATTR_ENVIRONMENT, InjectSecretsSettings.ENVIRONMENTS[0])
                : InjectSecretsSettings.ENVIRONMENTS[0];
    }

    @Override
    protected void writeExternal(@NotNull RunConfigurationBase<?> configuration, @NotNull Element element) {
        InjectSecretsSettings settings = InjectSecretsSettings.getOrCreate(configuration);
        Element child = new Element(ELEMENT_NAME);
        child.setAttribute(ATTR_ENABLED, String.valueOf(settings.enabled));
        child.setAttribute(ATTR_ENVIRONMENT, settings.selectedEnvironment);
        element.addContent(child);
    }

    @Override
    protected void patchCommandLine(@NotNull RunConfigurationBase configuration,
                                     RunnerSettings runnerSettings,
                                     @NotNull GeneralCommandLine cmdLine,
                                     @NotNull String runnerId) throws ExecutionException {

    }

    @Override
    public <T extends RunConfigurationBase<?>> void updateJavaParameters(T configuration,
                                                                          JavaParameters javaParameters,
                                                                          RunnerSettings runnerSettings) throws ExecutionException {
        // empty just to supress error warning
    }
}
