package com.abuscom.infisicalplugin.infisical.cache;

import com.abuscom.infisicalplugin.infisical.http.HttpApiResponse;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.google.gson.Gson;
import com.intellij.openapi.project.Project;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Map;

public class CurrentEnviroments {
    private static final String ENVIROMENT_PATH = "/api/v1/workspace/";
    private final InfisicalHttpClient httpClient;
    private final Gson gson = new Gson();
    public CurrentEnviroments(InfisicalHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public EnviromentsAPICallResponse enviroments(Project project, String token) throws InfisicalHttpException, IOException {
        Map<String,String> config = Cache.readConfig(project);
        String projectID = config.get("workspaceId");

        HttpApiResponse response = httpClient.send(
                "GET",
                ENVIROMENT_PATH
                        + projectID,
                Map.of("Content-Type", "application/json","Authorization","Bearer " + token),
                null
        );
        return gson.fromJson(response.body(), (Type) EnviromentsAPICallResponse.class);
    }
}
