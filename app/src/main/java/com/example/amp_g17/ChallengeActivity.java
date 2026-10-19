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

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class ChallengeActivity extends AppCompatActivity {
    private ImageView btnBack;
    private TextView tvTreasureTitle;
    private TextView tvTreasureRarity;
    private TextView tvQuestionNumber;
    private TextView tvQuestionText;
    private RadioGroup rgAnswers;
    private RadioButton rbOptionA;
    private RadioButton rbOptionB;
    private RadioButton rbOptionC;
    private RadioButton rbOptionD;
    private Button btnSubmit;

    // Các biến xử lý CountDownTimer
    private CountDownTimer countDownTimer;
    private static final long TIMER_DURATION_MS = 30000; // thoi gian moi cau hoi: 30 giay
    private boolean isSubmitted = false;

    // Chỉ số đáp án đúng
    private final int correctAnswerIndex = 1; // VD: o day 1 tuong trung co B la dap an dung
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_challenge);

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

        // Nhận dữ liệu truyền từ Intent (nếu có)
        Intent intent = getIntent();
        if (intent != null) {
            String title = intent.getStringExtra("EXTRA_TREASURE_TITLE");
            String rarity = intent.getStringExtra("EXTRA_TREASURE_RARITY");

            if (title != null && !title.isEmpty()) {
                tvTreasureTitle.setText(title);
            }
            if (rarity != null && !rarity.isEmpty()) {
                tvTreasureRarity.setText(rarity);
            }
        }

        // Nút Quay lại
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Nút Gửi câu trả lời
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkAnswerAndSubmit();
            }
        });
        // Bat dau dem nguoc
        startTimer();
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(TIMER_DURATION_MS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsLeft = (int) (millisUntilFinished / 1000);
                // Cập nhật giao diện đếm ngược (Ví dụ: hiển thị lên tvQuestionNumber)
                tvQuestionNumber.setText(String.format(Locale.getDefault(), "CÂU HỎI 01 - THỜI GIAN: ", secondsLeft));
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

        // Kiểm tra xem người chơi đã chọn đáp án chưa
        if (selectedId == -1) {
            Toast.makeText(this, "Vui lòng chọn một đáp án!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Xác định đáp án được chọn
        int selectedIndex = -1;
        if (selectedId == R.id.rbOptionA) {
            selectedIndex = 0;
        } else if (selectedId == R.id.rbOptionB) {
            selectedIndex = 1;
        } else if (selectedId == R.id.rbOptionC) {
            selectedIndex = 2;
        } else if (selectedId == R.id.rbOptionD) {
            selectedIndex = 3;
        }

        // Kiểm tra đáp án
        if (selectedIndex == correctAnswerIndex) {
            Toast.makeText(this, "Chính xác! Bạn nhận được kho báu.", Toast.LENGTH_SHORT).show();

            // Chuyển sang màn hình nhận thưởng
            Intent intent = new Intent(ChallengeActivity.this, ScoreActivity.class);
            intent.putExtra("EXTRA_TREASURE_NAME", tvTreasureTitle.getText().toString() + " - " + tvTreasureRarity.getText().toString());
            intent.putExtra("EXTRA_REWARD_POINTS", 400);
            intent.putExtra("EXTRA_TOTAL_POINTS", 1600);
            startActivity(intent);
            finish(); // Đóng Activity thử thách
        } else {
            Toast.makeText(this, "Đáp án chưa đúng. Hãy thử lại!", Toast.LENGTH_SHORT).show();
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
        // Hủy timer để tránh rò rỉ bộ nhớ (memory leak) khi thoát Activity
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
