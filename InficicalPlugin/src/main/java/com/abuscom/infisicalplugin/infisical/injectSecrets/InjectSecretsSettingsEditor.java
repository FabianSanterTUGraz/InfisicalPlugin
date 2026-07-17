package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.options.SettingsEditor;

import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.FlowLayout;

public class InjectSecretsSettingsEditor extends SettingsEditor<RunConfigurationBase<?>> {

    private final JCheckBox enabledCheckBox = new JCheckBox("Infisical Secrets in diese Run Configuration injizieren");

    @Override
    protected void resetEditorFrom(@NotNull RunConfigurationBase<?> configuration) {
        enabledCheckBox.setSelected(InjectSecretsSettings.getOrCreate(configuration).enabled);
    }

    @Override
    protected void applyEditorTo(@NotNull RunConfigurationBase<?> configuration) {
        InjectSecretsSettings.getOrCreate(configuration).enabled = enabledCheckBox.isSelected();
    }

    @Override
    protected @NotNull JComponent createEditor() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(enabledCheckBox);
        return panel;
    }
}