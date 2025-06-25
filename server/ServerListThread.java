package com.Frank.server;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerListThread extends Thread {
    private final Socket socket;
    private final String dirPath;

    public ServerListThread(Socket socket, String dirPath) {
        this.socket = socket;
        this.dirPath = dirPath;
    }

    @Override
    public void run() {
        OutputStream dataOs = null;
        DataOutputStream dataDos = null;
        try {
            dataOs = socket.getOutputStream();
            dataDos = new DataOutputStream(dataOs);

            // 获取指定目录下的文件和文件夹信息
            File dir = new File(dirPath);
            if (dir.exists() && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    StringBuilder listInfo = new StringBuilder();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    for (File file : files) {
                        String type = file.isDirectory() ? "d" : "-";
                        long size = file.length();
                        String lastModified = sdf.format(new Date(file.lastModified()));
                        listInfo.append(type)
                                .append(" ")
                                .append(size)
                                .append(" ")
                                .append(lastModified)
                                .append(" ")
                                .append(file.getName())
                                .append("\n");
                    }
                    System.out.println(listInfo);
                    dataDos.writeUTF(listInfo.toString());
                }
            }
        } catch (IOException e) {
            System.err.println("目录信息传输错误: " + e.getMessage());
        } finally {
            // 关闭资源
            closeResource(dataDos);
            closeResource(dataOs);
            closeSocket(socket);
        }
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