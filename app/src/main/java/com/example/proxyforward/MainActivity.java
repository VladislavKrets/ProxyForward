package com.example.proxyforward;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private EditText portEditText;
    private Button startButton, stopButton, wifiTetherButton;
    private TextView proxyStatusTextView, proxyURLTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        initializeViews();

        initializeListeners();

    }

    private void initializeListeners() {
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!portEditText.getText().toString().matches("\\d+")) return;
                int port = Integer.parseInt(portEditText.getText().toString());
                String ip = getIPAddress(true);
                if (ip.trim().startsWith("10.")) {
                    proxyStatusTextView.setText(getString(R.string.turn_on_tethering));
                    proxyURLTextView.setText(getString(R.string.connect_wifi));
                    return;
                }
                Intent intent = new Intent(MainActivity.this, ProxyService.class);
                intent.putExtra("port", port);
                startService(intent);
                proxyStatusTextView.setText(getString(R.string.proxy_is_running));
                proxyURLTextView.setText(String.format("%s:%d", getIPAddress(true), port));
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(new Intent(MainActivity.this, ProxyService.class));
                proxyStatusTextView.setText(getString(R.string.proxy_stopped));
                proxyURLTextView.setText("");
                stopButton.setEnabled(false);
                startButton.setEnabled(true);
            }
        });
        wifiTetherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchHotspotSettings();
            }
        });
    }

    private void initializeViews() {
        portEditText = findViewById(R.id.portEditText);
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);
        wifiTetherButton = findViewById(R.id.WiFiTetherButton);
        proxyStatusTextView = findViewById(R.id.proxyStatus);
        proxyURLTextView = findViewById(R.id.proxyURL);

        if (isProxyServiceRunning(ProxyService.class)) {
            startButton.setEnabled(false);
            stopButton.setEnabled(true);
        }
    }

    public String getIPAddress(boolean useIPv4) {
        try {
            boolean isIPv4;
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) { } // for now eat exceptions
        return "";
    }


    private void launchHotspotSettings(){
        Intent tetherSettings = new Intent();
        tetherSettings.setClassName("com.android.settings", "com.android.settings.TetherSettings");
        startActivity(tetherSettings);
    }



    private boolean isProxyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

}
