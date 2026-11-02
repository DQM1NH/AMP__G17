package com.example.amp_g17;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ScoreActivity extends AppCompatActivity {
    private ImageView btnBack;
    private TextView tvChestStatus;
    private TextView tvTreasureName;
    private TextView tvRewardPoints;
    private TextView tvTotalPoints;
    private Button btnContinue;
    private Button btnLeaderboard;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_score);

        btnBack = findViewById(R.id.btnBack);
        tvChestStatus = findViewById(R.id.tvChestStatus);
        tvTreasureName = findViewById(R.id.tvTreasureName);
        tvRewardPoints = findViewById(R.id.tvRewardPoints);
        tvTotalPoints = findViewById(R.id.tvTotalPoints);
        btnContinue = findViewById(R.id.btnContinue);
        btnLeaderboard = findViewById(R.id.btnLeaderboard);

        int rewardPoints = 400;

        Intent intent = getIntent();
        if (intent != null) {
            String treasureName = intent.getStringExtra("EXTRA_TREASURE_NAME");
            rewardPoints = intent.getIntExtra("EXTRA_REWARD_POINTS", 400);

            if (treasureName != null && !treasureName.isEmpty()) {
                tvTreasureName.setText(treasureName);
            } else {
                tvTreasureName.setText("Kho báu 01 - Rare");
            }

            tvRewardPoints.setText("+" + rewardPoints);
        }

        // Cập nhật điểm lên Firebase và hiển thị tổng điểm mới
        updateRewardToFirebase(rewardPoints);

        btnBack.setOnClickListener(v -> finish());
        btnContinue.setOnClickListener(v -> finish());

        btnLeaderboard.setOnClickListener(v -> {
            Intent rankIntent = new Intent(ScoreActivity.this, RankActivity.class);
            startActivity(rankIntent);
        });
    }

    private void updateRewardToFirebase(int rewardPoints) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            tvTotalPoints.setText("Chưa đăng nhập");
            return;
        }

        String uid = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        tvTotalPoints.setText("Đang tính điểm...");

        // 1. Đọc số điểm hiện tại từ Firebase
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            long currentTotal = 0;
            long currentChests = 0;

            if (doc != null && doc.exists()) {
                Long totalObj = doc.getLong("totalScore");
                Long chestObj = doc.getLong("completedTreasureCount");

                if (totalObj != null) currentTotal = totalObj;
                if (chestObj != null) currentChests = chestObj;
            }

            // 2. Tính Tổng điểm mới = Điểm hiện tại trên Firebase + Điểm nhận được
            long newTotal = currentTotal + rewardPoints;
            long newChests = currentChests + 1;

            // 3. Lưu tổng điểm mới lên Firebase
            Map<String, Object> updates = new HashMap<>();
            updates.put("totalScore", newTotal);
            updates.put("completedTreasureCount", newChests);
            updates.put("updatedAt", Timestamp.now());
            updates.put("lastActiveAt", Timestamp.now());

            db.collection("users").document(uid)
                    .set(updates, com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        // Hiển thị tổng điểm mới nhất
                        tvTotalPoints.setText(String.format(Locale.getDefault(), "%,d điểm", newTotal));
                        Toast.makeText(ScoreActivity.this, "Đã cộng +" + rewardPoints + " điểm vào tài khoản!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("ScoreActivity", "Lỗi lưu điểm Firebase: ", e);
                        tvTotalPoints.setText(String.format(Locale.getDefault(), "%,d điểm (Lỗi lưu)", newTotal));
                        Toast.makeText(ScoreActivity.this, "Lỗi Firebase Rules: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                    });
        }).addOnFailureListener(e -> {
            android.util.Log.e("ScoreActivity", "Lỗi đọc điểm Firebase: ", e);
            tvTotalPoints.setText("Lỗi kết nối!");
            Toast.makeText(ScoreActivity.this, "Lỗi Firebase: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        });
    }
}
