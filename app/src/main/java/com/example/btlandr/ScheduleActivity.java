package com.example.btlandr;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.*;
import android.os.*;
import android.widget.*;
import android.content.Intent;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

public class ScheduleActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String uid;

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private List<Event> eventList = new ArrayList<>();

    private EditText titleInput, noteInput;
    private Button startTimeButton, endTimeButton, saveEventButton;

    private long startTimeMillis = 0;
    private long endTimeMillis = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        uid = auth.getCurrentUser().getUid();

        titleInput = findViewById(R.id.titleInput);
        noteInput = findViewById(R.id.noteInput);
        startTimeButton = findViewById(R.id.startTimeButton);
        endTimeButton = findViewById(R.id.endTimeButton);
        saveEventButton = findViewById(R.id.saveEventButton);

        recyclerView = findViewById(R.id.eventRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EventAdapter(eventList, new EventAdapter.OnEventActionListener() {
            @Override
            public void onDelete(String eventId) {
                deleteEvent(eventId);
            }

            @Override
            public void onDetail(Event event) {
                Intent i = new Intent(ScheduleActivity.this, EventDetailActivity.class);
                i.putExtra("title", event.getTitle());
                i.putExtra("note", event.getNote());
                i.putExtra("start", event.getStartTime());
                i.putExtra("end", event.getEndTime());
                i.putExtra("category", event.getCategory());
                startActivity(i);
            }
        });

        recyclerView.setAdapter(adapter);

        startTimeButton.setOnClickListener(v -> pickDateTime(true));
        endTimeButton.setOnClickListener(v -> pickDateTime(false));

        saveEventButton.setOnClickListener(v -> {
            String title = titleInput.getText().toString();
            String note = noteInput.getText().toString();

            if (title.isEmpty() || startTimeMillis == 0) {
                Toast.makeText(this, "Vui lòng nhập tiêu đề và chọn thời gian!", Toast.LENGTH_SHORT).show();
                return;
            }

            Event event = new Event(title, note, startTimeMillis, endTimeMillis, "Cá nhân");
            addEvent(event);
            scheduleReminder(event.getTitle(), event.getNote(), event.getStartTime());
        });

        Button shareButton = findViewById(R.id.shareButton);
        shareButton.setOnClickListener(v -> {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String shareText = "Mã chia sẻ lịch của tôi: " + uid;
            Toast.makeText(this, shareText, Toast.LENGTH_LONG).show();

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_TEXT, shareText);
            intent.setType("text/plain");
            startActivity(Intent.createChooser(intent, "Chia sẻ qua..."));
        });

        loadEvents();
        createNotificationChannel();
    }

    private void pickDateTime(boolean isStart) {
        Calendar now = Calendar.getInstance();

        DatePickerDialog dateDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    TimePickerDialog timeDialog = new TimePickerDialog(
                            this,
                            (timeView, hour, minute) -> {
                                Calendar chosen = Calendar.getInstance();
                                chosen.set(year, month, dayOfMonth, hour, minute, 0);

                                if (isStart) {
                                    startTimeMillis = chosen.getTimeInMillis();
                                    startTimeButton.setText("Bắt đầu: " + chosen.getTime().toString());
                                } else {
                                    endTimeMillis = chosen.getTimeInMillis();
                                    endTimeButton.setText("Kết thúc: " + chosen.getTime().toString());
                                }
                            },
                            now.get(Calendar.HOUR_OF_DAY),
                            now.get(Calendar.MINUTE),
                            true
                    );
                    timeDialog.show();
                },
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH),
                now.get(Calendar.DAY_OF_MONTH)
        );
        dateDialog.show();
    }

    private void addEvent(Event event) {
        db.collection("UserAccount").document(uid).collection("events")
                .add(event)
                .addOnSuccessListener(doc -> Toast.makeText(this, "Đã lưu sự kiện!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadEvents() {
        db.collection("UserAccount").document(uid).collection("events")
                .orderBy("startTime", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    eventList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Event event = doc.toObject(Event.class);
                        event.setId(doc.getId());
                        eventList.add(event);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void deleteEvent(String eventId) {
        db.collection("UserAccount").document(uid).collection("events").document(eventId)
                .delete()
                .addOnSuccessListener(a -> Toast.makeText(this, "Đã xóa!", Toast.LENGTH_SHORT).show());
    }

    // 🔔 Đặt nhắc
    private void scheduleReminder(String title, String note, long timeInMillis) {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!am.canScheduleExactAlarms()) {
                Toast.makeText(this, "Thiếu quyền đặt báo nhắc chính xác", Toast.LENGTH_LONG).show();
                return;
            }
        }

        Intent intent = new Intent(this, ReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("note", note);

        PendingIntent pi = PendingIntent.getBroadcast(
                this,
                (int) System.currentTimeMillis(),
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        am.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pi);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "reminder_channel",
                    "Reminder Channel",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Nhắc nhở lịch học và sự kiện");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}
