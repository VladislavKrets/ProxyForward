package com.example.proxyforward;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    private EditText portEditText;
    private Button startButton, stopButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        portEditText = findViewById(R.id.portEditText);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        FlagSingleton.getInstance();
        if (isMyServiceRunning(ProxyService.class)) {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        }
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!portEditText.getText().toString().matches("\\d+")) return;
                Intent intent = new Intent(MainActivity.this, ProxyService.class);
                intent.putExtra("port", Integer.parseInt(portEditText.getText().toString()));
                startService(intent);
                //new ServerTask().execute();
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(MainActivity.this, ProxyService.class));
                //FlagSingleton.getInstance().setFlag(false);
                stopButton.setEnabled(false);
                startButton.setEnabled(true);
            }
        });

    }



    class ServerTask extends AsyncTask<Void, Void, Void>{
        private int port = 4333;

        @Override
        protected void onPreExecute() {
            port = Integer.parseInt(portEditText.getText().toString());
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
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
