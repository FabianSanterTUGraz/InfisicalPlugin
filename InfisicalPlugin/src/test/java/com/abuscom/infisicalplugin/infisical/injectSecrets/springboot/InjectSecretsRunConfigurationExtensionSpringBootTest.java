package com.abuscom.infisicalplugin.infisical.injectSecrets.springboot;

import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.spring.boot.run.SpringBootApplicationConfigurationTypeBase;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType;

/**
 * Pendant zu InjectSecretsRunConfigurationExtensionTest (Gradle) - deckt den seit dem
 * Spring-Boot-Split ausgelagerten Teil von isApplicableFor ab.
 */
public class InjectSecretsRunConfigurationExtensionSpringBootTest extends BasePlatformTestCase {

    private final InjectSecretsRunConfigurationExtensionSpringBoot extension = new InjectSecretsRunConfigurationExtensionSpringBoot();

    @Override
    protected void tearDown() throws Exception {
        try {
            TokenManager.getInstance().clearKeypass();
        } finally {
            super.tearDown();
        }
    }

    public void testIsApplicableFor_springBootConfiguration_isTrue() {
        assertTrue(extension.isApplicableFor(createSpringBootConfiguration()));
    }

    public void testIsApplicableFor_gradleConfiguration_isFalse() {
        assertFalse(extension.isApplicableFor(createGradleConfiguration()));
    }

    public void testIsApplicableFor_unrelatedConfiguration_isFalse() {
        ConfigurationFactory factory = ApplicationConfigurationType.getInstance().getConfigurationFactories()[0];
        RunConfigurationBase<?> config = (RunConfigurationBase<?>) factory.createTemplateConfiguration(getProject());

        assertFalse(extension.isApplicableFor(config));
    }

    public void testUpdateJavaParameters_invalidToken_doesNotTouchEnv() throws Exception {
        TokenManager.getInstance().clearKeypass();
        RunConfigurationBase<?> config = createSpringBootConfiguration();
        JavaParameters javaParameters = new JavaParameters();

        extension.updateJavaParameters(config, javaParameters, null);

        assertTrue(javaParameters.getEnv().isEmpty());
    }

    private RunConfigurationBase<?> createGradleConfiguration() {
        ConfigurationFactory factory = GradleExternalTaskConfigurationType.getInstance().getFactory();
        return (RunConfigurationBase<?>) factory.createTemplateConfiguration(getProject());
    }

    private RunConfigurationBase<?> createSpringBootConfiguration() {
        ConfigurationFactory factory = SpringBootApplicationConfigurationTypeBase.getInstance().getDefaultConfigurationFactory();
        return (RunConfigurationBase<?>) factory.createTemplateConfiguration(getProject());
    }
}
