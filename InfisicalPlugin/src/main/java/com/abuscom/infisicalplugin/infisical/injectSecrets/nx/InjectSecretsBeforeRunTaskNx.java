package com.abuscom.infisicalplugin.infisical.injectSecrets.nx;

import com.intellij.execution.BeforeRunTask;
import com.intellij.openapi.util.Key;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class InjectSecretsBeforeRunTaskNx extends BeforeRunTask<InjectSecretsBeforeRunTaskNx> {
    private static final String PROJECT = "project";
    private static final String PROJECT_ID = "projectId";
    private static final String ENVIROMENT  = "environment";

    public String project;
    public String projectId;
    public String environment;

    protected InjectSecretsBeforeRunTaskNx(@NotNull Key<InjectSecretsBeforeRunTaskNx> providerId) {
        super(providerId);
    }

    @Override
    public void readExternal(@NotNull Element element) {
        super.readExternal(element);
        project = element.getAttributeValue(PROJECT);
        projectId = element.getAttributeValue(PROJECT_ID);
        environment = element.getAttributeValue(ENVIROMENT);
    }

    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);
        if (project != null) {
            element.setAttribute(PROJECT, project);
        }
        if (projectId != null) {
            element.setAttribute(PROJECT_ID, projectId);
        }
        if (environment != null) {
            element.setAttribute(ENVIROMENT, environment);
        }
    }

    @Override
    public boolean equals(Object o)
    {
        if(!super.equals(o)) return false;
        InjectSecretsBeforeRunTaskNx that = (InjectSecretsBeforeRunTaskNx)o;
        return Objects.equals(project, that.project) && Objects.equals(projectId, that.projectId)
                && Objects.equals(environment, that.environment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), project, projectId, environment);
    }
}
