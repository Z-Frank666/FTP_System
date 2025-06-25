package com.Frank.server;

import com.Frank.JDBC.JdbcUtil;
import com.Frank.JDBC.User;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

public class UserManagementDialog extends JDialog {
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JCheckBox disableAccountCheckBox;
    private JTextField passwordField;
    private JTextField homeDirectoryField;
    private JCheckBox allowDownloadCheckBox;
    private JCheckBox allowUploadCheckBox;
    private JCheckBox allowDeleteCheckBox;
    private JCheckBox allowCreateDirCheckBox;

    // 用于包裹右侧详情的面板
    private JPanel detailsPanel;
    // 用于显示"请选择用户"的标签
    private JLabel promptLabel;
    // 右侧内容面板
    private JPanel rightContentPanel;

    public UserManagementDialog(JFrame parent) {
        super(parent, "User Management", true);
        setSize(600, 450);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(new Color(240, 240, 240));

        initComponents();
        loadUsersFromDatabase();
    }

    private void initComponents() {
        // 主面板
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.setBackground(new Color(240, 240, 240));

        // 左侧用户列表
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder("Users"));
        leftPanel.setPreferredSize(new Dimension(150, 0));
        leftPanel.setBackground(Color.WHITE);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setBackground(new Color(240, 240, 240));
        userList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedIndex = userList.getSelectedIndex();
                if (selectedIndex != -1) {
                    String selectedUsername = userListModel.getElementAt(selectedIndex);
                    loadUserDetails(selectedUsername);
                    // 显示详情面板，隐藏提示标签
                    showDetailsPanel(true);
                }
            }
        });
        leftPanel.add(new JScrollPane(userList), BorderLayout.CENTER);

        // 右侧用户详情面板
        detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        detailsPanel.setBackground(Color.WHITE);
        detailsPanel.setVisible(false); // 初始状态设为不可见

        // 禁用账户复选框
        disableAccountCheckBox = new JCheckBox("Disable Account");
        detailsPanel.add(disableAccountCheckBox);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 密码区域
        JPanel passwordPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        passwordPanel.setBackground(Color.WHITE);
        passwordPanel.add(new JLabel("Password:"));
        passwordField = new JTextField(15);
        passwordPanel.add(passwordField);
        detailsPanel.add(passwordPanel);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        // 主目录区域
        JPanel homePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        homePanel.setBackground(Color.WHITE);
        homePanel.add(new JLabel("Home:"));
        homeDirectoryField = new JTextField("C:\\", 15);
        homeDirectoryField.setEditable(false);
        homePanel.add(homeDirectoryField);
        JButton browseButton = new JButton("Browse...");
        browseButton.addActionListener(e -> browseHomeDirectory());
        homePanel.add(browseButton);
        detailsPanel.add(homePanel);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        // 权限设置
        JPanel permissionsPanel = new JPanel();
        permissionsPanel.setLayout(new BoxLayout(permissionsPanel, BoxLayout.Y_AXIS));
        permissionsPanel.setBorder(BorderFactory.createTitledBorder("Permissions for home directory"));
        permissionsPanel.setBackground(Color.WHITE);

        allowDownloadCheckBox = new JCheckBox("Allow Download");
        allowDownloadCheckBox.setSelected(false);
        allowUploadCheckBox = new JCheckBox("Allow Upload");
        allowUploadCheckBox.setSelected(false);
        allowDeleteCheckBox = new JCheckBox("Allow Delete");
        allowDeleteCheckBox.setSelected(false);
        allowCreateDirCheckBox = new JCheckBox("Allow Create Directory");
        allowCreateDirCheckBox.setSelected(false);

        permissionsPanel.add(allowDownloadCheckBox);
        permissionsPanel.add(allowUploadCheckBox);
        permissionsPanel.add(allowDeleteCheckBox);
        permissionsPanel.add(allowCreateDirCheckBox);

        detailsPanel.add(permissionsPanel);

        // 提示标签
        promptLabel = new JLabel("请从左侧列表选择一个用户查看详情");
        promptLabel.setHorizontalAlignment(JLabel.CENTER);
        promptLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        promptLabel.setForeground(Color.GRAY);

        // 右侧内容面板 - 修改为使用 CardLayout
        rightContentPanel = new JPanel(new CardLayout());
        rightContentPanel.add(promptLabel, "prompt");
        rightContentPanel.add(detailsPanel, "details");
        rightContentPanel.setBackground(Color.WHITE);

        // 底部按钮
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(240, 240, 240));

        JButton addButton = new JButton("Add...");
        JButton editButton = new JButton("Edit...");
        JButton deleteButton = new JButton("Delete");
        JButton saveButton = new JButton("Save");

        addButton.addActionListener(e -> addUser());
        editButton.addActionListener(e -> editUser());
        deleteButton.addActionListener(e -> deleteUser());
        saveButton.addActionListener(e -> saveChanges());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(saveButton);

        // 组装主界面
        mainPanel.add(leftPanel, BorderLayout.WEST);
        mainPanel.add(rightContentPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    // 新增方法：切换显示面板
    private void showDetailsPanel(boolean showDetails) {
        CardLayout cardLayout = (CardLayout) rightContentPanel.getLayout();
        if (showDetails) {
            cardLayout.show(rightContentPanel, "details");
        } else {
            cardLayout.show(rightContentPanel, "prompt");
        }
    }

    private void loadUsersFromDatabase() {
        String sql = "SELECT * FROM user";
        try {
            List<Object> result = JdbcUtil.executeQuery(sql, null, User.class);
            for (Object obj : result) {
                User user = (User) obj;
                userListModel.addElement(user.getUsername());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading users from database: " + e.getMessage());
        }
    }

    private void loadUserDetails(String username) {
        String sql = "SELECT * FROM user WHERE username = ?";
        Object[] params = {username};
        try {
            List<Object> result = JdbcUtil.executeQuery(sql, params, User.class);
            if (!result.isEmpty()) {
                User user = (User) result.get(0);
                passwordField.setText(user.getPassword());
                homeDirectoryField.setText(user.getHome());
                disableAccountCheckBox.setSelected(user.getDisabled());
                allowDownloadCheckBox.setSelected(user.getDownload());
                allowUploadCheckBox.setSelected(user.getUpload());
                allowDeleteCheckBox.setSelected(user.getDeleted());
                allowCreateDirCheckBox.setSelected(user.getCreated());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error loading user details: " + e.getMessage());
        }
    }

    private void browseHomeDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int returnVal = chooser.showOpenDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            homeDirectoryField.setText(chooser.getSelectedFile().getPath());
        }
    }

    private void addUser() {
        String newUser = JOptionPane.showInputDialog(this, "Enter new username:");
        if (newUser != null && !newUser.trim().isEmpty()) {
            String password = JOptionPane.showInputDialog(this, "Enter password for " + newUser + ":");
            if (password != null && !password.trim().isEmpty()) {
                String home = homeDirectoryField.getText();
                boolean download = allowDownloadCheckBox.isSelected();
                boolean upload = allowUploadCheckBox.isSelected();
                boolean delete = allowDeleteCheckBox.isSelected();
                boolean create = allowCreateDirCheckBox.isSelected();
                boolean disabled = disableAccountCheckBox.isSelected();

                String sql = "INSERT INTO user(username, password, home, download, upload, deleted, created, disabled) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                Object[] params = {newUser.trim(), password.trim(), home, download, upload, delete, create, disabled};
                try {
                    int result = JdbcUtil.executeUpdate(sql, params);
                    if (result > 0) {
                        userListModel.addElement(newUser.trim());
                        // 选中新添加的用户并显示详情
                        int index = userListModel.size() - 1;
                        userList.setSelectedIndex(index);
                        userList.ensureIndexIsVisible(index);
                    } else {
                        JOptionPane.showMessageDialog(this, "Failed to add user");
                    }
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this, "Error adding user: " + e.getMessage());
                }
            }
        }
    }

    private void editUser() {
        int selectedIndex = userList.getSelectedIndex();
        if (selectedIndex != -1) {
            String selectedUsername = userListModel.getElementAt(selectedIndex);
            String newPassword = new String(passwordField.getText());
            String home = homeDirectoryField.getText();
            boolean download = allowDownloadCheckBox.isSelected();
            boolean upload = allowUploadCheckBox.isSelected();
            boolean delete = allowDeleteCheckBox.isSelected();
            boolean create = allowCreateDirCheckBox.isSelected();
            boolean disabled = disableAccountCheckBox.isSelected();

            String sql = "UPDATE user SET password = ?, home = ?, download = ?, upload = ?, deleted = ?, created = ?, disabled = ? WHERE username = ?";
            Object[] params = {newPassword, home, download, upload, delete, create, disabled, selectedUsername};
            try {
                int result = JdbcUtil.executeUpdate(sql, params);
                if (result > 0) {
                    JOptionPane.showMessageDialog(this, "User information updated successfully");
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to update user information");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error updating user information: " + e.getMessage());
            }
        }
    }

    private void deleteUser() {
        int selectedIndex = userList.getSelectedIndex();
        if (selectedIndex != -1) {
            String selectedUsername = userListModel.getElementAt(selectedIndex);
            String sql = "DELETE FROM user WHERE username = ?";
            Object[] params = {selectedUsername};
            try {
                int result = JdbcUtil.executeUpdate(sql, params);
                if (result > 0) {
                    userListModel.remove(selectedIndex);
                    // 删除后隐藏详情面板，显示提示标签
                    showDetailsPanel(false);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to delete user");
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Error deleting user: " + e.getMessage());
            }
        }
    }

    private void saveChanges() {
        editUser();
        JOptionPane.showMessageDialog(this, "Changes saved successfully");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame();
            UserManagementDialog dialog = new UserManagementDialog(frame);
            dialog.setVisible(true);
        });
    }
}