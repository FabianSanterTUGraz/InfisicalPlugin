
package com.abuscom.infisicalplugin.infisical.http;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InfisicalHttpClient {

    public static final String DEFAULT_BASE_URL = "https://app.infisical.com";

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);

    private final String baseUrl;
    private final HttpClient httpClient;

    public InfisicalHttpClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
    }

    public HttpApiResponse send(String method, String path, Map<String, String> headers, String body) throws InfisicalHttpException {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(REQUEST_TIMEOUT);
        headers.forEach(requestBuilder::header);

        HttpRequest.BodyPublisher bodyPublisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(body);
        requestBuilder.method(method, bodyPublisher);

        HttpResponse<String> response;
        try {
            response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new InfisicalHttpException("Request to " + path + " failed", e);
        }

        if (response.statusCode() >= 300) {
            throw new InfisicalHttpException(response.statusCode(), response.body());
        }

        return new HttpApiResponse(response.statusCode(), flattenHeaders(response.headers()), response.body());
    }

    private static Map<String, String> flattenHeaders(HttpHeaders headers) {
        Map<String, String> flattened = new LinkedHashMap<>();
        headers.map().forEach((name, values) -> flattened.put(name, values.isEmpty() ? "" : firstValue(values)));
        return flattened;
    }

    private static String firstValue(List<String> values) {
        return values.get(0);
    }
}
