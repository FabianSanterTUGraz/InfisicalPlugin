package com.abuscom.infisicalplugin.infisical.injectSecrets.UiElements;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class NewProjektPanel extends DialogWrapper {

    public NewProjektPanel(@Nullable Project project) {
        super(project);
        setTitle("Neues Projekt erstellen");
        init();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return new JLabel("TODO: Inhalt für Sample Dialog One");
    }
}
