package com.example.proxyforward;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSocketThread extends Thread{
    private int port;

    public ServerSocketThread(int port) {
        this.port = port;
    }

    @Override
    public void run() {
        try {
            ServerSocket httpSocket = new ServerSocket(port);
            Socket clientSocket;
            while (FlagSingleton.getInstance().isFlag()) {
                clientSocket = httpSocket.accept();
                System.out.println("Socket accepted");
                new ClientSocketHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
