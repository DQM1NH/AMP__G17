package com.example.amp_g17;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class OpenChestActivity extends AppCompatActivity implements SensorEventListener {
    private ImageView btnBack;
    private ImageView imgChest;
    private ImageView imgShakeIcon;
    private TextView tvShakeStatus;

    // Các biến phục vụ việc quản lý cảm biến lắc (Accelerometer)
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float accelerationCurrent;
    private float accelerationLast;
    private float shakeThreshold = 12.0f; // Ngưỡng độ nhạy lực lắc
    private boolean isChestOpened = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_chest);

        btnBack = findViewById(R.id.btnBack);
        imgChest = findViewById(R.id.imgChest);
        imgShakeIcon = findViewById(R.id.imgShakeIcon);
        tvShakeStatus = findViewById(R.id.tvShakeStatus);

        // Khởi tạo SensorManager và Cảm biến gia tốc (Accelerometer)
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        accelerationCurrent = SensorManager.GRAVITY_EARTH;
        accelerationLast = SensorManager.GRAVITY_EARTH;

        btnBack.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Đăng ký lắng nghe sự kiện cảm biến khi Activity được hiển thị
        if (sensorManager != null && accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Hủy đăng ký cảm biến để tiết kiệm pin khi ứng dụng tạm dừng
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
            // Tính toán độ lớn gia tốc tổng hợp
            accelerationCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
            float delta = accelerationCurrent - accelerationLast;

            // Kiểm tra xem lực lắc có vượt quá ngưỡng cài đặt không
            if (delta > shakeThreshold) {
                onShakeDetected();
            }
        }
    }

    private void onShakeDetected() {
        isChestOpened = true; // Đánh dấu rương đã mở

        // Cập nhật giao diện khi phát hiện lắc
        tvShakeStatus.setText("Trạng thái: Đã mở rương thành công!");
        imgChest.setBackgroundResource(R.drawable.img_kho_bau_mo); // Đổi hình ảnh rương mở

        Toast.makeText(this, "Chúc mừng! Bạn đã mở thành công rương kho báu!", Toast.LENGTH_SHORT).show();

        // Đợi 1.5 giây để hiển thị hiệu ứng rương mở rồi chuyển sang Activity tiếp theo
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(OpenChestActivity.this, ChallengeActivity.class);
            // Hoặc chuyển thẳng sang RewardActivity nếu rương không có thử thách
            // Intent intent = new Intent(OpenChestActivity.this, RewardActivity.class);
            intent.putExtra("EXTRA_TREASURE_TITLE", "Kho báu 01");
            intent.putExtra("EXTRA_TREASURE_RARITY", "Rare");
            startActivity(intent);
            finish();
        }, 1500);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Không bắt buộc xử lý độ chính xác cảm biến trong bài toán này
    }
}
