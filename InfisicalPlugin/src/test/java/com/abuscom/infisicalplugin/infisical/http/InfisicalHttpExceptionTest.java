package com.abuscom.infisicalplugin.infisical.http;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void isAuthError_unauthorizedStatus_true() {
        InfisicalHttpException exception = new InfisicalHttpException(401, "{\"error\":\"invalid credentials\"}");

        assertTrue(exception.isAuthError());
    }

    @Test
    void isAuthError_forbiddenStatus_true() {
        InfisicalHttpException exception = new InfisicalHttpException(403, "{\"error\":\"forbidden\"}");

        assertTrue(exception.isAuthError());
    }

    @Test
    void isAuthError_notFoundWithSessionNotFoundBody_true() {
        InfisicalHttpException exception = new InfisicalHttpException(404,
                "{\"reqId\":\"req-1\",\"statusCode\":404,\"message\":\"The requested entity is not found\",\"error\":\"Session not found\"}");

        assertTrue(exception.isAuthError());
    }

    @Test
    void isAuthError_notFoundWithoutSessionNotFoundBody_false() {
        InfisicalHttpException exception = new InfisicalHttpException(404, "{\"error\":\"workspace not found\"}");

        assertFalse(exception.isAuthError());
    }

    @Test
    void isAuthError_otherStatus_false() {
        InfisicalHttpException exception = new InfisicalHttpException(500, "{\"error\":\"internal\"}");

        assertFalse(exception.isAuthError());
    }

    @Test
    void isAuthError_wrappedIOException_false() {
        InfisicalHttpException exception = new InfisicalHttpException("Request to /secrets failed", new IOException("Connection refused"));

        assertFalse(exception.isAuthError());
    }
}
