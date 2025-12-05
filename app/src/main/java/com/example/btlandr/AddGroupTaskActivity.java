package com.example.btlandr;

import android.app.*;
import android.content.*;
import android.os.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Calendar;
import java.util.List;

public class AddGroupTaskActivity extends AppCompatActivity {

    private EditText titleInput, noteInput;
    private Button startTimeButton, endTimeButton, saveEventButton;
    private CheckBox importantCheckBox;

    private long startMillis = 0, endMillis = 0;
    private String groupId, groupName, adminEmail;

    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group_task);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();
        groupId = getIntent().getStringExtra("groupId");
        groupName = getIntent().getStringExtra("groupName");
        adminEmail = getIntent().getStringExtra("adminEmail");

        titleInput = findViewById(R.id.titleInput);
        noteInput = findViewById(R.id.noteInput);
        startTimeButton = findViewById(R.id.startTimeButton);
        endTimeButton = findViewById(R.id.endTimeButton);
        saveEventButton = findViewById(R.id.saveEventButton);
        importantCheckBox = findViewById(R.id.importantCheckBox);

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
        boolean isImportant = importantCheckBox.isChecked();

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

        // ✅ Chỉ kiểm tra nếu task mới có important = true
        if (!isImportant) {
            // Task không quan trọng, lưu luôn không cần kiểm tra
            saveTaskToFirestore(title, note, startMillis, endMillis, isImportant);
            return;
        }

        // ✅ Bước 1: Lấy danh sách members trong group
        db.collection("Groups").document(groupId).get()
                .addOnSuccessListener(groupDoc -> {
                    if (groupDoc.exists()) {
                        List<String> members = (List<String>) groupDoc.get("members");
                        if (members != null && !members.isEmpty()) {
                            // Kiểm tra xung đột với important tasks của tất cả members
                            checkMembersImportantTasks(title, note, startMillis, endMillis, isImportant, members);
                        } else {
                            // Không có member nào, lưu luôn
                            saveTaskToFirestore(title, note, startMillis, endMillis, isImportant);
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy thông tin nhóm!", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải thông tin nhóm: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void checkMembersImportantTasks(String title, String note, long startMillis, long endMillis,
                                            boolean isImportant, List<String> members) {
        int totalMembers = members.size();
        int[] checkedMembers = {0};
        boolean[] hasConflict = {false};

        for (String memberId : members) {
            // Lấy thông tin member trước
            db.collection("UserAccount").document(memberId).get()
                    .addOnSuccessListener(memberDoc -> {
                        String memberName = memberDoc.exists() ? memberDoc.getString("username") : "Không rõ";
                        String memberEmail = memberDoc.exists() ? memberDoc.getString("email") : "";

                        // Kiểm tra Personal Important Tasks của member này
                        db.collection("UserAccount").document(memberId).collection("events")
                                .whereEqualTo("important", true)
                                .get()
                                .addOnSuccessListener(personalSnapshot -> {
                                    if (hasConflict[0]) return; // Đã có conflict rồi thì bỏ qua

                                    // Kiểm tra xung đột với personal tasks
                                    for (QueryDocumentSnapshot doc : personalSnapshot) {
                                        Long existingStart = doc.getLong("startTime");
                                        Long existingEnd = doc.getLong("endTime");

                                        if (existingStart != null && existingEnd != null) {
                                            if (isTimeOverlap(startMillis, endMillis, existingStart, existingEnd)) {
                                                hasConflict[0] = true;
                                                String taskTitle = doc.getString("title");
                                                Toast.makeText(this,
                                                        "⚠️ Xung đột với task quan trọng của " + memberName +
                                                                "\nTask: " + taskTitle,
                                                        Toast.LENGTH_LONG).show();
                                                return;
                                            }
                                        }
                                    }

                                    // Tăng số member đã kiểm tra
                                    checkedMembers[0]++;
                                    if (checkedMembers[0] == totalMembers && !hasConflict[0]) {
                                        // Sau khi check xong personal tasks, kiểm tra group tasks
                                        checkAllGroupImportantTasks(title, note, startMillis, endMillis, isImportant, members);
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    checkedMembers[0]++;
                                    if (checkedMembers[0] == totalMembers && !hasConflict[0]) {
                                        checkAllGroupImportantTasks(title, note, startMillis, endMillis, isImportant, members);
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        checkedMembers[0]++;
                        if (checkedMembers[0] == totalMembers && !hasConflict[0]) {
                            checkAllGroupImportantTasks(title, note, startMillis, endMillis, isImportant, members);
                        }
                    });
        }
    }

    private void checkAllGroupImportantTasks(String title, String note, long startMillis, long endMillis,
                                             boolean isImportant, List<String> members) {
        // Lấy tất cả groups mà các members tham gia
        int totalMembers = members.size();
        int[] checkedMembers = {0};
        boolean[] hasConflict = {false};

        for (String memberId : members) {
            // Lấy thông tin member
            db.collection("UserAccount").document(memberId).get()
                    .addOnSuccessListener(memberDoc -> {
                        String memberName = memberDoc.exists() ? memberDoc.getString("username") : "Không rõ";

                        db.collection("Groups")
                                .whereArrayContains("members", memberId)
                                .get()
                                .addOnSuccessListener(groupSnapshot -> {
                                    if (hasConflict[0]) return;

                                    int totalGroups = groupSnapshot.size();
                                    int[] checkedGroups = {0};

                                    if (totalGroups == 0) {
                                        checkedMembers[0]++;
                                        if (checkedMembers[0] == totalMembers && !hasConflict[0]) {
                                            saveTaskToFirestore(title, note, startMillis, endMillis, isImportant);
                                        }
                                        return;
                                    }

                                    for (QueryDocumentSnapshot groupDoc : groupSnapshot) {
                                        String checkGroupId = groupDoc.getId();

                                        db.collection("Groups").document(checkGroupId).collection("tasks")
                                                .whereEqualTo("important", true)
                                                .get()
                                                .addOnSuccessListener(taskSnapshot -> {
                                                    if (hasConflict[0]) return;

                                                    // Kiểm tra xung đột
                                                    for (QueryDocumentSnapshot taskDoc : taskSnapshot) {
                                                        Long existingStart = taskDoc.getLong("startTime");
                                                        Long existingEnd = taskDoc.getLong("endTime");

                                                        if (existingStart != null && existingEnd != null) {
                                                            if (isTimeOverlap(startMillis, endMillis, existingStart, existingEnd)) {
                                                                hasConflict[0] = true;
                                                                String taskTitle = taskDoc.getString("title");
                                                                String groupName = groupDoc.getString("groupName");
                                                                Toast.makeText(this,
                                                                        "⚠️ Xung đột với task nhóm của " + memberName +
                                                                                "\nNhóm: " + groupName +
                                                                                "\nTask: " + taskTitle,
                                                                        Toast.LENGTH_LONG).show();
                                                                return;
                                                            }
                                                        }
                                                    }

                                                    checkedGroups[0]++;
                                                    if (checkedGroups[0] == totalGroups) {
                                                        checkedMembers[0]++;
                                                        if (checkedMembers[0] == totalMembers && !hasConflict[0]) {
                                                            saveTaskToFirestore(title, note, startMillis, endMillis, isImportant);
                                                        }
                                                    }
                                                })
                                                .addOnFailureListener(e -> {
                                                    checkedGroups[0]++;
                                                    if (checkedGroups[0] == totalGroups) {
                                                        checkedMembers[0]++;
                                                        if (checkedMembers[0] == totalMembers && !hasConflict[0]) {
                                                            saveTaskToFirestore(title, note, startMillis, endMillis, isImportant);
                                                        }
                                                    }
                                                });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    checkedMembers[0]++;
                                    if (checkedMembers[0] == totalMembers && !hasConflict[0]) {
                                        saveTaskToFirestore(title, note, startMillis, endMillis, isImportant);
                                    }
                                });
                    })
                    .addOnFailureListener(e -> {
                        checkedMembers[0]++;
                        if (checkedMembers[0] == totalMembers && !hasConflict[0]) {
                            saveTaskToFirestore(title, note, startMillis, endMillis, isImportant);
                        }
                    });
        }
    }

    // ✅ Kiểm tra 2 khoảng thời gian có trùng nhau không
    private boolean isTimeOverlap(long start1, long end1, long start2, long end2) {
        return (start1 < end2 && end1 > start2);
    }

    private void saveTaskToFirestore(String title, String note, long startMillis, long endMillis, boolean isImportant) {
        Event event = new Event(title, note, startMillis, endMillis, "Nhóm: " + groupName + "(" + adminEmail + ")", isImportant);

        db.collection("Groups").document(groupId).collection("tasks")
                .add(event)
                .addOnSuccessListener(doc -> {
                    Toast.makeText(this, "✅ Đã tạo task nhóm!", Toast.LENGTH_SHORT).show();
                    scheduleReminder(title, note, startMillis);
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "❌ Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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