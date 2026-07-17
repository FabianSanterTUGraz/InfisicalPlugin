package com.abuscom.infisicalplugin.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.jetbrains.cef.remote.thrift.annotation.Nullable;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SelectProjectAction extends DefaultActionGroup {
    public SelectProjectAction()
    {
        super("Select Project", true);
        getTemplatePresentation().setIcon(AllIcons.Actions.GroupByFile);
    }

    @Override
    public AnAction[] getChildren(@Nullable AnActionEvent e) {
        List<AnAction> actions = new ArrayList<>();
        List<String> allProjects = List.of("Project 1", "Project2");

        for (String project : allProjects) {
            actions.add(new AnAction(project) {
                @Override
                public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
                    //ProjectSelection.getInstance().setSelectedProject(project);
                }
            });
        }
        return actions.toArray(new AnAction[0]);
    }
}

