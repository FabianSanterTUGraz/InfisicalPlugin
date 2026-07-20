package com.abuscom.infisicalplugin.infisical.cache;

import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.intellij.openapi.project.Project;
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionContext;

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
    public void setCache(GradleExecutionContext context) throws IOException {
        Project project = context.getProject();
        config = readConfig(project);

        String projectID = config.get("workspaceId");
        String token = TokenManager.getInstance().getTokenFromKeypass();
        String changedEnv = environment;

        environment = runConfigSelectedEnvironment != null ? runConfigSelectedEnvironment : config.get("defaultEnvironment");

        if(changedEnv.equals(environment))
        {
            return;
        }

        secrets.clear();

        try {
            InfisicalHttpClient httpClient = new InfisicalHttpClient(InfisicalHttpClient.DEFAULT_BASE_URL);
            SecretClient secretsClient = new SecretClient(httpClient);
            SecretsAPICallResponse response = secretsClient.secrets(projectID, environment,token);

            for(SecretEntry entry : response.secrets()){
                secrets.put(entry.secretKey(),entry.secretValue());
            }
        }
        catch (InfisicalHttpException e) {
            System.out.println("Error in fetching the secrets!!");
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

    public Instant getTimeStamp()
    {
        return timeStamp;
    }

    public void clearCache()
    {
        secrets.clear();
    }

    //security issue nur debug:
    public void printCache()
    {
        //secrets.forEach((key,value) -> System.out.println(key + "==" + value));
    }
}
