package com.example.btlandr;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

public class GroupDetailActivity extends AppCompatActivity {

    private TextView groupNameText, adminEmailText;
    private Button deleteGroupButton, addMemberButton, addGroupTaskButton, groupChatButton, renameGroupButton, zoomMeetingButton;
    private ListView membersListView;

    private LinearLayout containerOngoing, containerUpcoming, containerPast;

    private String groupId, groupName, adminId, adminEmail;
    private FirebaseFirestore db;
    private String currentUid;
    private boolean isAdmin;

    private List<String> memberUids = new ArrayList<>();
    private List<String> memberInfos = new ArrayList<>();
    private MemberAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_detail);

        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getUid();

        groupNameText = findViewById(R.id.groupNameText);
        adminEmailText = findViewById(R.id.adminEmailText);
        deleteGroupButton = findViewById(R.id.deleteGroupButton);
        addMemberButton = findViewById(R.id.addMemberButton);
        addGroupTaskButton = findViewById(R.id.addGroupTaskButton);
        groupChatButton = findViewById(R.id.groupChatButton);
        renameGroupButton = findViewById(R.id.renameGroupButton);
        zoomMeetingButton = findViewById(R.id.zoomMeetingButton);
        membersListView = findViewById(R.id.membersListView);

        containerOngoing = findViewById(R.id.containerOngoing);
        containerUpcoming = findViewById(R.id.containerUpcoming);
        containerPast = findViewById(R.id.containerPast);

        // Nhận dữ liệu từ Intent
        groupId = getIntent().getStringExtra("groupId");
        groupName = getIntent().getStringExtra("groupName");
        adminId = getIntent().getStringExtra("adminId");
        adminEmail = getIntent().getStringExtra("adminEmail");
        String groupName = getIntent().getStringExtra("groupName");

        groupNameText.setText("Tên nhóm: " + groupName);
        adminEmailText.setText("Quản lý: " + adminEmail);

        isAdmin = currentUid.equals(adminId);

        if (!isAdmin) {
            addMemberButton.setVisibility(Button.GONE);
            deleteGroupButton.setVisibility(Button.GONE);
            addGroupTaskButton.setVisibility(Button.GONE);
            renameGroupButton.setVisibility(Button.GONE);
        }

        addMemberButton.setOnClickListener(v -> showAddMemberDialog());
        deleteGroupButton.setOnClickListener(v -> confirmDeleteGroup());
        renameGroupButton.setOnClickListener(v -> showRenameGroupDialog());
        zoomMeetingButton.setOnClickListener(v -> handleZoomMeeting());
        groupChatButton.setOnClickListener(v -> {
            Intent i = new Intent(this, GroupChatActivity.class);
            i.putExtra("groupId", groupId);
            i.putExtra("groupName", getIntent().getStringExtra("groupName"));
            startActivity(i);
        });
        addGroupTaskButton.setOnClickListener(v -> {
            Intent i = new Intent(this, AddGroupTaskActivity.class);
            i.putExtra("groupId", groupId);
            i.putExtra("groupName", groupName);
            i.putExtra("adminEmail", adminEmail);
            startActivity(i);
        });

        adapter = new MemberAdapter(this, memberUids, memberInfos, isAdmin, this::confirmRemoveMember);
        membersListView.setAdapter(adapter);

        loadMembers();
        loadGroupTasks(); // 🔹 Realtime listener task
        loadZoomMeetingStatus(); // 🔹 Realtime listener for zoom meeting
    }

    // -------------------- 🔸 LOAD MEMBERS --------------------
    private void loadMembers() {
        db.collection("Groups").document(groupId)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) return;
                    
                    // Cập nhật tên nhóm realtime
                    String groupName = doc.getString("groupName");
                    if (groupName != null) {
                        groupNameText.setText("Tên nhóm: " + groupName);
                    }
                    
                    List<String> members = (List<String>) doc.get("members");
                    memberUids.clear();
                    memberInfos.clear();

                    if (members != null) {
                        for (String uid : members) {
                            db.collection("UserAccount").document(uid)
                                    .get()
                                    .addOnSuccessListener(userDoc -> {
                                        String name = userDoc.getString("username");
                                        String email = userDoc.getString("email");
                                        memberUids.add(uid);
                                        memberInfos.add((name != null ? name : "Ẩn danh") + " (" + email + ")");
                                        adapter.notifyDataSetChanged();
                                    });
                        }
                    }
                });
    }

    // -------------------- 🔸 ADD MEMBER --------------------
    private void showAddMemberDialog() {
        final EditText emailInput = new EditText(this);
        emailInput.setHint("Nhập email thành viên");
        emailInput.setPadding(32, 32, 32, 32);

        new AlertDialog.Builder(this)
                .setTitle("Thêm thành viên mới")
                .setView(emailInput)
                .setPositiveButton("Thêm", (d, w) -> {
                    String email = emailInput.getText().toString().trim();
                    if (email.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập email!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    db.collection("UserAccount")
                            .whereEqualTo("email", email)
                            .get()
                            .addOnSuccessListener(qs -> {
                                if (qs.isEmpty()) {
                                    Toast.makeText(this, "Không tìm thấy người dùng!", Toast.LENGTH_SHORT).show();
                                } else {
                                    String newUid = qs.getDocuments().get(0).getId();
                                    addMemberToGroup(newUid);
                                }
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void addMemberToGroup(String newUid) {
        if (memberUids.contains(newUid)) {
            Toast.makeText(this, "Người này đã có trong nhóm!", Toast.LENGTH_SHORT).show();
            return;
        }

        db.collection("Groups").document(groupId)
                .update("members", FieldValue.arrayUnion(newUid))
                .addOnSuccessListener(a -> Toast.makeText(this, "Đã thêm thành viên!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void confirmRemoveMember(String uid, String info) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa thành viên")
                .setMessage("Bạn có chắc muốn xóa " + info + " khỏi nhóm không?")
                .setPositiveButton("Xóa", (d, w) -> removeMember(uid))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void removeMember(String uidToRemove) {
        // ✅ Không cho phép admin tự xóa chính mình
        if (uidToRemove.equals(FirebaseAuth.getInstance().getUid())) {
            Toast.makeText(this, "Không thể xóa quản lý nhóm!", Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ Không cho phép người dùng tự xóa chính mình
        if (uidToRemove.equals(currentUid)) {
            Toast.makeText(this, "Không thể tự xóa bản thân khỏi nhóm!", Toast.LENGTH_SHORT).show();
            return;
        }

        DocumentReference groupRef = db.collection("Groups").document(groupId);

        groupRef.update("members", FieldValue.arrayRemove(uidToRemove))
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Đã xóa thành viên!", Toast.LENGTH_SHORT).show();
                    //showMembersList(); // Reload danh sách sau khi xóa
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // -------------------- 🔸 DELETE GROUP --------------------
    private void confirmDeleteGroup() {
        new AlertDialog.Builder(this)
                .setTitle("Xoá nhóm?")
                .setMessage("Bạn có chắc muốn xoá nhóm này không?")
                .setPositiveButton("Xoá", (d, w) -> deleteGroup())
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void deleteGroup() {
        db.collection("Groups").document(groupId)
                .delete()
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Đã xoá nhóm!", Toast.LENGTH_SHORT).show();
                    finish(); // ✅ Quay lại trang trước
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // -------------------- 🔸 ZOOM MEETING --------------------
    private void handleZoomMeeting() {
        if (!isAdmin) {
            Toast.makeText(this, "Chỉ quản lý nhóm mới có thể tạo meeting", Toast.LENGTH_SHORT).show();
            return;
        }

        // Check if meeting already exists
        db.collection("ZoomMeetings").document(groupId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String meetingLink = doc.getString("meetingLink");
                        showZoomOptions(meetingLink);
                    } else {
                        createZoomMeeting();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void createZoomMeeting() {
        String meetingLink = "https://zoom.us/meeting/" + groupId + "_" + System.currentTimeMillis();

        Map<String, Object> meetingData = new HashMap<>();
        meetingData.put("groupId", groupId);
        meetingData.put("groupName", groupName);
        meetingData.put("meetingLink", meetingLink);
        meetingData.put("createdBy", currentUid);
        meetingData.put("createdAt", System.currentTimeMillis());
        meetingData.put("isActive", true);

        db.collection("ZoomMeetings").document(groupId)
                .set(meetingData)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Đã tạo phòng họp!", Toast.LENGTH_SHORT).show();
                    showZoomOptions(meetingLink);
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showZoomOptions(String meetingLink) {
        new AlertDialog.Builder(this)
                .setTitle("Phòng họp Zoom")
                .setMessage(meetingLink)
                .setPositiveButton("Tham gia", (d, w) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(meetingLink));
                    startActivity(intent);
                })
                .setNeutralButton("Sao chép link", (d, w) -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                    android.content.ClipData clip = android.content.ClipData.newPlainText("Zoom Link", meetingLink);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(this, "Đã sao chép link!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Kết thúc", (d, w) -> endZoomMeeting())
                .show();
    }

    private void endZoomMeeting() {
        db.collection("ZoomMeetings").document(groupId)
                .delete()
                .addOnSuccessListener(a -> Toast.makeText(this, "Đã kết thúc phòng họp!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void loadZoomMeetingStatus() {
        db.collection("ZoomMeetings").document(groupId)
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        zoomMeetingButton.setText("🔴 Phòng họp đang diễn ra");
                        zoomMeetingButton.setBackgroundColor(0xFFF44336); // Red
                    } else {
                        zoomMeetingButton.setText("📹 Tạo phòng họp");
                        zoomMeetingButton.setBackgroundColor(0xFF2196F3); // Blue
                    }
                });
    }

    // -------------------- 🔸 LOAD GROUP TASKS --------------------
    private void loadGroupTasks() {
        db.collection("Groups").document(groupId).collection("tasks")
                .orderBy("startTime", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    long now = System.currentTimeMillis();

                    containerOngoing.removeAllViews();
                    containerUpcoming.removeAllViews();
                    containerPast.removeAllViews();

                    for (QueryDocumentSnapshot doc : snapshots) {
                        Event ev = doc.toObject(Event.class);
                        String eventId = doc.getId();

                        // Tạo layout item cho mỗi task
                        LinearLayout itemLayout = new LinearLayout(this);
                        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
                        itemLayout.setPadding(16, 8, 16, 8);

                        TextView titleView = new TextView(this);
                        titleView.setText("• " + ev.getTitle() + " (" + new Date(ev.getStartTime()) + ")");
                        titleView.setTextSize(15);
                        titleView.setLayoutParams(new LinearLayout.LayoutParams(
                                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

                        itemLayout.addView(titleView);

                        // 🔹 Nút xóa (chỉ admin thấy)
                        if (isAdmin) {
                            ImageButton deleteBtn = new ImageButton(this);
                            deleteBtn.setImageResource(android.R.drawable.ic_delete);
                            deleteBtn.setBackground(null);
                            deleteBtn.setOnClickListener(v -> confirmDeleteTask(eventId, ev.getTitle()));
                            itemLayout.addView(deleteBtn);
                        }

                        // Phân loại theo thời gian
                        if (ev.getEndTime() < now) {
                            containerPast.addView(itemLayout);
                        } else if (ev.getStartTime() > now) {
                            containerUpcoming.addView(itemLayout);
                        } else {
                            containerOngoing.addView(itemLayout);
                        }
                    }
                });
    }

    private void confirmDeleteTask(String taskId, String title) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa task")
                .setMessage("Bạn có chắc muốn xóa \"" + title + "\" khỏi nhóm không?")
                .setPositiveButton("Xóa", (d, w) -> deleteGroupTask(taskId))
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showRenameGroupDialog() {
        if (!isAdmin) {
            Toast.makeText(this, "Chỉ quản lý nhóm mới có thể đổi tên nhóm", Toast.LENGTH_SHORT).show();
            return;
        }

        EditText input = new EditText(this);
        input.setText(getIntent().getStringExtra("groupName"));
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(this)
                .setTitle("Đổi tên nhóm")
                .setView(input)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) {
                        Toast.makeText(GroupDetailActivity.this, "Tên nhóm không được trống", Toast.LENGTH_SHORT).show();
                    } else {
                        updateGroupName(newName);
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void updateGroupName(String newName) {
        db.collection("Groups").document(groupId)
                .update("groupName", newName)
                .addOnSuccessListener(a -> {
                    Toast.makeText(this, "Đã cập nhật tên nhóm!", Toast.LENGTH_SHORT).show();
                    // Không cần cập nhật thủ công - realtime listener sẽ tự động cập nhật
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void deleteGroupTask(String taskId) {
        db.collection("Groups").document(groupId).collection("tasks").document(taskId)
                .delete()
                .addOnSuccessListener(a -> Toast.makeText(this, "Đã xóa task!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
