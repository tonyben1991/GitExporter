package tech.omnidigit.gitexporter.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import git4idea.GitCommit;
import tech.omnidigit.gitexporter.config.GitExportConfig;
import tech.omnidigit.gitexporter.helper.GitExportTask;
import tech.omnidigit.gitexporter.helper.GitFilterTask;
import tech.omnidigit.gitexporter.ui.CommitPreviewDialog;
import tech.omnidigit.gitexporter.ui.GitExportDialog;

import java.util.List;

/*
 * Copyright (c) 2025 Tony Ben
 */

public class GitExportAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getProject();
        GitExportDialog gitExportDialog = new GitExportDialog(project);
        if (gitExportDialog.showAndGet()) {
            GitExportConfig config = gitExportDialog.getConfig();

            CommitPreviewDialog previewDialog = new CommitPreviewDialog(project);

            GitFilterTask gitFilterTask = new GitFilterTask(project, config, previewDialog);
            gitFilterTask.queue();

            if (previewDialog.showAndGet()) {
                List<GitCommit> selectedCommits = previewDialog.getSelectedCommits();
                GitExportTask gitExportTask = new GitExportTask(project, config, selectedCommits);
                gitExportTask.queue();
            }
        }
    }
}