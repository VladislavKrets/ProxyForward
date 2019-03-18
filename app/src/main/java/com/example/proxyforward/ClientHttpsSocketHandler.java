package com.example.proxyforward;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLSocketFactory;

public class ClientHttpsSocketHandler extends Thread{

    private Socket socket;

    public ClientHttpsSocketHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader clientInputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream clientOutputStream = socket.getOutputStream();
            String connect = clientInputStream.readLine().trim();
            String host = clientInputStream.readLine().trim();
            if (host.contains("www.gstatic.com") || host.contains("http://kobiitttxq/") || host.contains("http://wbcszqvswhcuy/")) return;
            String proxyConnection = clientInputStream.readLine().trim();
            String userAgent = clientInputStream.readLine().trim();

            String fullUrl = connect.split(" ")[1];
            String httpVersion = connect.split(" ")[2];
            String url = fullUrl.split(":")[0];
            String port = fullUrl.split(":")[1];
            System.out.println(connect);
            System.out.println(host);
            clientOutputStream.write("HTTP/1.1 200 Connection established\r\nProxy-agent: Simple-Proxy/1.1\r\n\r\n".getBytes());
            clientOutputStream.flush();
            Socket socket;
            if (port.equals("80")){
                socket = new Socket(url, Integer.parseInt(port));
            }
            else {
                //System.setProperty("javax.net.ssl.trustStore", "clienttrust");
                SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
                socket = ssf.createSocket(url, Integer.parseInt(port));
            }

            BufferedReader serverInputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            OutputStream serverOutputStream = socket.getOutputStream();

            serverOutputStream.write(String.format("GET / HTTP/1.1\r\nHost: %s\r\n\r\n", url).getBytes());

//            serverOutputStream.writeUTF(host + "\r\n");
//            serverOutputStream.writeUTF("Connection: keep-alive\r\n");
//            serverOutputStream.writeUTF("Cache-Control: max-age=0\r\n");
//            serverOutputStream.writeUTF("Upgrade-Insecure-Requests: 1\r\n");
//            serverOutputStream.writeUTF(userAgent + "\r\n");
//            serverOutputStream.writeUTF("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8\r\n");
//            serverOutputStream.writeUTF("Accept-Encoding: gzip, deflate, br\r\n");
//            serverOutputStream.writeUTF("Accept-Language: ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7\r\n\r\n");
            serverOutputStream.flush();

            String line;
            while ((line = serverInputStream.readLine()) != null){
                System.out.println(line);
                clientOutputStream.write((line).getBytes());
            };
            clientOutputStream.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
