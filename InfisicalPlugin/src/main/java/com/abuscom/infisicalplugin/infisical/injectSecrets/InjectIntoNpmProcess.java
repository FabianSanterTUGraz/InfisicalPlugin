package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.abuscom.infisicalplugin.infisical.cache.Cache;
import com.abuscom.infisicalplugin.infisical.cache.EnviromentsAPICallRequest;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.javascript.nodejs.execution.AbstractNodeTargetRunProfile;
import com.intellij.javascript.nodejs.execution.NodeTargetRun;
import com.intellij.javascript.nodejs.execution.runConfiguration.AbstractNodeRunConfigurationExtension;
import com.intellij.javascript.nodejs.execution.runConfiguration.NodeRunConfigurationLaunchSession;
import org.jetbrains.annotations.NotNull;
import com.intellij.execution.configuration.EnvironmentVariablesData;

import java.io.IOException;
import java.util.Map;


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
    public NodeRunConfigurationLaunchSession createLaunchSession(
            @NotNull AbstractNodeTargetRunProfile configuration,
            @NotNull ExecutionEnvironment environment) throws ExecutionException {
        return new NodeRunConfigurationLaunchSession() {
            @Override
            public void addNodeOptionsTo(@NotNull NodeTargetRun targetRun) throws ExecutionException {
                //Cache.getInstance().setCache();
                Map<String,String> test = Cache.getInstance().getSecrets();
                EnvironmentVariablesData data = EnvironmentVariablesData.create(test,true);
                targetRun.configureEnvironment(data);
            }
        };
    }
}
