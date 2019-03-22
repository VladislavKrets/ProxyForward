package com.example.proxyforward;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ProxyService extends Service {
    final String LOG_TAG = "myLogs";
    private int port;
    private ServerThreadTask serverThreadTask;

    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "onCreate");
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOG_TAG, "onStartCommand");
        port = intent.getIntExtra("port", 4333);
        startProxy();
        return super.onStartCommand(intent, flags, startId);
    }

    public void onDestroy() {
        serverThreadTask.interrupt();
        super.onDestroy();

        Log.d(LOG_TAG, "onDestroy");
    }

    public IBinder onBind(Intent intent) {
        Log.d(LOG_TAG, "onBind");
        return null;
    }

    void startProxy() {
        serverThreadTask = new ServerThreadTask(port);
        serverThreadTask.setDaemon(true);
        serverThreadTask.start();
        //new ServerTask().execute();
    }

    class ServerThreadTask extends Thread{
        private int port;

        public ServerThreadTask(int port) {
            this.port = port;
        }

        @Override
        public void run() {
            try {
                ServerSocket httpSocket = new ServerSocket(port);
                Socket clientSocket;
                while (!interrupted()) {
                    clientSocket = httpSocket.accept();
                    System.out.println("Socket accepted");
                    new ClientSocketHandler(clientSocket).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class ServerTask extends AsyncTask<Void, Void, Void> {
        private int port;

        @Override
        protected void onPreExecute() {
            port = ProxyService.this.port;
        }

        @Override
        protected Void doInBackground(Void... voids) {
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
            return null;
        }
    }
}
