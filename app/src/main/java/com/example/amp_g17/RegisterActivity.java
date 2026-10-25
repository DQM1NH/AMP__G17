package com.example.amp_g17;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {
    private ImageView btnBack;
    private EditText etPlayerName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private Button btnRegister;
    private TextView tvLogin;

    // Khai báo Firebase Authentication và Firestore
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Khởi tạo các instance của Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        btnBack = findViewById(R.id.btnBack);
        etPlayerName = findViewById(R.id.etPlayerName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myStart = new Intent(RegisterActivity.this, StartActivity.class);
                startActivity(myStart);
                finish();
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRegister();
            }
        });

        tvLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myLogin = new Intent(RegisterActivity.this, LoginActivity.class);
                startActivity(myLogin);
                finish();
            }
        });
    }

    private void handleRegister() {
        String playerName = etPlayerName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (TextUtils.isEmpty(playerName)) {
            etPlayerName.setError("Vui lòng nhập tên người chơi");
            etPlayerName.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Vui lòng nhập email");
            etEmail.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email không đúng định dạng");
            etEmail.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Vui lòng nhập mật khẩu");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Mật khẩu phải chứa ít nhất 6 ký tự");
            etPassword.requestFocus();
            return;
        }

        if (TextUtils.isEmpty(confirmPassword)) {
            etConfirmPassword.setError("Vui lòng xác nhận lại mật khẩu");
            etConfirmPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Mật khẩu xác nhận không khớp");
            etConfirmPassword.requestFocus();
            return;
        }

        // Vô hiệu hóa nút đăng ký để tránh bấm nhiều lần
        btnRegister.setEnabled(false);

        // Gọi hàm đăng ký với Firebase
        performRegister(playerName, email, password);
    }

    private void performRegister(final String playerName, final String email, String password) {
        // 1. Tạo tài khoản trong Firebase Authentication
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            String userId = user.getUid();

                            // 2. Chuẩn bị dữ liệu khớp CHÍNH XÁC với Firestore Security Rules
                            com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();

                            Map<String, Object> userData = new HashMap<>();
                            userData.put("displayName", playerName);
                            userData.put("avatarUrl", ""); // Mặc định chuỗi rỗng
                            userData.put("totalScore", 0);
                            userData.put("completedTreasureCount", 0);
                            userData.put("experiencePoints", 0);
                            userData.put("totalChallengesJoined", 0);
                            userData.put("createdAt", now);
                            userData.put("updatedAt", now);
                            userData.put("lastActiveAt", now);

                            // 3. Lưu vào collection "users"
                            db.collection("users").document(userId)
                                    .set(userData)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(RegisterActivity.this,
                                                "Đăng ký thành công! Chào mừng " + playerName,
                                                Toast.LENGTH_SHORT).show();

                                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                        startActivity(intent);
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        btnRegister.setEnabled(true);
                                        android.util.Log.e("FirestoreError", "Lỗi lưu dữ liệu: ", e);
                                        Toast.makeText(RegisterActivity.this,
                                                "Lỗi lưu dữ liệu: " + e.getLocalizedMessage(),
                                                Toast.LENGTH_LONG).show();
                                    });
                        }
                    } else {
                        btnRegister.setEnabled(true);
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Đăng ký thất bại";
                        Toast.makeText(RegisterActivity.this, "Lỗi đăng ký: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}