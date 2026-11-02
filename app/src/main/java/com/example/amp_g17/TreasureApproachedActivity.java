package com.example.amp_g17;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class TreasureApproachedActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1004;
    private static final float ACTIVATION_RADIUS_METERS = 50.0f; // Vùng kích hoạt 50m

    // Views trong XML Layout
    private ImageView btnBack;
    private TextView tvTarget;
    private TextView tvDistance;
    private TextView tvStatusTitle;
    private TextView tvStatusSub;
    private Button btnOpenChallenge;

    // Google Maps & Location Services
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    // Data Kho Báu
    private String targetName = "Kho báu 01";
    private String targetRarity = "Rare";
    private LatLng targetLatLng = new LatLng(21.0285, 105.8542);
    private float currentDistanceInMeters = -1;

    private boolean isInRange = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_treasure_approached);

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
        tvTarget = findViewById(R.id.tvTarget);
        tvDistance = findViewById(R.id.tvDistance);
        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvStatusSub = findViewById(R.id.tvStatusSub);
        btnOpenChallenge = findViewById(R.id.btnOpenChallenge);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("EXTRA_TREASURE_TITLE")) {
                targetName = intent.getStringExtra("EXTRA_TREASURE_TITLE");
            }
            if (intent.hasExtra("EXTRA_TREASURE_RARITY")) {
                targetRarity = intent.getStringExtra("EXTRA_TREASURE_RARITY");
            }
            double lat = intent.getDoubleExtra("EXTRA_LAT", 21.0285);
            double lng = intent.getDoubleExtra("EXTRA_LNG", 105.8542);
            targetLatLng = new LatLng(lat, lng);
        }

        tvTarget.setText("Mục tiêu: " + targetName);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        // CHỈNH SỬA: Bấm nút MỞ THỬ THÁCH -> Chuyển sang ChallengeActivity (Trả lời thử thách)
        btnOpenChallenge.setOnClickListener(v -> {
            if (isInRange) {
                Intent intent = new Intent(TreasureApproachedActivity.this, ChallengeActivity.class);
                intent.putExtra("EXTRA_TREASURE_TITLE", targetName);
                intent.putExtra("EXTRA_TREASURE_RARITY", targetRarity);
                startActivity(intent);
                finish(); // Đóng Activity hiện tại
            } else {
                Toast.makeText(this, "Bạn đã rời xa kho báu! Hãy tiến lại gần hơn (< 50m).", Toast.LENGTH_SHORT).show();
            }
        });
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
                        updateDistanceProgress(location);
                    }
                }
            }
        };
    }

    private void updateDistanceProgress(Location userLocation) {
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

        // Kiểm tra phạm vi bán kính 50m
        if (currentDistanceInMeters <= ACTIVATION_RADIUS_METERS) {
            isInRange = true;
            tvStatusTitle.setText("ĐÃ VÀO PHẠM VI KHO BÁU");
            tvStatusTitle.setTextColor(Color.parseColor("#008000"));
            tvStatusSub.setText("Thử thách đã được mở khóa");

            btnOpenChallenge.setEnabled(true);
            btnOpenChallenge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            btnOpenChallenge.setTextColor(Color.WHITE);
        } else {
            isInRange = false;
            tvStatusTitle.setText("ĐÃ RỜI KHỎI PHẠM VI");
            tvStatusTitle.setTextColor(Color.RED);
            tvStatusSub.setText("Vui lòng tiến lại gần dưới 50m");

            btnOpenChallenge.setEnabled(false);
            btnOpenChallenge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#B0B0B0")));
            btnOpenChallenge.setTextColor(Color.parseColor("#666666"));
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
                .snippet("Mục tiêu kích hoạt")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));

        mMap.addCircle(new CircleOptions()
                .center(targetLatLng)
                .radius(ACTIVATION_RADIUS_METERS)
                .strokeWidth(3f)
                .strokeColor(Color.parseColor("#4CAF50"))
                .fillColor(Color.parseColor("#224CAF50")));

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(targetLatLng, 17f));

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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationPermission();
            }
        }
    }
}