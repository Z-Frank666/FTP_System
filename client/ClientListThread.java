package com.Frank.client;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class ClientListThread extends Thread {
    private Socket dataSocket;
    private String FilePath;
    private String dir="";

    public ClientListThread(Socket dataSocket, String filePath) {
        this.dataSocket = dataSocket;
        this.FilePath = filePath;
    }

    public ClientListThread(Socket dataSocket){
        this.dataSocket = dataSocket;
    }

    @Override
    public void run() {
        try(InputStream socketIn = dataSocket.getInputStream();
            DataInputStream Dis = new DataInputStream(socketIn)){
            String msg = Dis.readUTF();
            dir = msg;
            System.out.println(dir);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            try {
                if (dataSocket != null && !dataSocket.isClosed()) {
                    dataSocket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getFileDirectory() {
        return dir;
    }

}