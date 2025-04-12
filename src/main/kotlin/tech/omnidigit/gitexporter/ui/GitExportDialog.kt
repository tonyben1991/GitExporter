package tech.omnidigit.gitexporter.ui

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import tech.omnidigit.gitexporter.config.GitExportConfig
import tech.omnidigit.gitexporter.store.GitExportSettings
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JLabel

/*
 * Copyright (c) 2025 Tony Ben
 */

class GitExportDialog(private val project: Project?) : DialogWrapper(project) {
    
    private val targetDirField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(
            "选择目标路径",
            "选择导出的目标路径",
            null,
            FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
                .withFileFilter(VirtualFile::isDirectory)
        )
    }
    private val startDateField = JBTextField(20)
    private val endDateField = JBTextField(20)
    private val authorField = JBTextField(20)
    private val messageField = JBTextField(20)
    private val dateFormat = "yyyy/MM/dd HH:mm:ss"

    init {
        title = "Git Export Configuration"
        loadSettings()
        init()
    }

    private fun loadSettings() {
        project?.let {
            val settings = GitExportSettings.getInstance(it)
            targetDirField.text = settings.targetDir
            startDateField.text = settings.startDate
            endDateField.text = settings.endDate
            authorField.text = settings.author
            messageField.text = settings.message
        }
    }

    private fun saveSettings() {
        project?.let {
            val settings = GitExportSettings.getInstance(it)
            settings.targetDir = targetDirField.text
            settings.startDate = startDateField.text
            settings.endDate = endDateField.text
            settings.author = authorField.text
            settings.message = messageField.text
        }
    }

    override fun createCenterPanel(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent("目标路径:", targetDirField)
            .addLabeledComponent("开始时间:", startDateField)
            .addLabeledComponent("结束时间:", endDateField)
            .addLabeledComponent("作者:", authorField)
            .addLabeledComponent("信息:", messageField)
            .addComponent(createHelpLabel())
            .panel
    }

    private fun createHelpLabel(): JComponent {
        return JLabel("<html><i>如果不需要某个过滤保持空白</i></html>").apply {
            border = BorderFactory.createEmptyBorder(5, 0, 0, 0)
        }
    }

    override fun doOKAction() {
        validateFields().takeIf { it.okEnabled }?.let {
            saveSettings()
            super.doOKAction()
        }
    }

    private fun validateFields(): ValidationInfo {
        return when {
            targetDirField.text.trim().isEmpty() ->
                ValidationInfo("目标目录不能为空", targetDirField)

            validateDateFields() != null -> validateDateFields()!!
            else -> ValidationInfo("OK").withOKEnabled()
        }
    }

    private fun validateDateFields(): ValidationInfo? {
        return try {
            val formatter = DateTimeFormatter.ofPattern(dateFormat)
            startDateField.text.trim().takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it, formatter) }
            endDateField.text.trim().takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it, formatter) }
            null
        } catch (e: DateTimeParseException) {
            ValidationInfo("Date format must be $dateFormat", startDateField)
        }
    }

    val config: GitExportConfig
        get() = GitExportConfig(
            targetDirField.text.trim(),
            authorField.text.trim(),
            messageField.text.trim(),
            startDateField.text.trim(),
            endDateField.text.trim()
        )
}