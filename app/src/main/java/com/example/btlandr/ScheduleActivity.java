package com.example.btlandr;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.btlandr.Event;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.Map;

public class ScheduleActivity extends AppCompatActivity {

    FirebaseFirestore db;
    String uid;

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Reminder Channel";
            String description = "Nhắc nhở lịch học và sự kiện";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("reminder_channel", name, importance);
            channel.setDescription(description);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    private void scheduleReminder(String title, String note, long timeInMillis) {
        Log.d("ALARM_TEST", "Đặt nhắc: " + title + " vào " + timeInMillis);

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("note", note);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        db = FirebaseFirestore.getInstance();
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            uid = auth.getCurrentUser().getUid();
            loadEvents();
        } else {
            Toast.makeText(this, "Bạn chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            finish(); // hoặc mở lại màn hình đăng nhập
            return;
        }

        // Lấy danh sách sự kiện khi mở app
        loadEvents();

        createNotificationChannel();

        Button logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

    }

    // Hàm thêm sự kiện mới
    private void addEvent(Event event) {
        db.collection("UserData").document(uid).collection("events")
                .add(event)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Đã thêm sự kiện!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Hàm tải danh sách sự kiện (Realtime)
    private void loadEvents() {
        db.collection("UserData").document(uid).collection("events")
                .orderBy("startTime", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Lỗi tải dữ liệu: ", error);
                        return;
                    }
                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Event e = doc.toObject(Event.class);
                            Log.d("Event", e.getTitle() + " | " + e.getCategory());
                        }
                    }
                });
    }

    // Hàm cập nhật sự kiện
    private void updateEvent(String eventId, Map<String, Object> updates) {
        db.collection("UserData").document(uid).collection("events").document(eventId)
                .update(updates)
                .addOnSuccessListener(a -> Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show());
    }

    // Hàm xóa sự kiện
    private void deleteEvent(String eventId) {
        db.collection("UserData").document(uid).collection("events").document(eventId)
                .delete()
                .addOnSuccessListener(a -> Toast.makeText(this, "Đã xóa!", Toast.LENGTH_SHORT).show());
    }
}
