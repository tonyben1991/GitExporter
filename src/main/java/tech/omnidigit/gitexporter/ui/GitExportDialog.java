package tech.omnidigit.gitexporter.ui;

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.Nullable;
import tech.omnidigit.gitexporter.config.GitExportConfig;
import tech.omnidigit.gitexporter.store.GitExportSettings;

import javax.swing.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/*
 * Copyright (c) 2025 Tony Ben
 */

public class GitExportDialog extends DialogWrapper {

    private final Project project;

    private final TextFieldWithBrowseButton targetDirField = new TextFieldWithBrowseButton();
    private final JBTextField startDateField = new JBTextField(20);
    private final JBTextField endDateField = new JBTextField(20);
    private final JBTextField authorField = new JBTextField(20);
    private final JBTextField messageField = new JBTextField(20);

    public GitExportDialog(@Nullable Project project) {
        super(project);
        this.project = project;
        setTitle("Git Export Configuration");
        initComponents();
        loadSettings();

        init();
    }

    private void loadSettings() {
        GitExportSettings settings = GitExportSettings.getInstance(project);
        targetDirField.setText(settings.getTargetDir());
        startDateField.setText(settings.getStartDate());
        endDateField.setText(settings.getEndDate());
        authorField.setText(settings.getAuthor());
        messageField.setText(settings.getMessage());
    }

    private void saveSettings() {
        GitExportSettings settings = GitExportSettings.getInstance(project);
        settings.setTargetDir(targetDirField.getText());
        settings.setStartDate(startDateField.getText());
        settings.setEndDate(endDateField.getText());
        settings.setAuthor(authorField.getText());
        settings.setMessage(messageField.getText());
    }

    private void initComponents() {
        // 配置路径选择器
        targetDirField.addBrowseFolderListener(
                "选择目标路径",
                "选择导出的目标路径",
                null,
                FileChooserDescriptorFactory.createSingleLocalFileDescriptor()
                        .withFileFilter(VirtualFile::isDirectory)
        );

    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return FormBuilder.createFormBuilder()
                .addLabeledComponent("目标路径:", targetDirField)
                .addLabeledComponent("开始时间:", startDateField)
                .addLabeledComponent("结束时间:", endDateField)
                .addLabeledComponent("作者:", authorField)
                .addLabeledComponent("信息:", messageField)
                .addComponent(createHelpLabel())
                .getPanel();
    }

    private JComponent createHelpLabel() {
        JLabel helpLabel = new JLabel("<html><i>如果不需要某个过滤保持空白</i></html>");
        helpLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        return helpLabel;
    }

    @Override
    protected void doOKAction() {
        if (validateFields().okEnabled) {
            saveSettings();
            super.doOKAction();
        }
    }

    private ValidationInfo validateFields() {
        if (targetDirField.getText().trim().isEmpty()) {
            return new ValidationInfo("目标目录不能为空", targetDirField);
        }

        ValidationInfo dateValidation = validateDateFields();
        if (dateValidation != null) {
            return dateValidation;
        }

        return new ValidationInfo("OK").withOKEnabled();
    }

    private ValidationInfo validateDateFields() {
        String format = "yyyy/MM/dd HH:mm:ss";

        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(format);
            if (!startDateField.getText().trim().isEmpty()) {
                LocalDate.parse(startDateField.getText().trim(), dateTimeFormatter);
            }
            if (!endDateField.getText().trim().isEmpty()) {
                LocalDate.parse(endDateField.getText().trim(), dateTimeFormatter);
            }
        } catch (DateTimeParseException e) {
            e.printStackTrace();
            return new ValidationInfo("Date format must be " + format, startDateField);
        }
        return null;
    }

    public GitExportConfig getConfig() {
        return new GitExportConfig(
                targetDirField.getText().trim(),
                authorField.getText().trim(),
                messageField.getText().trim(),
                startDateField.getText().trim(),
                endDateField.getText().trim()
        );
    }
}
