package com.abuscom.infisicalplugin.infisical.injectSecrets.nx;

import com.intellij.execution.BeforeRunTask;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Persistiert project/projectId/environment ueber {@link PersistentStateComponent} statt ueber die
 * mit IU-2025.3.5 als {@code @Deprecated} markierten {@link BeforeRunTask#readExternal}/
 * {@link BeforeRunTask#writeExternal}. Gleiches Muster wie JetBrains' eigenes
 * {@code com.intellij.ide.browsers.LaunchBrowserBeforeRunTask} (per javap gegen die gebuendelte
 * app.jar verifiziert): die Basisklasse ueberspringt den alten XML-Attribut-Pfad automatisch,
 * sobald die Subklasse PersistentStateComponent implementiert.
 */
public class InjectSecretsBeforeRunTaskNx extends BeforeRunTask<InjectSecretsBeforeRunTaskNx>
        implements PersistentStateComponent<InjectSecretsBeforeRunTaskNx.State> {

    public static class State {
        public String project;
        public String projectId;
        public String environment;
    }

    private State state = new State();

    protected InjectSecretsBeforeRunTaskNx(@NotNull Key<InjectSecretsBeforeRunTaskNx> providerId) {
        super(providerId);
    }

    @Override
    public @NotNull State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public String getProject() {
        return state.project;
    }

    public void setProject(String project) {
        state.project = project;
    }

    public String getProjectId() {
        return state.projectId;
    }

    public void setProjectId(String projectId) {
        state.projectId = projectId;
    }

    public String getEnvironment() {
        return state.environment;
    }

    public void setEnvironment(String environment) {
        state.environment = environment;
    }

    @Override
    public boolean equals(Object o)
    {
        if(!super.equals(o)) return false;
        InjectSecretsBeforeRunTaskNx that = (InjectSecretsBeforeRunTaskNx)o;
        return Objects.equals(state.project, that.state.project) && Objects.equals(state.projectId, that.state.projectId)
                && Objects.equals(state.environment, that.state.environment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), state.project, state.projectId, state.environment);
    }
}
