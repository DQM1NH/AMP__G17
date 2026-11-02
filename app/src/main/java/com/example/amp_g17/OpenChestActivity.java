package com.example.amp_g17;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class OpenChestActivity extends AppCompatActivity implements SensorEventListener {

    private ImageView btnBack;
    private ImageView imgChest;
    private ImageView imgShakeIcon;
    private TextView tvShakeStatus;

    // Cảm biến gia tốc (Accelerometer)
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float accelerationCurrent;
    private float accelerationLast;
    private static final float SHAKE_THRESHOLD = 12.0f; // Ngưỡng độ nhạy lực lắc
    private boolean isChestOpened = false;

    // Dịch vụ rung
    private Vibrator vibrator;

    private String treasureTitle = "Kho báu 01";
    private String treasureRarity = "Rare";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_chest);

        initViews();
        getIntentData();
        setupSensor();
        initVibrator();
        setupListeners();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        imgChest = findViewById(R.id.imgChest);
        imgShakeIcon = findViewById(R.id.imgShakeIcon);
        tvShakeStatus = findViewById(R.id.tvShakeStatus);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("EXTRA_TREASURE_TITLE")) {
                treasureTitle = intent.getStringExtra("EXTRA_TREASURE_TITLE");
            }
            if (intent.hasExtra("EXTRA_TREASURE_RARITY")) {
                treasureRarity = intent.getStringExtra("EXTRA_TREASURE_RARITY");
            }
        }
    }

    private void setupSensor() {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }

        accelerationCurrent = SensorManager.GRAVITY_EARTH;
        accelerationLast = SensorManager.GRAVITY_EARTH;
    }

    private void initVibrator() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vibratorManager != null) {
                vibrator = vibratorManager.getDefaultVibrator();
            }
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER && !isChestOpened) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            accelerationLast = accelerationCurrent;
            accelerationCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
            float delta = accelerationCurrent - accelerationLast;

            // Bắt sự kiện khi lực lắc vượt ngưỡng
            if (delta > SHAKE_THRESHOLD) {
                onShakeDetected();
            }
        }
    }

    private void onShakeDetected() {
        isChestOpened = true; // Đánh dấu đã lắc thành công

        // Cập nhật giao diện rương mở
        tvShakeStatus.setText("Trạng thái: Đã mở rương thành công!");
        imgChest.setBackgroundResource(R.drawable.img_kho_bau_mo);

        // Phát hiệu ứng rung
        triggerVibration();

        Toast.makeText(this, "Chúc mừng! Bạn đã mở thành công rương kho báu!", Toast.LENGTH_SHORT).show();

        // Đợi 1.5s hiển thị hình rương mở rồi chuyển sang RewardActivity (ScoreActivity)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intent = new Intent(OpenChestActivity.this, ScoreActivity.class);
            intent.putExtra("EXTRA_TREASURE_NAME", treasureTitle + " - " + treasureRarity);
            intent.putExtra("EXTRA_REWARD_POINTS", 400);
            intent.putExtra("EXTRA_TOTAL_POINTS", 1600);
            startActivity(intent);
            finish(); // Đóng OpenChestActivity
        }, 1500);
    }

    private void triggerVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(400, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(400);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Không bắt buộc xử lý
    }
}
