package com.example.proxyforward;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.StringTokenizer;
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
            DataOutputStream out =
                    new DataOutputStream(clientSocket.getOutputStream());
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));

            String inputLine, outputLine;
            int cnt = 0;
            String urlToCall = "";
            ///////////////////////////////////
            //begin get request from client
            String[] tokens = request.split(" ");
            urlToCall = tokens[1];
            //can redirect this to output log
            System.out.println("Request for : " + urlToCall);
            while ((inputLine = in.readLine()) != null) {
                try {
                    StringTokenizer tok = new StringTokenizer(inputLine);
                    tok.nextToken();
                } catch (Exception e) {
                    break;
                }

            }
            //end get request from client
            ///////////////////////////////////


            BufferedReader rd = null;
            try {
                //System.out.println("sending request
                //to real server for url: "
                //        + urlToCall);
                ///////////////////////////////////
                //begin send request to server, get response from server
                URL url = new URL(urlToCall);
                URLConnection conn = url.openConnection();
                conn.setDoInput(true);
                //not doing HTTP posts
                conn.setDoOutput(false);
                //System.out.println("Type is: "
                //+ conn.getContentType());
                //System.out.println("content length: "
                //+ conn.getContentLength());
                //System.out.println("allowed user interaction: "
                //+ conn.getAllowUserInteraction());
                //System.out.println("content encoding: "
                //+ conn.getContentEncoding());
                //System.out.println("content type: "
                //+ conn.getContentType());

                // Get the response
                InputStream is = null;
                HttpURLConnection huc = (HttpURLConnection)conn;
                if (conn.getContentLength() > 0) {
                    try {
                        is = conn.getInputStream();
                        rd = new BufferedReader(new InputStreamReader(is));
                    } catch (IOException ioe) {
                        System.out.println(
                                "********* IO EXCEPTION **********: " + ioe);
                    }
                }
                //end send request to server, get response from server
                ///////////////////////////////////

                ///////////////////////////////////
                //begin send response to client
                byte by[] = new byte[ BUFFER_SIZE ];
                int index = is.read( by, 0, BUFFER_SIZE );
                while ( index != -1 )
                {
                    out.write( by, 0, index );
                    index = is.read( by, 0, BUFFER_SIZE );
                }
                out.flush();

                //end send response to client
                ///////////////////////////////////
            } catch (Exception e) {
                //can redirect this to error log
                System.err.println("Encountered exception: " + e);
                //encountered error - just send nothing back, so
                //processing can continue
                out.writeBytes("");
            }

            //close out all resources
            if (rd != null) {
                rd.close();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (clientSocket != null) {
               clientSocket.close();
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

