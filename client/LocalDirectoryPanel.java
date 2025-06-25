package com.Frank.client;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LocalDirectoryPanel extends JPanel {
    private JTable fileTable;
    private DefaultTableModel tableModel;
    public static JTextField pathField;
    private File currentDir;
    private JButton backButton, deleteButton, refreshButton, addButton;
    private ClientForm clientForm;

    public LocalDirectoryPanel(ClientForm clientForm) {
        this.clientForm = clientForm;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("本地目录"));
        setBackground(new Color(240, 240, 240));

        initComponents();
        loadDirectory(new File("F:\\Desk\\计算机系统能力实训\\"));
    }

    private void initComponents() {
        // 顶部工具栏
        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.setBackground(new Color(240, 240, 240));

        // 返回按钮
        backButton = new JButton("←");
        backButton.setBackground(new Color(100, 200, 100));
        backButton.setForeground(Color.WHITE);
        backButton.addActionListener(e -> goBack());

        // 路径输入框
        pathField = new JTextField();
        pathField.addActionListener(e -> {
            File newDir = new File(pathField.getText());
            if (newDir.exists() && newDir.isDirectory()) {
                loadDirectory(newDir);
            } else {
                JOptionPane.showMessageDialog(this, "路径不存在或不是目录", "错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        // 删除按钮
        deleteButton = new JButton("×");
        deleteButton.setBackground(new Color(200, 100, 100));
        deleteButton.setForeground(Color.WHITE);
        deleteButton.addActionListener(e -> deleteSelected());

        // 刷新按钮
        refreshButton = new JButton("↻");
        refreshButton.setBackground(new Color(100, 200, 100));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.addActionListener(e -> loadDirectory(currentDir));

        // 添加按钮
        addButton = new JButton("+");
        addButton.setBackground(new Color(100, 100, 200));
        addButton.setForeground(Color.WHITE);
        addButton.addActionListener(e -> showAddDialog());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.add(backButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(addButton);

        toolbarPanel.add(buttonPanel, BorderLayout.WEST);
        toolbarPanel.add(pathField, BorderLayout.CENTER);
        add(toolbarPanel, BorderLayout.NORTH);

        // 文件表格
        String[] columns = {"名称", "大小", "修改时间"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        fileTable = new JTable(tableModel);
        fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        fileTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = fileTable.rowAtPoint(e.getPoint());
                    if (row >= 0) {
                        fileTable.setRowSelectionInterval(row, row);
                        String name = (String) tableModel.getValueAt(row, 0);
                        File selected = new File(currentDir, name);

                        // 创建右键菜单
                        JPopupMenu popupMenu = new JPopupMenu();

                        if (selected.isDirectory()) {
                            // 文件夹右键菜单
                            JMenuItem uploadDirItem = new JMenuItem("上传文件夹");
                            uploadDirItem.addActionListener(ev -> {
                                if (ClientForm.currentUser.getUpload()) {
                                    int option = JOptionPane.showConfirmDialog(
                                            LocalDirectoryPanel.this,
                                            "是否要上传文件夹: " + name + "?",
                                            "上传确认",
                                            JOptionPane.YES_NO_OPTION
                                    );
                                    if (option == JOptionPane.YES_OPTION) {
                                        uploadDirectory(selected);
                                    }
                                } else {
                                    JOptionPane.showMessageDialog(LocalDirectoryPanel.this, "你没有上传权限", "错误", JOptionPane.ERROR_MESSAGE);
                                }
                            });
                            popupMenu.add(uploadDirItem);
                        } else {
                            // 文件右键菜单
                            JMenuItem uploadItem = new JMenuItem("上传文件");
                            uploadItem.addActionListener(ev -> {
                                if (ClientForm.currentUser.getUpload()) {
                                    int option = JOptionPane.showConfirmDialog(
                                            LocalDirectoryPanel.this,
                                            "是否要上传文件: " + name + "?",
                                            "上传确认",
                                            JOptionPane.YES_NO_OPTION
                                    );
                                    if (option == JOptionPane.YES_OPTION) {
                                        uploadFile(selected);
                                    }
                                } else {
                                    JOptionPane.showMessageDialog(LocalDirectoryPanel.this, "你没有上传权限", "错误", JOptionPane.ERROR_MESSAGE);
                                }
                            });
                            popupMenu.add(uploadItem);
                        }

                        // 显示菜单
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                } else if (e.getClickCount() == 2) {
                    // 保留原有的双击功能
                    int row = fileTable.getSelectedRow();
                    if (row >= 0) {
                        String name = (String) tableModel.getValueAt(row, 0);
                        File selected = new File(currentDir, name);
                        if (selected.isDirectory()) {
                            loadDirectory(selected);
                        }
                    }
                }
            }
        });

        add(new JScrollPane(fileTable), BorderLayout.CENTER);
    }

    private void uploadFile(File file) {
        System.out.println("开始上传文件: " + file.getName());
        ClientCtrlThread.addMessage("STOR " + file.getName());

        try {
            Socket socket = new Socket("127.0.0.1", 20);
            ClientUploadThread clientUploadThread = new ClientUploadThread(socket, file);

            // 添加进度监听器
            clientUploadThread.addProgressListener((progress, transferredBytes, totalBytes, elapsedTime) -> {
                SwingUtilities.invokeLater(() -> {
                    clientForm.updateProgress(progress, transferredBytes, totalBytes, elapsedTime);
                });
            });

            clientUploadThread.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void uploadDirectory(File dir) {
        System.out.println("开始上传文件夹: " + dir.getName());
        // 递归遍历文件夹
        recursiveUpload(dir, "");
    }

    private void recursiveUpload(File remoteDir, String remotePath) {
        File[] files = remoteDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 如果是文件夹，递归上传
                    String newRemotePath = remotePath + "/" + file.getName();
                    recursiveUpload(file, newRemotePath);
                } else {
                    // 如果是文件，调用上传文件的方法
                    try {
                        uploadFile(file, remotePath);
                    } catch (Exception ex) {
                        System.err.println("上传文件 " + file.getName() + " 失败: " + ex.getMessage());
                    }
                }
            }
        }
    }

    private void uploadFile(File file, String remotePath) {
        System.out.println("开始上传文件: " + file.getName());
        ClientCtrlThread.addMessage("STOR " + remotePath + "/" + file.getName());

        try {
            Socket socket = new Socket("127.0.0.1", 20);
            ClientUploadThread clientUploadThread = new ClientUploadThread(socket, file);

            // 添加进度监听器
            clientUploadThread.addProgressListener((progress, transferredBytes, totalBytes, elapsedTime) -> {
                SwingUtilities.invokeLater(() -> {
                    clientForm.updateProgress(progress, transferredBytes, totalBytes, elapsedTime);
                });
            });

            clientUploadThread.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void loadDirectory(File dir) {
        currentDir = dir;
        pathField.setText(dir.getAbsolutePath());
        tableModel.setRowCount(0);

        File[] files = dir.listFiles();
        if (files != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            // 添加返回上级目录项
            if (dir.getParentFile() != null) {
                tableModel.addRow(new Object[]{"..", "", ""});
            }

            // 添加文件和目录
            for (File file : files) {
                String size = file.isDirectory() ? "" : formatFileSize(file.length());
                String date = sdf.format(new Date(file.lastModified()));
                tableModel.addRow(new Object[]{file.getName(), size, date});
            }
        }
    }

    private void goBack() {
        if (currentDir.getParentFile() != null) {
            loadDirectory(currentDir.getParentFile());
        }
    }

    private void deleteSelected() {
        int row = fileTable.getSelectedRow();
        if (row >= 0) {
            String name = (String) tableModel.getValueAt(row, 0);
            if (name.equals("..")) return;

            File toDelete = new File(currentDir, name);
            int confirm = JOptionPane.showConfirmDialog(this,
                    "确定要删除 '" + name + "' 吗?", "确认删除",
                    JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                if (toDelete.isDirectory()) {
                    deleteDirectory(toDelete);
                } else {
                    toDelete.delete();
                }
                loadDirectory(currentDir);
            }
        }
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    private void showAddDialog() {
        JPanel panel = new JPanel(new GridLayout(2, 1));
        panel.add(new JLabel("选择操作:"));

        JButton addFileBtn = new JButton("添加文件");
        JButton addDirBtn = new JButton("添加文件夹");

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(addFileBtn);
        buttonPanel.add(addDirBtn);
        panel.add(buttonPanel);

        JDialog dialog = new JDialog();
        dialog.setTitle("添加");
        dialog.setModal(true);
        dialog.setSize(300, 150);
        dialog.setLocationRelativeTo(this);
        dialog.add(panel);

        addFileBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "输入文件名:");
            if (name != null && !name.trim().isEmpty()) {
                try {
                    new File(currentDir, name).createNewFile();
                    loadDirectory(currentDir);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "创建文件失败: " + ex.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
            dialog.dispose();
        });

        addDirBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "输入文件夹名:");
            if (name != null && !name.trim().isEmpty()) {
                if (new File(currentDir, name).mkdir()) {
                    loadDirectory(currentDir);
                } else {
                    JOptionPane.showMessageDialog(this, "创建文件夹失败",
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
            dialog.dispose();
        });

        dialog.setVisible(true);
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("本地目录浏览器");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);

            LocalDirectoryPanel panel = new LocalDirectoryPanel(null);
            frame.add(panel);
            frame.setVisible(true);
        });
    }
}