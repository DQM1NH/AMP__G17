package com.example.amp_g17;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class StartActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);

        btnLogin.setOnClickListener(v ->
                Toast.makeText(this, "Bạn đã chọn Đăng nhập", Toast.LENGTH_SHORT).show());

        btnRegister.setOnClickListener(v ->
                Toast.makeText(this, "Bạn đã chọn Đăng ký", Toast.LENGTH_SHORT).show());
    }
}
