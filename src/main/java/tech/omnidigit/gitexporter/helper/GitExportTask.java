package tech.omnidigit.gitexporter.helper;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CompilerModuleExtension;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import git4idea.GitCommit;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;
import tech.omnidigit.gitexporter.config.GitExportConfig;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/*
 * Copyright (c) 2025 Tony Ben
 */

public class GitExportTask extends Task.Backgroundable {
    private final Project project;
    private final GitExportConfig config;
    private final List<GitCommit> commits;

    public GitExportTask(Project project, GitExportConfig config, List<GitCommit> commits) {
        super(project, "Exporting Git Changes", true);
        this.project = project;
        this.config = config;
        this.commits = commits;
    }

    @Override
    public void run(@NotNull ProgressIndicator indicator) {
        // 实现导出逻辑
        GitRepository repo = findGitRepo();
        if (repo == null) {
            throw new IllegalStateException("No Git repository found in the project.");
        }

        if (commits.isEmpty()) {
            System.out.println("No commits to export.");
            return;
        }

        exportFiles(commits);
    }

    public GitRepository findGitRepo() {
        // 查找项目对应的 Git 仓库
        return GitRepositoryManager.getInstance(project).getRepositories().get(0);
    }

    public void exportFiles(List<GitCommit> commits) {
        for (GitCommit commit : commits) {
            // 获取提交中修改的文件
            Collection<VirtualFile> files = commit.getChanges().stream()
                    .map(Change::getVirtualFile)
                    .filter(Objects::nonNull)
                    .toList();

            for (VirtualFile file : files) {
                // 导出文件
                // 如果是 Java 文件，查找对应的 class 文件
                if (file.getName().endsWith(".java")) {
                    findAndExportClassFile(file);
                } else {
                    exportFile(file);
                }
            }
        }
    }

    private void findAndExportClassFile(VirtualFile javaFile) {
        PsiJavaFile psiJavaFile = ApplicationManager.getApplication().runReadAction((Computable<PsiJavaFile>) () -> {
            if (javaFile != null && javaFile.isValid()) {
                return (PsiJavaFile) PsiManager.getInstance(getProject()).findFile(javaFile);
            }
            return null;
        });
        List<VirtualFile> classOutputFiles = getClassOutputFiles(psiJavaFile, true);

        // 在输出目录中查找 class 文件
        if (classOutputFiles != null && !classOutputFiles.isEmpty()) {
            exportFile(classOutputFiles);
        }
    }

    public List<VirtualFile> getClassOutputFiles(PsiJavaFile javaFile, boolean includeInnerClasses) {
        List<VirtualFile> paths = new ArrayList<>();
        Module module = ModuleUtil.findModuleForFile(javaFile);

        if (module == null) {
            return paths;
        }

        // 获取模块输出目录
        VirtualFile outputDir = CompilerModuleExtension.getInstance(module).getCompilerOutputPath();

        if (outputDir == null) {
            return paths;
        }

        // 获取包名和类名
        String packageName = ApplicationManager.getApplication().runReadAction((Computable<String>) () -> {
            if (javaFile.isValid()) {
                return javaFile.getPackageName();
            }
            return null;
        });
        ;
        String className = javaFile.getName().replace(".java", "");

        // 主类路径
        String mainClassPath = buildClassPath(outputDir.getPath(), packageName, className);
        paths.add(LocalFileSystem.getInstance().findFileByPath(mainClassPath));

        // 内部类路径
        if (includeInnerClasses) {
            File outputFolder = new File(outputDir.getPath(),
                    packageName.replace('.', File.separatorChar));

            if (outputFolder.exists()) {
                String pattern = className + "$*.class";
                File[] innerClasses = outputFolder.listFiles(
                        (dir, name) -> name.matches(pattern.replace("$", "\\$").replace("*", ".*"))
                );

                if (innerClasses != null) {
                    for (File innerClass : innerClasses) {
                        paths.add(LocalFileSystem.getInstance().findFileByPath(innerClass.getAbsolutePath()));
                    }
                }
            }
        }

        return paths;
    }

    private static String buildClassPath(String outputRoot, String packageName, String className) {
        String packagePath = packageName.replace('.', File.separatorChar);
        return outputRoot + File.separator + packagePath + File.separator + className + ".class";
    }

    private void exportFile(List<VirtualFile> files) {
        for (VirtualFile file : files) {
            exportFile(file);
        }
    }

    private void exportFile(VirtualFile file) {
        // 实现文件导出逻辑
        System.out.println("Exporting file: " + file.getPath());
        try {
            // 创建目标目录
            File targetDir = new File(config.targetDir());

            String basePath = project.getBasePath();
            // 复制文件到目标目录
            File sourceFile = new File(file.getPath());
            File targetFile = new File(targetDir, file.getPath().replace(basePath, ""));
            if (!targetFile.getParentFile().exists()) {
                targetFile.getParentFile().mkdirs();
            }
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
