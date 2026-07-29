package com.abuscom.infisicalplugin.infisical.cache;

import com.abuscom.infisicalplugin.infisical.cache.Secrets.SecretClient;
import com.abuscom.infisicalplugin.infisical.cache.Secrets.SecretEntry;
import com.abuscom.infisicalplugin.infisical.cache.Secrets.SecretsAPICallResponse;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CacheVersionControlTest {

    private record SecretStub(String value, int version) {}

    private HttpServer server;
    private Cache cache;
    private SecretClient secretClient;
    private final AtomicReference<Map<String, SecretStub>> currentSecrets = new AtomicReference<>(Map.of());

    @BeforeEach
    void setUp() throws IOException {
        cache = Cache.getInstance();
        cache.clearCache();

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/v4/secrets", exchange -> respond(exchange, 200, toSecretsJson(currentSecrets.get())));
        server.start();
        secretClient = new SecretClient(new InfisicalHttpClient("http://localhost:" + server.getAddress().getPort()));
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void firstFetch_populatesSecretsAndTheirVersions() throws InfisicalHttpException {
        currentSecrets.set(Map.of("A", new SecretStub("1", 1)));

        cache.applyEnvironment("proj-1", "dev", "token", secretClient);

        assertEquals(Map.of("A", "1"), cache.getSecrets());
        assertEquals(1, cache.getSecretVersion("A"));
    }

    @Test
    void reapplyingSameEnvironmentWithUnchangedVersion_doesNotRefetchSecretValues() throws InfisicalHttpException {
        currentSecrets.set(Map.of("A", new SecretStub("1", 1)));
        cache.applyEnvironment("proj-1", "dev", "token", secretClient);
        assertEquals(Map.of("A", "1"), cache.getSecrets());

        // Server-side value changes but the version stays the same - in a real Infisical instance
        // this can't happen (version always bumps on update). Used here to prove the cache trusts
        // the version number and skips the refetch instead of ever comparing values directly.
        currentSecrets.set(Map.of("A", new SecretStub("999", 1)));
        cache.applyEnvironment("proj-1", "dev", "token", secretClient);

        assertEquals(Map.of("A", "1"), cache.getSecrets());
    }

    @Test
    void versionBump_triggersRefetchEvenForTheSameEnvironment() throws InfisicalHttpException {
        currentSecrets.set(Map.of("A", new SecretStub("1", 1)));
        cache.applyEnvironment("proj-1", "dev", "token", secretClient);
        assertEquals(Map.of("A", "1"), cache.getSecrets());

        currentSecrets.set(Map.of("A", new SecretStub("2", 2)));
        cache.applyEnvironment("proj-1", "dev", "token", secretClient);

        assertEquals(Map.of("A", "2"), cache.getSecrets());
        assertEquals(2, cache.getSecretVersion("A"));
    }

    @Test
    void newSecretAddedWithoutVersionBumpOnExisting_stillTriggersRefetch() throws InfisicalHttpException {
        currentSecrets.set(Map.of("A", new SecretStub("1", 1)));
        cache.applyEnvironment("proj-1", "dev", "token", secretClient);
        assertEquals(Map.of("A", "1"), cache.getSecrets());

        currentSecrets.set(Map.of(
                "A", new SecretStub("1", 1),
                "B", new SecretStub("2", 1)
        ));
        cache.applyEnvironment("proj-1", "dev", "token", secretClient);

        assertEquals(Map.of("A", "1", "B", "2"), cache.getSecrets());
        assertEquals(1, cache.getSecretVersion("B"));
    }

    @Test
    void getSecretVersion_returnsMinusOneForUnknownKey() {
        assertEquals(-1, cache.getSecretVersion("DOES_NOT_EXIST"));
    }

    @Test
    void hasVersionChanged_returnsTrueWhenNoBaselineExistsYet() {
        SecretsAPICallResponse metadata = new SecretsAPICallResponse(List.of(new SecretEntry("A", null, 1)));

        assertTrue(cache.hasVersionChanged(metadata));
    }

    @Test
    void hasVersionChanged_returnsFalseWhenVersionsMatchKnownBaseline() throws InfisicalHttpException {
        currentSecrets.set(Map.of("A", new SecretStub("1", 1)));
        cache.applyEnvironment("proj-1", "dev", "token", secretClient);

        SecretsAPICallResponse sameMetadata = new SecretsAPICallResponse(List.of(new SecretEntry("A", null, 1)));

        assertFalse(cache.hasVersionChanged(sameMetadata));
    }

    private static String toSecretsJson(Map<String, SecretStub> secrets) {
        StringBuilder json = new StringBuilder("{\"secrets\":[");
        boolean first = true;
        for (Map.Entry<String, SecretStub> entry : secrets.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("{\"secretKey\":\"").append(entry.getKey())
                    .append("\",\"secretValue\":\"").append(entry.getValue().value())
                    .append("\",\"version\":").append(entry.getValue().version())
                    .append("}");
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
}
