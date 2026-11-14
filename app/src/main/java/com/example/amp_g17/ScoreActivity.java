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

import nl.dionsegijn.konfetti.xml.KonfettiView;
import nl.dionsegijn.konfetti.core.PartyFactory;
import nl.dionsegijn.konfetti.core.emitter.Emitter;
import nl.dionsegijn.konfetti.core.emitter.EmitterConfig;
import nl.dionsegijn.konfetti.core.models.Shape;
import nl.dionsegijn.konfetti.core.models.Size;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

public class ScoreActivity extends AppCompatActivity {
    private ImageView btnBack;
    private TextView tvChestStatus;
    private TextView tvTreasureName;
    private TextView tvRewardPoints;
    private TextView tvTotalPoints;
    private Button btnContinue;
    private Button btnLeaderboard;
    private KonfettiView konfettiView;

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
        konfettiView = findViewById(R.id.konfettiView);

        String treasureId = "";
        int rewardPoints = 400;

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("EXTRA_TREASURE_ID")) {
                treasureId = intent.getStringExtra("EXTRA_TREASURE_ID");
            }
            
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
        updateRewardToFirebase(treasureId, rewardPoints);

        btnBack.setOnClickListener(v -> finish());
        btnContinue.setOnClickListener(v -> finish());

        btnLeaderboard.setOnClickListener(v -> {
            Intent rankIntent = new Intent(ScoreActivity.this, RankActivity.class);
            startActivity(rankIntent);
        });
    }

    private void updateRewardToFirebase(String treasureId, int rewardPoints) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            tvTotalPoints.setText("Chưa đăng nhập");
            return;
        }

        String uid = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        tvTotalPoints.setText("Đang xử lý...");

        // Kiểm tra xem kho báu này đã được mở chưa (BR-14)
        if (treasureId != null && !treasureId.isEmpty()) {
            db.collection("completions")
                    .whereEqualTo("userId", uid)
                    .whereEqualTo("treasureId", treasureId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                            // Đã tồn tại bản ghi hoàn thành => Chống nhận thưởng lặp lại
                            Toast.makeText(this, "Bạn đã từng nhận thưởng từ kho báu này!", Toast.LENGTH_LONG).show();
                            
                            // Chỉ lấy điểm hiện tại ra hiển thị chứ không cộng thêm
                            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                                long currentTotal = (doc != null && doc.getLong("totalScore") != null) ? doc.getLong("totalScore") : 0;
                                tvTotalPoints.setText(String.format(Locale.getDefault(), "%,d điểm", currentTotal));
                                tvRewardPoints.setText("+0 (Đã mở)");
                            });
                        } else {
                            // Chưa mở => Tiến hành cộng điểm
                            processRewardAddition(db, uid, treasureId, rewardPoints);
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi kiểm tra lịch sử: " + e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // Không có ID kho báu => Vẫn cộng điểm (chế độ demo/fallback)
            processRewardAddition(db, uid, null, rewardPoints);
        }
    }

    private void processRewardAddition(FirebaseFirestore db, String uid, String treasureId, int rewardPoints) {
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

            // 2. Tính Tổng điểm mới
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
                        // 4. Lưu lại lịch sử vào collection completions để đánh dấu đã nhận
                        if (treasureId != null) {
                            Map<String, Object> completionRecord = new HashMap<>();
                            completionRecord.put("userId", uid);
                            completionRecord.put("treasureId", treasureId);
                            completionRecord.put("rewardPoints", rewardPoints);
                            completionRecord.put("completedAt", Timestamp.now());
                            db.collection("completions").add(completionRecord);
                        }

                        // Hiển thị tổng điểm mới nhất
                        tvTotalPoints.setText(String.format(Locale.getDefault(), "%,d điểm", newTotal));
                        Toast.makeText(ScoreActivity.this, "Đã cộng +" + rewardPoints + " điểm vào tài khoản!", Toast.LENGTH_SHORT).show();
                        shootFireworks();
                    })
                    .addOnFailureListener(e -> {
                        android.util.Log.e("ScoreActivity", "Lỗi lưu điểm Firebase: ", e);
                        tvTotalPoints.setText(String.format(Locale.getDefault(), "%,d điểm (Lỗi lưu)", newTotal));
                    });
        }).addOnFailureListener(e -> {
            tvTotalPoints.setText("Lỗi kết nối!");
        });
    }

    private void shootFireworks() {
        EmitterConfig emitterConfig = new Emitter(100L, TimeUnit.MILLISECONDS).max(100);
        konfettiView.start(
                new PartyFactory(emitterConfig)
                        .spread(360)
                        .colors(Arrays.asList(0xfce18a, 0xff726d, 0xf4306d, 0xb48def))
                        .setSpeedBetween(0f, 30f)
                        .position(new nl.dionsegijn.konfetti.core.Position.Relative(0.5, 0.3))
                        .build()
        );
    }
}
