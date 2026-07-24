package com.abuscom.infisicalplugin.infisical.http;

public class InfisicalHttpException extends Exception {

    private final int statusCode;
    private final String responseBody;

    public InfisicalHttpException(int statusCode, String responseBody) {
        super("Infisical API request failed with status " + statusCode + "Request body" + responseBody);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public InfisicalHttpException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = -1;
        this.responseBody = null;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }


    public String getUserMessage() {
        if (statusCode == 401 || statusCode == 403) {
            return "Ungültiger oder abgelaufener Token — bitte erneut einloggen.";
        }
        if (statusCode == -1) {
            return "Infisical ist nicht erreichbar (Netzwerkfehler).";
        }
        return "Infisical-Anfrage fehlgeschlagen (Status " + statusCode + ").";
    }
}
