package com.abuscom.infisicalplugin.toolwindow.login;

//password safe for intelij
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.credentialStore.Credentials;

import com.intellij.openapi.util.Pass;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.net.URLDecoder;
import java.util.logging.Logger;


import java.net.InetSocketAddress;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import javax.swing.JOptionPane;

public  class LoginCallBackServer
{
    public  final int PORT = LoginPanel.LOGIN_CALLBACK_PORT;
    private HttpServer Server;
    private String jwtToken;
    private String email;
    private Runnable onTokenReceived;

    public  void startServer() throws IOException
    {
        Server = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
        Server.createContext("/", this::handle);
        Server.setExecutor(null);
        Server.start();
    }

    public void handle(HttpExchange exchange) throws  IOException
    {
        try {
            // Handle CORS preflight request
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

                CredentialAttributes attributes = new CredentialAttributes(
                        CredentialAttributesKt.generateServiceName("InfisicalPlugin","jwtToken")
                );

                JsonObject json = JsonParser.parseString(bodyStr).getAsJsonObject();
                this.jwtToken = json.get("JTWToken").getAsString();
                this.email = json.get("email").getAsString();

                Credentials credentials = new Credentials(email,jwtToken);
                PasswordSafe.getInstance().set(attributes,credentials);

                Credentials test = PasswordSafe.getInstance().get(attributes);
                String testUsername = test.getUserName();
                String testToken = test.getPasswordAsString();
            }

            String successMessage = "Login Successful! You can close this window.";
            exchange.getResponseHeaders().set("Content-Type","text/plain");
            exchange.sendResponseHeaders(200, successMessage.getBytes().length);
            exchange.getResponseBody().write(successMessage.getBytes());

        } finally {
            exchange.close();
        }
    }
}