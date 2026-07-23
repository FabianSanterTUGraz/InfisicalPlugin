package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.spring.boot.run.SpringBootApplicationConfigurationTypeBase;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.plugins.gradle.service.execution.GradleExternalTaskConfigurationType;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * isApplicableFor/isEnabledFor need a real RunConfigurationBase instance (Project-backed), so
 * these run as BasePlatformTestCase (real lightweight Project + Application) rather than plain
 * JUnit 5 - see LoginTests.java for why PasswordSafe-touching code is blocked without one.
 */
public class InjectSecretsRunConfigurationExtensionTest extends BasePlatformTestCase {

    private final InjectSecretsRunConfigurationExtension extension = new InjectSecretsRunConfigurationExtension();

    @Override
    protected void tearDown() throws Exception {
        try {
            TokenManager.getInstance().clearKeypass();
        } finally {
            super.tearDown();
        }
    }

    public void testIsApplicableFor_gradleConfiguration_isTrue() {
        assertTrue(extension.isApplicableFor(createGradleConfiguration()));
    }

    public void testIsApplicableFor_springBootConfiguration_isTrue() {
        assertTrue(extension.isApplicableFor(createSpringBootConfiguration()));
    }

    public void testIsApplicableFor_unrelatedConfiguration_isFalse() {
        ConfigurationFactory factory = ApplicationConfigurationType.getInstance().getConfigurationFactories()[0];
        RunConfigurationBase<?> config = (RunConfigurationBase<?>) factory.createTemplateConfiguration(getProject());

        assertFalse(extension.isApplicableFor(config));
    }

    public void testIsEnabledFor_reflectsInjectSecretsSettings() {
        RunConfigurationBase<?> config = createSpringBootConfiguration();

        assertFalse(extension.isEnabledFor(config, null));

        InjectSecretsSettings.getOrCreate(config).enabled = true;
        assertTrue(extension.isEnabledFor(config, null));
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

    // TokenManager.isTokenValid() only inspects the unsigned "exp" claim, so a structurally
    // valid but unsigned JWT is enough to exercise the "token present and not expired" path
    // without a real Infisical login.
    static String fakeJwt(long expiresInSeconds) {
        String header = base64Url("{\"alg\":\"none\"}");
        String payload = base64Url("{\"exp\":" + (Instant.now().getEpochSecond() + expiresInSeconds) + "}");
        return header + "." + payload + ".sig";
    }

    private static String base64Url(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
