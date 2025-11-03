package com.example.btlandr;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        TextView tv = findViewById(R.id.tvWelcome);
        tv.setText("Chào mừng bạn đến trang chủ!");
    }
}
