package com.Frank.server;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SystemLogDialog extends JDialog {
    private JTextArea logArea;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

    public SystemLogDialog(JFrame parent) {
        super(parent, "System Log", false);
        setSize(800, 600);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        initComponents();
    }

    private void initComponents() {
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 日志显示区域
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.WHITE);

        // 自动滚动
        DefaultCaret caret = (DefaultCaret)logArea.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        // 控制按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(e -> logArea.setText(""));

        buttonPanel.add(clearButton);

        // 组装界面
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    // 添加日志方法
    public void addLog(String username, String action) {
        String timestamp = timeFormat.format(new Date());
        String logEntry = String.format("[%s] %s: %s\n", timestamp, username, action);

        SwingUtilities.invokeLater(() -> {
            logArea.append(logEntry);
        });
    }

    // 示例使用方法
    public static void main(String[] args) {
        JFrame frame = new JFrame();
        SystemLogDialog dialog = new SystemLogDialog(frame);
        dialog.setVisible(true);

        // 模拟日志添加
        dialog.addLog("user1", "connected to server");
        dialog.addLog("user2", "uploaded file: report.pdf");
        dialog.addLog("user1", "downloaded file: manual.docx");
        dialog.addLog("user3", "disconnected from server");
    }
}
