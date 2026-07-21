package com.abuscom.infisicalplugin.infisical.login;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.net.InetSocketAddress;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class LoginCallBackServer {
    public final int PORT = LoginPanel.LOGIN_CALLBACK_PORT;
    private static HttpServer server;
    private String jwtToken;
    private String email;
    private final TokenManager tokenManager;

    public LoginCallBackServer(TokenManager tokenManager) {
        this.tokenManager = tokenManager;
    }

    public void startServer() throws IOException {
        if(server != null)
        {
            server.stop(0);
        }
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        server.createContext("/", this::handle);
        server.setExecutor(null);
        server.start();
    }

    public void stopServer()
    {
        if(server != null) {
            server.stop(0);
        }
    }

    public void handle(HttpExchange exchange) throws IOException {
        try {
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
                exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
                exchange.sendResponseHeaders(200, 0);
                return;
            }

            if ("POST".equals(exchange.getRequestMethod())) {
                byte[] body = exchange.getRequestBody().readAllBytes();
                String bodyStr = new String(body, StandardCharsets.UTF_8);
                System.out.println("Body: " + bodyStr);

                JsonObject json = JsonParser.parseString(bodyStr).getAsJsonObject();
                this.jwtToken = json.get("JTWToken").getAsString();
                this.email = json.get("email").getAsString();

                tokenManager.setTokenInKeypass(jwtToken);
            }

            String successMessage = "Login Successful! You can close this window.";
            exchange.getResponseHeaders().set("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, successMessage.getBytes().length);
            exchange.getResponseBody().write(successMessage.getBytes());
            server.stop(0);
        } finally {
            exchange.close();
        }
    }
}