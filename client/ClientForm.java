package com.Frank.client;

import com.Frank.JDBC.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientForm extends JFrame{

    public static final Map<Integer, String> RESPONSE_CODES = new HashMap<>();

    static {
        // 1xx - 预备应答
        RESPONSE_CODES.put(110, "Restart marker reply");
        RESPONSE_CODES.put(120, "Service ready in nnn minutes");
        RESPONSE_CODES.put(125, "Data connection already open, transfer starting");
        RESPONSE_CODES.put(150, "File status okay, about to open data connection");

        // 2xx - 成功应答
        RESPONSE_CODES.put(200, "Command okay");
        RESPONSE_CODES.put(211, "System status reply");
        RESPONSE_CODES.put(212, "Directory status reply");
        RESPONSE_CODES.put(214, "Help message");
        RESPONSE_CODES.put(220, "Service ready for new user");
        RESPONSE_CODES.put(221, "Service closing control connection");
        RESPONSE_CODES.put(225, "Data connection open, no transfer in progress");
        RESPONSE_CODES.put(226, "Closing data connection, file transfer successful");
        RESPONSE_CODES.put(227, "Entering Passive Mode (h1,h2,h3,h4,p1,p2)");
        RESPONSE_CODES.put(230, "User logged in, proceed");
        RESPONSE_CODES.put(250, "Requested file action okay, completed");
        RESPONSE_CODES.put(257, "PATHNAME created");

        // 3xx - 中间应答
        RESPONSE_CODES.put(331, "User name okay, need password");
        RESPONSE_CODES.put(332, "Need account for login");
        RESPONSE_CODES.put(350, "Requested file action pending further information");

        // 4xx - 临时错误
        RESPONSE_CODES.put(421, "Service not available, closing control connection");
        RESPONSE_CODES.put(425, "Can't open data connection");
        RESPONSE_CODES.put(426, "Connection closed, transfer aborted");
        RESPONSE_CODES.put(450, "Requested file action not taken, file unavailable");
        RESPONSE_CODES.put(451, "Requested action aborted, local error in processing");
        RESPONSE_CODES.put(452, "Requested action not taken, insufficient storage space");

        // 5xx - 永久错误
        RESPONSE_CODES.put(500, "Syntax error, command unrecognized");
        RESPONSE_CODES.put(501, "Syntax error in parameters or arguments");
        RESPONSE_CODES.put(502, "Command not implemented");
        RESPONSE_CODES.put(503, "Bad sequence of commands");
        RESPONSE_CODES.put(530, "Not logged in");
        RESPONSE_CODES.put(532, "Need account for storing files");
        RESPONSE_CODES.put(550, "Requested action not taken, file unavailable");
        RESPONSE_CODES.put(551, "Requested action aborted, page type unknown");
        RESPONSE_CODES.put(552, "Requested file action aborted, exceeded storage allocation");
        RESPONSE_CODES.put(553, "Requested action not taken, file name not allowed");
    }

    private ClientCtrlThread clientCtrlThread;
    private ClientReaderThread clientReaderThread;
    private ClientListThread clientListThread;

    private String username;
    private char[] password;
    private Boolean isPositive = false;

    // 顶部控制面板组件
    private JTextField serverAddressField;
    private JTextField portField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton connectButton;
    private JButton disconnectButton;
    private JButton exitButton;

    // 传输方式选择
    private JRadioButton activeModeRadio;
    private JRadioButton passiveModeRadio;

    // 目录显示区域
    private JTable localTable;
    private JTable remoteTable;
    public JProgressBar transferProgressBar;

    // 状态信息标签
    public JLabel speedLabel;
    public JLabel transferredLabel;
    public JLabel timeElapsedLabel;
    public JLabel timeRemainingLabel;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private long startTime;

    private Socket controlSocket;

    public static String server;

    static RemoteDirectoryPanel panelRemote;

    public static User currentUser; // 添加静态User对象

    public ClientForm(String username, char[] password) throws UnknownHostException {
        this.username = username;
        this.password = password;

        currentUser = new User(username, new String(password)); // 初始化静态User对象

        setTitle("MyFTPClient");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(240, 240, 240)); // 设置浅灰色背景
        initUI();
    }

    private void initUI() throws UnknownHostException {
        // 主面板采用边框布局
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(240, 240, 240));

        // 1. 顶部控制面板
        JPanel controlPanel = createControlPanel();
        mainPanel.add(controlPanel, BorderLayout.NORTH);

        // 2. 中部目录显示区（左右分栏）
        JSplitPane splitPane = createDirectorySplitPane();
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // 3. 底部传输状态栏
        JPanel statusPanel = createStatusPanel();
        mainPanel.add(statusPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createControlPanel() throws UnknownHostException {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("服务器连接"));
        panel.setBackground(new Color(240, 240, 240));

        // 服务器地址
        panel.add(new JLabel("服务器地址:"));
        InetAddress addr = InetAddress.getLocalHost();
        System.out.println("Local HostAddress: " + addr.getHostAddress());
        String remoteaddr = addr.getHostAddress();
        serverAddressField = new JTextField(remoteaddr, 12);
        panel.add(serverAddressField);

        // 端口
        panel.add(new JLabel("端口:"));
        portField = new JTextField("21", 4);
        panel.add(portField);

        // 用户名
        panel.add(new JLabel("用户名:"));
        usernameField = new JTextField(username, 10);
//        usernameField.setEditable(false);
        panel.add(usernameField);

        // 密码
        panel.add(new JLabel("密码:"));
        passwordField = new JPasswordField(10);
        passwordField.setText(new String(password));
        panel.add(passwordField);

        // 连接按钮
        connectButton = new JButton("连接");
        connectButton.addActionListener(this::connectToServer);
        panel.add(connectButton);

        // 断开按钮
        disconnectButton = new JButton("断开连接");
        disconnectButton.setEnabled(false);
        disconnectButton.addActionListener(this::disconnectFromServer);
        panel.add(disconnectButton);

        // 退出按钮
        exitButton = new JButton("退出");
        exitButton.addActionListener(e -> switchToLoginForm());
        panel.add(exitButton);

        // 传输方式选择
        JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        modePanel.setBorder(BorderFactory.createTitledBorder("传输方式"));
        modePanel.setBackground(new Color(240, 240, 240));
        ButtonGroup modeGroup = new ButtonGroup();

        activeModeRadio = new JRadioButton("主动", isPositive);
        passiveModeRadio = new JRadioButton("被动",!isPositive);

        // 添加事件监听
        activeModeRadio.addActionListener(e -> {
            isPositive = true;
            System.out.println("切换为主动模式");
        });

        passiveModeRadio.addActionListener(e -> {
            isPositive = false;
            System.out.println("切换为被动模式");
        });

        // 确保初始状态同步
        isPositive = activeModeRadio.isSelected();

        modeGroup.add(activeModeRadio);
        modeGroup.add(passiveModeRadio);

        modePanel.add(activeModeRadio);
        modePanel.add(passiveModeRadio);

        panel.add(modePanel);

        return panel;
    }

    private JSplitPane createDirectorySplitPane() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.5);
        splitPane.setBackground(new Color(240, 240, 240));

        // 本地目录面板
        JPanel localPanel = new JPanel(new BorderLayout());
        LocalDirectoryPanel panel = new LocalDirectoryPanel(this);
        localPanel.add(panel, BorderLayout.CENTER);

        // 远程目录面板
        JPanel remotePanel = new JPanel(new BorderLayout());
        panelRemote = new RemoteDirectoryPanel(this);
        remotePanel.add(panelRemote, BorderLayout.CENTER);

        splitPane.setLeftComponent(localPanel);
        splitPane.setRightComponent(remotePanel);

        return splitPane;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 5));
        panel.setBorder(BorderFactory.createTitledBorder("当前传输"));
        panel.setBackground(new Color(240, 240, 240));

        // 传输进度条
        transferProgressBar = new JProgressBar(0, 100);
        transferProgressBar.setStringPainted(true);
        transferProgressBar.setValue(0);
        transferProgressBar.setString("0%");
        panel.add(transferProgressBar, BorderLayout.CENTER);

        // 状态信息
        JPanel infoPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        infoPanel.setBackground(new Color(240, 240, 240));

        speedLabel = new JLabel("速度: 0 KB/s", JLabel.CENTER);
        transferredLabel = new JLabel("已传输: 0 MB", JLabel.CENTER);
        timeElapsedLabel = new JLabel("已用时: 00:00:00", JLabel.CENTER);
        timeRemainingLabel = new JLabel("剩余时间: 00:00:00", JLabel.CENTER);

        infoPanel.add(speedLabel);
        infoPanel.add(transferredLabel);
        infoPanel.add(timeElapsedLabel);
        infoPanel.add(timeRemainingLabel);

        panel.add(infoPanel, BorderLayout.SOUTH);

        return panel;
    }

    public static String generatePortCommand(String ip, int port) {
        // 分割IP为四个部分
        String[] ipParts = ip.split("\\.");
        if (ipParts.length != 4) {
            throw new IllegalArgumentException("Invalid IP format");
        }

        // 计算端口号的p1和p2
        int p1 = port / 256;
        int p2 = port % 256;

        // 组合成PORT命令格式
        return String.format("PORT %s,%s,%s,%s,%d,%d",
                ipParts[0], ipParts[1], ipParts[2], ipParts[3], p1, p2);
    }


    private void connectToServer(ActionEvent e) {
        server = serverAddressField.getText();
        int port = Integer.parseInt(portField.getText());
        String user = usernameField.getText();
        char[] pass = passwordField.getPassword();
        boolean isActiveMode = activeModeRadio.isSelected();

        try {
            // 先创建Socket连接
            controlSocket = new Socket(server,port);
            // 绑定本地端口
//            controlSocket.bind(new InetSocketAddress("0.0.0.0", 1026));
            // 连接到服务器
//            controlSocket.connect(new InetSocketAddress(server, port));

            clientCtrlThread = new ClientCtrlThread(controlSocket,clientListThread);
            clientReaderThread = new ClientReaderThread(controlSocket, this,clientListThread);
            clientCtrlThread.start();
            clientReaderThread.start();

            // 发送认证命令
            ClientCtrlThread.addMessage("USER " + user);
            ClientCtrlThread.addMessage("PASS " + new String(pass));

            if (isActiveMode) {
                ClientCtrlThread.addMessage(generatePortCommand(server, port + 2));
            }

            // 启动一个线程来处理服务器响应
            executorService.submit(() -> {
                try {
                    while (true) {
                        String response = clientReaderThread.getResponse();
                        String[] parts = response.split(" ", 2);
                        int code = Integer.parseInt(parts[0]);
                        if (code == 230) {
                            // 登录成功
                            SwingUtilities.invokeLater(() -> {
                                connectButton.setEnabled(false);
                                disconnectButton.setEnabled(true);
                                JOptionPane.showMessageDialog(this,
                                        String.format("已连接到服务器: %s:%d\n用户: %s\n模式: %s",
                                                server, port, user, isActiveMode ? "主动" : "被动"),
                                        "连接成功", JOptionPane.INFORMATION_MESSAGE);
                            });
                            Socket datasocket = new Socket("127.0.0.1", 20);
                            ClientPermissionThread clientPermissionThread = new ClientPermissionThread(datasocket);
                            clientPermissionThread.start();
//                            SwingUtilities.invokeLater(() -> {
//                                panelRemote.addButton.setEnabled(currentUser.getCreated());
//                                panelRemote.deleteButton.setEnabled(currentUser.getDeleted());
//                            });
                        } else if (code >= 400) {
                            // 错误处理
                            SwingUtilities.invokeLater(() -> {
                                connectButton.setEnabled(true);
                                disconnectButton.setEnabled(false);
                                JOptionPane.showMessageDialog(this, "连接失败: " + RESPONSE_CODES.get(code),
                                        "错误", JOptionPane.ERROR_MESSAGE);
                            });
                            break;
                        }
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                } catch (UnknownHostException ex) {
                    throw new RuntimeException(ex);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            });

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "连接失败: " + ex.getMessage(),
                    "错误", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void disconnectFromServer(ActionEvent e) {
        ClientCtrlThread.addMessage("QUIT");
        try {
            if (clientCtrlThread != null) {
                clientCtrlThread.shutdown();
            }
            if (clientReaderThread != null) {
                clientReaderThread.interrupt();
            }
            if (controlSocket != null && !controlSocket.isClosed()) {
                controlSocket.close();
            }
        } catch (Exception ex) {
            System.err.println("关闭线程出错: " + ex.getMessage());
        }

        // 模拟断开连接
        connectButton.setEnabled(true);
        disconnectButton.setEnabled(false);
        JOptionPane.showMessageDialog(this, "已断开服务器连接", "断开连接", JOptionPane.INFORMATION_MESSAGE);
    }

    private void switchToLoginForm() {
        // 关闭当前窗口
        this.dispose();
        // 显示登录窗口
//        new ClientLoginForm().setVisible(true);
    }

    public void updateProgress(double progress, long transferredBytes, long totalBytes, long elapsedTime) {
        int progressPercentage = (int) (progress * 100);
        transferProgressBar.setValue(progressPercentage);
        transferProgressBar.setString(progressPercentage + "%");

        // 计算速度
        double speed = (double) transferredBytes / elapsedTime * 1000 / 1024; // KB/s
        DecimalFormat df = new DecimalFormat("#.00");
        speedLabel.setText("速度: " + df.format(speed) + " KB/s");

        // 计算已传输字节数
        transferredLabel.setText("已传输: " + formatFileSize(transferredBytes));

        // 计算已用时间
        long seconds = elapsedTime / 1000;
        timeElapsedLabel.setText("已用时间: " + formatTime(seconds));

        // 计算剩余时间
        if (progress > 0) {
            long remainingTime = (long) ((totalBytes - transferredBytes) / speed * 1024 / 1000);
            timeRemainingLabel.setText("剩余时间: " + formatTime(remainingTime));
        } else {
            timeRemainingLabel.setText("剩余时间: 未知");
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.1f MB", size / (1024.0 * 1024.0));
        return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
    }

    private String formatTime(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientForm form = null;
            try {
                form = new ClientForm("Frank", "123456".toCharArray());
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
            form.setVisible(true);
        });
    }
}