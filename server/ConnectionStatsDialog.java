package com.Frank.server;

import com.Frank.JDBC.JdbcUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class ConnectionStatsDialog extends JDialog {
    private DefaultTableModel tableModel;

    public static Statistic statistic;

    static {
        String sql = "select * from statistic";
        try {
            List<Object> res = JdbcUtil.executeQuery(sql,null,Statistic.class);
            statistic = (Statistic) res.get(0);
            System.out.println(statistic);
        }catch (Exception e){
        }
    }

    public ConnectionStatsDialog(JFrame parent) {
        super(parent, "Connection Statistics", true);
        setSize(400, 400);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(Color.WHITE);

        initComponents();
        loadData();
    }

    private void initComponents() {
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(Color.WHITE);

        // 创建表格模型
        String[] columns = {"Statistic", "Value"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // 表格不可编辑
            }
        };

        // 创建表格
        JTable statsTable = new JTable(tableModel);
        statsTable.setGridColor(Color.LIGHT_GRAY);
        statsTable.setShowGrid(true);
        statsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        statsTable.getTableHeader().setReorderingAllowed(false);

        // 设置表格样式
        statsTable.setFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        statsTable.getTableHeader().setFont(new Font("Microsoft YaHei", Font.BOLD, 12));
        statsTable.setRowHeight(25);

        // 添加表格到滚动面板
        JScrollPane scrollPane = new JScrollPane(statsTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        scrollPane.getViewport().setBackground(Color.WHITE);

        // 底部按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(Color.WHITE);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> refreshData());

        JButton resetButton = new JButton("Reset All");
        resetButton.addActionListener(e -> resetStats());

        buttonPanel.add(refreshButton);
        buttonPanel.add(resetButton);

        // 组装主界面
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void loadData() {
        // 清空现有数据
        tableModel.setRowCount(0);

        String sql = "select * from statistic";
        try {
            List<Object> res = JdbcUtil.executeQuery(sql,null,Statistic.class);
            statistic = (Statistic) res.get(0);
            System.out.println(statistic);
        }catch (Exception e){
        }

        // 添加统计数据（与图片一致）
        addStatRow("Total Connections", String.valueOf(statistic.getTotal_connections()));
        addStatRow("Current Connections", String.valueOf(FTPServerGUI.onlineUsers.size()));
        addStatRow("Files Downloaded", String.valueOf(statistic.getFiles_downloaded()));
        addStatRow("Files Uploaded", String.valueOf(statistic.getFiles_uploaded()));
        addStatRow("Failed Downloads", String.valueOf(statistic.getFailed_downloads()));
        addStatRow("Failed Uploads", String.valueOf(statistic.getFailed_uploads()));
        addStatRow("Total kilobytes received", String.valueOf(statistic.getTotal_kilobytes_reveived()));
        addStatRow("Total kilobytes sent", String.valueOf(statistic.getTotal_kilobytes_sent()));
    }

    private void addStatRow(String statistic, String value) {
        tableModel.addRow(new Object[]{statistic, value});
    }

    private void refreshData() {
        // 这里实现刷新逻辑
        loadData(); // 暂时使用示例数据
        JOptionPane.showMessageDialog(this, "Statistics refreshed",
                "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private void resetStats() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to reset all statistics?",
                "Confirm Reset", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            // 这里实现重置逻辑
            loadData(); // 暂时重置为0
            JOptionPane.showMessageDialog(this,
                    "All statistics have been reset",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            ConnectionStatsDialog dialog = new ConnectionStatsDialog(frame);
            dialog.setVisible(true);
        });
    }
}
