package com.abuscom.infisicalplugin.infisical.cache;

import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CacheEnvironmentSwitchTest {

    private HttpServer server;
    private Cache cache;
    private SecretClient secretClient;

    @BeforeEach
    void setUp() throws IOException, ReflectiveOperationException {
        cache = Cache.getInstance();
        resetEnvironmentState();

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.start();
        secretClient = new SecretClient(new InfisicalHttpClient("http://localhost:" + server.getAddress().getPort()));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void switchingEnvironment_dropsPreviousEnvironmentsSecrets() {
        stubSecretsEndpoint(Map.of(
                "dev", Map.of("A", "1", "B", "2", "C", "3"),
                "staging", Map.of("D", "4", "E", "5", "F", "6")
        ));

        cache.applyEnvironment("proj-1", "dev", "token", secretClient);
        assertEquals(Map.of("A", "1", "B", "2", "C", "3"), cache.getSecrets());

        cache.applyEnvironment("proj-1", "staging", "token", secretClient);
        assertEquals(Map.of("D", "4", "E", "5", "F", "6"), cache.getSecrets());
        assertFalse(cache.getSecrets().containsKey("A"));
        assertFalse(cache.getSecrets().containsKey("B"));
        assertFalse(cache.getSecrets().containsKey("C"));
    }

    @Test
    void reapplyingSameEnvironment_doesNotRefetch() {
        stubSecretsEndpoint(Map.of(
                "dev", Map.of("A", "1"),
                // if applyEnvironment refetched on a no-op "switch", the second call below would
                // silently pick these up instead of keeping the secrets from the first call
                "dev-if-refetched" , Map.of("A", "999")
        ));

        cache.applyEnvironment("proj-1", "dev", "token", secretClient);
        assertEquals(Map.of("A", "1"), cache.getSecrets());

        cache.applyEnvironment("proj-1", "dev", "token", secretClient);
        assertEquals(Map.of("A", "1"), cache.getSecrets());
    }

    private void stubSecretsEndpoint(Map<String, Map<String, String>> secretsByEnvironment) {
        server.createContext("/api/v4/secrets", exchange -> {
            Map<String, String> params = parseQuery(exchange.getRequestURI().getQuery());
            Map<String, String> secrets = secretsByEnvironment.getOrDefault(params.get("environment"), Map.of());
            respond(exchange, 200, toSecretsJson(secrets));
        });
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            String[] keyValue = pair.split("=", 2);
            params.put(keyValue[0], keyValue[1]);
        }
        return params;
    }

    private static String toSecretsJson(Map<String, String> secrets) {
        StringBuilder json = new StringBuilder("{\"secrets\":[");
        boolean first = true;
        for (Map.Entry<String, String> entry : secrets.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("{\"secretKey\":\"").append(entry.getKey())
                    .append("\",\"secretValue\":\"").append(entry.getValue()).append("\"}");
            first = false;
        }
        return json.append("]}").toString();
    }

    private static void respond(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // Cache is a process-wide singleton (see Cache.getInstance()) - reset its private
    // "environment" field between tests so one test's applyEnvironment calls don't leak into
    // the next (mirrors the reflection-based cleanup already used in LoginTests.java).
    private void resetEnvironmentState() throws ReflectiveOperationException {
        Field environmentField = Cache.class.getDeclaredField("environment");
        environmentField.setAccessible(true);
        environmentField.set(cache, "");
        cache.getSecrets().clear();
    }
}
