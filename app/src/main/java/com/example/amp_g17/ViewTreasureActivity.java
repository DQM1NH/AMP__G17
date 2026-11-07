package com.example.amp_g17;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
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

public class ViewTreasureActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1002;
    private static final float HUNT_RADIUS_METERS = 50.0f; // Bán kính cho phép mở rương (50m)

    // Views
    private ImageView btnBack;
    private TextView tvTreasureName, tvStatusActive, tvDescription, tvRarity, tvDifficulty, tvDistance, tvRewardRange;
    private Button btnStartHunt;

    // Google Map & Location
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    // Data Kho Báu
    private String treasureName = "Kho báu 01";
    private String treasureRarity = "Rare";
    private String treasureDifficulty = "Trung bình";
    private String treasureDescription = "Di chuyển đến điểm đánh dấu để mở khóa thử thách của kho báu.";
    private String treasureRewardRange = "200~800 điểm";
    private LatLng treasureLatLng = new LatLng(21.0285, 105.8542);
    private float currentDistanceInMeters = -1;

    private String challengeId;
    private String challengeQuestion;
    private String challengeType;
    private java.util.ArrayList<String> challengeOptions;
    private int correctOptionIndex = -1;
    private String treasureId;
    private int rewardPoints = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_treasure);

        initViews();
        getIntentData();
        setupListeners();
        setupLocationServices();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapFragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTreasureName = findViewById(R.id.tvTreasureName);
        tvStatusActive = findViewById(R.id.tvStatusActive);
        tvDescription = findViewById(R.id.tvDescription);
        tvRarity = findViewById(R.id.tvRarity);
        tvDifficulty = findViewById(R.id.tvDifficulty);
        tvDistance = findViewById(R.id.tvDistance);
        tvRewardRange = findViewById(R.id.tvRewardRange);
        btnStartHunt = findViewById(R.id.btnStartHunt);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("EXTRA_TREASURE_NAME")) treasureName = intent.getStringExtra("EXTRA_TREASURE_NAME");
            if (intent.hasExtra("EXTRA_RARITY")) treasureRarity = intent.getStringExtra("EXTRA_RARITY");
            if (intent.hasExtra("EXTRA_DIFFICULTY")) treasureDifficulty = intent.getStringExtra("EXTRA_DIFFICULTY");
            if (intent.hasExtra("EXTRA_DESCRIPTION")) treasureDescription = intent.getStringExtra("EXTRA_DESCRIPTION");
            if (intent.hasExtra("EXTRA_REWARD_RANGE")) treasureRewardRange = intent.getStringExtra("EXTRA_REWARD_RANGE");
            double lat = intent.getDoubleExtra("EXTRA_LAT", 21.0285);
            double lng = intent.getDoubleExtra("EXTRA_LNG", 105.8542);
            treasureLatLng = new LatLng(lat, lng);

            // lay du leiu tu intent
            challengeId = intent.getStringExtra("EXTRA_CHALLENGE_ID");
            challengeQuestion = intent.getStringExtra("EXTRA_CHALLENGE_QUESTION");
            challengeType = intent.getStringExtra("EXTRA_CHALLENGE_TYPE");
            challengeOptions = intent.getStringArrayListExtra("EXTRA_CHALLENGE_OPTIONS");
            correctOptionIndex = intent.getIntExtra("EXTRA_CORRECT_OPTION_INDEX", -1);
            treasureId = intent.getStringExtra("EXTRA_TREASURE_ID");
            rewardPoints = intent.getIntExtra("EXTRA_REWARD_POINTS", 0);
        }

        tvTreasureName.setText(treasureName);
        tvRarity.setText(treasureRarity);
        tvDifficulty.setText(treasureDifficulty);
        tvDescription.setText(treasureDescription);
        tvRewardRange.setText(treasureRewardRange);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // CHỈNH SỬA: Bấm nút "BẮT ĐẦU SĂN" chuyển sang HuntingActivity
        btnStartHunt.setOnClickListener(v -> {
            if (challengeQuestion == null || challengeOptions == null
                    || challengeOptions.size() < 4 || correctOptionIndex < 0) {
                Toast.makeText(this, "Kho báu này chưa có thử thách hợp lệ. Hãy quay lại chọn kho báu khác!", Toast.LENGTH_LONG).show();
                return;
            }

            Toast.makeText(this, "Bắt đầu chuyến săn: " + treasureName, Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(ViewTreasureActivity.this, HuntingActivity.class);
            intent.putExtra("EXTRA_TREASURE_TITLE", treasureName);
            intent.putExtra("EXTRA_TREASURE_RARITY", treasureRarity);
            intent.putExtra("EXTRA_LAT", treasureLatLng.latitude);
            intent.putExtra("EXTRA_LNG", treasureLatLng.longitude);

            intent.putExtra("EXTRA_CHALLENGE_ID", challengeId);
            intent.putExtra("EXTRA_CHALLENGE_QUESTION", challengeQuestion);
            intent.putExtra("EXTRA_CHALLENGE_TYPE", challengeType);
            intent.putStringArrayListExtra("EXTRA_CHALLENGE_OPTIONS", challengeOptions);
            intent.putExtra("EXTRA_CORRECT_OPTION_INDEX", correctOptionIndex);
            intent.putExtra("EXTRA_TREASURE_ID", treasureId);
            intent.putExtra("EXTRA_REWARD_POINTS", rewardPoints);
            startActivity(intent);
            finish();
        });
    }

    private void setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Khởi tạo LocationRequest: Cập nhật vị trí mỗi 2 giây, tối thiểu 1 giây
        locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000)
                .setMinUpdateIntervalMillis(1000)
                .build();

        // Khởi tạo LocationCallback nhận dữ liệu thời gian thực
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        updateDistanceUI(location);
                    }
                }
            }
        };
    }

    private void updateDistanceUI(Location userLocation) {
        float[] results = new float[1];
        Location.distanceBetween(
                userLocation.getLatitude(), userLocation.getLongitude(),
                treasureLatLng.latitude, treasureLatLng.longitude,
                results
        );

        currentDistanceInMeters = results[0];

        // Định dạng hiển thị khoảng cách
        if (currentDistanceInMeters >= 1000) {
            tvDistance.setText(String.format("%.1f km", currentDistanceInMeters / 1000));
        } else {
            tvDistance.setText(String.format("%d m", (int) currentDistanceInMeters));
        }

        btnStartHunt.setBackgroundTintList(ContextCompat.getColorStateList(this, android.R.color.holo_green_light));
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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
                .position(treasureLatLng)
                .title(treasureName)
                .snippet("Độ hiếm: " + treasureRarity)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(treasureLatLng, 16f));

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
        // Dừng theo dõi vị trí khi Activity không hiển thị để tiết kiệm pin
        stopLocationUpdates();
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