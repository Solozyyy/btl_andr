package com.example.btlandr;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private TextView tvUsername, tvEmail, tvCreatedAt, tvPersonalEventsCount, tvManagedGroupsCount,
            tvInvitedGroupsCount;
    private Button btnChangeUsername, btnChangePassword, btnDeleteAccount, btnBack;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseUser currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(this, "Chưa đăng nhập!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        uid = currentUser.getUid();

        // Ánh xạ views
        tvUsername = findViewById(R.id.tvUsername);
        tvEmail = findViewById(R.id.tvEmail);
        tvCreatedAt = findViewById(R.id.tvCreatedAt);
        tvPersonalEventsCount = findViewById(R.id.tvPersonalEventsCount);
        tvManagedGroupsCount = findViewById(R.id.tvManagedGroupsCount);
        tvInvitedGroupsCount = findViewById(R.id.tvInvitedGroupsCount);

        btnChangeUsername = findViewById(R.id.btnChangeUsername);
        btnChangePassword = findViewById(R.id.btnChangePassword);
        btnDeleteAccount = findViewById(R.id.btnDeleteAccount);
        btnBack = findViewById(R.id.btnBack);

        // Load thông tin user
        loadUserInfo();
        loadStatistics();

        // Nút quay lại
        btnBack.setOnClickListener(v -> finish());

        // Nút đổi tên
        btnChangeUsername.setOnClickListener(v -> showChangeUsernameDialog());

        // Nút đổi mật khẩu
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());

        // Nút xóa tài khoản
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void loadUserInfo() {
        db.collection("UserAccount").document(uid)
                .addSnapshotListener((doc, e) -> {
                    if (e != null || doc == null || !doc.exists()) {
                        Toast.makeText(this, "Lỗi tải thông tin!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String username = doc.getString("username");
                    String email = doc.getString("email");
                    Long createdAt = doc.getLong("createdAt");

                    tvUsername.setText(username != null ? username : "Chưa có tên");
                    tvEmail.setText(email != null ? email : "Chưa có email");

                    if (createdAt != null) {
                        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
                        tvCreatedAt.setText(sdf.format(new Date(createdAt)));
                    } else {
                        tvCreatedAt.setText("Không rõ");
                    }
                });
    }

    private void loadStatistics() {
        // Đếm số sự kiện cá nhân
        db.collection("UserAccount").document(uid).collection("events")
                .addSnapshotListener((snapshots, e) -> {
                    if (e == null && snapshots != null) {
                        tvPersonalEventsCount.setText(String.valueOf(snapshots.size()));
                    }
                });

        // Đếm số nhóm quản lý
        db.collection("Groups")
                .whereEqualTo("adminId", uid)
                .addSnapshotListener((snapshots, e) -> {
                    if (e == null && snapshots != null) {
                        tvManagedGroupsCount.setText(String.valueOf(snapshots.size()));
                    }
                });

        // Đếm số nhóm được mời
        db.collection("Groups")
                .whereArrayContains("members", uid)
                .addSnapshotListener((snapshots, e) -> {
                    if (e == null && snapshots != null) {
                        int invitedCount = 0;
                        for (QueryDocumentSnapshot doc : snapshots) {
                            String adminId = doc.getString("adminId");
                            if (!uid.equals(adminId)) {
                                invitedCount++;
                            }
                        }
                        tvInvitedGroupsCount.setText(String.valueOf(invitedCount));
                    }
                });
    }

    private void showChangeUsernameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Đổi tên người dùng");

        final EditText input = new EditText(this);
        input.setHint("Nhập tên mới");
        input.setText(tvUsername.getText().toString());
        input.setPadding(48, 32, 48, 32);
        builder.setView(input);

        builder.setPositiveButton("Lưu", (dialog, which) -> {
            String newUsername = input.getText().toString().trim();
            if (newUsername.isEmpty()) {
                Toast.makeText(this, "Tên không được để trống!", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("username", newUsername);

            db.collection("UserAccount").document(uid)
                    .update(updates)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Đã đổi tên thành công!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showChangePasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Đổi mật khẩu");

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_change_password, null);
        EditText etOldPassword = dialogView.findViewById(R.id.etOldPassword);
        EditText etNewPassword = dialogView.findViewById(R.id.etNewPassword);
        EditText etConfirmPassword = dialogView.findViewById(R.id.etConfirmPassword);

        builder.setView(dialogView);

        builder.setPositiveButton("Đổi mật khẩu", (dialog, which) -> {
            String oldPass = etOldPassword.getText().toString().trim();
            String newPass = etNewPassword.getText().toString().trim();
            String confirmPass = etConfirmPassword.getText().toString().trim();

            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (newPass.length() < 6) {
                Toast.makeText(this, "Mật khẩu mới phải có ít nhất 6 ký tự!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newPass.equals(confirmPass)) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show();
                return;
            }

            changePassword(oldPass, newPass);
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void changePassword(String oldPassword, String newPassword) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Xác thực lại với mật khẩu cũ
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), oldPassword);

        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Đổi mật khẩu mới
                    user.updatePassword(newPassword)
                            .addOnSuccessListener(aVoid1 -> {
                                Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Lỗi đổi mật khẩu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Mật khẩu cũ không đúng!", Toast.LENGTH_SHORT).show();
                });
    }

    private void showDeleteAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚠️ Xóa tài khoản");
        builder.setMessage("Bạn có chắc chắn muốn xóa tài khoản?\n\n" +
                "• Tất cả sự kiện cá nhân sẽ bị xóa\n" +
                "• Các nhóm bạn quản lý sẽ bị xóa\n" +
                "• Bạn sẽ bị xóa khỏi tất cả nhóm\n\n" +
                "Hành động này KHÔNG THỂ hoàn tác!");

        builder.setPositiveButton("Xóa tài khoản", (dialog, which) -> {
            showConfirmPasswordDialog();
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showConfirmPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Xác nhận mật khẩu");

        final EditText input = new EditText(this);
        input.setHint("Nhập mật khẩu để xác nhận");
        input.setInputType(
                android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        input.setPadding(48, 32, 48, 32);
        builder.setView(input);

        builder.setPositiveButton("Xác nhận", (dialog, which) -> {
            String password = input.getText().toString().trim();
            if (password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập mật khẩu!", Toast.LENGTH_SHORT).show();
                return;
            }
            deleteAccount(password);
        });

        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void deleteAccount(String password) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            Toast.makeText(this, "Không tìm thấy thông tin người dùng!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Xác thực lại
        AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), password);

        user.reauthenticate(credential)
                .addOnSuccessListener(aVoid -> {
                    // Xóa dữ liệu Firestore trước
                    deleteUserData(() -> {
                        // Sau đó xóa tài khoản Firebase Auth
                        user.delete()
                                .addOnSuccessListener(aVoid1 -> {
                                    Toast.makeText(this, "Tài khoản đã bị xóa!", Toast.LENGTH_SHORT).show();
                                    // Xóa SharedPreferences
                                    getSharedPreferences("UserPrefs", MODE_PRIVATE).edit().clear().apply();
                                    // Về màn login
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(this, "Lỗi xóa tài khoản: " + e.getMessage(), Toast.LENGTH_SHORT)
                                            .show();
                                });
                    });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Mật khẩu không đúng!", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteUserData(Runnable onComplete) {
        // Xóa thông tin user
        db.collection("UserAccount").document(uid)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    // Xóa các nhóm do user quản lý
                    db.collection("Groups")
                            .whereEqualTo("adminId", uid)
                            .get()
                            .addOnSuccessListener(snapshots -> {
                                for (QueryDocumentSnapshot doc : snapshots) {
                                    doc.getReference().delete();
                                }

                                // Xóa user khỏi tất cả nhóm khác
                                db.collection("Groups")
                                        .whereArrayContains("members", uid)
                                        .get()
                                        .addOnSuccessListener(groupSnapshots -> {
                                            for (QueryDocumentSnapshot groupDoc : groupSnapshots) {
                                                groupDoc.getReference()
                                                        .update("members", com.google.firebase.firestore.FieldValue
                                                                .arrayRemove(uid));
                                            }
                                            onComplete.run();
                                        });
                            });
                });
    }
}
