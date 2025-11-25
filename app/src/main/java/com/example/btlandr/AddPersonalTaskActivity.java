package com.example.btlandr;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.*;
import java.text.SimpleDateFormat;

public class AddPersonalTaskActivity extends AppCompatActivity {

    private EditText titleInput, noteInput;
    private LinearLayout startTimeButton, endTimeButton;
    private TextView startTimeText, endTimeText;
    private Button saveEventButton;
    private long startMillis = 0, endMillis = 0;

    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_personal_task);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        titleInput = findViewById(R.id.titleInput);
        noteInput = findViewById(R.id.noteInput);
        startTimeButton = findViewById(R.id.startTimeButton);
        endTimeButton = findViewById(R.id.endTimeButton);
        startTimeText = findViewById(R.id.startTimeText);
        endTimeText = findViewById(R.id.endTimeText);
        saveEventButton = findViewById(R.id.saveEventButton);

        startTimeButton.setOnClickListener(v -> pickDateTime(true));
        endTimeButton.setOnClickListener(v -> pickDateTime(false));

        saveEventButton.setOnClickListener(v -> saveEvent());
    }

    private void pickDateTime(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) ->
                new TimePickerDialog(this, (t, h, min) -> {
                    cal.set(y, m, d, h, min);
                    String formattedTime = formatDateTime(cal);

                    if (isStart) {
                        startMillis = cal.getTimeInMillis();
                        startTimeText.setText(formattedTime);
                    } else {
                        endMillis = cal.getTimeInMillis();
                        endTimeText.setText(formattedTime);
                    }
                }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show(),
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private String formatDateTime(Calendar cal) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault());
        return sdf.format(cal.getTime());
    }

    private void saveEvent() {
        String title = titleInput.getText().toString().trim();
        String note = noteInput.getText().toString().trim();

        // ⚠️ Validate dữ liệu
        if (title.isEmpty()) {
            titleInput.setError("Vui lòng nhập tiêu đề");
            titleInput.requestFocus();
            return;
        }

        if (startMillis == 0) {
            Toast.makeText(this, "Vui lòng chọn thời gian bắt đầu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endMillis == 0) {
            Toast.makeText(this, "Vui lòng chọn thời gian kết thúc", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endMillis <= startMillis) {
            Toast.makeText(this, "Thời gian kết thúc phải sau thời gian bắt đầu", Toast.LENGTH_SHORT).show();
            return;
        }

        if (startMillis < System.currentTimeMillis()) {
            Toast.makeText(this, "Không thể chọn thời gian trong quá khứ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (note.length() > 500) {
            noteInput.setError("Ghi chú quá dài (tối đa 500 ký tự)");
            noteInput.requestFocus();
            return;
        }

        // ✅ Nếu hợp lệ, lưu vào Firestore
        Event event = new Event(title, note, startMillis, endMillis, "Cá nhân");

        db.collection("UserAccount")
                .document(uid)
                .collection("events")
                .add(event)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "Đã lưu sự kiện!", Toast.LENGTH_SHORT).show();
                    scheduleReminder(event.getTitle(), event.getNote(), event.getStartTime());
                    finish();
                })
                .addOnFailureListener(e -> {
                    // ✅ Xử lý offline
                    if (!NetworkUtil.isOnline(this)) {
                        Toast.makeText(this, "Không có mạng - lưu tạm offline", Toast.LENGTH_SHORT).show();
                        scheduleReminder(event.getTitle(), event.getNote(), event.getStartTime());
                        finish();
                    } else {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }


    // 🔔 Tạo nhắc nhở
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