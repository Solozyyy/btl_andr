package com.example.btlandr;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home); // dùng lại layout home.xml

        Button btnPersonal = findViewById(R.id.btnPersonal);
        Button btnGroup = findViewById(R.id.btnGroup);

        // Chuyển sang màn task cá nhân
        btnPersonal.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, PersonalTaskActivity.class);
            startActivity(i);
        });

        // Chuyển sang màn task nhóm
        btnGroup.setOnClickListener(v -> {
            Intent i = new Intent(MainActivity.this, GroupTaskActivity.class);
            startActivity(i);
        });
    }
}
