package com.abuscom.infisicalplugin.infisical.auth;

import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UniversalAuthClientTest {

    private HttpServer server;
    private UniversalAuthClient authClient;
    private String receivedRequestBody;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api/v1/auth/universal-auth/login", exchange -> {
            try (InputStream is = exchange.getRequestBody()) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                is.transferTo(buffer);
                receivedRequestBody = buffer.toString(StandardCharsets.UTF_8);
            }

            String responseJson = "{\"accessToken\":\"test-token\",\"expiresIn\":7200,\"tokenType\":\"Bearer\"}";
            byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();

        InfisicalHttpClient httpClient = new InfisicalHttpClient("http://localhost:" + server.getAddress().getPort());
        authClient = new UniversalAuthClient(httpClient);
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void login_returnsAccessToken_onSuccess() throws InfisicalHttpException {
        AccessToken token = authClient.login("test-client-id", "test-client-secret");

        assertEquals("test-token", token.value());
        assertTrue(receivedRequestBody.contains("\"clientId\":\"test-client-id\""));
        assertTrue(receivedRequestBody.contains("\"clientSecret\":\"test-client-secret\""));
    }
}
