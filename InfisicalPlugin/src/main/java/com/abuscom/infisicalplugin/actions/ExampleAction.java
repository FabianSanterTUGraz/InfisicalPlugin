package com.abuscom.infisicalplugin.actions;

import com.abuscom.infisicalplugin.errorMessages.ErrorNotifier;
import com.abuscom.infisicalplugin.infisical.cache.Cache;
import com.abuscom.infisicalplugin.infisical.http.InfisicalHttpException;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.abuscom.infisicalplugin.infisical.cache.Secrets.*;

public class ExampleAction extends DumbAwareAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        Dialog dialog = new Dialog(project);
        if (!dialog.showAndGet()) {
            return;
        }
        //da die logik bauen
        String inputPath = dialog.getPath() + "\\.env";
        String environment = dialog.getSelectedEnvironment();

        assert project != null;
        String path =  project.getBasePath();
        path = Paths.get(Objects.requireNonNull(project.getBasePath()), inputPath).toString();
        System.out.println("test:" + path + environment);

        if(!Files.exists(Paths.get(path)))
        {
            ErrorNotifier.notify(project,"No valid file path!");
            return;
        }

        Cache.getInstance().setRunConfigSelection(true, environment);
        try {
            Cache.getInstance().setCache(project);
        } catch (IOException | InfisicalHttpException event) {
            ErrorNotifier.notify(project, event);
            return;
        }

        Map<String,String> secrets = Cache.getInstance().getSecrets();

        List<String> lines = secrets.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .toList();
        try {
            Files.write(Paths.get(path),lines);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        System.out.println("test:" + lines);
    }
}
