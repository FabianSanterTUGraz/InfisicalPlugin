package com.abuscom.infisicalplugin.infisical.injectSecrets.node;

import com.intellij.execution.Executor;
import com.intellij.execution.application.ApplicationConfigurationType;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.javascript.nodejs.execution.AbstractNodeTargetRunProfile;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;

/**
 * The actual Node.js/npm run configuration classes live in the separate (non-bundled) "NodeJS"
 * plugin, so a minimal in-test subclass of AbstractNodeTargetRunProfile (itself part of the
 * already-bundled "JavaScript" plugin) stands in for it here - avoids adding another bundled
 * plugin dependency just for this instanceof check.
 * <p>
 * createLaunchSession(...).addNodeOptionsTo(...) (the actual injection logic) is NOT covered:
 * it needs a real NodeTargetRun, which needs a configured Node interpreter, and even then would
 * hit the same hardcoded InfisicalHttpClient.DEFAULT_BASE_URL limitation documented on
 * InjectIntoGradleProcessTest.
 */
public class InjectIntoNpmProcessTest extends BasePlatformTestCase {

    private final InjectIntoNpmProcess process = new InjectIntoNpmProcess();

    public void testIsApplicableFor_anyNodeTargetRunProfile_isTrue() {
        assertTrue(process.isApplicableFor(createNodeTargetRunProfile()));
    }

    private AbstractNodeTargetRunProfile createNodeTargetRunProfile() {
        ConfigurationFactory factory = ApplicationConfigurationType.getInstance().getConfigurationFactories()[0];
        return new TestNodeTargetRunProfile(getProject(), factory);
    }

    private static final class TestNodeTargetRunProfile extends AbstractNodeTargetRunProfile {

        TestNodeTargetRunProfile(Project project, ConfigurationFactory factory) {
            super(project, factory, "test-node-run-profile");
        }

        @Override
        public NodeJsInterpreter getInterpreter() {
            return null;
        }

        @Override
        public SettingsEditor<? extends AbstractNodeTargetRunProfile> createConfigurationEditor() {
            return null;
        }

        @Override
        public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment environment) {
            return null;
        }
    }
}
