package tech.omnidigit.gitexporter.store;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/*
 * Copyright (c) 2025 Tony Ben
 */

@State(
        name = "GitExporterSettings",
        storages = @Storage(value = "gitExporterSettings.xml", roamingType = RoamingType.PER_OS)
)
public class GitExportSettings implements PersistentStateComponent<GitExportSettings.State> {

    // 内部状态类
    public static class State {
        public String targetDir = "";
        public String startDate = "";
        public String endDate = "";
        public String author = "";
        public String message = "";
    }

    private State state = new State();

    @Override
    public @Nullable State getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = state;
    }

    public static GitExportSettings getInstance(Project project) {
        return project.getService(GitExportSettings.class);
    }

    public String getTargetDir() {
        return state.targetDir;
    }

    public void setTargetDir(String value) {
        state.targetDir = value;
    }

    public String getStartDate() {
        return state.startDate;
    }

    public void setStartDate(String startDate) {
        state.startDate = startDate;
    }

    public String getEndDate() {
        return state.endDate;
    }

    public void setEndDate(String endDate) {
        state.endDate = endDate;
    }

    public String getAuthor() {
        return state.author;
    }

    public void setAuthor(String author) {
        state.author = author;
    }

    public String getMessage() {
        return state.message;
    }

    public void setMessage(String message) {
        state.message = message;
    }

}
