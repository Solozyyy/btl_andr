package com.example.btlandr;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;
    TextView tvRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvRegister = findViewById(R.id.tvRegister);

        // Nút Login
        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            // Giả lập: tài khoản mặc định để test
            if (email.equals("admin") && pass.equals("123")) {
                Toast.makeText(this, "Login thành công!", Toast.LENGTH_SHORT).show();

                // Chuyển sang trang chủ
                Intent i = new Intent(LoginActivity.this, HomeActivity.class);
                startActivity(i);
                finish();
            } else {
                Toast.makeText(this, "Sai email hoặc mật khẩu!", Toast.LENGTH_SHORT).show();
            }
        });

        // Chuyển sang màn hình đăng ký
        tvRegister.setOnClickListener(v -> {
            Intent i = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(i);
        });
    }
}
