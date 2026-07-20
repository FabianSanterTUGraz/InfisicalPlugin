package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.ComboBox;

import org.jetbrains.annotations.NotNull;

import com.abuscom.infisicalplugin.infisical.login.LoginUser;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.FlowLayout;

public class InjectSecretsSettingsEditor extends SettingsEditor<RunConfigurationBase<?>> {

    private final JCheckBox enabledCheckBox = new JCheckBox("Infisical Secrets in diese Run Configuration injizieren");
    private final ComboBox<String> environmentComboBox = new ComboBox<>(InjectSecretsSettings.ENVIRONMENTS);
    private final JButton loginButton = new JButton("Login");

    public InjectSecretsSettingsEditor() {
        loginButton.addActionListener(e -> new LoginUser().login());
    }

    @Override
    protected void resetEditorFrom(@NotNull RunConfigurationBase<?> configuration) {
        InjectSecretsSettings settings = InjectSecretsSettings.getOrCreate(configuration);
        enabledCheckBox.setSelected(settings.enabled);
        environmentComboBox.setSelectedItem(settings.selectedEnvironment);
    }

    @Override
    protected void applyEditorTo(@NotNull RunConfigurationBase<?> configuration) {
        InjectSecretsSettings settings = InjectSecretsSettings.getOrCreate(configuration);
        settings.enabled = enabledCheckBox.isSelected();
        settings.selectedEnvironment = (String) environmentComboBox.getSelectedItem();
    }

    @Override
    protected @NotNull JComponent createEditor() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.add(enabledCheckBox);
        panel.add(environmentComboBox);
        panel.add(loginButton);
        return panel;
    }
}
