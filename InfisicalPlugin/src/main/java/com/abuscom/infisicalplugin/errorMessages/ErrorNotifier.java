package com.abuscom.infisicalplugin.errorMessages;

import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;

import java.io.IOException;

/**
 * Einziger Ort, an dem Fehler-Notifications im Plugin erzeugt werden. Übernimmt zusätzlich die
 * Übersetzung von Exceptions in nutzerfreundliche Texte, damit kein catch-Block mehr rohe
 * {@code e.getMessage()}-Ausgaben anzeigt oder Fehler einfach verschluckt/in eine RuntimeException
 * wirft.
 */
public final class ErrorNotifier {

    private static final Logger LOG = Logger.getInstance(ErrorNotifier.class);

    private ErrorNotifier() {
    }

    public static void notify(Project project, String message) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Infisical Notifications")   // muss exakt der id aus plugin.xml entsprechen
                .createNotification("Infisical", message, NotificationType.ERROR)
                .notify(project);
    }

    public static void notify(Project project, Throwable error) {
        LOG.warn("Infisical-Fehler", error);
        if (error instanceof InfisicalHttpException httpException && httpException.isAuthError()) {
            TokenManager.getInstance().clearKeypass();
        }
        notify(project, toUserMessage(error));
    }

    private static String toUserMessage(Throwable error) {
        if (error instanceof InfisicalHttpException httpException) {
            return httpException.getUserMessage();
        }
        if (error instanceof IOException) {
            return "Infisical-Konfiguration (.infisical.json) konnte nicht gelesen werden: " + error.getMessage();
        }
        return "Unerwarteter Fehler: " + error.getMessage();
    }
}
