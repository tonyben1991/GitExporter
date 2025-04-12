package tech.omnidigit.gitexporter.helper

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import git4idea.GitCommit
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import tech.omnidigit.gitexporter.config.GitExportConfig
import tech.omnidigit.gitexporter.ui.CommitPreviewDialog
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/*
 * Copyright (c) 2025 Tony Ben
 */

class GitFilterTask(
    project: Project,
    private val config: GitExportConfig,
    private val previewDialog: CommitPreviewDialog
) : Backgroundable(project, "Git Exporter", true) {

    override fun run(indicator: ProgressIndicator) {
        val repo = findGitRepo() ?: throw IllegalStateException("No Git repository found in the project.")
        previewDialog.addCommits(filterCommits(repo))
    }

    private fun findGitRepo(): GitRepository? {
        return GitRepositoryManager.getInstance(project).repositories.firstOrNull()
    }

    private fun filterCommits(repo: GitRepository): List<GitCommit> {
        return try {
            GitHistoryUtils.history(project, repo.root)
                .filter { commit ->
                    matchesAuthor(commit) &&
                            matchesMessage(commit) &&
                            matchesDateRange(commit)
                }
        } catch (e: VcsException) {
            e.printStackTrace()
            throw RuntimeException(e)
        }
    }

    private fun matchesAuthor(commit: GitCommit): Boolean {
        return config.author.isEmpty() ||
                commit.author.name == config.author
    }

    private fun matchesMessage(commit: GitCommit): Boolean {
        return config.message.isEmpty() ||
                commit.subject.contains(config.message)
    }

    private fun matchesDateRange(commit: GitCommit): Boolean {
        val commitTime = Instant.ofEpochSecond(commit.authorTime)
        return isAfterStartDate(commitTime) && isBeforeEndDate(commitTime)
    }

    private fun isAfterStartDate(commitTime: Instant): Boolean {
        return config.startDate.takeIf { it.isNotEmpty() }?.let { dateStr ->
            parseDateTime(dateStr)?.let { startDate ->
                !commitTime.isBefore(startDate)
            } ?: true // If parsing fails, include the commit
        } ?: true // No start date filter
    }

    private fun isBeforeEndDate(commitTime: Instant): Boolean {
        return config.endDate.takeIf { it.isNotEmpty() }?.let { dateStr ->
            parseDateTime(dateStr)?.let { endDate ->
                !commitTime.isAfter(endDate)
            } ?: true // If parsing fails, include the commit
        } ?: true // No end date filter
    }

    private fun parseDateTime(dateTimeStr: String): Instant? {
        return try {
            DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")
                .parse(dateTimeStr)
                .let { LocalDateTime.from(it) }
                .atZone(ZoneId.systemDefault())
                .toInstant()
        } catch (e: DateTimeParseException) {
            e.printStackTrace()
            null
        }
    }
}