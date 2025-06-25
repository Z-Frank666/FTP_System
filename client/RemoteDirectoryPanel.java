package com.Frank.client;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.Frank.client.ClientForm.server;


public class RemoteDirectoryPanel extends JPanel {
    private JTable fileTable;
    private DefaultTableModel tableModel;
    private JTextField pathField;
    private File currentDir = new File("F:\\Desk\\计算机系统能力实训\\ServerFileList");
    public JButton backButton, deleteButton, refreshButton, addButton;
    private JButton fetchButton; // 添加获取远程目录的按钮

    private boolean directoryFetched = false;

    public ClientDownloadThread clientDownloadThread;
    public ClientListThread clientListThread;
    private ClientForm clientForm;


    public RemoteDirectoryPanel(ClientForm clientForm) {
        this.clientForm = clientForm;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("远程目录"));
        setBackground(new Color(240, 240, 240));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // 初始化获取远程目录的按钮
        fetchButton = new JButton("获取远程目录");
        fetchButton.addActionListener(e -> fetchRemoteDirectory());
        topPanel.add(fetchButton);

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
        deleteButton.setEnabled(false);
//        if(ClientForm.currentUser.getDeleted())deleteButton.setEnabled(true);
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
        addButton.setEnabled(false);
//        if(ClientForm.currentUser.getCreated())addButton.setEnabled(true);
        addButton.addActionListener(e -> showAddDialog());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.add(backButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(refreshButton);
        buttonPanel.add(addButton);

        toolbarPanel.add(buttonPanel, BorderLayout.WEST);
        toolbarPanel.add(pathField, BorderLayout.CENTER);
        topPanel.add(toolbarPanel);

        add(topPanel, BorderLayout.NORTH);

        // 初始化远程目录表格
        fileTable = new JTable();

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
                            JMenuItem openItem = new JMenuItem("打开");
                            openItem.addActionListener(ev -> loadDirectory(selected));

                            JMenuItem downloadDirItem = new JMenuItem("下载文件夹");
                            downloadDirItem.addActionListener(ev -> {
                                if (ClientForm.currentUser.getDownload()) {
                                    int option = JOptionPane.showConfirmDialog(
                                            RemoteDirectoryPanel.this,
                                            "是否要下载文件夹: " + name + "?",
                                            "下载确认",
                                            JOptionPane.YES_NO_OPTION
                                    );
                                    if (option == JOptionPane.YES_OPTION) {
                                        downloadDirectory(selected);
                                    }
                                } else {
                                    JOptionPane.showMessageDialog(RemoteDirectoryPanel.this, "你没有下载权限", "错误", JOptionPane.ERROR_MESSAGE);
                                }
                            });

                            popupMenu.add(openItem);
                            popupMenu.add(downloadDirItem);
                        } else {
                            // 文件右键菜单
                            JMenuItem downloadItem = new JMenuItem("下载");
                            downloadItem.addActionListener(ev -> {
                                if (ClientForm.currentUser.getDownload()) {
                                    int option = JOptionPane.showConfirmDialog(
                                            RemoteDirectoryPanel.this,
                                            "是否要下载文件: " + name + "?",
                                            "下载确认",
                                            JOptionPane.YES_NO_OPTION
                                    );
                                    if (option == JOptionPane.YES_OPTION) {
                                        try {
                                            downloadFile(selected);
                                        } catch (Exception ex) {
                                            throw new RuntimeException(ex);
                                        }
                                    }
                                } else {
                                    JOptionPane.showMessageDialog(RemoteDirectoryPanel.this, "你没有下载权限", "错误", JOptionPane.ERROR_MESSAGE);
                                }
                            });

                            popupMenu.add(downloadItem);
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

        JScrollPane scrollPane = new JScrollPane(fileTable);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void fetchRemoteDirectory() {
        try {
            System.out.println("开始接收目录：");
            ClientCtrlThread.addMessage("LIST ");
            Socket dataSocket = new Socket(server, 20); // 假设服务器地址和端口
            clientListThread = new ClientListThread(dataSocket);
            clientListThread.start();
            clientListThread.join(); // 等待线程执行完毕

            String directoryInfo = clientListThread.getFileDirectory();
            updateRemoteDirectory(directoryInfo);

            // 隐藏获取按钮
            fetchButton.setVisible(false);
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private void fetchRemoteDirectory(String filePath) {
        try {
            System.out.println("开始接收目录：");
            ClientCtrlThread.addMessage("LIST "+filePath);
            Socket dataSocket = new Socket(server, 20); // 假设服务器地址和端口
            clientListThread = new ClientListThread(dataSocket);
            clientListThread.start();
            clientListThread.join(); // 等待线程执行完毕

            String directoryInfo = clientListThread.getFileDirectory();
            updateRemoteDirectory(directoryInfo);
            // 隐藏获取按钮
            fetchButton.setVisible(false);
        } catch (UnknownHostException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    public void updateRemoteDirectory(String directoryInfo) {
        // 解析原始目录数据（图1格式）
        String[] lines = directoryInfo.split("\n");
        ArrayList<Object> rowData = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            // 使用正则表达式匹配三种格式：
            // 1. 文件：- 大小 时间 文件名
            // 2. 目录：d 大小 时间 目录名
            // 3. 其他情况
            Matcher matcher = Pattern.compile("^(\\S+)\\s+(\\S+)\\s+(\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2})\\s+(.+)$")
                    .matcher(line);

            if (matcher.matches()) {
                String type = matcher.group(1); // - 或 d
                String rawSize = matcher.group(2);
                String time = matcher.group(3);
                String name = matcher.group(4);

                // 转换大小格式（字节 → KB/MB）
                String displaySize = "";
                if (!"0".equals(rawSize) && !rawSize.isEmpty()) {
                    try {
                        long sizeBytes = Long.parseLong(rawSize);
                        displaySize = formatFileSizeTrans(sizeBytes);
                    } catch (NumberFormatException e) {
                        displaySize = rawSize; // 保留原始值
                    }
                }

                // 处理文件名显示（截断长文件名）
                String displayName = name;

                // 添加到表格数据
                rowData.add(new Object[]{displayName, displaySize, time});
            }
        }

        // 更新表格模型
        tableModel = new DefaultTableModel(
                rowData.toArray(new Object[0][]),
                new String[]{"名称", "大小", "修改时间"}
        ) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        pathField.setText(currentDir.getAbsolutePath());

        fileTable.setModel(tableModel);

        directoryFetched = true;
        repaint();
    }

    // 字节单位转换（与图2格式一致）
    private String formatFileSizeTrans(long size) {
        if (size < 1024) {
            return size + "B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1fKB", size / 1024.0);
        } else {
            return String.format("%.1fMB", size / (1024.0 * 1024.0));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!directoryFetched) {
            g.setColor(Color.GRAY);
            g.setFont(new Font("Arial", Font.PLAIN, 20));
            g.drawString("请点击按钮获取远程目录", getWidth() / 2 - 100, getHeight() / 2);
        }
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
        deleteButton.setEnabled(false);

        // 刷新按钮
        refreshButton = new JButton("↻");
        refreshButton.setBackground(new Color(100, 200, 100));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.addActionListener(e -> loadDirectory(currentDir));

        // 添加按钮
        addButton = new JButton("+");
        addButton.setBackground(new Color(100, 100, 200));
        addButton.setForeground(Color.WHITE);
        addButton.setEnabled(false);

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
                            JMenuItem openItem = new JMenuItem("打开");
                            openItem.addActionListener(ev -> loadDirectory(selected));

                            JMenuItem downloadDirItem = new JMenuItem("下载文件夹");
                            downloadDirItem.addActionListener(ev -> {
                                int option = JOptionPane.showConfirmDialog(
                                        RemoteDirectoryPanel.this,
                                        "是否要下载文件夹: " + name + "?",
                                        "下载确认",
                                        JOptionPane.YES_NO_OPTION
                                );
                                if (option == JOptionPane.YES_OPTION) {
                                    downloadDirectory(selected);
                                }
                            });

                            popupMenu.add(openItem);
                            popupMenu.add(downloadDirItem);
                        } else {
                            // 文件右键菜单
                            JMenuItem downloadItem = new JMenuItem("下载");
                            downloadItem.addActionListener(ev -> {
                                int option = JOptionPane.showConfirmDialog(
                                        RemoteDirectoryPanel.this,
                                        "是否要下载文件: " + name + "?",
                                        "下载确认",
                                        JOptionPane.YES_NO_OPTION
                                );
                                if (option == JOptionPane.YES_OPTION) {
                                    try {
                                        downloadFile(selected);
                                    } catch (Exception ex) {
                                        throw new RuntimeException(ex);
                                    }
                                }
                            });

                            popupMenu.add(downloadItem);
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

    private void downloadFile(File file) {
        System.out.println("开始下载文件: " + file.getName());
        ClientCtrlThread.addMessage("RETR " + file.getPath());

        try {
            Socket socket = new Socket(server, 20);
            System.out.println(LocalDirectoryPanel.pathField.getText());
            String basePath = LocalDirectoryPanel.pathField.getText();
            String localFilePath = basePath + "\\" + file.getName();
            clientDownloadThread = new ClientDownloadThread(socket, localFilePath);

            // 添加进度监听器
            clientDownloadThread.addProgressListener((progress, transferredBytes, totalBytes, elapsedTime) -> {
                SwingUtilities.invokeLater(() -> {
                    clientForm.updateProgress(progress, transferredBytes, totalBytes, elapsedTime);
                });
            });

            clientDownloadThread.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void downloadDirectory(File dir) {
        System.out.println("开始下载文件夹: " + dir.getName());
        // 在本地创建对应的文件夹
        String basePath = LocalDirectoryPanel.pathField.getText();
        String localBasePath = basePath +"\\" + dir.getName();
        File localDir = new File(localBasePath);
        if (!localDir.exists()) {
            localDir.mkdirs();
        }

        // 递归遍历文件夹
        recursiveDownload(dir, localBasePath);

        JOptionPane.showMessageDialog(this,
                "文件夹下载完成: " + dir.getName(),
                "下载完成",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void recursiveDownload(File remoteDir, String localPath) {
        File[] files = remoteDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    // 如果是文件夹，创建本地文件夹并递归下载
                    String newLocalPath = localPath + File.separator + file.getName();
                    File newLocalDir = new File(newLocalPath);
                    if (!newLocalDir.exists()) {
                        newLocalDir.mkdirs();
                    }
                    recursiveDownload(file, newLocalPath);
                } else {
                    // 如果是文件，调用下载文件的方法
                    try {
                        downloadFile(file, localPath);
                    } catch (Exception ex) {
                        System.err.println("下载文件 " + file.getName() + " 失败: " + ex.getMessage());
                    }
                }
            }
        }
    }

    private void downloadFile(File file, String localPath) {
        System.out.println("开始下载文件: " + file.getName());
        ClientCtrlThread.addMessage("RETR " + file.getPath());

        try {
            Socket socket = new Socket(server, 20);
            String localFilePath = localPath + File.separator + file.getName();
            clientDownloadThread = new ClientDownloadThread(socket, localFilePath);

            // 添加进度监听器
            // 添加进度监听器
            clientDownloadThread.addProgressListener((progress, transferredBytes, totalBytes, elapsedTime) -> {
                SwingUtilities.invokeLater(() -> {
                    clientForm.updateProgress(progress, transferredBytes, totalBytes, elapsedTime);
                });
            });

            clientDownloadThread.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void loadDirectory(File dir) {
        currentDir = dir;
        pathField.setText(dir.getAbsolutePath());
        tableModel.setRowCount(0);

        fetchRemoteDirectory(dir.getAbsolutePath());

    }

    private void goBack() {
        // 定义根目录
        File rootDir = new File("F:\\Desk\\计算机系统能力实训\\ServerFileList");

        // 检查当前目录是否已经是根目录或其子目录
        if (currentDir.getParentFile() != null &&
                !currentDir.getAbsolutePath().equals(rootDir.getAbsolutePath())) {

            // 检查回退后是否会超过根目录
            if (currentDir.getParentFile().getAbsolutePath().startsWith(rootDir.getAbsolutePath())) {
                loadDirectory(currentDir.getParentFile());
            }
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
                ClientCtrlThread.addMessage("DELE " + toDelete.getPath());
                loadDirectory(currentDir);
            }
            loadDirectory(currentDir);
        }
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
            File addfile = new File(currentDir, name);
            if (name != null && !name.trim().isEmpty()) {
                try {
//                    new File(currentDir, name).createNewFile();
                    ClientCtrlThread.addMessage("CREA " + addfile.getPath());
                    loadDirectory(currentDir);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "创建文件失败: " + ex.getMessage(),
                            "错误", JOptionPane.ERROR_MESSAGE);
                }
            }
            loadDirectory(currentDir);
            dialog.dispose();
        });

        addDirBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "输入文件夹名:");
            String dir = currentDir + File.separator + name;
            if (name != null && !name.trim().isEmpty()) {
//                if (new File(currentDir, name).mkdir()) {
//                    loadDirectory(currentDir);
//                }
                ClientCtrlThread.addMessage("CREA " + dir);
                loadDirectory(currentDir);
            }else {
                JOptionPane.showMessageDialog(this, "创建文件夹失败",
                        "错误", JOptionPane.ERROR_MESSAGE);
            }
            dialog.dispose();
        });
        loadDirectory(currentDir);
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
            JFrame frame = new JFrame("远程目录浏览器");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);

            RemoteDirectoryPanel panel = null;
            try {
                panel = new RemoteDirectoryPanel(new ClientForm("123","141".toCharArray()));
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            frame.add(panel);
            frame.setVisible(true);
        });
    }
}