package com.example.proxyforward;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
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
        Intent notificationIntent = new Intent(this, MainActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel("com.example.proxyforward",
                    "ProxyService", NotificationManager.IMPORTANCE_NONE);
            notificationChannel.setLightColor(Color.BLUE);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(notificationChannel);
        }
        startForeground(1, new NotificationCompat.Builder(this,
                "com.example.proxyforward")
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Server is running")
                .setContentIntent(pendingIntent)
                .build());
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
