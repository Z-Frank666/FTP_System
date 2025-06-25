package com.Frank.server;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;

public class OnlineUsersDialog extends JDialog {
    private JTable userTable;
    private DefaultTableModel tableModel;

    public OnlineUsersDialog(JFrame parent) {
        super(parent, "Online Users", true);
        setSize(600, 300);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(Color.WHITE);

        initComponents();
        loadSampleData(); // 加载示例数据
    }

    private void initComponents() {
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(Color.WHITE);

        // 创建表格模型
        String[] columns = {"ThreadID", "Username", "IP Address", "Login Time"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 表格不可编辑
            }
        };

        // 创建表格
        userTable = new JTable(tableModel);
        userTable.setGridColor(Color.BLACK);
        userTable.setShowGrid(true);
        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userTable.getTableHeader().setReorderingAllowed(false);

        // 设置表格样式
        userTable.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        userTable.getTableHeader().setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        userTable.setRowHeight(25);

        // 添加表格到滚动面板
        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        scrollPane.getViewport().setBackground(Color.WHITE);

        // 底部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshData());

        JButton disconnectButton = new JButton("Disconnect");
        disconnectButton.addActionListener(e -> disconnectUser());

        buttonPanel.add(refreshButton);
        buttonPanel.add(disconnectButton);

        // 组装主界面
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    void loadSampleData() {
        // 清空现有数据
        tableModel.setRowCount(0);

        // 从静态列表中加载在线用户信息
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        for (OnlineUser user : FTPServerGUI.onlineUsers) {
            Object[] rowData = {
                    user.getThreadId(),
                    user.getUsername(),
                    user.getIP(),
                    user.getLoginTime().format(formatter)
            };
            tableModel.addRow(rowData);
        }
    }

    private void refreshData() {
        // 这里实现刷新逻辑
        loadSampleData(); // 暂时使用示例数据
        JOptionPane.showMessageDialog(this, "User list refreshed",
                "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void disconnectUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow >= 0) {
            int threadId = (int) tableModel.getValueAt(selectedRow, 0);
            tableModel.removeRow(selectedRow);
            JOptionPane.showMessageDialog(this,
                    "Disconnected user with ThreadID: " + threadId,
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Please select a user to disconnect",
                    "Warning", JOptionPane.WARNING_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            OnlineUsersDialog dialog = new OnlineUsersDialog(frame);
            dialog.setVisible(true);
        });
    }
}
