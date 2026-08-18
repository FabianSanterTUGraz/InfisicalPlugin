package com.abuscom.infisicalplugin.infisical.cache;

import com.abuscom.infisicalplugin.infisical.cache.Secrets.*;
import com.abuscom.infisicalplugin.infisical.cache.Secrets.Tagging.TagListRequest;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.*;
import java.util.regex.Pattern;

public class Cache {
    private static final Logger LOG = Logger.getInstance(Cache.class);
    private static final Cache INSTANCE = new Cache();
    private final Map<String,String> secrets = new HashMap<>();
    private Map<String,String> config;
    private String environment = "";
    private boolean runConfigInjectionEnabled = false;
    private String runConfigSelectedEnvironment;
    private final String SLUG_NAME = "specificpaths";
    private final String INFISICAL_JSON = ".infisical.json";

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

    public String getCurrentEnviroment()
    {
        return environment;
    }

    // make the actual secrets api call and copy .env into cache but only of the current environment.
    public void setCache(Project project) throws IOException, InfisicalHttpException {
        config = readConfig(project,INFISICAL_JSON);
        SecretClient secretClient= new SecretClient(new InfisicalHttpClient(InfisicalHttpClient.DEFAULT_BASE_URL));
        String projectID = config.get("workspaceId");
        String token = TokenManager.getInstance().getTokenFromKeypass();
        String newEnvironment = runConfigSelectedEnvironment != null ? runConfigSelectedEnvironment : config.get("defaultEnvironment");
        applyEnvironment(projectID, newEnvironment, token, secretClient);
        applyLocalEnvironment(project);
    }

    void applyEnvironment(String projectID, String newEnvironment, String token, SecretClient secretClient) throws InfisicalHttpException {
        environment = newEnvironment;
        secrets.clear();

        TagListRequest tag = resolveMachineSpecificTag(projectID, token, secretClient);

        SecretsAPICallResponse response = secretClient.secrets(projectID, newEnvironment, token);

        for (SecretEntry entry : response.secrets()) {
            boolean alreadyTagged = entry.tags() != null
                    && entry.tags().stream().anyMatch(t -> t.slug().equals(SLUG_NAME));

            if(tag != null && !alreadyTagged && looksLikeUserSpecificPath(entry.secretValue()))
            {
                try {
                    secretClient.tagVariable(projectID,entry.secretKey(),environment,token,tag.id());
                } catch (InfisicalHttpException e) {
                    LOG.warn("Konnte Secret '" + entry.secretKey() + "' nicht mit '" + SLUG_NAME + "' taggen", e);
                }
            }

            secrets.put(entry.secretKey(), entry.secretValue());
        }
    }

    private TagListRequest resolveMachineSpecificTag(String projectID, String token, SecretClient secretClient) {
        try {
            Optional<TagListRequest> existingTag = secretClient.findTagBySlug(projectID, SLUG_NAME, token);
            return existingTag.isPresent() ? existingTag.get() : secretClient.createTag(projectID, SLUG_NAME, "RED", token);
        } catch (InfisicalHttpException e) {
            LOG.warn("Konnte Tag '" + SLUG_NAME + "' nicht abrufen/anlegen", e);
            return null;
        }
    }


    public void applyLocalEnvironment(Project project) throws IOException {
        Path localInfisicalJson = Paths.get(Objects.requireNonNull(project.getBasePath()),".infisical.local.json");
        if (!Files.exists(localInfisicalJson)) {
            return;
        }

        Map<String, String> localOverrides = readConfig(project, ".infisical.local.json");
        if (localOverrides == null) {
            localOverrides = new HashMap<>();
        }

        applyOverrides(localOverrides);
    }

    private void applyOverrides(Map<String, String> overrides) {
        overrides.forEach((key, value) -> {
            if (value != null && !value.isBlank()) {
                secrets.put(key, value);
            }
        });
    }

    public static Map<String,String> readConfig(Project project,String jsonPath) throws IOException
    {
        Path configPath = Paths.get(Objects.requireNonNull(project.getBasePath()),jsonPath);
        String JSON = Files.readString(configPath);
        return new Gson().fromJson(JSON,new TypeToken<Map<String, String>>(){}.getType());
    }

    public static void writeConfig(Project project,String jsonPath,String newWorkspaceId) throws IOException
    {
        Map<String,String> file = readConfig(project,".infisical.json");
        file.put("workspaceId", newWorkspaceId);
        Files.writeString(Paths.get(project.getBasePath(),jsonPath),new Gson().toJson(file));
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

    public boolean infisicalJsonExists(Project project)
    {
        return Files.exists(Paths.get(Objects.requireNonNull(project.getBasePath()), ".infisical.json"));
    }

    private static final Pattern USER_SPECIFIC_PATH = Pattern.compile(
            "(?i)^(?:file:/{0,3})?(?:[a-z]:[\\\\/]|/).+"
    );

    public static boolean looksLikeUserSpecificPath(String value) {
        return value != null && USER_SPECIFIC_PATH.matcher(value).find();
    }
}
