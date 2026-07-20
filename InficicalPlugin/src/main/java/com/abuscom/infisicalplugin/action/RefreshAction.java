package com.abuscom.infisicalplugin.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

import com.abuscom.infisicalplugin.infisical.cache.Cache;

public class RefreshAction extends  AnAction{
    public RefreshAction()
    {
        super("Refresh","Refresh",AllIcons.Actions.Refresh);
    }
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {

    }
}

