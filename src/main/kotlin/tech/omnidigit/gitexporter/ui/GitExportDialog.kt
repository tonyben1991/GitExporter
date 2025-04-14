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
import tech.omnidigit.gitexporter.resource.GitExportBundle
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
            GitExportBundle.message("button.browse"),
            GitExportBundle.message("label.target_dir"),
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
        title = GitExportBundle.message("dialog.title")
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
            .addLabeledComponent(GitExportBundle.message("label.target_dir"), targetDirField)
            .addLabeledComponent(GitExportBundle.message("label.start_date"), startDateField)
            .addLabeledComponent(GitExportBundle.message("label.end_date"), endDateField)
            .addLabeledComponent(GitExportBundle.message("label.author"), authorField)
            .addLabeledComponent(GitExportBundle.message("label.message"), messageField)
            .addComponent(createHelpLabel())
            .panel
        
        
    }

    private fun createHelpLabel(): JComponent {
        return JLabel("<html><i>${GitExportBundle.message("help.text")}</i></html>").apply {
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
                ValidationInfo(GitExportBundle.message("error.dir_required"), targetDirField)

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
            ValidationInfo(GitExportBundle.message("error.date_format"), startDateField)
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