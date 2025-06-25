package com.Frank.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class ClientUploadThread extends Thread {
    private Socket dataSocket;
    private File file;
    private final AtomicLong transferredBytes = new AtomicLong(0);
    private final AtomicLong totalBytes = new AtomicLong(0);
    private static final int BUFFER_SIZE = 8192;
    private List<DownloadProgressListener> listeners = new ArrayList<>();
    private long startTime;

    public ClientUploadThread(Socket dataSocket, File file) {
        this.dataSocket = dataSocket;
        this.file = file;
        this.startTime = System.currentTimeMillis();
        this.totalBytes.set(file.length());
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
        try (FileInputStream fileIn = new FileInputStream(file);
             OutputStream socketOut = dataSocket.getOutputStream()) {

            // 发送文件大小
            byte[] sizeBytes = longToBytes(file.length());
            socketOut.write(sizeBytes);
            socketOut.flush();

            // 开始发送文件内容
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                socketOut.write(buffer, 0, bytesRead);
                transferredBytes.addAndGet(bytesRead);
                double progress = getProgress();
                notifyProgressListeners(progress);
            }

            System.out.println("文件上传完成: " + file.getName());
        } catch (IOException e) {
            System.err.println("文件上传失败: " + e.getMessage());
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

    // long转字节数组（大端序）
    private byte[] longToBytes(long value) {
        byte[] bytes = new byte[8];
        for (int i = 7; i >= 0; i--) {
            bytes[i] = (byte) (value & 0xFF);
            value >>= 8;
        }
        return bytes;
    }

    // 获取当前上传进度（0.0 - 1.0）
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
}