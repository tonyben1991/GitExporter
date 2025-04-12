package tech.omnidigit.gitexporter.helper

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.CompilerModuleExtension
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiManager
import git4idea.GitCommit
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import tech.omnidigit.gitexporter.config.GitExportConfig
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/*
 * Copyright (c) 2025 Tony Ben
 */

class GitExportTask(
    private val project: Project,
    private val config: GitExportConfig,
    private val commits: List<GitCommit>
) : Task.Backgroundable(project, "Exporting Git Changes", true) {

    override fun run(indicator: ProgressIndicator) {
        val repo = findGitRepo() ?: throw IllegalStateException("No Git repository found in the project.")
        if (commits.isEmpty()) {
            println("No commits to export.")
            return
        }
        exportFiles(commits)
    }

    private fun findGitRepo(): GitRepository? {
        return GitRepositoryManager.getInstance(project).repositories.firstOrNull()
    }

    private fun exportFiles(commits: List<GitCommit>) {
        commits.forEach { commit ->
            commit.changes
                .mapNotNull { it.virtualFile }
                .forEach { file ->
                    if (file.name.endsWith(".java")) {
                        findAndExportClassFile(file)
                    } else {
                        exportFile(file)
                    }
                }
        }
    }

    private fun findAndExportClassFile(javaFile: VirtualFile) {
        val psiJavaFile = runReadAction {
            if (javaFile.isValid) {
                PsiManager.getInstance(project).findFile(javaFile) as? PsiJavaFile
            } else null
        }

        psiJavaFile?.let {
            true.getClassOutputFiles(it)
                .takeIf { files -> files.isNotEmpty() }
                ?.let(::exportFiles)
        }
    }

    private fun Boolean.getClassOutputFiles(javaFile: PsiJavaFile): List<VirtualFile> {
        val module = ModuleUtil.findModuleForFile(javaFile) ?: return emptyList()
        val outputDir = CompilerModuleExtension.getInstance(module)?.compilerOutputPath ?: return emptyList()

        return runReadAction {
            if (!javaFile.isValid) return@runReadAction emptyList()

            val packageName = javaFile.packageName
            val className = javaFile.name.removeSuffix(".java")
            val paths = mutableListOf<VirtualFile>()

            // Main class
            buildClassPath(outputDir.path, packageName, className)
                .let { LocalFileSystem.getInstance().findFileByPath(it) }
                ?.let { paths.add(it) }

            // Inner classes
            if (this) {
                val outputFolder = File(
                    outputDir.path,
                    packageName.replace('.', File.separatorChar)
                )

                if (outputFolder.exists()) {
                    val pattern = """$className\$\d*.class""".toRegex()
                    outputFolder.listFiles { _, name -> pattern.matches(name) }
                        ?.forEach { innerClass ->
                            LocalFileSystem.getInstance().findFileByPath(innerClass.absolutePath)
                                ?.let { paths.add(it) }
                        }
                }
            }

            paths
        }
    }

    private fun buildClassPath(outputRoot: String, packageName: String, className: String): String {
        val packagePath = packageName.replace('.', File.separatorChar)
        return "$outputRoot${File.separator}$packagePath${File.separator}$className.class"
    }

    private fun exportFiles(files: List<VirtualFile>) {
        files.forEach(::exportFile)
    }

    private fun exportFile(file: VirtualFile) {
        println("Exporting file: ${file.path}")
        try {
            val targetDir = File(config.targetDir)
            val basePath = project.basePath ?: return
            
            val sourceFile = File(file.path)
            val relativePath = file.path.removePrefix(basePath)
            val targetFile = File(targetDir, relativePath)
            
            targetFile.parentFile?.mkdirs()
            Files.copy(sourceFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}