package com.abuscom.infisicalplugin.infisical.cache.Secrets;

import com.abuscom.infisicalplugin.infisical.cache.Secrets.ListProjects.ListProjectsResponse;
import com.abuscom.infisicalplugin.infisical.cache.Secrets.Tagging.TagListResponse;
import com.abuscom.infisicalplugin.infisical.cache.Secrets.Tagging.TagListRequest;
import com.abuscom.infisicalplugin.infisical.http.HttpApiResponse;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.google.gson.Gson;

import java.util.List;
import java.util.Map;
import java.util.Optional;


public class SecretClient {

    private static final String SECRETS_PATH = "/api/v4/secrets";
    private static final String PROJECTS_PATH = "/api/v1/projects";
    //hardcoded falls sich der Tag-Slug für Overrides ändern sollte hier anpassen:
    private static final String OVERRIDE_TAG_SLUG = "specificpaths";

    private final InfisicalHttpClient httpClient;
    private final Gson gson = new Gson();
    public SecretClient(InfisicalHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public SecretsAPICallResponse secrets(String projectID, String environment, String token) throws InfisicalHttpException
    {
        return secrets(projectID, environment, token, true);
    }

    private SecretsAPICallResponse secrets(String projectID, String environment, String token, boolean includePersonalOverrides) throws InfisicalHttpException
    {
        HttpApiResponse response = httpClient.send(
                "GET",
                SECRETS_PATH
                + "?projectId=" + projectID + "&environment=" + environment + "&includePersonalOverrides=" + includePersonalOverrides,
                Map.of("Content-Type", "application/json","Authorization","Bearer " + token),
                null
        );
        return gson.fromJson(response.body(), SecretsAPICallResponse.class);
    }

    /**
     * Personal-Override-Einträge in der Response von includePersonalOverrides=true tragen die
     * Tags des zugrundeliegenden shared Secrets offenbar nicht mit (von Infisical nicht
     * dokumentiert, aber empirisch beobachtet: ein überschriebenes Secret verschwand sonst aus
     * dieser Liste). Deshalb wird die Tag-Zugehörigkeit über den shared-Abruf (ohne Overrides)
     * bestimmt und der aktuelle Wert (inkl. Override) separat dazugemischt.
     */
    public List<SecretEntry> secretsWithTag(String projectID, String environment, String token) throws InfisicalHttpException
    {
        SecretsAPICallResponse sharedOnly = secrets(projectID, environment, token, false);
        List<String> taggedKeys = sharedOnly.secrets().stream()
                .filter(s -> s.tags() != null && s.tags().stream().anyMatch(t -> t.slug().equals(OVERRIDE_TAG_SLUG)))
                .map(SecretEntry::secretKey)
                .toList();

        SecretsAPICallResponse withOverrides = secrets(projectID, environment, token, true);
        return withOverrides.secrets().stream()
                .filter(s -> taggedKeys.contains(s.secretKey()))
                .toList();
    }

    public SecretsAPICallResponse fetchMetadata(String projectID, String environment, String token) throws InfisicalHttpException
    {
        HttpApiResponse response = httpClient.send(
                "GET",
                PROJECTS_PATH
                        + "?projectId=" + projectID + "&environment=" + environment +"&viewSecretValue=false&expandSecretReferences=false",
                Map.of("Content-Type", "application/json","Authorization","Bearer " + token),
                null
        );
        return gson.fromJson(response.body(), SecretsAPICallResponse.class);
    }

    public TagListRequest createTag(String projectId, String slug, String color, String token) throws InfisicalHttpException
    {
        String body = gson.toJson(Map.of("slug", slug, "color", color));

        HttpApiResponse response = httpClient.send(
                "POST",
                PROJECTS_PATH + "/" + projectId + "/tags",
                Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token),
                body
        );
        return gson.fromJson(response.body(), TagListRequest.class);
    }

    public void tagVariable(String projectID, String variableName , String environment, String token,String tagId) throws InfisicalHttpException
    {
        String body = gson.toJson(Map.of("projectId",projectID, "environment", environment, "tagIds", List.of(tagId)));

        HttpApiResponse response = httpClient.send(
                "PATCH",
                SECRETS_PATH
                       + "/" + variableName,
                Map.of("Content-Type", "application/json","Authorization","Bearer " + token),
                body
        );
    }

    public Optional<TagListRequest> findTagBySlug(String projectID, String slug, String token) throws InfisicalHttpException
    {
        HttpApiResponse response = httpClient.send(
                "GET",
                PROJECTS_PATH + "/" + projectID + "/tags",
                Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token),
                null
        );
        TagListResponse list = gson.fromJson(response.body(), TagListResponse.class);
        return list.tags().stream()
                .filter(t -> t.slug().equals(slug))
                .findFirst();
    }

    public ListProjectsResponse listProjects(String token) throws InfisicalHttpException
    {
        HttpApiResponse response = httpClient.send("GET", PROJECTS_PATH, Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token),
                null);
        return gson.fromJson(response.body(), ListProjectsResponse.class);
    }

    public void createEnvironment(String projectID, String newName, String token) throws InfisicalHttpException
    {
        String body = gson.toJson(Map.of("name", newName, "slug", newName, "position", 1));

        httpClient.send(
                "POST",
                PROJECTS_PATH + "/" + projectID + "/environments",
                Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token),
                body
        );
    }

    public void setOverride(String projectID, String secretName, String environment, String value, String token) throws InfisicalHttpException
    {
        String body = gson.toJson(Map.of(
                "projectId", projectID,
                "environment", environment,
                "type", "personal",
                "secretValue", value,
                "secretPath", "/"
        ));

        com.abuscom.infisicalplugin.infisical.http.HttpApiResponse response = httpClient.send("PATCH", SECRETS_PATH + "/" + secretName, Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token),
                body);
    }

    public void createOverride(String projectID, String secretName, String environment, String value, String token) throws InfisicalHttpException
    {
        String body = gson.toJson(Map.of(
                "projectId", projectID,
                "environment", environment,
                "type", "personal",
                "secretValue", value,
                "secretPath", "/"
        ));

        com.abuscom.infisicalplugin.infisical.http.HttpApiResponse response = httpClient.send("POST", SECRETS_PATH + "/" + secretName, Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token),
                body);
    }
}
