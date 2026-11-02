package com.example.amp_g17;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.auth.FirebaseAuth;

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

import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final float OPEN_CHEST_RADIUS_METERS = 50.0f;

    private TextView tvGreeting;
    private TextView tvScore;
    private Button  btnMapTab;
    private Button btnRank;
    private Button btnLogout;

    // Google Maps & Location
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location currentLocation;
    private boolean isFirstLocationUpdate = true;

    // Class quản lý thông tin Rương kho báu
    private static class TreasureItem {
        String title;
        String rarity;
        String difficulty;
        String description;
        String rewardRange;
        LatLng position;

        TreasureItem(String title, String rarity, String difficulty, String description, String rewardRange, LatLng position) {
            this.title = title;
            this.rarity = rarity;
            this.difficulty = difficulty;
            this.description = description;
            this.rewardRange = rewardRange;
            this.position = position;
        }
    }

    private final List<TreasureItem> treasureList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Kiểm tra xác thực (NFR-09)
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(MapActivity.this, StartActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        setContentView(R.layout.activity_map);

        initViews();
        setupListeners();
        initLocationServices();

        // Khởi tạo Google Map Fragment
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void initViews() {
        tvGreeting = findViewById(R.id.tvGreeting);
        tvScore = findViewById(R.id.tvScore);
        btnMapTab = findViewById(R.id.btnMapTab);
        btnRank = findViewById(R.id.btnRank);
    }

    private void setupListeners() {
        // Navigation chuyển sang màn hình Bảng xếp hạng
        btnRank.setOnClickListener(v -> {
            Intent intent = new Intent(MapActivity.this, RankActivity.class);
            startActivity(intent);
        });
    }

    private void initLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Khởi tạo Callback lắng nghe cập nhật vị trí định kỳ
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        currentLocation = location;
                        LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                        // Lần đầu lấy được vị trí -> Zoom camera & khởi tạo danh sách kho báu
                        if (isFirstLocationUpdate) {
                            isFirstLocationUpdate = false;
                            if (mMap != null) {
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f));
                            }
                            generateSampleTreasures(currentLatLng);
                        }
                    }
                }
            }
        };
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);

        checkLocationPermission();

        // Xử lý sự kiện khi nhấn vào điểm (Marker) trên bản đồ
        mMap.setOnMarkerClickListener(marker -> {
            TreasureItem selectedTreasure = (TreasureItem) marker.getTag();
            if (selectedTreasure != null) {
                Intent intent = new Intent(MapActivity.this, ViewTreasureActivity.class);

                // Đóng gói và truyền các Intent Extras khớp với bên ViewTreasureActivity nhận
                intent.putExtra("EXTRA_TREASURE_NAME", selectedTreasure.title);
                intent.putExtra("EXTRA_RARITY", selectedTreasure.rarity);
                intent.putExtra("EXTRA_DIFFICULTY", selectedTreasure.difficulty);
                intent.putExtra("EXTRA_DESCRIPTION", selectedTreasure.description);
                intent.putExtra("EXTRA_REWARD_RANGE", selectedTreasure.rewardRange);
                intent.putExtra("EXTRA_LAT", selectedTreasure.position.latitude);
                intent.putExtra("EXTRA_LNG", selectedTreasure.position.longitude);

                startActivity(intent);
            }
            return true; // Trả về true để không hiển thị InfoWindow mặc định của Google Maps
        });
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            enableMyLocationOnMap();
            checkLocationSettingsAndFetch();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void enableMyLocationOnMap() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
        }
    }

    private void checkLocationSettingsAndFetch() {
        // 1. Kiểm tra GPS trên thiết bị
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager != null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGpsEnabled && !isNetworkEnabled) {
            showEnableGpsDialog();
            return;
        }

        // 2. Lấy vị trí từ Cache trước, nếu rỗng sẽ yêu cầu cập nhật vị trí mới
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
                if (location != null) {
                    currentLocation = location;
                    LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());
                    if (isFirstLocationUpdate) {
                        isFirstLocationUpdate = false;
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 17f));
                        generateSampleTreasures(currentLatLng);
                    }
                }
                // Luôn khởi chạy đăng ký theo dõi vị trí liên tục
                startLocationUpdates();
            });
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
                .setMinUpdateIntervalMillis(2000)
                .build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
    }

    private void showEnableGpsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Chưa bật vị trí (GPS)")
                .setMessage("Ứng dụng cần GPS để định vị kho báu quanh bạn. Bạn có muốn bật ngay không?")
                .setPositiveButton("Cài đặt", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void generateSampleTreasures(LatLng playerPos) {
        mMap.clear();
        treasureList.clear();

        double lat = playerPos.latitude;
        double lng = playerPos.longitude;

        treasureList.add(new TreasureItem("Kho báu 01", "Rare", "Trung bình", "Kho báu cổ được cất giấu từ lâu, di chuyển lại gần để khám phá.", "200~800 điểm", new LatLng(lat + 0.0002, lng + 0.0002)));
        treasureList.add(new TreasureItem("Kho báu 02", "Common", "Dễ", "Kho báu phổ thông nằm ở khu vực trống.", "100~400 điểm", new LatLng(lat - 0.0012, lng + 0.0015)));
        treasureList.add(new TreasureItem("Kho báu 03", "Epic", "Khó", "Kho báu huyền thoại chứa nhiều điểm thưởng giá trị.", "500~1500 điểm", new LatLng(lat + 0.0018, lng - 0.0012)));

        for (TreasureItem item : treasureList) {
            float markerColor = BitmapDescriptorFactory.HUE_YELLOW;
            if (item.rarity.equals("Common")) markerColor = BitmapDescriptorFactory.HUE_AZURE;
            else if (item.rarity.equals("Epic")) markerColor = BitmapDescriptorFactory.HUE_MAGENTA;

            // 1. Tạo Marker trên bản đồ
            com.google.android.gms.maps.model.Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(item.position)
                    .title(item.title + " - " + item.rarity)
                    .snippet("Nhấn để xem thông tin chi tiết")
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

            // 2. Gắn đối tượng TreasureItem vào Tag của Marker
            if (marker != null) {
                marker.setTag(item);
            }
        }
    }

    private void handleChestClick(TreasureItem treasure) {
        if (currentLocation == null) {
            Toast.makeText(this, "Đang xác định vị trí của bạn, vui lòng đợi...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tính khoảng cách giữa vị trí người chơi và vị trí kho báu
        float[] results = new float[1];
        Location.distanceBetween(
                currentLocation.getLatitude(), currentLocation.getLongitude(),
                treasure.position.latitude, treasure.position.longitude,
                results
        );

        float distanceInMeters = results[0];

        if (distanceInMeters <= OPEN_CHEST_RADIUS_METERS) {
            Toast.makeText(this, "Bạn đã đủ gần (" + (int) distanceInMeters + "m). Mở rương!", Toast.LENGTH_SHORT).show();

            // Chuyển sang màn hình Mở rương kho báu
            Intent intent = new Intent(MapActivity.this, OpenChestActivity.class);
            intent.putExtra("EXTRA_TREASURE_TITLE", treasure.title);
            intent.putExtra("EXTRA_TREASURE_RARITY", treasure.rarity);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Bạn còn cách kho báu " + (int) distanceInMeters + "m! Hãy di chuyển lại gần hơn (< 50m).", Toast.LENGTH_LONG).show();
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
        // Tạm dừng cập nhật vị trí khi activity ngưng hoạt động để tiết kiệm pin
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocationOnMap();
                checkLocationSettingsAndFetch();
            } else {
                Toast.makeText(this, "Quyền vị trí bị từ chối. Ứng dụng không thể hoạt động đúng!", Toast.LENGTH_LONG).show();
            }
        }
    }
}
