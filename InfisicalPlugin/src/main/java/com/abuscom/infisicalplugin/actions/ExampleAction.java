package com.abuscom.infisicalplugin.actions;

import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ExampleAction extends DumbAwareAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        NotificationGroupManager.getInstance()
                .getNotificationGroup("Infisical Notifications")
                .createNotification("Infisical", "ExampleAction ausgeführt", NotificationType.INFORMATION)
                .notify(project);
    }
}
