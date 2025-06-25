package com.Frank.client;

import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientCtrlThread extends Thread {
    // 使用线程安全的LinkedBlockingQueue替代LinkedList
    public static BlockingQueue<String> MsgQueue = new LinkedBlockingQueue<>();
    private Socket socket;
    private volatile boolean running = true;
    private ClientListThread clientListThread;
    private ClientReaderThread clientReaderThread;

    public ClientCtrlThread(Socket socket, ClientListThread clientListThread) {
        this.socket = socket;
        this.clientListThread = clientListThread;
        this.clientReaderThread = clientReaderThread;
    }

    @Override
    public void run() {
        try (DataOutputStream dos = new DataOutputStream(socket.getOutputStream())) {
            while (running) {
                String message = MsgQueue.take(); // 阻塞等待消息

                if ("QUIT".equalsIgnoreCase(message)) {
                    shutdown();
                    break;
                }

                dos.writeUTF(message);
                dos.flush();
                System.out.println("[Sent] " + message);
            }
        } catch (Exception ex) {
            System.err.println("Client error: " + ex.getMessage());
        }
    }

    public void shutdown() {
        running = false;
        System.out.println("Client shutting down...");
    }

    // 静态方法供外部添加消息
    public static void addMessage(String message) {
        synchronized (MsgQueue) {
            MsgQueue.offer(message);
        }
    }
}