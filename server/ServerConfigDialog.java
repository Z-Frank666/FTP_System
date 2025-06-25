package com.Frank.server;

import com.Frank.JDBC.JdbcUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class ServerConfigDialog extends JDialog {
    private JTextField portField;
    private JTextField maxConnectionsField;
    private JTextField timeoutField;
    private JTextArea welcomeMessageArea;
    private JTextArea goodbyeMessageArea;

    public static Integer port = 21;
    public static Integer maxConnections = 10;
    public static Integer timeout = 5;
    public static String welcomeMessage = "Welcome to Frank's FTP Server!";
    public static String goodbyeMessage = "Bye!";

    static {
        String sql = "select * from config";
        try {
            List<Object> res = JdbcUtil.executeQuery(sql,null,Config.class);
            Config config = (Config)res.get(0);
            System.out.println(config);
            port = config.getPort();
            maxConnections = config.getMax_connections();
            timeout = config.getTimeout();
            welcomeMessage = config.getWelcome_msg();
            goodbyeMessage = config.getGoodbye_msg();
        }catch (Exception e) {
//            JOptionPane.showMessageDialog(,"Error in getting the config!");
        }
    }

    public ServerConfigDialog(JFrame parent) {
        super(parent, "Server Configuration", true);
        setSize(500, 400);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(new Color(240, 240, 240));

        String sql = "select * from config";
        try {
            List<Object> res = JdbcUtil.executeQuery(sql,null,Config.class);
            Config config = (Config)res.get(0);
            System.out.println(config);
            port = config.getPort();
            maxConnections = config.getMax_connections();
            timeout = config.getTimeout();
            welcomeMessage = config.getWelcome_msg();
            goodbyeMessage = config.getGoodbye_msg();
        }catch (Exception e) {
            JOptionPane.showMessageDialog(this,"Error in getting the config!");
        }

        initComponents();
    }

    private void initComponents() {
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        mainPanel.setBackground(new Color(240, 240, 240));

        // 配置项面板
        JPanel configPanel = new JPanel(new GridBagLayout());
        configPanel.setBackground(new Color(240, 240, 240));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);

        // FTP Port
        gbc.gridx = 0;
        gbc.gridy = 0;
        configPanel.add(new JLabel("FTP Port:"), gbc);

        gbc.gridx = 1;
        portField = new JTextField(port.toString(), 10);
        configPanel.add(portField, gbc);

        // Max Connections
        gbc.gridx = 0;
        gbc.gridy = 1;
        configPanel.add(new JLabel("Max Connections:"), gbc);

        gbc.gridx = 1;
        maxConnectionsField = new JTextField(maxConnections.toString(), 10);
        configPanel.add(maxConnectionsField, gbc);

        // Connection Timeout
        gbc.gridx = 0;
        gbc.gridy = 2;
        configPanel.add(new JLabel("Connection Timeout:"), gbc);

        gbc.gridx = 1;
        JPanel timeoutPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        timeoutPanel.setBackground(new Color(240, 240, 240));
        timeoutField = new JTextField(timeout.toString(), 5);
        timeoutPanel.add(timeoutField);
        timeoutPanel.add(new JLabel("minutes"));
        configPanel.add(timeoutPanel, gbc);

        // Welcome Message
        gbc.gridx = 0;
        gbc.gridy = 3;
        configPanel.add(new JLabel("Welcome Message:"), gbc);

        gbc.gridx = 1;
        welcomeMessageArea = new JTextArea(welcomeMessage, 3, 20);
        welcomeMessageArea.setLineWrap(true);
        configPanel.add(new JScrollPane(welcomeMessageArea), gbc);

        // Goodbye Message
        gbc.gridx = 0;
        gbc.gridy = 4;
        configPanel.add(new JLabel("Goodbye Message:"), gbc);

        gbc.gridx = 1;
        goodbyeMessageArea = new JTextArea(goodbyeMessage, 3, 20);
        goodbyeMessageArea.setLineWrap(true);
        configPanel.add(new JScrollPane(goodbyeMessageArea), gbc);

        // Apply按钮
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.anchor = GridBagConstraints.EAST;
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                applyConfig();
            }
        });
        configPanel.add(applyButton, gbc);

        mainPanel.add(configPanel, BorderLayout.CENTER);
        add(mainPanel);
    }

    private void applyConfig() {
        // 这里实现配置应用逻辑
        try {
            int port = Integer.parseInt(portField.getText());
            int maxConn = Integer.parseInt(maxConnectionsField.getText());
            int timeout = Integer.parseInt(timeoutField.getText());
            String welcomeMsg = welcomeMessageArea.getText();
            String goodbyeMsg = goodbyeMessageArea.getText();

            // 验证配置
            if (port <= 0 || port > 65535||port == 20) {
                throw new NumberFormatException("Invalid port number");
            }
            if (maxConn <= 0) {
                throw new NumberFormatException("Invalid max connections");
            }
            if (timeout <= 0) {
                throw new NumberFormatException("Invalid timeout value");
            }

            String sql = "update config set port = ?,max_connections = ?,timeout = ?,welcome_msg = ?,goodbye_msg = ? where id = 1";
            Object[] params = {port, maxConn, timeout, welcomeMsg, goodbyeMsg};

            try{
                int result = JdbcUtil.executeUpdate(sql, params);
                if(result==1){
                    JOptionPane.showMessageDialog(this, "Successfully updated config");
                }else{
                    JOptionPane.showMessageDialog(this, "Failed to update config");
                }
            }catch (Exception e){
                JOptionPane.showMessageDialog(this, "Error update config");
            }
            // 应用配置
            JOptionPane.showMessageDialog(this,
                    "Configuration applied successfully:\n" +
                            "FTP Port: " + port + "\n" +
                            "Max Connections: " + maxConn + "\n" +
                            "Connection Timeout: " + timeout + " minutes\n" +
                            "Welcome Message: " + welcomeMsg + "\n" +
                            "Goodbye Message: " + goodbyeMsg,
                    "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Invalid configuration: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            ServerConfigDialog dialog = new ServerConfigDialog(frame);
            dialog.setVisible(true);
        });
    }
}
