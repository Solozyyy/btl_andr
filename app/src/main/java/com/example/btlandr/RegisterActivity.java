package com.example.btlandr;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class RegisterActivity extends AppCompatActivity {

    EditText etEmail, etPass, etConfirm;
    Button btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        etEmail = findViewById(R.id.etEmail);
        etPass = findViewById(R.id.etPassword);
        etConfirm = findViewById(R.id.etConfirm);
        btnRegister = findViewById(R.id.btnRegister);
        TextView tvLogin = findViewById(R.id.tvLogin);

        btnRegister.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String pass = etPass.getText().toString().trim();
            String confirm = etConfirm.getText().toString().trim();

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ!", Toast.LENGTH_SHORT).show();
            } else if (!pass.equals(confirm)) {
                Toast.makeText(this, "Mật khẩu không trùng khớp!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();

            }
        });

        tvLogin.setOnClickListener(v -> {
            // Chuyển về màn Login
            Intent i = new Intent(RegisterActivity.this, LoginActivity.class);
            startActivity(i);
            finish();
        });
    }
}
