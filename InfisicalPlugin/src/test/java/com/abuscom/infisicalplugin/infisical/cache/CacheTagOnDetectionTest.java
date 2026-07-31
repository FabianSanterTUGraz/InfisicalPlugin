package com.abuscom.infisicalplugin.infisical.cache;

import com.abuscom.infisicalplugin.infisical.cache.Secrets.SecretClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the "tag machine-specific secrets" feature added to Cache.applyEnvironment():
 * a secret whose value looks like a local filesystem path gets tagged in Infisical
 * (PATCH .../secrets/{key} with tagIds) unless it already carries the tag, and the
 * tag itself is only created once (findTagBySlug first, createTag only if missing).
 */
class CacheTagOnDetectionTest {

    private static final String PROJECT_ID = "proj-1";
    private static final String ENVIRONMENT = "dev";
    private static final String SLUG = "specificpaths";
    private static final String USER_SPECIFIC_VALUE = "/home/dev/id_rsa";

    private final Gson gson = new Gson();

    private HttpServer server;
    private Cache cache;
    private SecretClient secretClient;

    private final AtomicInteger createTagCalls = new AtomicInteger();
    private final List<String> patchedSecretKeys = Collections.synchronizedList(new ArrayList<>());
    private final List<String> patchBodies = Collections.synchronizedList(new ArrayList<>());

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
    void userSpecificPathValue_getsPatchedWithExistingTagId() throws InfisicalHttpException {
        stubExistingTag("tag-1");
        stubSecrets(secretJson("PATH_VAR", USER_SPECIFIC_VALUE, List.of()));

        cache.applyEnvironment(PROJECT_ID, ENVIRONMENT, "token", secretClient);

        assertEquals(0, createTagCalls.get(), "tag already exists, createTag must not be called");
        assertEquals(List.of("PATH_VAR"), patchedSecretKeys);

        JsonObject body = gson.fromJson(patchBodies.get(0), JsonObject.class);
        assertEquals(PROJECT_ID, body.get("projectId").getAsString());
        assertEquals(ENVIRONMENT, body.get("environment").getAsString());
        assertEquals("tag-1", body.getAsJsonArray("tagIds").get(0).getAsString());
    }

    @Test
    void secretAlreadyCarryingTheTag_isNotPatchedAgain() throws InfisicalHttpException {
        stubExistingTag("tag-1");
        stubSecrets(secretJson("PATH_VAR", USER_SPECIFIC_VALUE, List.<String[]>of(new String[]{"tag-1", SLUG})));

        cache.applyEnvironment(PROJECT_ID, ENVIRONMENT, "token", secretClient);

        assertTrue(patchedSecretKeys.isEmpty(), "secret already has the tag, no PATCH should be sent");
    }

    @Test
    void nonPathLikeValue_isNeverPatched() throws InfisicalHttpException {
        stubExistingTag("tag-1");
        stubSecrets(secretJson("GREETING", "hello world", List.of()));

        cache.applyEnvironment(PROJECT_ID, ENVIRONMENT, "token", secretClient);

        assertTrue(patchedSecretKeys.isEmpty(), "value does not look like a path, must not be tagged");
    }

    @Test
    void tagMissing_isCreatedOnceAndItsIdIsUsedForThePatch() throws InfisicalHttpException {
        stubTagMissingThenCreated("new-tag-id");
        stubSecrets(secretJson("PATH_VAR", USER_SPECIFIC_VALUE, List.of()));

        cache.applyEnvironment(PROJECT_ID, ENVIRONMENT, "token", secretClient);

        assertEquals(1, createTagCalls.get());
        assertEquals(List.of("PATH_VAR"), patchedSecretKeys);

        JsonObject body = gson.fromJson(patchBodies.get(0), JsonObject.class);
        assertEquals("new-tag-id", body.getAsJsonArray("tagIds").get(0).getAsString());
    }

    private void stubExistingTag(String tagId) {
        server.createContext("/api/v1/projects", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                respond(exchange, 200, tagListJson(List.<String[]>of(new String[]{tagId, SLUG})));
            } else if ("POST".equals(exchange.getRequestMethod())) {
                createTagCalls.incrementAndGet();
                respond(exchange, 200, tagJson(tagId));
            } else {
                respond(exchange, 404, "{}");
            }
        });
    }

    private void stubTagMissingThenCreated(String createdTagId) {
        server.createContext("/api/v1/projects", exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                respond(exchange, 200, tagListJson(List.of()));
            } else if ("POST".equals(exchange.getRequestMethod())) {
                createTagCalls.incrementAndGet();
                respond(exchange, 200, tagJson(createdTagId));
            } else {
                respond(exchange, 404, "{}");
            }
        });
    }

    private void stubSecrets(String secretEntryJson) {
        server.createContext("/api/v4/secrets", exchange -> {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if ("GET".equals(method) && path.equals("/api/v4/secrets")) {
                respond(exchange, 200, "{\"secrets\":[" + secretEntryJson + "]}");
            } else if ("PATCH".equals(method)) {
                String secretKey = path.substring("/api/v4/secrets/".length());
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                patchedSecretKeys.add(secretKey);
                patchBodies.add(body);
                respond(exchange, 200, "{}");
            } else {
                respond(exchange, 404, "{}");
            }
        });
    }

    private static String secretJson(String key, String value, List<String[]> tags) {
        StringBuilder tagsJson = new StringBuilder("[");
        boolean first = true;
        for (String[] tag : tags) {
            if (!first) {
                tagsJson.append(",");
            }
            tagsJson.append("{\"id\":\"").append(tag[0]).append("\",\"slug\":\"").append(tag[1]).append("\"}");
            first = false;
        }
        tagsJson.append("]");
        return "{\"secretKey\":\"" + key + "\",\"secretValue\":\"" + value + "\",\"tags\":" + tagsJson + "}";
    }

    private static String tagListJson(List<String[]> tags) {
        StringBuilder json = new StringBuilder("{\"tags\":[");
        boolean first = true;
        for (String[] tag : tags) {
            if (!first) {
                json.append(",");
            }
            json.append(tagJsonFragment(tag[0], tag[1]));
            first = false;
        }
        return json.append("]}").toString();
    }

    private static String tagJson(String tagId) {
        return tagJsonFragment(tagId, SLUG);
    }

    private static String tagJsonFragment(String id, String slug) {
        return "{\"id\":\"" + id + "\",\"slug\":\"" + slug + "\",\"color\":\"RED\",\"projectId\":\"" + PROJECT_ID + "\"}";
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
    // the next (mirrors the reflection-based cleanup in CacheEnvironmentSwitchTest).
    private void resetEnvironmentState() throws ReflectiveOperationException {
        Field environmentField = Cache.class.getDeclaredField("environment");
        environmentField.setAccessible(true);
        environmentField.set(cache, "");

        cache.getSecrets().clear();
    }
}