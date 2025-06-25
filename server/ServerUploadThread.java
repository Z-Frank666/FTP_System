package com.Frank.server;


import java.io.*;
import java.net.Socket;

public class ServerUploadThread extends Thread {
    private final Socket socket;
    private final String fileName;
    public static String filePath = "F:\\Desk\\计算机系统能力实训\\ServerFileList\\"; // 要保存的文件路径

    public ServerUploadThread(Socket socket, String fileName) {
        this.socket = socket;
        this.fileName = fileName;
    }

    @Override
    public void run() {
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
        InputStream is = null;
        try {
            // 创建文件输出流
            File file = new File(filePath+fileName);
            fos = new FileOutputStream(file);
            bos = new BufferedOutputStream(fos);
            is = socket.getInputStream();

            System.out.println("开始接收文件。。。");

            // 先接收文件大小信息（8字节）
            byte[] sizeBytes = new byte[8];
            is.read(sizeBytes);
            long fileSize = bytesToLong(sizeBytes);

            // 接收文件内容
            byte[] buffer = new byte[8192];
            long totalBytesRead = 0;
            int bytesRead;
            while (totalBytesRead < fileSize && (bytesRead = is.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                bos.write(buffer, 0, bytesRead);
                totalBytesRead += bytesRead;
            }

            int bytes =  ConnectionStatsDialog.statistic.getTotal_kilobytes_reveived();
            ConnectionStatsDialog.statistic.setTotal_kilobytes_reveived(bytes+(int)totalBytesRead/1024);
            int files = ConnectionStatsDialog.statistic.getFiles_uploaded();
            ConnectionStatsDialog.statistic.setFiles_uploaded(files+1);

            ServerCtrlThread.updateStatistic(ConnectionStatsDialog.statistic);

            System.out.println("文件接收完成: " + filePath+fileName);
        } catch (IOException e) {
            System.err.println("文件接收错误: " + e.getMessage());
        } finally {
            // 关闭资源
            closeResource(bos);
            closeResource(fos);
            closeResource(is);
            closeSocket(socket);
        }
    }

    // 辅助方法：将8字节数组转换为long
    private long bytesToLong(byte[] bytes) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result <<= 8;
            result |= (bytes[i] & 0xFF);
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
}