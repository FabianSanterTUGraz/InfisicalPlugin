package com.abuscom.infisicalplugin.infisical.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InfisicalHttpExceptionTest {

    @Test
    void getUserMessage_unauthorizedStatus_mentionsInvalidToken() {
        InfisicalHttpException exception = new InfisicalHttpException(401, "{\"error\":\"invalid credentials\"}");

        assertEquals("Ungültiger oder abgelaufener Token — bitte erneut einloggen.", exception.getUserMessage());
    }

    @Test
    void getUserMessage_forbiddenStatus_mentionsInvalidToken() {
        InfisicalHttpException exception = new InfisicalHttpException(403, "{\"error\":\"forbidden\"}");

        assertEquals("Ungültiger oder abgelaufener Token — bitte erneut einloggen.", exception.getUserMessage());
    }

    @Test
    void getUserMessage_wrappedIOException_mentionsNetworkUnreachable() {
        InfisicalHttpException exception = new InfisicalHttpException("Request to /secrets failed", new IOException("Connection refused"));

        assertEquals("Infisical ist nicht erreichbar (Netzwerkfehler).", exception.getUserMessage());
    }

    @Test
    void getUserMessage_otherStatus_includesStatusCode() {
        InfisicalHttpException exception = new InfisicalHttpException(500, "{\"error\":\"internal\"}");

        assertEquals("Infisical-Anfrage fehlgeschlagen (Status 500).", exception.getUserMessage());
    }
}
