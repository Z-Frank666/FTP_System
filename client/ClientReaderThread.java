package com.Frank.client;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class ClientReaderThread extends Thread {
    private Socket socket;
    private BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();
    private ClientForm clientForm;
    private ClientListThread clientListThread;

    public ClientReaderThread(Socket socket, ClientForm clientForm, ClientListThread clientListThread) {
        this.socket = socket;
        this.clientForm = clientForm;
        this.clientListThread = clientListThread;
    }

    @Override
    public void run() {
        try (InputStream is = socket.getInputStream();
             DataInputStream dis = new DataInputStream(is)) {

            socket.setSoTimeout(5000); // 设置5秒超时

            while (!socket.isClosed()) {
                try {
                    String msg = dis.readUTF();
                    System.out.println("Received from server: " + msg);

                    // 解析响应码
                    String[] parts = msg.split(" ", 2);
                    int code = Integer.parseInt(parts[0]);
                    String description = ClientForm.RESPONSE_CODES.getOrDefault(code, "Unknown response");

                    if (code == 421){
                        JOptionPane.showMessageDialog(clientForm,"Exceeding the maximum connection limit, please wait!");
                    }

                    System.out.println(code + " " + description);

                    // 将消息放入队列供主线程处理
                    responseQueue.put(msg);

                } catch (java.net.SocketTimeoutException e) {
                    // 超时后继续检查socket状态
                    continue;
                } catch (Exception e) {
                    System.out.println("Connection closed: " + e.getMessage());
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("Reader thread error: " + e.getMessage());
        }
    }

    // 供外部获取响应消息
    public String getResponse() throws InterruptedException {
        return responseQueue.take();
    }
}