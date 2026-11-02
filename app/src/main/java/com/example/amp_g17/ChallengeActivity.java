package com.example.amp_g17;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Locale;

public class ChallengeActivity extends AppCompatActivity {

    private ImageView btnBack;
    private TextView tvTreasureTitle, tvTreasureRarity, tvQuestionNumber, tvQuestionText;
    private RadioGroup rgAnswers;
    private RadioButton rbOptionA, rbOptionB, rbOptionC, rbOptionD;
    private Button btnSubmit;

    // CountDownTimer 30s
    private CountDownTimer countDownTimer;
    private static final long TIMER_DURATION_MS = 30000;
    private boolean isSubmitted = false;

    // Đáp án đúng mẫu (Index 1 = B. Hà Nội)
    private final int correctAnswerIndex = 1;

    private String treasureTitle = "Kho báu 01";
    private String treasureRarity = "Rare";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);

        initViews();
        displayChallengeData();
        setupListeners();

        // Khởi chạy đếm ngược 30 giây
        startTimer();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTreasureTitle = findViewById(R.id.tvTreasureTitle);
        tvTreasureRarity = findViewById(R.id.tvTreasureRarity);
        tvQuestionNumber = findViewById(R.id.tvQuestionNumber);
        tvQuestionText = findViewById(R.id.tvQuestionText);
        rgAnswers = findViewById(R.id.rgAnswers);
        rbOptionA = findViewById(R.id.rbOptionA);
        rbOptionB = findViewById(R.id.rbOptionB);
        rbOptionC = findViewById(R.id.rbOptionC);
        rbOptionD = findViewById(R.id.rbOptionD);
        btnSubmit = findViewById(R.id.btnSubmit);
    }

    private void displayChallengeData() {
        Intent intent = getIntent();
        if (intent != null) {
            String title = intent.getStringExtra("EXTRA_TREASURE_TITLE");
            String rarity = intent.getStringExtra("EXTRA_TREASURE_RARITY");

            if (title != null && !title.isEmpty()) treasureTitle = title;
            if (rarity != null && !rarity.isEmpty()) treasureRarity = rarity;

            tvTreasureTitle.setText(treasureTitle);
            tvTreasureRarity.setText(treasureRarity);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnSubmit.setOnClickListener(v -> {
            if (!isSubmitted) {
                checkAnswerAndSubmit();
            }
        });
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(TIMER_DURATION_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000);
                tvQuestionNumber.setText(String.format(Locale.getDefault(), "CÂU HỎI 01 - THỜI GIAN: %02ds", secondsLeft));
            }

            @Override
            public void onFinish() {
                if (!isSubmitted) {
                    isSubmitted = true;
                    tvQuestionNumber.setText("CÂU HỎI 01 - HẾT GIỜ!");
                    disableAnswerOptions();
                    Toast.makeText(ChallengeActivity.this, "Đã hết 30 giây! Bạn đã thất bại ở thử thách này.", Toast.LENGTH_LONG).show();
                }
            }
        }.start();
    }

    private void checkAnswerAndSubmit() {
        int selectedId = rgAnswers.getCheckedRadioButtonId();

        if (selectedId == -1) {
            Toast.makeText(this, "Vui lòng chọn một đáp án!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isSubmitted = true;

        int selectedIndex = -1;
        if (selectedId == R.id.rbOptionA) selectedIndex = 0;
        else if (selectedId == R.id.rbOptionB) selectedIndex = 1;
        else if (selectedId == R.id.rbOptionC) selectedIndex = 2;
        else if (selectedId == R.id.rbOptionD) selectedIndex = 3;

        // XỬ LÝ TRẢ LỜI ĐÚNG -> CHUYỂN SANG OpenChestActivity
        if (selectedIndex == correctAnswerIndex) {
            Toast.makeText(this, "Chính xác! Chuẩn bị lắc mở rương...", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(ChallengeActivity.this, OpenChestActivity.class);
            intent.putExtra("EXTRA_TREASURE_TITLE", treasureTitle);
            intent.putExtra("EXTRA_TREASURE_RARITY", treasureRarity);
            startActivity(intent);
            finish(); // Đóng ChallengeActivity
        } else {
            Toast.makeText(this, "Đáp án chưa đúng. Bạn đã bỏ lỡ kho báu!", Toast.LENGTH_SHORT).show();
            disableAnswerOptions();
        }
    }

    private void disableAnswerOptions() {
        btnSubmit.setEnabled(false);
        for (int i = 0; i < rgAnswers.getChildCount(); i++) {
            rgAnswers.getChildAt(i).setEnabled(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}