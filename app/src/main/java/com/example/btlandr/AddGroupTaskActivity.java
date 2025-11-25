package com.example.btlandr;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;

public class AddGroupTaskActivity extends AppCompatActivity {

    private EditText titleInput, noteInput;
    private Button startTimeButton, endTimeButton, saveEventButton;

    private long startMillis = 0, endMillis = 0;
    private String groupId;

    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group_task);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();
        groupId = getIntent().getStringExtra("groupId");

        titleInput = findViewById(R.id.titleInput);
        noteInput = findViewById(R.id.noteInput);
        startTimeButton = findViewById(R.id.startTimeButton);
        endTimeButton = findViewById(R.id.endTimeButton);
        saveEventButton = findViewById(R.id.saveEventButton);

        startTimeButton.setOnClickListener(v -> pickDateTime(true));
        endTimeButton.setOnClickListener(v -> pickDateTime(false));
        saveEventButton.setOnClickListener(v -> saveEvent());
    }

    private void pickDateTime(boolean isStart) {
        Calendar cal = Calendar.getInstance();

        new DatePickerDialog(this, (view, y, m, d) -> {
            new TimePickerDialog(this, (t, h, min) -> {
                cal.set(y, m, d, h, min);
                if (isStart) {
                    startMillis = cal.getTimeInMillis();
                    startTimeButton.setText("Bắt đầu: " + cal.getTime());
                } else {
                    endMillis = cal.getTimeInMillis();
                    endTimeButton.setText("Kết thúc: " + cal.getTime());
                }
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void saveEvent() {
        String title = titleInput.getText().toString().trim();
        String note = noteInput.getText().toString().trim();

        // ✅ Validate dữ liệu đầu vào
        if (title.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tiêu đề!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (startMillis == 0 || endMillis == 0) {
            Toast.makeText(this, "Vui lòng chọn thời gian bắt đầu và kết thúc!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (endMillis < startMillis) {
            Toast.makeText(this, "Thời gian kết thúc phải sau thời gian bắt đầu!", Toast.LENGTH_SHORT).show();
            return;
        }

        Event event = new Event(title, note, startMillis, endMillis, "Nhóm");
        db.collection("Groups").document(groupId).collection("tasks")
                .add(event)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Đã tạo task nhóm!", Toast.LENGTH_SHORT).show();
                    scheduleReminder(event.getTitle(), event.getNote(), event.getStartTime());
                    finish(); // ✅ Quay lại GroupDetailActivity
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // 🔔 Lên lịch thông báo
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
}
