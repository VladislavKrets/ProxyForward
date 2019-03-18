package com.example.proxyforward;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;

public class ClientHttpSocketHandler extends Thread{

    private Socket socket;

    public ClientHttpSocketHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader clientInputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream clientOutputStream = socket.getOutputStream();

            String line;
            while ((line = clientInputStream.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
