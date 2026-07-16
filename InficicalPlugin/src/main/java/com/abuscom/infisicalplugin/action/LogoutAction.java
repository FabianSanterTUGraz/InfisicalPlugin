package com.abuscom.infisicalplugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

public class LogoutAction extends  AnAction{
    public LogoutAction()
    {
        super("Logout","Abmelden",AllIcons.Actions.Exit);
    }
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {

    }
}
