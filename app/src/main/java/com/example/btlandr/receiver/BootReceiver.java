package com.example.btlandr.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d("BootReceiver", "Khởi động lại thông báo sau khi reboot");
            // Ở đây có thể gọi lại hàm scheduleReminder hoặc khôi phục dữ liệu từ
            // Firestore/local
        }
    }
}
