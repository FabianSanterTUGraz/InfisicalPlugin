package com.abuscom.infisicalplugin.infisical.cache.Secrets;

import com.abuscom.infisicalplugin.infisical.http.HttpApiResponse;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.google.gson.Gson;

import java.util.Map;


public class SecretClient {

    private static final String SECRETS_PATH = "/api/v4/secrets";
    private final InfisicalHttpClient httpClient;
    private final Gson gson = new Gson();
    public SecretClient(InfisicalHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public SecretsAPICallResponse secrets(String projectID, String enviroment, String token) throws InfisicalHttpException
    {
        HttpApiResponse response = httpClient.send(
                "GET",
                SECRETS_PATH
                + "?projectId=" + projectID + "&environment=" + enviroment,
                Map.of("Content-Type", "application/json","Authorization","Bearer " + token),
                null
        );
        return gson.fromJson(response.body(), SecretsAPICallResponse.class);
    }

    public SecretsAPICallResponse fetchMetadata(String projectID, String enviroment, String token) throws InfisicalHttpException
    {
        HttpApiResponse response = httpClient.send(
                "GET",
                SECRETS_PATH
                        + "?projectId=" + projectID + "&environment=" + enviroment +"&viewSecretValue=false&expandSecretReferences=false",
                Map.of("Content-Type", "application/json","Authorization","Bearer " + token),
                null
        );
        return gson.fromJson(response.body(), SecretsAPICallResponse.class);
    }
}
