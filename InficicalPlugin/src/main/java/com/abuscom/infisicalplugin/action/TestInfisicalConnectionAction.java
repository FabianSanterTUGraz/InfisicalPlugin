package com.abuscom.infisicalplugin.action;

import com.abuscom.infisicalplugin.infisical.auth.AccessToken;
import com.abuscom.infisicalplugin.infisical.auth.UniversalAuthClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpClient;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;

/**
 * Temporäres Debug-Werkzeug, um {@link UniversalAuthClient}/{@link InfisicalHttpClient} manuell
 * in der Sandbox-IDE gegen die echte Infisical-API zu testen. Wird entfernt, sobald #6/#9 eine
 * echte Settings-UI bzw. ein Tool-Window bereitstellen.
 */
public class TestInfisicalConnectionAction extends AnAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent event) {
        Project project = event.getProject();

        String clientId = Messages.showInputDialog(
                project, "Infisical Client ID", "Infisical Verbindung testen", Messages.getQuestionIcon());
        if (clientId == null || clientId.isBlank()) {
            return;
        }

        String clientSecret = Messages.showPasswordDialog(
                project, "Infisical Client Secret", "Infisical Verbindung testen", Messages.getQuestionIcon());
        if (clientSecret == null || clientSecret.isBlank()) {
            return;
        }

        ProgressManager.getInstance().run(new Task.Backgroundable(project, "Infisical Login wird getestet") {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    InfisicalHttpClient httpClient = new InfisicalHttpClient(InfisicalHttpClient.DEFAULT_BASE_URL);
                    UniversalAuthClient authClient = new UniversalAuthClient(httpClient);
                    AccessToken token = authClient.login(clientId, clientSecret);
                    showNotification(project, "Login erfolgreich, Token gültig bis " + token.expiresAt(), NotificationType.INFORMATION);
                } catch (InfisicalHttpException e) {
                    showNotification(project, "Login fehlgeschlagen: " + e.getMessage(), NotificationType.ERROR);
                }
            }
        });
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    private static void showNotification(Project project, String content, NotificationType type) {
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Infisical Notifications")
                .createNotification(content, type)
                .notify(project);
    }
}