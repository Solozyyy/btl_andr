package com.example.btlandr;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        Button btnPersonal = findViewById(R.id.btnPersonal);
        Button btnGroup = findViewById(R.id.btnGroup);

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
    }
}
