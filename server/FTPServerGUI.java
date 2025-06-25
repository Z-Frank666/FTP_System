package com.Frank.server;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FTPServerGUI extends JFrame {
    public static final Map<Integer, String> RESPONSE_CODES = new HashMap<>();
    public static List<OnlineUser> onlineUsers = new ArrayList<>();

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

    public static Boolean isStarted = false;

    public static ServerSocket serverSocket;

    public static ServerSocket DataSocket;

    public static List<Socket> onLineSockets = new ArrayList<>();

    public static Socket socket;

    private static Socket data;

    public static Integer serverPort;

    public static OnlineUsersDialog onlineUsersDialog;

    static {
        try {
            serverSocket = new ServerSocket(ServerConfigDialog.port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            DataSocket = new ServerSocket(20);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JFrame frame = new JFrame();
    public static SystemLogDialog dialog = new SystemLogDialog(frame);

    public FTPServerGUI() {
        setTitle("FTP Server");  // 修改为图片中的标题
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        initComponents();
    }

    public static boolean isPortAvailable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // 如果能成功创建ServerSocket，说明端口未被占用
            // 设置为true使serverSocket立即关闭
            serverSocket.setReuseAddress(true);
            return true;
        } catch (Exception e) {
            // 端口被占用时会抛出BindException
            return false;
        }
    }

    private void initComponents() {
        // 创建菜单栏
        JMenuBar menuBar = new JMenuBar();

        JMenu serverMenu = new JMenu("服务器");
        JMenu optionsMenu = new JMenu("选项");

        menuBar.add(serverMenu);
        menuBar.add(optionsMenu);
        setJMenuBar(menuBar);

        // 创建工具栏按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 创建顶部控制面板（圆形按钮）
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 5, 0));
        controlPanel.setBackground(new Color(240, 240, 240));

        // 创建圆形启动按钮
        JButton startButton = createCircleButton("启动", new Color(50, 200, 50));
//        startButton.addActionListener(e -> {
//            JOptionPane.showMessageDialog(this, "服务器启动");
//        });

        // 创建圆形停止按钮
        JButton stopButton = createCircleButton("停止", new Color(200, 50, 50));
//        stopButton.addActionListener(e -> {
//            JOptionPane.showMessageDialog(this, "服务器停止");
//        });

        controlPanel.add(startButton);
        controlPanel.add(stopButton);

        // 创建五个功能按钮并添加真实图标


        JButton logButton = new JButton("系统日志");
        JButton userButton = new JButton("用户管理");
        JButton configButton = new JButton("服务器配置");
        JButton onlineButton = new JButton("在线用户");
        JButton statsButton = new JButton("连接数据统计");


        logButton.setPreferredSize(new Dimension(100, 50)); // 统一按钮尺寸
        userButton.setPreferredSize(new Dimension(100, 50));
        configButton.setPreferredSize(new Dimension(100, 50));
        onlineButton.setPreferredSize(new Dimension(100, 50));
        statsButton.setPreferredSize(new Dimension(100, 50));

        logButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        userButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        configButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        onlineButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        statsButton.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        startButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "服务器启动");
            isStarted = true;
            dialog.addLog("", "Server Started");
            new Thread(() -> {
                try {
                    Integer lstport = 0;
                    while (isStarted) {
                        System.out.println(ServerConfigDialog.maxConnections);
                        System.out.println(onlineUsers.size());
                        socket = serverSocket.accept();
                        InetAddress clientIp = socket.getInetAddress(); // 获取客户端 IP 地址对象
                        String clientIpStr = clientIp.getHostAddress();     // 转为字符串形式（如 "192.168.1.101"）

                        InetAddress serverIp = socket.getLocalAddress(); // 获取服务端 IP 地址对象
                        String serverIpStr = serverIp.getHostAddress();     // 转为字符串形式

                        onLineSockets.add(socket);
                        if (socket.getPort() != lstport + 1)
                            dialog.addLog(serverIpStr, "Accepted connection from " + clientIpStr);
                        lstport = socket.getPort();
                        // 传递数据连接的 ServerSocket
                        ThreadPoolManager.executeCtrlTask(new ServerCtrlThread(socket, DataSocket));
                    }
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }).start();
        });

        stopButton.addActionListener(e -> {
            JOptionPane.showMessageDialog(this, "服务器停止");
            isStarted = false;
            try {
                socket.close();
                dialog.addLog("","Server closed");
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        });

        logButton.addActionListener(e -> {
            dialog.setVisible(true);
            // 模拟日志添加
        });

        userButton.addActionListener(e -> {
            JFrame frame = new JFrame();
            UserManagementDialog dialog = new UserManagementDialog(frame);
            dialog.setVisible(true);
        });

        configButton.addActionListener(e -> {
            JFrame frame = new JFrame();
            ServerConfigDialog dialog = new ServerConfigDialog(frame);
            dialog.setVisible(true);
        });

        onlineButton.addActionListener(e -> {
            JFrame frame = new JFrame();
            onlineUsersDialog = new OnlineUsersDialog(frame);
            onlineUsersDialog.setVisible(true);
        });

        statsButton.addActionListener(e -> {
            JFrame frame = new JFrame();
            ConnectionStatsDialog dialog = new ConnectionStatsDialog(frame);
            dialog.setVisible(true);
        });

        buttonPanel.add(logButton);
        buttonPanel.add(userButton);
        buttonPanel.add(configButton);
        buttonPanel.add(onlineButton);
        buttonPanel.add(statsButton);

        // 主内容面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(buttonPanel, BorderLayout.CENTER);


        add(mainPanel);
    }

    // 创建圆形按钮
    private JButton createCircleButton(String text, Color color) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // 绘制圆形背景
                g2.setColor(color);
                g2.fillOval(0, 0, getSize().width, getSize().height);

                // 绘制文字
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(text)) / 2;
                int y = (getHeight() + fm.getAscent()) / 2;
                g2.drawString(text, x, y);
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(40, 40); // 圆形直径
            }
        };

        button.setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        return button;
    }

    public static void main(String[] args) {
        try {
            // 设置系统外观
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // 设置UI样式
            UIManager.put("Button.arc", 20); // 圆角按钮
            UIManager.put("Button.background", new Color(240, 240, 240));
            UIManager.put("Panel.background", new Color(240, 240, 240));
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            FTPServerGUI gui = new FTPServerGUI();
            gui.setVisible(true);
        });
    }
}