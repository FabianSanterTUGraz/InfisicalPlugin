package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.util.Key;

import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

public class InjectSecretsSettings {

    public static String[] ENVIRONMENTS = {"LOGIN PLEASE.."};

    public static final Key<InjectSecretsSettings> KEY =
            Key.create("com.abuscom.infisicalplugin.InjectSecretsSettings");

    private static final String ELEMENT_NAME = "InfisicalSecretsInjection";
    private static final String ATTR_ENABLED = "enabled";
    private static final String ATTR_ENVIRONMENT = "environment";

    public boolean enabled = false;
    public String selectedEnvironment = ENVIRONMENTS[0];

    public static InjectSecretsSettings getOrCreate(@NotNull RunConfigurationBase<?> config) {
        InjectSecretsSettings settings = config.getCopyableUserData(KEY);
        if (settings == null) {
            settings = new InjectSecretsSettings();
            config.putCopyableUserData(KEY, settings);
        }
        return settings;
    }

    /**
     * Gemeinsame Persistenz-Logik fuer alle RunConfigurationExtensions, die Infisical-Secrets
     * injizieren koennen (Gradle, npm/Node, ...) - vermeidet doppelte readExternal/writeExternal
     * Implementierungen pro Extension.
     */
    public static void readExternal(@NotNull RunConfigurationBase<?> configuration, @NotNull Element element) {
        Element child = element.getChild(ELEMENT_NAME);
        InjectSecretsSettings settings = getOrCreate(configuration);
        settings.enabled = child != null && Boolean.parseBoolean(child.getAttributeValue(ATTR_ENABLED, "false"));
        settings.selectedEnvironment = child != null
                ? child.getAttributeValue(ATTR_ENVIRONMENT, ENVIRONMENTS[0])
                : ENVIRONMENTS[0];
    }

    public static void writeExternal(@NotNull RunConfigurationBase<?> configuration, @NotNull Element element) {
        InjectSecretsSettings settings = getOrCreate(configuration);
        Element child = new Element(ELEMENT_NAME);
        child.setAttribute(ATTR_ENABLED, String.valueOf(settings.enabled));
        child.setAttribute(ATTR_ENVIRONMENT, settings.selectedEnvironment);
        element.addContent(child);
    }
}
