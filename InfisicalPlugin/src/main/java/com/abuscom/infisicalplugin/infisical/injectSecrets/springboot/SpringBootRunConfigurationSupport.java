package com.abuscom.infisicalplugin.infisical.injectSecrets.springboot;

import com.intellij.execution.configurations.RunConfigurationBase;

/**
 * Prueft per Reflection statt per direktem Klassenverweis, ob eine Run-Config eine
 * Spring-Boot-Run-Config ist. Grund: ein statischer {@code instanceof
 * SpringBootApplicationRunConfiguration} hinterlaesst im Bytecode einen harten Verweis auf
 * com.intellij.spring.boot, den der JetBrains-Marketplace-Verifier in seinem Testabbild fuer
 * IU-253.x nicht aufloesen kann (obwohl das Plugin in einer echten Ultimate-Installation vorhanden
 * ist) und deshalb als "Package not found" meldet - das blockiert den Marketplace-Upload, auch
 * wenn com.intellij.spring bereits korrekt als optionale Abhaengigkeit deklariert ist
 * (siehe plugin.xml/withSpringBoot.xml). Reflection vermeidet den harten Bytecode-Verweis.
 */
final class SpringBootRunConfigurationSupport {

    private static final String SPRING_BOOT_RUN_CONFIGURATION_CLASS =
            "com.intellij.spring.boot.run.SpringBootApplicationRunConfiguration";

    private SpringBootRunConfigurationSupport() {
    }

    static boolean isSpringBootRunConfiguration(RunConfigurationBase<?> configuration) {
        try {
            Class<?> springBootRunConfigurationClass = Class.forName(SPRING_BOOT_RUN_CONFIGURATION_CLASS);
            return springBootRunConfigurationClass.isInstance(configuration);
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
