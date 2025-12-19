package com.example.btlandr.activity;

import android.app.*;
import android.content.Intent;
import android.os.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.example.btlandr.R;
import com.example.btlandr.model.Event;
import com.example.btlandr.util.NetworkUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.*;
import java.text.SimpleDateFormat;
import com.example.btlandr.receiver.ReminderReceiver;

public class AddPersonalTaskActivity extends AppCompatActivity {

    private EditText titleInput, noteInput;
    private LinearLayout startTimeButton, endTimeButton;
    private TextView startTimeText, endTimeText;
    private Button saveEventButton;
    private CheckBox importantCheckBox;
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
        importantCheckBox = findViewById(R.id.importantCheckBox);

        startTimeButton.setOnClickListener(v -> pickDateTime(true));
        endTimeButton.setOnClickListener(v -> pickDateTime(false));

        saveEventButton.setOnClickListener(v -> saveEvent());
    }

    private void pickDateTime(boolean isStart) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(this, (view, y, m, d) -> new TimePickerDialog(this, (t, h, min) -> {
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
        boolean isImportant = importantCheckBox.isChecked();

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

        // ✅ Chỉ kiểm tra nếu task mới có important = true
        if (!isImportant) {
            // Task không quan trọng, lưu luôn không cần kiểm tra
            saveTaskToFirestore(title, note, startMillis, endMillis, isImportant);
            return;
        }

        // ✅ Kiểm tra xung đột với important tasks
        checkTimeConflictWithImportantTasks(title, note, startMillis, endMillis, isImportant);
    }

    private void checkTimeConflictWithImportantTasks(String title, String note, long startMillis, long endMillis,
            boolean isImportant) {
        // Bước 1: Kiểm tra Personal Important Tasks
        db.collection("UserAccount").document(uid).collection("events")
                .whereEqualTo("important", true)
                .get()
                .addOnSuccessListener(personalSnapshot -> {
                    // Kiểm tra xung đột với personal tasks
                    for (QueryDocumentSnapshot doc : personalSnapshot) {
                        Long existingStart = doc.getLong("startTime");
                        Long existingEnd = doc.getLong("endTime");

                        if (existingStart != null && existingEnd != null) {
                            if (isTimeOverlap(startMillis, endMillis, existingStart, existingEnd)) {
                                Toast.makeText(this,
                                        "⚠️ Thời gian trùng với task quan trọng: " + doc.getString("title"),
                                        Toast.LENGTH_LONG).show();
                                return;
                            }
                        }
                    }

                    // Bước 2: Kiểm tra Group Important Tasks
                    checkGroupImportantTasks(title, note, startMillis, endMillis, isImportant);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi kiểm tra: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkGroupImportantTasks(String title, String note, long startMillis, long endMillis,
            boolean isImportant) {
        // Lấy tất cả groups mà user tham gia
        db.collection("Groups")
                .whereArrayContains("members", uid)
                .get()
                .addOnSuccessListener(groupSnapshot -> {
                    if (groupSnapshot.isEmpty()) {
                        // Không có group nào, lưu luôn
                        saveTaskToFirestore(title, note, startMillis, endMillis, isImportant);
                        return;
                    }

                    // Đếm số group cần kiểm tra
                    int totalGroups = groupSnapshot.size();
                    int[] checkedGroups = { 0 };
                    boolean[] hasConflict = { false };

                    for (QueryDocumentSnapshot groupDoc : groupSnapshot) {
                        String checkGroupId = groupDoc.getId();

                        db.collection("Groups").document(checkGroupId).collection("tasks")
                                .whereEqualTo("important", true)
                                .get()
                                .addOnSuccessListener(taskSnapshot -> {
                                    if (hasConflict[0])
                                        return; // Đã có conflict rồi thì bỏ qua

                                    // Kiểm tra xung đột
                                    for (QueryDocumentSnapshot taskDoc : taskSnapshot) {
                                        Long existingStart = taskDoc.getLong("startTime");
                                        Long existingEnd = taskDoc.getLong("endTime");

                                        if (existingStart != null && existingEnd != null) {
                                            if (isTimeOverlap(startMillis, endMillis, existingStart, existingEnd)) {
                                                hasConflict[0] = true;
                                                Toast.makeText(this,
                                                        "⚠️ Thời gian trùng với task nhóm quan trọng: "
                                                                + taskDoc.getString("title"),
                                                        Toast.LENGTH_LONG).show();
                                                return;
                                            }
                                        }
                                    }

                                    // Tăng số group đã kiểm tra
                                    checkedGroups[0]++;
                                    if (checkedGroups[0] == totalGroups && !hasConflict[0]) {
                                        // Đã kiểm tra hết, không có xung đột -> Lưu
                                        saveTaskToFirestore(title, note, startMillis, endMillis, isImportant);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    checkedGroups[0]++;
                                    if (checkedGroups[0] == totalGroups && !hasConflict[0]) {
                                        saveTaskToFirestore(title, note, startMillis, endMillis, isImportant);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi kiểm tra nhóm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // ✅ Kiểm tra 2 khoảng thời gian có trùng nhau không
    private boolean isTimeOverlap(long start1, long end1, long start2, long end2) {
        // Trùng khi: start1 < end2 VÀ end1 > start2
        return (start1 < end2 && end1 > start2);
    }

    private void saveTaskToFirestore(String title, String note, long startMillis, long endMillis, boolean isImportant) {
        // ✅ Tạo Event với constructor đúng (startTime, endTime)
        Event event = new Event(title, note, startMillis, endMillis, "Cá nhân", isImportant);

        db.collection("UserAccount")
                .document(uid)
                .collection("events")
                .add(event)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "✅ Đã lưu sự kiện!", Toast.LENGTH_SHORT).show();
                    scheduleReminder(title, note, startMillis);
                    finish();
                })
                .addOnFailureListener(e -> {
                    // ✅ Xử lý offline
                    if (!NetworkUtil.isOnline(this)) {
                        Toast.makeText(this, "Không có mạng - lưu tạm offline", Toast.LENGTH_SHORT).show();
                        scheduleReminder(title, note, startMillis);
                        finish();
                    } else {
                        Toast.makeText(this, "❌ Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                PendingIntent.FLAG_IMMUTABLE);

        am.setExact(AlarmManager.RTC_WAKEUP, timeInMillis, pi);
    }
}