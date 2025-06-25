package com.Frank.server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class ServerPermissionThread extends Thread {
    private final Socket socket;
    private Boolean download;
    private Boolean upload;
    private Boolean deleted;
    private Boolean created;

    public ServerPermissionThread(Socket socket, Boolean download, Boolean upload, Boolean deleted, Boolean created) {
        this.socket = socket;
        this.download = download;
        this.upload = upload;
        this.deleted = deleted;
        this.created = created;
    }

    @Override
    public void run() {
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(socket.getOutputStream());

            // 发送权限信息
            dos.writeBoolean(download);
            dos.writeBoolean(upload);
            dos.writeBoolean(deleted);
            dos.writeBoolean(created);

            System.out.println("权限信息传输完成");
        } catch (IOException e) {
            System.err.println("权限信息传输错误: " + e.getMessage());
        } finally {
            // 关闭资源
            closeResource(dos);
            closeSocket(socket);
        }
    }

    // 关闭输入输出流的辅助方法
    private void closeResource(java.io.Closeable resource) {
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