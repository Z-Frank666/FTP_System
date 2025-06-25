# FTP 系统项目说明

## 项目简介
本项目是一个基于Java的FTP（文件传输协议）系统实现，包含客户端和服务端完整功能，支持文件上传/下载、用户权限管理、连接状态监控等核心功能，适用于学习和轻量级文件共享场景。

## 实现功能
### 客户端功能
- **用户登录**：通过 `ClientLoginForm` 实现登录界面交互，支持与服务端验证（依赖JDBC用户数据）
- **文件传输**：
  - 上传：`ClientUploadThread` 处理文件上传逻辑；
  - 下载：`ClientDownloadThread` 实现文件下载，并通过 `DownloadProgressListener` 监听进度。
- **目录管理**：`LocalDirectoryPanel`（本地目录）和 `RemoteDirectoryPanel`（服务端目录）实现双面板文件浏览。
- **权限控制**：`ClientPermissionThread` 与服务端交互获取/修改用户权限。

### 服务端功能
- **连接管理**：`ServerCtrlThread` 处理客户端控制指令（如登录、退出），`ThreadPoolManager` 管理线程池实现并发连接。
- **文件操作**：`ServerUploadThread` 处理文件上传存储，`ServerListThread` 响应目录列表请求。
- **用户管理**：`UserManagementDialog` 提供图形化用户增删改查界面，依赖JDBC与数据库交互。
- **状态监控**：`ConnectionStatsDialog`（连接统计）、`SystemLogDialog`（系统日志）和 `OnlineUsersDialog`（在线用户）实现运行状态可视化。

## 技术栈
- **核心语言**：Java（基于JDK 8+）。
- **GUI框架**：Swing（客户端 `ClientForm`、服务端 `FTPServerGUI` 界面实现）。
- **数据库**：通过 `JdbcUtil.java` 封装JDBC连接，支持关系型数据库（如MySQL）存储用户信息。
- **并发处理**：多线程技术（`ClientCtrlThread`、`ServerDataThread` 等）配合线程池（`ThreadPoolManager`）实现高并发连接。
- **协议实现**：自定义简单文件传输协议（响应码通过 `ResCode.java` 定义）。

## 项目结构
```
FTP_system/
├── JDBC/           # 数据库交互模块
│   ├── JdbcUtil.java   # JDBC连接工具类
│   └── User.java       # 用户实体类
├── client/         # 客户端模块
│   ├── ClientForm.java         # 主界面
│   ├── ClientLoginForm.java    # 登录界面
│   ├── ClientUploadThread.java # 上传线程
│   └── ...（其他功能线程及面板类）
└── server/         # 服务端模块
    ├── FTPServerGUI.java       # 服务端主界面
    ├── ServerCtrlThread.java   # 控制指令处理线程
    ├── UserManagementDialog.java # 用户管理界面
    └── ...（其他管理及功能线程类）
```
        
