package com.abuscom.infisicalplugin.infisical.cache;

import java.util.List;

public record EnviromentsAPICallResponse(Workspace workspace) {
    public record Workspace(List<EnvironmentEntry> environments) {}
}
