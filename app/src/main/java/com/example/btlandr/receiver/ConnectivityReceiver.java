package com.example.btlandr.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.widget.Toast;

import com.example.btlandr.util.NetworkUtil;

public class ConnectivityReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        boolean connected = NetworkUtil.isOnline(context);
        if (connected) {
            Toast.makeText(context, "Mạng đã trở lại — đồng bộ dữ liệu!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Mất kết nối mạng", Toast.LENGTH_SHORT).show();
        }
    }
}
