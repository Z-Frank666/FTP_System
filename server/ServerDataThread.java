package com.Frank.server;

import java.io.*;
import java.net.Socket;

public class ServerDataThread extends Thread {
    private final Socket socket;
    private final String filePath; // 要传输的文件路径

    public ServerDataThread(Socket socket, String filePath) {
        this.socket = socket;
        this.filePath = filePath;
    }

    @Override
    public void run() {
        FileInputStream fis = null;
        BufferedInputStream bis = null;
        OutputStream os = null;
        try {
            // 检查文件是否存在
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                System.err.println("文件不存在: " + filePath);
                return;
            }

            fis = new FileInputStream(file);
            bis = new BufferedInputStream(fis);
            os = socket.getOutputStream();

            System.out.println("开始传输。。。");

            // 先发送文件大小信息（8字节）
            long fileSize = file.length();
            int bytes = ConnectionStatsDialog.statistic.getTotal_kilobytes_sent();
            ConnectionStatsDialog.statistic.setTotal_kilobytes_sent(bytes+(int)fileSize/1024);
            int files = ConnectionStatsDialog.statistic.getFiles_downloaded();
            ConnectionStatsDialog.statistic.setFiles_downloaded(files+1);

            ServerCtrlThread.updateStatistic(ConnectionStatsDialog.statistic);

            byte[] sizeBytes = longToBytes(fileSize);
            os.write(sizeBytes);

            // 传输文件内容
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = bis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            System.out.println("文件传输完成: " + filePath);
        } catch (IOException e) {
            System.err.println("文件传输错误: " + e.getMessage());
        } finally {
            // 关闭资源
            closeResource(bis);
            closeResource(fis);
            closeResource(os);
            closeSocket(socket);
        }
    }

    // 辅助方法：将long转换为8字节数组
    private byte[] longToBytes(long value) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return result;
    }

    // 关闭输入输出流的辅助方法
    private void closeResource(Closeable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 关闭Socket的辅助方法
    private void closeSocket(Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 测试
    public static void main(String[] args) throws Exception {
        java.net.ServerSocket serverSocket = new java.net.ServerSocket(20);
        Socket socket = serverSocket.accept();
        String filepath = "F:\\Desk\\计算机系统能力实训\\ServerFileList\\多线程FTP服务器系统的设计与实现.doc";
        ServerDataThread serverDataThread = new ServerDataThread(socket, filepath);
        serverDataThread.start();
    }
}