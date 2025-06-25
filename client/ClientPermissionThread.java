package com.Frank.client;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

import static com.Frank.client.ClientForm.currentUser;

public class ClientPermissionThread extends Thread {
    private Socket dataSocket;

    public ClientPermissionThread(Socket dataSocket) {
        this.dataSocket = dataSocket;
    }

    @Override
    public void run() {
        DataInputStream dis = null;
        try {
            dis = new DataInputStream(dataSocket.getInputStream());

            // 接收权限信息（顺序必须与发送方一致）
            boolean canDownload = dis.readBoolean();
            boolean canUpload = dis.readBoolean();
            boolean canDelete = dis.readBoolean();
            boolean canCreate = dis.readBoolean();

            System.out.println("接收到的权限信息：");
            System.out.println("下载权限：" + canDownload);
            System.out.println("上传权限：" + canUpload);
            System.out.println("删除权限：" + canDelete);
            System.out.println("创建权限：" + canCreate);

            currentUser.setDownload(canDownload);
            currentUser.setUpload(canUpload);
            currentUser.setCreated(canCreate);
            currentUser.setDeleted(canDelete);

            SwingUtilities.invokeLater(() -> {
                ClientForm.panelRemote.addButton.setEnabled(currentUser.getCreated());
                ClientForm.panelRemote.deleteButton.setEnabled(currentUser.getDeleted());
            });

        } catch (IOException e) {
            System.err.println("权限信息接收错误: " + e.getMessage());
        } finally {
            // 关闭资源
            try {
                if (dis != null) dis.close();
                if (dataSocket != null) dataSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
