package com.example.amp_g17;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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

        // Hien du lieu diem cua nguoi choi
        Intent intent = getIntent();
        if (intent != null) {
            String treasureName = intent.getStringExtra("EXTRA_TREASURE_NAME");
            int rewardPoints = intent.getIntExtra("EXTRA_REWARD_POINTS", 400);
            int totalPoints = intent.getIntExtra("EXTRA_TOTAL_POINTS", 1600);

            if (treasureName != null && !treasureName.isEmpty()) {
                tvTreasureName.setText(treasureName);
            } else {
                tvTreasureName.setText("Kho báu 01 - Rare");
            }

            tvRewardPoints.setText("+" + rewardPoints);
            tvTotalPoints.setText(String.format("%,d điểm", totalPoints));
        }

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Nút XEM BẢNG XẾP HẠNG
        btnLeaderboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: Chuyển sang LeaderboardActivity
                // Intent intent = new Intent(RewardActivity.this, LeaderboardActivity.class);
                // startActivity(intent);
            }
        });
    }
}
