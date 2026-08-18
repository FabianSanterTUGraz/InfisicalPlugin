package com.abuscom.infisicalplugin.infisical.cache.Secrets.ListProjects;

//If project id null then search for projects else take project Id
public record ListProjectsRequest(String projectId) {
}
