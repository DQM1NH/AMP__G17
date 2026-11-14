package com.example.amp_g17;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Locale;

public class ProfileActivity extends AppCompatActivity {
    private ImageView btnBackProfile;
    private TextView tvProfileName, tvProfileEmail, tvProfileScore, tvProfileChests;
    private Button btnEditProfile, btnLogout;
    private ListenerRegistration userListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        btnBackProfile = findViewById(R.id.btnBackProfile);
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileEmail = findViewById(R.id.tvProfileEmail);
        tvProfileScore = findViewById(R.id.tvProfileScore);
        tvProfileChests = findViewById(R.id.tvProfileChests);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnLogout = findViewById(R.id.btnLogout);

        btnBackProfile.setOnClickListener(v -> finish());
        btnLogout.setOnClickListener(v -> showLogoutDialog());
        btnEditProfile.setOnClickListener(v -> showEditNameDialog());

        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }

        tvProfileEmail.setText("Email: " + (currentUser.getEmail() != null ? currentUser.getEmail() : "N/A"));

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        userListener = db.collection("users").document(currentUser.getUid()).addSnapshotListener((doc, error) -> {
            if (error != null || doc == null || !doc.exists()) return;
            
            String name = doc.getString("displayName");
            Long scoreObj = doc.getLong("totalScore");
            Long chestObj = doc.getLong("completedTreasureCount");

            long score = scoreObj != null ? scoreObj : 0;
            long chests = chestObj != null ? chestObj : 0;

            tvProfileName.setText(name != null && !name.isEmpty() ? name : "Người chơi");
            tvProfileScore.setText(String.format(Locale.getDefault(), "Tổng điểm: %,d điểm", score));
            tvProfileChests.setText("Kho báu đã mở: " + chests);
        });
    }

    private void showEditNameDialog() {
        EditText input = new EditText(this);
        input.setHint("Nhập tên hiển thị mới");

        new AlertDialog.Builder(this)
                .setTitle("Đổi tên")
                .setView(input)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String newName = input.getText().toString().trim();
                    if (!newName.isEmpty()) {
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid())
                                    .update("displayName", newName)
                                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Đã cập nhật tên!", Toast.LENGTH_SHORT).show())
                                    .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                        }
                    }
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất không?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    FirebaseAuth.getInstance().signOut();
                    Intent intent = new Intent(ProfileActivity.this, StartActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) {
            userListener.remove();
        }
    }
}
