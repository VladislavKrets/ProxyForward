package com.example.proxyforward;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientSocketHandler extends Thread {
    public static final Pattern CONNECT_PATTERN = Pattern.compile("CONNECT (.+):(.+) HTTP/(1\\.[01])",
            Pattern.CASE_INSENSITIVE);
    private static final int BUFFER_SIZE = 32768;
    private final Socket clientSocket;
    private boolean previousWasR = false;

    public ClientSocketHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() {
        try {
            String request = readLine(clientSocket);
            if (request.startsWith("CONNECT ")) HttpsHandler(request);
            else HttpHandler(request);
        } catch (IOException e) {
            e.printStackTrace();  // TODO: implement catch
        } finally {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();  // TODO: implement catch
            }
        }
    }
    private void HttpHandler(String request){
        try {
            InputStream clientInputStream = clientSocket.getInputStream();
            OutputStream clientOutputStream = clientSocket.getOutputStream();
            System.out.println(request);
            String url;
            try {
               url = request.split(" ")[1];
            }
            catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                return;
            }
            System.out.println(url);
            if (!url.matches("http://.+\\.\\w+(/.+)*/?")){
                clientInputStream.close();
                clientOutputStream.close();
                clientSocket.close();
                return;
            }
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(clientSocket.getOutputStream(),
                    "ISO-8859-1");

            final Socket forwardSocket;
            String host = url.split("/")[2];
            try {
                forwardSocket = new Socket(host, 80);
                System.out.println(forwardSocket);
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();  // TODO: implement catch
                outputStreamWriter.write("HTTP/" + host + " 502 Bad Gateway\r\n");
                outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
                outputStreamWriter.write("\r\n");
                outputStreamWriter.flush();
                return;
            }
            try {

                Thread remoteToClient = new Thread() {
                    @Override
                    public void run() {
                        forwardData(forwardSocket, clientSocket);
                    }
                };
                remoteToClient.start();
                System.out.println(url.replaceAll("http://.+\\.\\w+/", "/"));
                forwardSocket.getOutputStream().write(("GET " + url.replaceAll("http://.+\\.\\w+/", "/") + " HTTP/1.1\r\n").getBytes());
                try {
                    if (previousWasR) {
                        int read = clientSocket.getInputStream().read();
                        if (read != -1) {
                            if (read != '\n') {
                                forwardSocket.getOutputStream().write(read);
                            }
                            forwardData(clientSocket, forwardSocket);
                        } else {
                            if (!forwardSocket.isOutputShutdown()) {
                                forwardSocket.shutdownOutput();
                            }
                            if (!clientSocket.isInputShutdown()) {
                                clientSocket.shutdownInput();
                            }
                        }
                    } else {
                        forwardData(clientSocket, forwardSocket);
                    }
                } finally {
                    try {
                        remoteToClient.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();  // TODO: implement catch
                    }
                }
            } finally {
                forwardSocket.close();
            }


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void HttpsHandler(String request) throws IOException{
        Matcher matcher = CONNECT_PATTERN.matcher(request);
        if (matcher.matches()) {
            String header;
            do {
                header = readLine(clientSocket);
            } while (!"".equals(header));
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(clientSocket.getOutputStream(),
                    "ISO-8859-1");

            final Socket forwardSocket;
            try {
                forwardSocket = new Socket(matcher.group(1), Integer.parseInt(matcher.group(2)));
                System.out.println(forwardSocket);
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();  // TODO: implement catch
                outputStreamWriter.write("HTTP/" + matcher.group(3) + " 502 Bad Gateway\r\n");
                outputStreamWriter.write("Proxy-agent: Simple/0.1\r\n");
                outputStreamWriter.write("\r\n");
                outputStreamWriter.flush();
                return;
            }
            try {
                outputStreamWriter.write("HTTP/" + matcher.group(3) + " 200 Connection established\r\n");
                outputStreamWriter.write("\r\n");
                outputStreamWriter.flush();

                Thread remoteToClient = new Thread() {
                    @Override
                    public void run() {
                        forwardData(forwardSocket, clientSocket);
                    }
                };
                remoteToClient.start();
                try {
                    if (previousWasR) {
                        int read = clientSocket.getInputStream().read();
                        if (read != -1) {
                            if (read != '\n') {
                                forwardSocket.getOutputStream().write(read);
                            }
                            forwardData(clientSocket, forwardSocket);
                        } else {
                            if (!forwardSocket.isOutputShutdown()) {
                                forwardSocket.shutdownOutput();
                            }
                            if (!clientSocket.isInputShutdown()) {
                                clientSocket.shutdownInput();
                            }
                        }
                    } else {
                        forwardData(clientSocket, forwardSocket);
                    }
                } finally {
                    try {
                        remoteToClient.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();  // TODO: implement catch
                    }
                }
            } finally {
                forwardSocket.close();
            }
        }
    }


    private static void forwardData(Socket inputSocket, Socket outputSocket) {
        try {
            InputStream inputStream = inputSocket.getInputStream();
            try {
                OutputStream outputStream = outputSocket.getOutputStream();
                try {
                    byte[] buffer = new byte[4096];
                    int read;
                    do {
                        read = inputStream.read(buffer);
                        if (read > 0) {
                            outputStream.write(buffer, 0, read);
                            if (inputStream.available() < 1) {
                                outputStream.flush();
                            }
                        }
                    } while (read >= 0);
                } finally {
                    if (!outputSocket.isOutputShutdown()) {
                        outputSocket.shutdownOutput();
                    }
                }
            } finally {
                if (!inputSocket.isInputShutdown()) {
                    inputSocket.shutdownInput();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();  // TODO: implement catch
        }
    }

    private String readLine(Socket socket) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        int next;
        readerLoop:
        while ((next = socket.getInputStream().read()) != -1) {
            if (previousWasR && next == '\n') {
                previousWasR = false;
                continue;
            }
            previousWasR = false;
            switch (next) {
                case '\r':
                    previousWasR = true;
                    break readerLoop;
                case '\n':
                    break readerLoop;
                default:
                    byteArrayOutputStream.write(next);
                    break;
            }
        }
        return byteArrayOutputStream.toString("ISO-8859-1");
    }
}

