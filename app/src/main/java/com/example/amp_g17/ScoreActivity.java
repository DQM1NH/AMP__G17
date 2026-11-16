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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
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

    private String treasureId;
    private String challengeId;

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
            treasureId = intent.getStringExtra("EXTRA_TREASURE_ID");
            challengeId = intent.getStringExtra("EXTRA_CHALLENGE_ID");
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
        DocumentReference userRef = db.collection("users").document(uid);
        DocumentReference completionRef = (treasureId != null && !treasureId.isEmpty())
                ? db.collection("completions").document(uid + "_" + treasureId)
                : null;

        tvTotalPoints.setText("Đang tính điểm...");

        db.runTransaction(transaction -> {
            boolean alreadyCompleted = completionRef != null && transaction.get(completionRef).exists();
            DocumentSnapshot userDoc = transaction.get(userRef);

            long currentTotal = 0;
            long currentChests = 0;
            if (userDoc.exists()) {
                Long totalObj = userDoc.getLong("totalScore");
                Long chestObj = userDoc.getLong("completedTreasureCount");
                if (totalObj != null) currentTotal = totalObj;
                if (chestObj != null) currentChests = chestObj;
            }

            if (alreadyCompleted) {
                return new long[]{0, currentTotal}; // đã nhận rồi, không cộng lại
            }

            long newTotal = currentTotal + rewardPoints;

            if (completionRef != null) {
                Map<String, Object> completion = new HashMap<>();
                completion.put("userId", uid);
                completion.put("treasureId", treasureId);
                completion.put("challengeId", challengeId);
                completion.put("rewardPoints", rewardPoints);
                completion.put("status", "COMPLETED");
                completion.put("completedAt", Timestamp.now());
                transaction.set(completionRef, completion);
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("totalScore", newTotal);
            updates.put("completedTreasureCount", currentChests + 1);
            updates.put("updatedAt", Timestamp.now());
            updates.put("lastActiveAt", Timestamp.now());
            transaction.set(userRef, updates, com.google.firebase.firestore.SetOptions.merge());

            return new long[]{1, newTotal};
        }).addOnSuccessListener(result -> {
            boolean awarded = result[0] == 1;
            long total = result[1];
            tvTotalPoints.setText(String.format(Locale.getDefault(), "%,d điểm", total));
            if (awarded) {
                Toast.makeText(ScoreActivity.this, "Đã cộng +" + rewardPoints + " điểm vào tài khoản!", Toast.LENGTH_SHORT).show();
            } else {
                tvRewardPoints.setText("+0");
                Toast.makeText(ScoreActivity.this, "Kho báu này đã được mở trước đó, không cộng thêm điểm.", Toast.LENGTH_LONG).show();
            }
        }).addOnFailureListener(e -> {
            android.util.Log.e("ScoreActivity", "Lỗi lưu kết quả kho báu: ", e);
            tvTotalPoints.setText("Lỗi lưu điểm!");
            Toast.makeText(ScoreActivity.this, "Lỗi Firebase: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
        });
    }
}