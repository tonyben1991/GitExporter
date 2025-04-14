package tech.omnidigit.gitexporter.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBScrollPane
import git4idea.GitCommit
import tech.omnidigit.gitexporter.resource.CommitPreviewBundle
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.text.SimpleDateFormat
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/*
 * Copyright (c) 2025 Tony Ben
 */

class CommitPreviewDialog(private val project: Project) : DialogWrapper(project) {

    private val model = DefaultListModel<GitCommit>()
    private val commitList = JList(model).apply {
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        cellRenderer = CommitListRenderer()
    }

    private val treeModel = DefaultTreeModel(
        DefaultMutableTreeNode(CommitPreviewBundle.message("placeholder.select_commit"))
    )
    private val fileTree = JTree(treeModel).apply {
        isRootVisible = false
        showsRootHandles = true
    }

    private val commitSelections = mutableMapOf<GitCommit, Boolean>()
    private val exportClassFiles = JCheckBox(
        CommitPreviewBundle.message("checkbox.export_class"),
        true
    )
    private val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

    init {
        title = CommitPreviewBundle.message("dialog.title")
        init()

        commitList.addListSelectionListener { e ->
            if (!e.valueIsAdjusting) {
                commitList.selectedValue?.let { updateFileTree(it) }
            }
        }

        commitList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                val index = commitList.locationToIndex(e.point)
                if (index >= 0) {
                    val rect = commitList.getCellBounds(index, index)
                    val shouldToggle = when {
                        e.clickCount == 1 && rect.contains(e.point) && e.x <= rect.x + 20 -> true
                        e.clickCount == 2 && rect.contains(e.point) && e.x > rect.x + 20 -> true
                        else -> false
                    }
                    if (shouldToggle) {
                        model.getElementAt(index)?.let { toggleSelection(it) }
                    }
                }
            }
        })
    }

    override fun createCenterPanel(): JComponent {
        return JSplitPane(JSplitPane.HORIZONTAL_SPLIT).apply {
            leftComponent = createCommitPanel()
            rightComponent = createFileTreePanel()
        }
    }

    private fun createCommitPanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            add(JBScrollPane(commitList), BorderLayout.CENTER)
            add(createButtonPanel(), BorderLayout.NORTH)
            add(exportClassFiles, BorderLayout.SOUTH)
        }
    }

    private fun createButtonPanel(): JPanel {
        return JPanel().apply {
            add(JButton(CommitPreviewBundle.message("button.select_all")).apply {
                addActionListener { selectAllCommits() }
            })
            add(JButton(CommitPreviewBundle.message("button.deselect_all")).apply {
                addActionListener { deselectAllCommits() }
            })
            add(JButton(CommitPreviewBundle.message("button.invert")).apply {
                addActionListener { toggleSelection() }
            })
        }
    }

    private fun createFileTreePanel(): JPanel {
        return JPanel(BorderLayout()).apply {
            add(JBScrollPane(fileTree), BorderLayout.CENTER)
            border = BorderFactory.createTitledBorder(CommitPreviewBundle.message("tree.root"))
            preferredSize = Dimension(400, 200)
        }
    }

    private fun updateFileTree(commit: GitCommit) {
        val root = DefaultMutableTreeNode(CommitPreviewBundle.message("tree.root"))
        getFiles(commit)?.takeIf { it.isNotEmpty() }?.let { files ->
            val basePath = project.basePath
            val basePathLength = basePath?.lastIndexOf('/') ?: 0
            val nodeMap = mutableMapOf("" to root)

            files.forEach { file ->
                val fullPath = file.path
                val relativePath = basePath?.let { bp ->
                    if (fullPath.startsWith(bp)) fullPath.substring(basePathLength) else fullPath
                }?.removePrefix("/") ?: fullPath

                var parent = root
                val currentPath = StringBuilder()

                relativePath.split('/').forEach { part ->
                    if (currentPath.isNotEmpty()) currentPath.append('/')
                    currentPath.append(part)

                    val pathKey = currentPath.toString()
                    val node = nodeMap.getOrPut(pathKey) {
                        DefaultMutableTreeNode(part).also { parent.add(it) }
                    }
                    parent = node
                }
            }
        }

        treeModel.setRoot(root)
        expandAllNodes()
    }

    private fun expandAllNodes() {
        var row = 0
        while (row < fileTree.rowCount) {
            fileTree.expandRow(row++)
        }
    }

    private fun toggleSelection(commit: GitCommit) {
        commitSelections[commit] = !commitSelections.getOrDefault(commit, false)
        commitList.repaint()
    }

    private fun selectAllCommits() {
        commitSelections.keys.forEach { commitSelections[it] = true }
        commitList.repaint()
    }

    private fun deselectAllCommits() {
        commitSelections.keys.forEach { commitSelections[it] = false }
        commitList.repaint()
    }

    private fun toggleSelection() {
        commitSelections.keys.forEach { commitSelections[it] = !commitSelections[it]!! }
        commitList.repaint()
    }

    val selectedCommits: List<GitCommit>
        get() = commitSelections.filter { it.value }.keys.toList()

    val shouldExportClassFiles: Boolean
        get() = exportClassFiles.isSelected

    fun addCommits(commits: List<GitCommit>) {
        commits.forEach { addCommit(it) }
    }

    fun addCommit(commit: GitCommit) {
        model.addElement(commit)
        commitSelections[commit] = false
    }

    fun removeCommit(commit: GitCommit) {
        model.removeElement(commit)
        commitSelections.remove(commit)
    }

    fun clearCommits() {
        model.clear()
        commitSelections.clear()
    }

    private inner class CommitListRenderer : JPanel(BorderLayout()), ListCellRenderer<GitCommit> {
        private val checkBox = JCheckBox()
        private val label = JLabel()

        init {
            isOpaque = true
            add(checkBox, BorderLayout.WEST)
            add(Box.createHorizontalStrut(10), BorderLayout.CENTER)
            add(label, BorderLayout.CENTER)
        }

        override fun getListCellRendererComponent(
            list: JList<out GitCommit>,
            commit: GitCommit,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            label.text = "${dateFormat.format(commit.authorTime)} - ${commit.subject}"
            checkBox.isSelected = commitSelections.getOrDefault(commit, false)

            background = if (isSelected) list.selectionBackground else list.background
            label.foreground = if (isSelected) list.selectionForeground else list.foreground
            checkBox.foreground = label.foreground

            return this
        }
    }

    private fun getFiles(commit: GitCommit): Collection<VirtualFile>? {
        return commit.changes
            .mapNotNull { it.virtualFile }
            .takeIf { it.isNotEmpty() }
    }
}