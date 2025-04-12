package tech.omnidigit.gitexporter.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.components.JBScrollPane;
import git4idea.GitCommit;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;

/*
 * Copyright (c) 2025 Tony Ben
 */

public class CommitPreviewDialog extends DialogWrapper {

    private final Project project;

    private final JList<GitCommit> commitList;
    private final DefaultListModel<GitCommit> model;
    private final Map<GitCommit, Boolean> commitSelections;
    private final JCheckBox exportClassFiles;

    private final JTree fileTree;
    private final DefaultTreeModel treeModel;

    public CommitPreviewDialog(Project project) {
        super(project);

        this.project = project;
        model = new DefaultListModel<>();
        commitList = new JList<>(model);
        commitList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 初始化文件树
        treeModel = new DefaultTreeModel(new DefaultMutableTreeNode("请选择一个提交"));
        fileTree = new JTree(treeModel);
        fileTree.setRootVisible(false);
        fileTree.setShowsRootHandles(true);

        // 创建复选框映射
        commitSelections = new HashMap<>();

        // 设置自定义渲染器
        commitList.setCellRenderer(new CommitListRenderer());

        commitList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                GitCommit selected = commitList.getSelectedValue();
                if (selected != null) {
                    updateFileTree(selected);
                }
            }
        });

        // 添加鼠标监听器处理选择
        commitList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int index = commitList.locationToIndex(e.getPoint());
                if (index >= 0) {
                    boolean flag = false;
                    Rectangle rect = commitList.getCellBounds(index, index);
                    if (e.getClickCount() == 1 && rect.contains(e.getPoint()) && e.getX() <= rect.x + 20) {
                        flag = true;
                    } else if (e.getClickCount() == 2 && rect.contains(e.getPoint()) && e.getX() > rect.x + 20) {
                        flag = true;
                    }
                    if (flag) {
                        GitCommit commit = model.getElementAt(index);
                        toggleSelection(commit);
                    }
                }
            }
        });

        // 添加导出选项
        exportClassFiles = new JCheckBox("同时导出对应的.class文件", true);

        setTitle("选择要导出的提交");
        init();
    }

    @Override
    protected JComponent createCenterPanel() {


        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        JPanel commitPanel = new JPanel(new BorderLayout());
        // 添加滚动面板
        commitPanel.add(new JBScrollPane(commitList), BorderLayout.CENTER);

        // 添加按钮面板
        JPanel buttonPanel = new JPanel();
        JButton selectAllButton = new JButton("全选");
        JButton deselectAllButton = new JButton("取消全选");
        JButton invertSelectionButton = new JButton("反选");

        selectAllButton.addActionListener(e -> selectAllCommits());
        deselectAllButton.addActionListener(e -> deselectAllCommits());
        invertSelectionButton.addActionListener(e -> toggleSelection());

        buttonPanel.add(selectAllButton);
        buttonPanel.add(deselectAllButton);
        buttonPanel.add(invertSelectionButton);

        commitPanel.add(buttonPanel, BorderLayout.NORTH);
        commitPanel.add(exportClassFiles, BorderLayout.SOUTH);

        // 文件树面板
        JPanel fileTreePanel = new JPanel(new BorderLayout());
        fileTreePanel.add(new JBScrollPane(fileTree), BorderLayout.CENTER);
        fileTreePanel.setBorder(BorderFactory.createTitledBorder("修改的文件"));
        fileTreePanel.setPreferredSize(new Dimension(400, 200));

        // 主布局
        splitPane.setLeftComponent(commitPanel);
        splitPane.setRightComponent(fileTreePanel);
        splitPane.setDividerLocation(0.5); // 初始分割比例为50%

        return splitPane;
    }

    private void updateFileTree(GitCommit commit) {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("修改的文件");
        Collection<VirtualFile> files = getFiles(commit);

        if (files != null && !files.isEmpty()) {
            // 获取项目基础路径
            String basePath = project.getBasePath();
            int basePathLength = basePath != null ? basePath.lastIndexOf("/") : 0;

            // 构建路径树
            Map<String, DefaultMutableTreeNode> nodeMap = new HashMap<>();
            nodeMap.put("", root);

            for (VirtualFile file : files) {
                String fullPath = file.getPath();
                // 从完整路径中去掉项目基础路径
                String relativePath = basePath != null && fullPath.startsWith(basePath)
                        ? fullPath.substring(basePathLength)
                        : fullPath;

                // 去掉开头的斜杠
                if (relativePath.startsWith("/")) {
                    relativePath = relativePath.substring(1);
                }

                String[] parts = relativePath.split("/");
                DefaultMutableTreeNode parent = root;
                StringBuilder currentPath = new StringBuilder();

                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) currentPath.append("/");
                    currentPath.append(parts[i]);

                    String pathKey = currentPath.toString();
                    DefaultMutableTreeNode node = nodeMap.get(pathKey);

                    if (node == null) {
                        node = new DefaultMutableTreeNode(parts[i]);
                        parent.add(node);
                        nodeMap.put(pathKey, node);
                    }
                    parent = node;
                }
            }
        }

        treeModel.setRoot(root);
        expandAllNodes(fileTree, 0, fileTree.getRowCount());
    }

    private void expandAllNodes(JTree tree, int startingIndex, int rowCount) {
        for (int i = startingIndex; i < rowCount; ++i) {
            tree.expandRow(i);
        }

        if (tree.getRowCount() != rowCount) {
            expandAllNodes(tree, rowCount, tree.getRowCount());
        }
    }

    private void toggleSelection(GitCommit commit) {
        boolean selected = !commitSelections.getOrDefault(commit, false);
        commitSelections.put(commit, selected);
        commitList.repaint();
    }

    // 全选
    private void selectAllCommits() {
        commitSelections.forEach((commit, selected) -> {
            commitSelections.put(commit, true);
        });
        commitList.repaint();
    }

    // 取消全选
    private void deselectAllCommits() {
        commitSelections.forEach((commit, selected) -> {
            commitSelections.put(commit, false);
        });
        commitList.repaint();
    }

    // 反选
    private void toggleSelection() {
        commitSelections.forEach((commit, selected) -> {
            commitSelections.put(commit, !selected);
        });
        commitList.repaint();
    }

    public List<GitCommit> getSelectedCommits() {
        return commitSelections.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(Map.Entry::getKey)
                .toList();
    }

    public boolean shouldExportClassFiles() {
        return exportClassFiles.isSelected();
    }

    public void addCommits(List<GitCommit> commits) {
        commits.forEach(this::addCommit);
    }

    // 添加新的提交记录
    public void addCommit(GitCommit commit) {
        model.addElement(commit);
        commitSelections.put(commit, false);
    }

    // 删除指定的提交记录
    public void removeCommit(GitCommit commit) {
        model.removeElement(commit);
        commitSelections.remove(commit);
    }

    // 清空所有提交记录
    public void clearCommits() {
        model.clear();
        commitSelections.clear();
    }

    // 自定义列表渲染器
    private class CommitListRenderer extends JPanel implements ListCellRenderer<GitCommit> {
        private final JCheckBox checkBox = new JCheckBox();
        private final JLabel label = new JLabel();

        private GitCommit currentCommit;

        private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"); // 定义时间格式

        public CommitListRenderer() {
            setOpaque(true);
            setLayout(new BorderLayout());
            add(checkBox, BorderLayout.WEST);
            add(Box.createHorizontalStrut(10), BorderLayout.CENTER);
            add(label, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends GitCommit> list, GitCommit commit, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            currentCommit = commit;

            label.setText(String.format("%s - %s", dateFormat.format(commit.getAuthorTime()), commit.getSubject()));
            checkBox.setSelected(commitSelections.getOrDefault(commit, false));

            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            label.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            checkBox.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());

            return this;
        }
    }


    private Collection<VirtualFile> getFiles(GitCommit commit) {
        Collection<VirtualFile> files = commit.getChanges().stream()
                .map(Change::getVirtualFile)
                .filter(Objects::nonNull)
                .toList();
        return files;
    }
}
