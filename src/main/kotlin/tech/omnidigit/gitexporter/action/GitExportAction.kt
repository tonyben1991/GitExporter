package tech.omnidigit.gitexporter.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import tech.omnidigit.gitexporter.helper.GitExportTask
import tech.omnidigit.gitexporter.helper.GitFilterTask
import tech.omnidigit.gitexporter.ui.CommitPreviewDialog
import tech.omnidigit.gitexporter.ui.GitExportDialog

/*
 * Copyright (c) 2025 Tony Ben
 */

class GitExportAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val gitExportDialog = GitExportDialog(project)

        if (gitExportDialog.showAndGet()) {
            val config = gitExportDialog.config
            val previewDialog = CommitPreviewDialog(project)

            GitFilterTask(project, config, previewDialog).queue()

            if (previewDialog.showAndGet()) {
                val selectedCommits = previewDialog.selectedCommits
                GitExportTask(project, config, selectedCommits).queue()
            }
        }
    }
}