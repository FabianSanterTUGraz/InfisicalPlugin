package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.abuscom.infisicalplugin.errorMessages.ErrorNotifier;
import com.abuscom.infisicalplugin.infisical.cache.Cache;
import com.abuscom.infisicalplugin.infisical.cache.EnviromentsAPICallRequest;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.javascript.nodejs.execution.AbstractNodeTargetRunProfile;
import com.intellij.javascript.nodejs.execution.NodeTargetRun;
import com.intellij.javascript.nodejs.execution.runConfiguration.AbstractNodeRunConfigurationExtension;
import com.intellij.javascript.nodejs.execution.runConfiguration.NodeRunConfigurationLaunchSession;
import com.intellij.openapi.options.SettingsEditor;
import org.jetbrains.annotations.NotNull;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import org.jdom.Element;

import java.io.IOException;


/**
 * Hookt sich über den Node.js-spezifischen Extension-Point
 * {@code JavaScript.nodeRunConfigurationExtension} ein (Targets API) - NICHT über den
 * generischen {@code com.intellij.runConfigurationExtension}/{@code patchCommandLine}, den
 * npm-/Node-Run-Configs nicht aufrufen.
 */
public class InjectIntoNpmProcess extends AbstractNodeRunConfigurationExtension {
    @Override
    public boolean isApplicableFor(@NotNull AbstractNodeTargetRunProfile configuration) {
        // TODO: true fuer alle, oder z.B. per Cache.isRunConfigInjectionEnabled() einschraenken?
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <P extends AbstractNodeTargetRunProfile> SettingsEditor<P> createEditor(@NotNull P configuration) {
        return (SettingsEditor<P>) (SettingsEditor<?>) new InjectSecretsSettingsEditor();
    }

    @Override
    public String getEditorTitle() {
        return "Infisical";
    }

    @Override
    protected void readExternal(@NotNull AbstractNodeTargetRunProfile configuration, @NotNull Element element) {
        InjectSecretsSettings.readExternal(configuration, element);
    }

    @Override
    protected void writeExternal(@NotNull AbstractNodeTargetRunProfile configuration, @NotNull Element element) {
        InjectSecretsSettings.writeExternal(configuration, element);
    }

    @Override
    public NodeRunConfigurationLaunchSession createLaunchSession(
            @NotNull AbstractNodeTargetRunProfile configuration,
            @NotNull ExecutionEnvironment environment) throws ExecutionException {
        return new NodeRunConfigurationLaunchSession() {
            @Override
            public void addNodeOptionsTo(@NotNull NodeTargetRun targetRun) throws ExecutionException {

                if(!Cache.getInstance().infisicalJsonExists(configuration.getProject()))
                {
                    ErrorNotifier.notify(configuration.getProject(),"No json file given in the root!");
                    return;
                }
                else if (!TokenManager.getInstance().isTokenValid()) {
                    ErrorNotifier.notify(configuration.getProject(),"No valid jwt-Token given!(not logged in or expired)");
                    return;
                }

                try {
                    Cache.getInstance().setCache(targetRun.getProject());
                } catch (IOException | InfisicalHttpException e) {
                    ErrorNotifier.notify(targetRun.getProject(), e);
                    return;
                }
                EnvironmentVariablesData data = EnvironmentVariablesData.create(Cache.getInstance().getSecrets(),true);
                targetRun.setEnvData(data);
            }
        };
    }
}
