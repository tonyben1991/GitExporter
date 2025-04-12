package tech.omnidigit.gitexporter.helper;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsException;
import git4idea.GitCommit;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;
import tech.omnidigit.gitexporter.config.GitExportConfig;
import tech.omnidigit.gitexporter.ui.CommitPreviewDialog;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

/*
 * Copyright (c) 2025 Tony Ben
 */

public class GitFilterTask extends Task.Backgroundable {

    private final Project project;
    private final GitExportConfig config;

    private final CommitPreviewDialog previewDialog;

    public GitFilterTask(Project project, GitExportConfig config, CommitPreviewDialog previewDialog) {
        super(project, "Exporting Git Changes", true);
        this.project = project;
        this.config = config;
        this.previewDialog = previewDialog;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        // 实现导出逻辑
        GitRepository repo = findGitRepo();
        if (repo == null) {
            throw new IllegalStateException("No Git repository found in the project.");
        }

        previewDialog.addCommits(filterCommits(repo));
    }

    public GitRepository findGitRepo() {
        // 查找项目对应的 Git 仓库
        return GitRepositoryManager.getInstance(project).getRepositories().get(0);
    }

    public List<GitCommit> filterCommits(GitRepository repo) {
        // 根据配置筛选提交
        List<GitCommit> commits;
        try {
            commits = GitHistoryUtils.history(project, repo.getRoot());

            commits = commits.stream()
                    .filter(commit -> config.author() == null || config.author().isEmpty() ||
                            commit.getAuthor().getName().equals(config.author()))
                    .filter(commit -> config.message() == null || config.message().isEmpty() ||
                            commit.getSubject().contains(config.message()))
                    .filter(commit -> {
                        Instant commitTime = Instant.ofEpochSecond(commit.getAuthorTime());

                        // 处理开始日期
                        boolean startDateValid = true;
                        if (config.startDate() != null && !config.startDate().isEmpty()) {
                            try {
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                                LocalDateTime startDateTime = LocalDateTime.parse(config.startDate(), formatter);
                                Instant startDate = startDateTime.atZone(ZoneId.systemDefault()).toInstant();
                                startDateValid = !commitTime.isBefore(startDate);
                            } catch (DateTimeParseException e) {
                                e.printStackTrace();
                            }
                        }

                        // 处理结束日期
                        boolean endDateValid = true;
                        if (config.endDate() != null && !config.endDate().isEmpty()) {
                            try {
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                                LocalDateTime endDateTime = LocalDateTime.parse(config.endDate(), formatter);
                                Instant endDate = endDateTime.atZone(ZoneId.systemDefault()).toInstant();
                                endDateValid = !commitTime.isAfter(endDate);
                            } catch (DateTimeParseException e) {
                                e.printStackTrace();
                            }
                        }

                        return startDateValid && endDateValid;
                    })
                    .collect(Collectors.toList());

        } catch (VcsException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        return commits;
    }

}
