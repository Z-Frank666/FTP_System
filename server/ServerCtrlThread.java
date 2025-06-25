package com.Frank.server;

import com.Frank.JDBC.JdbcUtil;
import com.Frank.JDBC.User;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class ServerCtrlThread extends Thread {
    private Socket socket;
    private ServerSocket dataServerSocket; // 数据连接的 ServerSocket
    private static final String DEFAULT_DIR = "F:\\Desk\\计算机系统能力实训\\ServerFileList\\";
    private String currentUsername;

    public ServerCtrlThread(Socket socket, ServerSocket dataServerSocket) {
        this.socket = socket;
        this.dataServerSocket = dataServerSocket;
    }

    @Override
    public void run() {
        try {
            InputStream is = socket.getInputStream();
            DataInputStream dis = new DataInputStream(is);

            while (true) {
                try {
                    String msg = dis.readUTF();
                    System.out.println(msg);
                    response(msg);
                } catch (Exception e) {
                    // 连接断开，移除在线用户
                    removeOnlineUser();
                    FTPServerGUI.dialog.addLog(socket.getInetAddress().getHostAddress(), "logged out");
                    FTPServerGUI.onLineSockets.remove(socket);
                    dis.close();
                    socket.close();
                    break;
                }
            }
        } catch (Exception e) {
            // 连接异常断开，移除在线用户
            removeOnlineUser();
            FTPServerGUI.dialog.addLog(socket.getInetAddress().getHostAddress(), "logged out");
            FTPServerGUI.onLineSockets.remove(socket);
            throw new RuntimeException(e);
        }
    }

    private void response(String msg) throws Exception {
        String[] parts = msg.trim().split("\\s+", 2);
        List<String> result = Arrays.asList(parts);
        String typ = result.get(0);
        OutputStream os = socket.getOutputStream();
        DataOutputStream dos = new DataOutputStream(os);
        if (typ.equals("USER")) {
            if (result.size() > 1) {
                currentUsername = result.get(1);
                Integer code = 331;
                dos.writeUTF(code.toString());
                System.out.println(code);
                FTPServerGUI.dialog.addLog(currentUsername, FTPServerGUI.RESPONSE_CODES.get(code));
                if(FTPServerGUI.onlineUsers.size()>=ServerConfigDialog.maxConnections){
                    code = 421;
                    dos.writeUTF(code.toString());
                    System.out.println(code);
                    FTPServerGUI.onLineSockets.remove(socket);
                }
            }
        } else if (typ.equals("PASS")) {
            if (result.size() > 1 && currentUsername != null) {
                String password = result.get(1);
                String sql = "SELECT * FROM user WHERE username = ? AND password = ?";
                Object[] params = {currentUsername, password};
                try {
                    List<Object> resultList = JdbcUtil.executeQuery(sql, params, User.class);
                    if (!resultList.isEmpty()) {
                        Integer code = 230;
                        dos.writeUTF(code.toString());
                        System.out.println(code);
                        FTPServerGUI.dialog.addLog(currentUsername, FTPServerGUI.RESPONSE_CODES.get(code));

                        // 添加在线用户信息
                        OnlineUser user = new OnlineUser(
                                String.valueOf(socket.getPort()),
                                currentUsername,
                                socket.getInetAddress().getHostAddress(),
                                LocalDateTime.now()
                        );
                        FTPServerGUI.onlineUsers.add(user);

                        Integer conn = ConnectionStatsDialog.statistic.getTotal_connections();
                        ConnectionStatsDialog.statistic.setTotal_connections(conn + 1);
                        updateStatistic(ConnectionStatsDialog.statistic);

                        // 更新在线用户对话框
                        if (FTPServerGUI.onlineUsersDialog != null) {
                            FTPServerGUI.onlineUsersDialog.loadSampleData();
                        }

                        User currentuser = (User)resultList.get(0);
                        System.out.println(currentuser);

                        //发送权限信息
                        Socket dataSocket = dataServerSocket.accept();
                        ServerPermissionThread serverPermissionThread = new ServerPermissionThread(dataSocket,currentuser.getDownload(),currentuser.getUpload(),currentuser.getDeleted(),currentuser.getCreated());
                        ThreadPoolManager.executeDataTask(serverPermissionThread);
                    } else {
                        Integer code = 530;
                        dos.writeUTF(code.toString());
                        System.out.println(code);
                        FTPServerGUI.dialog.addLog(currentUsername, FTPServerGUI.RESPONSE_CODES.get(code));
                    }
                } catch (Exception e) {
                    Integer code = 421;
                    dos.writeUTF(code.toString());
                    System.out.println(code);
                    FTPServerGUI.dialog.addLog(currentUsername, FTPServerGUI.RESPONSE_CODES.get(code));
                }
            }
        } else if (typ.equals("PORT")) {
            // 处理 PORT 命令
            if (result.size() > 1) {
                String portCmd = result.get(1);
                // 解析 PORT 命令格式：h1,h2,h3,h4,p1,p2
                // 其中 h1-h4 是 IP 地址，p1,p2 是端口号的两个字节
                try {
                    String[] itemparts = portCmd.split(",");
                    if (itemparts.length == 6) {
                        // 重建客户端 IP 地址
                        String clientIp = itemparts[0] + "." + itemparts[1] + "." + itemparts[2] + "." + itemparts[3];
                        // 计算端口号：p1*256 + p2
                        int clientPort = Integer.parseInt(itemparts[4]) * 256 + Integer.parseInt(itemparts[5]);

                        // 存储客户端数据连接信息
                        String clientInfo = clientIp + ":" + clientPort;
                        System.out.println("PORT command received, client data port: " + clientInfo);

                        // 发送成功响应码 200
                        Integer code = 200;
                        dos.writeUTF(code.toString());
                        System.out.println(code);
                        FTPServerGUI.dialog.addLog(currentUsername, FTPServerGUI.RESPONSE_CODES.get(code));

                        // 创建主动数据连接
                        //                            Socket activeDataSocket = new Socket(clientIp, clientPort);
//                            // 设置超时
//                            activeDataSocket.setSoTimeout(30000); // 30秒超时

                        // 存储主动数据连接，供后续命令使用
                        // 注意：这里需要一个成员变量来存储这个连接
                        // 例如：private Socket activeDataSocket;
//                            this.activeDataSocket = activeDataSocket;

                        // 记录成功建立的主动连接
                        FTPServerGUI.dialog.addLog(currentUsername, "Active data connection established to " + clientInfo);
                    } else {
                        // PORT 命令格式错误
                        Integer errorCode = 501;
                        dos.writeUTF(errorCode.toString());
                        System.out.println(errorCode);
                        FTPServerGUI.dialog.addLog(currentUsername, FTPServerGUI.RESPONSE_CODES.get(errorCode));
                    }
                } catch (NumberFormatException e) {
                    // PORT 命令参数格式错误
                    Integer errorCode = 501;
                    dos.writeUTF(errorCode.toString());
                    System.out.println(errorCode);
                    FTPServerGUI.dialog.addLog(currentUsername, FTPServerGUI.RESPONSE_CODES.get(errorCode));
                }
            } else {
                // PORT 命令缺少参数
                Integer errorCode = 501;
                dos.writeUTF(errorCode.toString());
                System.out.println(errorCode);
                FTPServerGUI.dialog.addLog(currentUsername, FTPServerGUI.RESPONSE_CODES.get(errorCode));
            }
        } else if (typ.equals("PASV")) {
            
        } else if (typ.equals("RETR")) {
            if (result.size() > 1) {
                String filePath = result.get(1);
                // 发送响应码 150，表示准备开始传输文件
                Integer code = 150;
                dos.writeUTF(code.toString());
                System.out.println(code);
                FTPServerGUI.dialog.addLog(currentUsername, FTPServerGUI.RESPONSE_CODES.get(code));

                // 接受数据连接
                Socket dataSocket = dataServerSocket.accept();

                // 启动数据连接线程
                ServerDataThread dataThread = new ServerDataThread(dataSocket, filePath);
                ThreadPoolManager.executeDataTask(dataThread);

                // 发送响应码 226，表示文件传输完成
                code = 226;
                dos.writeUTF(code.toString());
                System.out.println(code);
                FTPServerGUI.dialog.addLog(currentUsername, FTPServerGUI.RESPONSE_CODES.get(code));
            }
        } else if (typ.equals("STOR")) {
            if (result.size() > 1) {
                String fileName = result.get(1);
                // 发送响应码 150，表示准备开始接收文件
                Integer code = 150;
                dos.writeUTF(code.toString());
                System.out.println(code);
                FTPServerGUI.dialog.addLog(currentUsername, FTPServerGUI.RESPONSE_CODES.get(code));

                // 接受数据连接
                Socket dataSocket = dataServerSocket.accept();

                // 启动上传线程
                ServerUploadThread uploadThread = new ServerUploadThread(dataSocket, fileName);
                ThreadPoolManager.executeDataTask(uploadThread);

                // 发送响应码 226，表示文件上传完成
                code = 226;
                dos.writeUTF(code.toString());
                System.out.println(code);
                FTPServerGUI.dialog.addLog(currentUsername, FTPServerGUI.RESPONSE_CODES.get(code));
            }
        } else if (typ.equals("LIST")) {
            try {
                String filePath = "";
                if(result.size() ==1) {
                    filePath = DEFAULT_DIR;
                }else{
                    filePath = result.get(1);
                }
                // 发送响应码 150，表示准备开始传输目录信息
                Integer code = 150;
                dos.writeUTF(code.toString());
                System.out.println(code);
                FTPServerGUI.dialog.addLog(currentUsername, FTPServerGUI.RESPONSE_CODES.get(code));

                // 接受数据连接
                Socket dataSocket = dataServerSocket.accept();

                System.out.println(filePath);
                // 启动目录信息传输线程
                ServerListThread listThread = new ServerListThread(dataSocket, filePath);
                ThreadPoolManager.executeDataTask(listThread);

                // 发送响应码 226，表示目录信息传输完成
                code = 226;
                dos.writeUTF(code.toString());
                System.out.println(code);
                FTPServerGUI.dialog.addLog(currentUsername, FTPServerGUI.RESPONSE_CODES.get(code));
            } catch (IOException e) {
                // 发送错误响应码 426
                Integer errorCode = 426;
                dos.writeUTF(errorCode.toString());
                System.out.println(errorCode);
                FTPServerGUI.dialog.addLog(currentUsername, FTPServerGUI.RESPONSE_CODES.get(errorCode));
                e.printStackTrace();
            }
        }else if(typ.equals("DELE")) {
            String filePath = result.get(1);
            File toDelete = new File(filePath);
            if (toDelete.isDirectory()) {
                deleteDirectory(toDelete);
            } else {
                toDelete.delete();
            }
            Integer code = 250;
            dos.writeUTF(code.toString());
            System.out.println(code);
            FTPServerGUI.dialog.addLog(currentUsername,FTPServerGUI.RESPONSE_CODES.get(code));
        }else if(typ.equals("CREA")) {
            String filePath = result.get(1);
            if(hasExtension(filePath)) {
                new File(filePath).createNewFile();
            }else{
                new File(filePath).mkdir();
            }
            Integer code = 250;
            dos.writeUTF(code.toString());
            System.out.println(code);
            FTPServerGUI.dialog.addLog(currentUsername,FTPServerGUI.RESPONSE_CODES.get(code));
        }
    }

    // 添加方法：移除在线用户
    private void removeOnlineUser() {
        if (currentUsername != null) {
            String clientIp = socket.getInetAddress().getHostAddress();
            int clientPort = socket.getPort();

            // 查找并移除匹配的在线用户
            FTPServerGUI.onlineUsers.removeIf(user ->
                    user.getUsername().equals(currentUsername) &&
                            user.getIP().equals(clientIp) &&
                            user.getThreadId().equals(String.valueOf(clientPort))
            );

            // 更新在线用户对话框
            if (FTPServerGUI.onlineUsersDialog != null) {
                FTPServerGUI.onlineUsersDialog.loadSampleData();
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

    public static boolean hasExtension(String path) {
        if (path == null || path.trim().isEmpty()) return false;

        // 排除以路径分隔符结尾的情况（如 C:\temp\ 或 /home/user/）
        if (path.endsWith("\\") || path.endsWith("/")) return false;

        // 查找最后一个点（.）的位置
        int lastDotIndex = path.lastIndexOf('.');
        // 条件：点存在 且 不在开头 且 不在路径分隔符后（如 .gitignore是特殊文件）
        return lastDotIndex > 0
                && lastDotIndex > path.lastIndexOf('\\')
                && lastDotIndex > path.lastIndexOf('/');
    }

    public static void updateStatistic(Statistic statistic){
        String sql = "update statistic set total_connections = ?,files_downloaded = ?,files_uploaded = ?,failed_downloads = ?,failed_uploads = ?,total_kilobytes_reveived = ?,total_kilobytes_sent = ? where id = 1";
        Object [] param = {statistic.getTotal_connections(), statistic.getFiles_downloaded(), statistic.getFiles_uploaded(),statistic.getFailed_downloads(),statistic.getFailed_uploads(),statistic.getTotal_kilobytes_reveived(),statistic.getTotal_kilobytes_sent()};
        try {
            int res = JdbcUtil.executeUpdate(sql, param);
            if(res==1){
                System.out.println("修改成功");
            }else{
                System.out.println("修改失败");
            }
        }catch (Exception e){
        }
    }

    public static void main(String[] args) {

    }
}