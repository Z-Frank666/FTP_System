package com.Frank.client;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ClientDownloadThread extends Thread {
    private Socket dataSocket;
    private String localFilePath;
    private final AtomicLong transferredBytes = new AtomicLong(0);
    private final AtomicLong totalBytes = new AtomicLong(0);
    private static final int BUFFER_SIZE = 8192;
    private List<DownloadProgressListener> listeners = new ArrayList<>();
    private long startTime;

    public ClientDownloadThread(Socket dataSocket, String localFilePath) {
        this.dataSocket = dataSocket;
        this.localFilePath = localFilePath;
        this.startTime = System.currentTimeMillis();
    }

    public void addProgressListener(DownloadProgressListener listener) {
        listeners.add(listener);
    }

    private void notifyProgressListeners(double progress) {
        for (DownloadProgressListener listener : listeners) {
            listener.onProgressUpdate(progress, transferredBytes.get(), totalBytes.get(), System.currentTimeMillis() - startTime);
        }
    }

    @Override
    public void run() {
        try (InputStream socketIn = dataSocket.getInputStream();
             FileOutputStream fileOut = new FileOutputStream(localFilePath)) {

            // 协议约定：先接收8字节的文件大小
            byte[] sizeBytes = new byte[8];
            socketIn.read(sizeBytes);
            long fileSize = bytesToLong(sizeBytes);
            totalBytes.set(fileSize);

            // 开始接收文件内容
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = socketIn.read(buffer)) != -1) {
                fileOut.write(buffer, 0, bytesRead);
                transferredBytes.addAndGet(bytesRead);
                double progress = getProgress();
                notifyProgressListeners(progress);
            }

            System.out.println("文件下载完成: " + localFilePath);
        } catch (IOException e) {
            System.err.println("文件下载失败: " + e.getMessage());
        } finally {
            try {
                if (dataSocket != null && !dataSocket.isClosed()) {
                    dataSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 字节数组转long（大端序）
    private long bytesToLong(byte[] bytes) {
        long value = 0;
        for (int i = 0; i < 8; i++) {
            value = (value << 8) | (bytes[i] & 0xFF);
        }
        return value;
    }

    // 获取当前下载进度（0.0 - 1.0）
    public double getProgress() {
        if (totalBytes.get() <= 0) return 0;
        return (double) transferredBytes.get() / totalBytes.get();
    }

    // 获取已传输字节数
    public long getTransferredBytes() {
        return transferredBytes.get();
    }

    // 获取文件总大小
    public long getTotalBytes() {
        return totalBytes.get();
    }


    public static void main(String[] args) throws Exception {
        Socket socket = new Socket("127.0.0.1", 20);
        String filename = "1.doc";
        String localpath = "F:\\Desk\\计算机系统能力实训\\LocalFileList\\" + filename;
        ClientDownloadThread downloadThread = new ClientDownloadThread(socket, localpath);

        downloadThread.start();
    }
}