package com.abuscom.infisicalplugin.infisical.injectSecrets;

import com.abuscom.infisicalplugin.infisical.cache.Cache;
import com.abuscom.infisicalplugin.infisical.login.TokenManager;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener;
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.gradle.tooling.CancellationToken;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.util.GradleVersion;
import org.jetbrains.plugins.gradle.service.execution.GradleExecutionContext;
import org.jetbrains.plugins.gradle.settings.GradleExecutionSettings;

/**
 * configureSettings() short-circuits before ever calling Cache.setCache() (which would need a
 * real .infisical.json + network call) when the token is invalid or injection is disabled - both
 * paths are fully offline, so they run as real assertions rather than blocked/documented cases.
 * The "happy path" that actually fetches secrets isn't covered here: InfisicalHttpClient.DEFAULT_BASE_URL
 * is hardcoded in Cache.setCache(), so it can't be pointed at a local test stub without a
 * production-code seam - see CacheEnvironmentSwitchTest for the same limitation worked around via
 * the package-private applyEnvironment().
 */
public class InjectIntoGradleProcessTest extends BasePlatformTestCase {

    private final InjectIntoGradleProcess process = new InjectIntoGradleProcess();

    @Override
    protected void tearDown() throws Exception {
        try {
            TokenManager.getInstance().clearKeypass();
            Cache.getInstance().setRunConfigSelection(false, null);
        } finally {
            super.tearDown();
        }
    }

    public void testConfigureSettings_invalidToken_addsNoEnvironmentVariables() {
        TokenManager.getInstance().clearKeypass();
        Cache.getInstance().setRunConfigSelection(true, "dev");

        GradleExecutionSettings settings = new GradleExecutionSettings();
        process.configureSettings(settings, new TestGradleExecutionContext(getProject(), settings));

        assertTrue(settings.getEnv().isEmpty());
    }

    public void testConfigureSettings_injectionDisabled_addsNoEnvironmentVariables() {
        TokenManager.getInstance().setTokenInKeypass(InjectSecretsRunConfigurationExtensionTest.fakeJwt(3600));
        Cache.getInstance().setRunConfigSelection(false, "dev");

        GradleExecutionSettings settings = new GradleExecutionSettings();
        process.configureSettings(settings, new TestGradleExecutionContext(getProject(), settings));

        assertTrue(settings.getEnv().isEmpty());
    }

    private static final class TestGradleExecutionContext extends UserDataHolderBase implements GradleExecutionContext {

        private final Project project;
        private final GradleExecutionSettings settings;
        private final ExternalSystemTaskId taskId;

        TestGradleExecutionContext(Project project, GradleExecutionSettings settings) {
            this.project = project;
            this.settings = settings;
            this.taskId = ExternalSystemTaskId.create(new ProjectSystemId("GRADLE"), ExternalSystemTaskType.EXECUTE_TASK, project);
        }

        @Override
        public Project getProject() {
            return project;
        }

        @Override
        public String getProjectPath() {
            return project.getBasePath();
        }

        @Override
        public ExternalSystemTaskId getTaskId() {
            return taskId;
        }

        @Override
        public GradleExecutionSettings getSettings() {
            return settings;
        }

        @Override
        public ExternalSystemTaskNotificationListener getListener() {
            return ExternalSystemTaskNotificationListener.NULL_OBJECT;
        }

        @Override
        public CancellationToken getCancellationToken() {
            return null;
        }

        @Override
        public BuildEnvironment getBuildEnvironment() {
            return null;
        }

        @Override
        public GradleVersion getGradleVersion() {
            return null;
        }
    }
}
