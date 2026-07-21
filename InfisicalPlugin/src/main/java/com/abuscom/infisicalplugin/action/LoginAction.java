package com.abuscom.infisicalplugin.action;

import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import org.jetbrains.annotations.NotNull;

public class LoginAction extends AnAction {

    public LoginAction() {
        super("Logout", "Infisical-Token entfernen", AllIcons.Actions.Exit);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        TokenManager.getInstance().clearKeypass();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}