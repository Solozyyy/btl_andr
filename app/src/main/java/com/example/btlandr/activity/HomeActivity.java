package com.example.btlandr.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.btlandr.R;
import com.example.btlandr.activity.LoginActivity;
import com.example.btlandr.activity.PersonalTaskActivity;
import com.example.btlandr.activity.GroupTaskActivity;
import com.example.btlandr.activity.ProfileActivity;
import com.example.btlandr.util.NetworkUtil;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {
    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        auth = FirebaseAuth.getInstance();

        Button btnPersonal = findViewById(R.id.btnPersonal);
        Button btnGroup = findViewById(R.id.btnGroup);
        Button btnProfile = findViewById(R.id.btnProfile);
        Button btnLogout = findViewById(R.id.btnLogout);

        if (NetworkUtil.isOnline(this)) {
            Toast.makeText(this, "Đang trực tuyến", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Đang ngoại tuyến — dữ liệu hiển thị từ bộ nhớ cục bộ", Toast.LENGTH_LONG).show();
        }

        // Chuyển sang màn task cá nhân
        btnPersonal.setOnClickListener(v -> {
            Intent i = new Intent(HomeActivity.this, PersonalTaskActivity.class);
            startActivity(i);
        });

        // Chuyển sang màn task nhóm
        btnGroup.setOnClickListener(v -> {
            Intent i = new Intent(HomeActivity.this, GroupTaskActivity.class);
            startActivity(i);
        });

        // Chuyển sang màn thông tin cá nhân
        btnProfile.setOnClickListener(v -> {
            Intent i = new Intent(HomeActivity.this, ProfileActivity.class);
            startActivity(i);
        });

        // Đăng xuất
        btnLogout.setOnClickListener(v -> {

            if (auth.getCurrentUser() != null) {
                FirebaseAuth.getInstance().signOut();
            }

            SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear(); // Xoá toàn bộ dữ liệu cục bộ
            editor.apply();

            Toast.makeText(this, "Đăng xuất thành công!", Toast.LENGTH_SHORT).show();

            Intent i = new Intent(HomeActivity.this, LoginActivity.class);
            startActivity(i);
            finish();
        });
    }
}
