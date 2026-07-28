package com.abuscom.infisicalplugin.infisical.cache;

import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.project.Project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.*;
import java.util.regex.Pattern;

public class Cache {
    private static final Cache INSTANCE = new Cache();
    private final Map<String,String> secrets = new HashMap<>();
    private final Map<String,Integer> secretVersions = new HashMap<>();
    private Map<String,String> config;
    private String environment = "";
    private boolean hasFetched = false;
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
        config = readConfig(project,".infisical.json");
        SecretClient secretClient= new SecretClient(new InfisicalHttpClient(InfisicalHttpClient.DEFAULT_BASE_URL));
        String projectID = config.get("workspaceId");
        String token = TokenManager.getInstance().getTokenFromKeypass();
        String newEnvironment = runConfigSelectedEnvironment != null ? runConfigSelectedEnvironment : config.get("defaultEnvironment");

        applyEnvironment(projectID, newEnvironment, token, secretClient);
        applyLocalEnvironment(project);
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

        SecretsAPICallResponse metadata = secretClient.fetchMetadata(projectID, newEnvironment, token);
        boolean versionChanged = hasVersionChanged(metadata);
        updateSecretVersions(metadata);

        if (hasFetched && Objects.equals(previousEnvironment, newEnvironment) && !versionChanged) {
            return;
        }
        hasFetched = true;

        secrets.clear();

        SecretsAPICallResponse response = secretClient.secrets(projectID, newEnvironment, token);

        for (SecretEntry entry : response.secrets()) {
            secrets.put(entry.secretKey(), entry.secretValue());
        }
    }


    public void applyLocalEnvironment(Project project) throws IOException {
        Path localInfisicalJson = Paths.get(Objects.requireNonNull(project.getBasePath()),".infisical.local.json");
        Map<String,String> localOverrides;
        if (Files.exists(localInfisicalJson)) {
            localOverrides = readConfig(project,".infisical.local.json");
            if(localOverrides == null)
            {
                localOverrides = new HashMap<>();
            }
        }
        else
        {
            localOverrides = new HashMap<>();
        }

        Set<String> missingKeys = findMissingUserSpecificKeys(localOverrides);
        if(!missingKeys.isEmpty())
        {
            appendMissingKeys(localInfisicalJson, localOverrides, missingKeys);
            throw new IOException("Lokalen Ovveride bitte ausfüllen!!");
        }

        applyOverrides(localOverrides);
    }

    private Set<String> findMissingUserSpecificKeys(Map<String, String> localOverrides) {
        Set<String> missing = new HashSet<>();
        for (String key : secrets.keySet()) {
            if (looksLikeUserSpecificPath(secrets.get(key)) && !localOverrides.containsKey(key)) {
                missing.add(key);
            }
        }
        return missing;
    }

    private void applyOverrides(Map<String, String> overrides) {
        overrides.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                secrets.put(key, value);
            }
        });
    }

    private void appendMissingKeys(Path localInfisicalJson, Map<String, String> localOverrides, Set<String> missingKeys) throws IOException {
        for (String key : missingKeys) {
            localOverrides.put(key, "");
        }
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(localOverrides);
        Files.writeString(localInfisicalJson, json);
    }

    public static Map<String,String> readConfig(Project project,String jsonPath) throws IOException
    {
        Path configPath = Paths.get(Objects.requireNonNull(project.getBasePath()),jsonPath);
        String JSON = Files.readString(configPath);
        return new Gson().fromJson(JSON,new TypeToken<Map<String, String>>(){}.getType());
    }

    public Map<String,String> getSecrets()
    {
        return secrets;
    }

    boolean hasVersionChanged(SecretsAPICallResponse metadata)
    {
        for (SecretEntry entry : metadata.secrets()) {
            if(entry.version() != getSecretVersion(entry.secretKey()))
            {
                return true;
            }
        }
        return false;
    }

    public int getSecretVersion(String secretKey)
    {
        return secretVersions.getOrDefault(secretKey, -1);
    }

    private void updateSecretVersions(SecretsAPICallResponse metadata)
    {
        for (SecretEntry entry : metadata.secrets()) {
            secretVersions.put(entry.secretKey(), entry.version());
        }
    }

    public void clearCache()
    {
        environment = "";
        hasFetched = false;
        Cache.getInstance().getSecrets().clear();
        secretVersions.clear();
    }

    public boolean infisicalJsonExists(Project project)
    {
        return Files.exists(Paths.get(Objects.requireNonNull(project.getBasePath()), ".infisical.json"));
    }

    private static final Pattern USER_SPECIFIC_PATH = Pattern.compile(
            "(?i)^[a-z]:[\\\\/]users[\\\\/][^\\\\/]+|^/users/[^/]+|^/home/[^/]+"
    );

    public static boolean looksLikeUserSpecificPath(String value) {
        return value != null && USER_SPECIFIC_PATH.matcher(value).find();
    }
}
