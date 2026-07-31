package com.abuscom.infisicalplugin.infisical.cache.Secrets;

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

    private final InfisicalHttpClient httpClient;
    private final Gson gson = new Gson();
    public SecretClient(InfisicalHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public SecretsAPICallResponse secrets(String projectID, String environment, String token) throws InfisicalHttpException
    {
        HttpApiResponse response = httpClient.send(
                "GET",
                SECRETS_PATH
                + "?projectId=" + projectID + "&environment=" + environment,
                Map.of("Content-Type", "application/json","Authorization","Bearer " + token),
                null
        );
        return gson.fromJson(response.body(), SecretsAPICallResponse.class);
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

    public TagResponse createTag(String projectId, String slug, String color, String token) throws InfisicalHttpException
    {
        String body = gson.toJson(Map.of("slug", slug, "color", color));

        HttpApiResponse response = httpClient.send(
                "POST",
                PROJECTS_PATH + "/" + projectId + "/tags",
                Map.of("Content-Type", "application/json", "Authorization", "Bearer " + token),
                body
        );
        return gson.fromJson(response.body(), TagResponse.class);
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

    public Optional<TagResponse> findTagBySlug(String projectID, String slug, String token) throws InfisicalHttpException
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
}
