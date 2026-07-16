package com.abuscom.infisicalplugin.action;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.icons.AllIcons;
import org.jetbrains.annotations.NotNull;

public class SelectEnvironmentAction extends  AnAction{
    public SelectEnvironmentAction()
    {
        super("Select Environment","Refresh",AllIcons.Actions.GroupByFile);
    }
    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
    }
}

