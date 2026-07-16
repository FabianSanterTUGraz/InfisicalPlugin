package com.abuscom.infisicalplugin.toolwindow;

import com.abuscom.infisicalplugin.toolwindow.login.LoginPanel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public final class InfisicalToolWindowFactory implements ToolWindowFactory {

    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new LoginPanel(), BorderLayout.NORTH);
        panel.add(new JLabel("Infisical: noch keine Secrets geladen.", JLabel.CENTER), BorderLayout.CENTER);

        Content content = ContentFactory.getInstance().createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}