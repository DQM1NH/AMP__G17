package com.example.amp_g17;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

public class HuntingActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1003;
    private static final float APPROACH_RADIUS_METERS = 50.0f; // Bán kính tiếp cận (50m)

    // Views
    private ImageView btnBack;
    private TextView tvTarget, tvDistance, tvChallengeTitle, tvChallengeSub;
    private LinearLayout btnChallengeStatus;

    // Google Maps & Location
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;
    private Polyline polyline;

    // Data Kho Báu
    private String targetName = "Kho báu 01";
    private String targetRarity = "Rare";
    private LatLng targetLatLng = new LatLng(21.0285, 105.8542);
    private float currentDistanceInMeters = -1;

    // Biến kiểm soát tự động chuyển màn hình 1 lần
    private boolean isAutoNavigated = false;

    // Âm thanh & Rung
    private Vibrator vibrator;
    private ToneGenerator toneGenerator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hunting);

        initViews();
        getIntentData();
        setupListeners();
        setupLocationServices();
        initAlertServices();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTarget = findViewById(R.id.tvTarget);
        tvDistance = findViewById(R.id.tvDistance);
        btnChallengeStatus = findViewById(R.id.btnChallengeStatus);
        tvChallengeTitle = findViewById(R.id.tvChallengeTitle);
        tvChallengeSub = findViewById(R.id.tvChallengeSub);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("EXTRA_TREASURE_TITLE")) targetName = intent.getStringExtra("EXTRA_TREASURE_TITLE");
            if (intent.hasExtra("EXTRA_TREASURE_RARITY")) targetRarity = intent.getStringExtra("EXTRA_TREASURE_RARITY");
            double lat = intent.getDoubleExtra("EXTRA_LAT", 21.0285);
            double lng = intent.getDoubleExtra("EXTRA_LNG", 105.8542);
            targetLatLng = new LatLng(lat, lng);
        }
        tvTarget.setText("Mục tiêu: " + targetName);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnChallengeStatus.setOnClickListener(v -> {
            if (currentDistanceInMeters > APPROACH_RADIUS_METERS) {
                Toast.makeText(this, "Bạn cần di chuyển lại gần kho báu hơn (< 50m)!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void initAlertServices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            VibratorManager vibratorManager = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            if (vibratorManager != null) {
                vibrator = vibratorManager.getDefaultVibrator();
            }
        } else {
            vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        }

        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setMinUpdateIntervalMillis(1000)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        updateHuntingProgress(location);
                    }
                }
            }
        };
    }

    private void updateHuntingProgress(Location userLocation) {
        LatLng userLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());

        float[] results = new float[1];
        Location.distanceBetween(
                userLocation.getLatitude(), userLocation.getLongitude(),
                targetLatLng.latitude, targetLatLng.longitude,
                results
        );

        currentDistanceInMeters = results[0];

        // Cập nhật khoảng cách UI
        if (currentDistanceInMeters >= 1000) {
            tvDistance.setText(String.format("%.1f km", currentDistanceInMeters / 1000));
        } else {
            tvDistance.setText(String.format("%d m", (int) currentDistanceInMeters));
        }

        // Vẽ đường Polyline dẫn đường
        if (mMap != null) {
            if (polyline != null) polyline.remove();
            polyline = mMap.addPolyline(new PolylineOptions()
                    .add(userLatLng, targetLatLng)
                    .width(8f)
                    .color(Color.BLUE)
                    .geodesic(true));
        }

        // KIỂM TRA ĐIỀU KIỆN <= 50M -> PHÁT ÂM THANH & TỰ ĐỘNG CHUYỂN SANG TreasureApproachedActivity
        if (currentDistanceInMeters <= APPROACH_RADIUS_METERS && !isAutoNavigated) {
            isAutoNavigated = true; // Đánh dấu đã chuyển để tránh lặp lại

            // 1. Cập nhật giao diện
            tvChallengeTitle.setText("[ ĐÃ ĐẾN NƠI ] CHUYỂN MÀN HÌNH...");
            tvChallengeTitle.setTextColor(Color.parseColor("#008000"));
            tvChallengeSub.setText("Đang mở giao diện tiếp cận kho báu");

            // 2. Phát âm thanh & rung thông báo
            triggerAlert();

            Toast.makeText(this, "Đã vào phạm vi 50m! Đang tiếp cận kho báu...", Toast.LENGTH_SHORT).show();

            // 3. Đợi 1 giây để âm thanh phát hoàn tất rồi tự động chuyển Activity
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Intent intent = new Intent(HuntingActivity.this, TreasureApproachedActivity.class);
                intent.putExtra("EXTRA_TREASURE_TITLE", targetName);
                intent.putExtra("EXTRA_TREASURE_RARITY", targetRarity);
                intent.putExtra("EXTRA_LAT", targetLatLng.latitude);
                intent.putExtra("EXTRA_LNG", targetLatLng.longitude);
                startActivity(intent);
                finish(); // Đóng HuntingActivity
            }, 1000);
        }
    }

    private void triggerAlert() {
        // Phát âm thanh bíp thông báo
        if (toneGenerator != null) {
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 600);
        }

        // Hiệu ứng rung
        if (vibrator != null && vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(500);
            }
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        mMap.addMarker(new MarkerOptions()
                .position(targetLatLng)
                .title(targetName)
                .snippet("Mục tiêu kho báu")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng, 16f));

        checkLocationPermission();
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            startLocationUpdates();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationPermission();
            }
        }
    }
}