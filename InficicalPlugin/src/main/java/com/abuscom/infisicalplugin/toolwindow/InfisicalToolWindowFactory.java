package com.abuscom.infisicalplugin.toolwindow;

import com.abuscom.infisicalplugin.action.*;
import com.abuscom.infisicalplugin.toolwindow.login.LoginPanel;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.*;

public final class InfisicalToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        //das menu oben in der UI wenn man dazu machen will dann hiwe adden
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        actionGroup.add(new LoginAction());
        actionGroup.add(new LogoutAction());
        actionGroup.add(new RefreshAction());
        actionGroup.add(new SelectProjectAction());

        actionGroup.add(new SelectEnvironmentAction());

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar("Infisical Tool Window", actionGroup,true);
        SimpleToolWindowPanel panel = new SimpleToolWindowPanel(true);
        panel.setToolbar(toolbar.getComponent());

        JPanel content = new JPanel(new GridLayout(1,2));
        content.add(new LoginPanel());
        content.add(new JLabel("Infisical: noch keine Secrets geladen.", JLabel.CENTER), BorderLayout.CENTER);
        panel.setContent(content);

        Content toolWindowContent = ContentFactory.getInstance().createContent(panel,"",false);
        toolWindow.getContentManager().addContent(toolWindowContent);
    }
}