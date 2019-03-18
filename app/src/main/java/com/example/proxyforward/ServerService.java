package com.example.proxyforward;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class ServerService extends Service {
    public ServerService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        new ServerSocketThread(intent.getIntExtra("port", 4333)).start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        FlagSingleton.getInstance().setFlag(false);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
