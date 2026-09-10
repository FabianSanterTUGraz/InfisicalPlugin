package com.abuscom.infisicalplugin.infisical.injectSecrets.nx;

import com.abuscom.infisicalplugin.errorMessages.ErrorNotifier;
import com.abuscom.infisicalplugin.infisical.cache.Cache;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.abuscom.infisicalplugin.infisical.injectSecrets.InjectSecretsSettings;
import com.abuscom.infisicalplugin.infisical.injectSecrets.InjectSecretsSettingsEditor;
import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.intellij.openapi.options.SettingsEditor;
import dev.nx.console.run.NxCommandConfiguration;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.io.IOException;
import java.util.Map;

public class InjectSecretsRunConfigurationsExtensionNx extends RunConfigurationExtension {
    @Override
    public <T extends RunConfigurationBase<?>> void updateJavaParameters(@NonNull T t, @NotNull JavaParameters javaParameters, @Nullable RunnerSettings runnerSettings) throws ExecutionException {

    }

    /**
     * Hypothese: NxCommandConfiguration startet ihren Prozess ueber eine GeneralCommandLine-basierte
     * CommandLineState, wodurch die Plattform diese Methode automatisch vor dem Prozessstart aufruft
     * (wie z.B. bei einfachen Application-/External-Tool-Run-Configs). Ob das bei Nx Console
     * tatsaechlich so ist, laesst sich mangels Source des Plugins nicht ohne Test verifizieren -
     * falls hier nichts ankommt, braucht Nx einen eigenen Extension-Point wie Gradle
     * ({@link com.abuscom.infisicalplugin.infisical.injectSecrets.gradle.InjectIntoGradleProcess})
     * oder Node ({@link com.abuscom.infisicalplugin.infisical.injectSecrets.node.InjectIntoNpmProcess}).
     */
    @Override
    protected void patchCommandLine(@NotNull RunConfigurationBase configuration,
                                     RunnerSettings runnerSettings,
                                     @NotNull GeneralCommandLine cmdLine,
                                     @NotNull String runnerId) throws ExecutionException {
    }

    @Override
    public boolean isApplicableFor(@NonNull RunConfigurationBase<?> runConfigurationBase) {
        return runConfigurationBase instanceof NxCommandConfiguration;
    }

    @Override
    protected String getEditorTitle() {
        return "Infisical";
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <P extends RunConfigurationBase<?>> SettingsEditor<P> createEditor(@NotNull P configuration) {
        return (SettingsEditor<P>) new InjectSecretsSettingsEditor();
    }

    @Override
    protected void readExternal(@NotNull RunConfigurationBase<?> configuration, @NotNull Element element) {
        InjectSecretsSettings.readExternal(configuration, element);
    }

    @Override
    protected void writeExternal(@NotNull RunConfigurationBase<?> configuration, @NotNull Element element) {
        InjectSecretsSettings.writeExternal(configuration, element);
    }
}