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

    // Data câu hỏi
    private int correctAnswerIndex = 1;
    
    // Data rương
    private String treasureId = "";
    private String treasureName = "Kho báu 01";
    private String treasureRarity = "Rare";
    private int treasureRewardPoints = 400;

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
            if (intent.hasExtra("EXTRA_TREASURE_ID")) treasureId = intent.getStringExtra("EXTRA_TREASURE_ID");
            if (intent.hasExtra("EXTRA_TREASURE_NAME")) treasureName = intent.getStringExtra("EXTRA_TREASURE_NAME");
            if (intent.hasExtra("EXTRA_TREASURE_RARITY")) treasureRarity = intent.getStringExtra("EXTRA_TREASURE_RARITY");
            if (intent.hasExtra("EXTRA_REWARD_POINTS")) treasureRewardPoints = intent.getIntExtra("EXTRA_REWARD_POINTS", 400);

            tvTreasureTitle.setText(treasureName);
            tvTreasureRarity.setText(treasureRarity);
            
            generateRandomQuestionByRarity(treasureRarity);
        }
    }

    private void generateRandomQuestionByRarity(String rarity) {
        // Sinh câu hỏi mẫu dựa theo độ hiếm của rương
        java.util.Random random = new java.util.Random();
        int r = random.nextInt(3);
        
        switch (rarity) {
            case "Common":
            case "Uncommon":
                // Câu hỏi dễ
                if (r == 0) {
                    tvQuestionText.setText("1 + 1 bằng mấy?");
                    rbOptionA.setText("A. 1");
                    rbOptionB.setText("B. 2");
                    rbOptionC.setText("C. 3");
                    rbOptionD.setText("D. 4");
                    correctAnswerIndex = 1; // Đáp án B
                } else if (r == 1) {
                    tvQuestionText.setText("Mặt trời mọc ở hướng nào?");
                    rbOptionA.setText("A. Hướng Đông");
                    rbOptionB.setText("B. Hướng Tây");
                    rbOptionC.setText("C. Hướng Nam");
                    rbOptionD.setText("D. Hướng Bắc");
                    correctAnswerIndex = 0; // Đáp án A
                } else {
                    tvQuestionText.setText("Thủ đô của Việt Nam là gì?");
                    rbOptionA.setText("A. Đà Nẵng");
                    rbOptionB.setText("B. Hà Nội");
                    rbOptionC.setText("C. Hồ Chí Minh");
                    rbOptionD.setText("D. Hải Phòng");
                    correctAnswerIndex = 1; // Đáp án B
                }
                break;
            case "Rare":
            case "Epic":
                // Câu hỏi trung bình - khó
                if (r == 0) {
                    tvQuestionText.setText("Trong bảng tuần hoàn, nguyên tố Oxi có ký hiệu là gì?");
                    rbOptionA.setText("A. Ox");
                    rbOptionB.setText("B. O");
                    rbOptionC.setText("C. O2");
                    rbOptionD.setText("D. Oxi");
                    correctAnswerIndex = 1; // Đáp án B
                } else if (r == 1) {
                    tvQuestionText.setText("Bức họa 'Mona Lisa' là tác phẩm của ai?");
                    rbOptionA.setText("A. Vincent van Gogh");
                    rbOptionB.setText("B. Pablo Picasso");
                    rbOptionC.setText("C. Leonardo da Vinci");
                    rbOptionD.setText("D. Michelangelo");
                    correctAnswerIndex = 2; // Đáp án C
                } else {
                    tvQuestionText.setText("Thuyết tương đối do ai đề xuất?");
                    rbOptionA.setText("A. Isaac Newton");
                    rbOptionB.setText("B. Nikola Tesla");
                    rbOptionC.setText("C. Albert Einstein");
                    rbOptionD.setText("D. Galileo Galilei");
                    correctAnswerIndex = 2; // Đáp án C
                }
                break;
            case "Legendary":
                // Câu hỏi rất khó
                if (r == 0) {
                    tvQuestionText.setText("Ngôn ngữ lập trình nào được thiết kế bởi Bjarne Stroustrup?");
                    rbOptionA.setText("A. Java");
                    rbOptionB.setText("B. Python");
                    rbOptionC.setText("C. C++");
                    rbOptionD.setText("D. Ruby");
                    correctAnswerIndex = 2; // Đáp án C
                } else if (r == 1) {
                    tvQuestionText.setText("Đỉnh núi cao nhất trong hệ mặt trời nằm ở hành tinh nào?");
                    rbOptionA.setText("A. Trái Đất");
                    rbOptionB.setText("B. Sao Hỏa");
                    rbOptionC.setText("C. Sao Mộc");
                    rbOptionD.setText("D. Sao Kim");
                    correctAnswerIndex = 1; // Đáp án B
                } else {
                    tvQuestionText.setText("Ai là người đầu tiên đặt chân lên mặt trăng?");
                    rbOptionA.setText("A. Yuri Gagarin");
                    rbOptionB.setText("B. Buzz Aldrin");
                    rbOptionC.setText("C. Neil Armstrong");
                    rbOptionD.setText("D. Michael Collins");
                    correctAnswerIndex = 2; // Đáp án C
                }
                break;
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
                tvQuestionNumber.setText(String.format(Locale.getDefault(), "CÂU HỎI THỬ THÁCH - THỜI GIAN: %02ds", secondsLeft));
            }

            @Override
            public void onFinish() {
                if (!isSubmitted) {
                    isSubmitted = true;
                    tvQuestionNumber.setText("CÂU HỎI - HẾT GIỜ!");
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
            intent.putExtra("EXTRA_TREASURE_ID", treasureId);
            intent.putExtra("EXTRA_TREASURE_NAME", treasureName);
            intent.putExtra("EXTRA_TREASURE_RARITY", treasureRarity);
            intent.putExtra("EXTRA_REWARD_POINTS", treasureRewardPoints);
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