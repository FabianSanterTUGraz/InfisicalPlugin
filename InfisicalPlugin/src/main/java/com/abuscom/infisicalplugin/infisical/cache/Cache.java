package com.abuscom.infisicalplugin.infisical.cache;

import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.intellij.openapi.project.Project;
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionContext;
import com.intellij.openapi.project.Project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Cache {
    private static final Cache INSTANCE = new Cache();
    private final Map<String,String> secrets = new HashMap<>();
    private Instant timeStamp = Instant.now();
    private Map<String,String> config;
    private String environment = "";
    private boolean runConfigInjectionEnabled = false;
    private String runConfigSelectedEnvironment;

    private Cache(){}

    public static Cache getInstance()
    {
        return INSTANCE;
    }

    public void setRunConfigSelection(boolean enabled, String selectedEnvironment)
    {
        this.runConfigInjectionEnabled = enabled;
        this.runConfigSelectedEnvironment = selectedEnvironment;
    }

    public boolean isRunConfigInjectionEnabled()
    {
        return runConfigInjectionEnabled;
    }

    // make the actual secrets api call and copy .env into cache but only of the current enviroment.
    public void setCache(Project project) throws IOException, InfisicalHttpException {
        config = readConfig(project);

        String projectID = config.get("workspaceId");
        String token = TokenManager.getInstance().getTokenFromKeypass();
        String newEnvironment = runConfigSelectedEnvironment != null ? runConfigSelectedEnvironment : config.get("defaultEnvironment");

        applyEnvironment(projectID, newEnvironment, token, new SecretClient(new InfisicalHttpClient(InfisicalHttpClient.DEFAULT_BASE_URL)));
    }

    /**
     * Switches the cached environment and, if it actually changed, drops the previous
     * environment's secrets before fetching the new one's - so a run against environment B never
     * still sees leftover keys from an earlier run against environment A. Deliberately free of any
     * Project/GradleExecutionContext dependency so it's directly testable in plain JUnit (see
     * CacheEnvironmentSwitchTest) without needing a running IntelliJ Platform Application.
     */
    void applyEnvironment(String projectID, String newEnvironment, String token, SecretClient secretClient) throws InfisicalHttpException {
        String previousEnvironment = environment;
        environment = newEnvironment;

        if (previousEnvironment.equals(newEnvironment)) {
            return;
        }

        secrets.clear();

        SecretsAPICallResponse response = secretClient.secrets(projectID, newEnvironment, token);

        for (SecretEntry entry : response.secrets()) {
            secrets.put(entry.secretKey(), entry.secretValue());
        }
    }

    public static Map<String,String> readConfig(Project project) throws IOException
    {
        Path configPath = Paths.get(Objects.requireNonNull(project.getBasePath()),".infisical.json");
        String JSON = Files.readString(configPath);
        return new Gson().fromJson(JSON,new TypeToken<Map<String, String>>(){}.getType());
    }

    public Map<String,String> getSecrets()
    {
        return secrets;
    }

    public void clearCache()
    {
        environment = "";
        Cache.getInstance().getSecrets().clear();
    }
}
