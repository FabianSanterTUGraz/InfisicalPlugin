package com.abuscom.infisicalplugin.infisical.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InfisicalHttpClientTest {

    private HttpServer server;
    private InfisicalHttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/ok", exchange -> respond(exchange, 200, "{\"status\":\"ok\"}"));
        server.createContext("/unauthorized", exchange -> respond(exchange, 401, "{\"error\":\"invalid credentials\"}"));
        server.start();
        client = new InfisicalHttpClient("http://localhost:" + server.getAddress().getPort());
    }

    @AfterEach
    void tearDown() {
        server.stop(0);
    }

    @Test
    void send_returnsResponse_onSuccess() throws InfisicalHttpException {
        HttpApiResponse response = client.send("GET", "/ok", Map.of(), null);

        assertEquals(200, response.statusCode());
        assertEquals("{\"status\":\"ok\"}", response.body());
    }

    @Test
    void send_throwsException_onNon2xxStatus() {
        InfisicalHttpException exception = assertThrows(
                InfisicalHttpException.class,
                () -> client.send("GET", "/unauthorized", Map.of(), null)
        );

        assertEquals(401, exception.getStatusCode());
        assertEquals("{\"error\":\"invalid credentials\"}", exception.getResponseBody());
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
