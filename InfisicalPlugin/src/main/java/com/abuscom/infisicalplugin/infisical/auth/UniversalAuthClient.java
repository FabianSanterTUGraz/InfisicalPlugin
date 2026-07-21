package com.abuscom.infisicalplugin.infisical.auth;

import com.abuscom.infisicalplugin.infisical.http.HttpApiResponse;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.google.gson.Gson;

import java.time.Instant;
import java.util.Map;

public class UniversalAuthClient {

    private static final String LOGIN_PATH = "/api/v1/auth/universal-auth/login";

    private final InfisicalHttpClient httpClient;
    private final Gson gson = new Gson();

    public UniversalAuthClient(InfisicalHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public AccessToken login(String clientId, String clientSecret) throws InfisicalHttpException {
        String requestJson = gson.toJson(new UniversalAuthLoginRequest(clientId, clientSecret));
        HttpApiResponse response = httpClient.send(
                "POST",
                LOGIN_PATH,
                Map.of("Content-Type", "application/json"),
                requestJson
        );

        UniversalAuthLoginResponse loginResponse = gson.fromJson(response.body(), UniversalAuthLoginResponse.class);
        return new AccessToken(loginResponse.accessToken());
    }
}
