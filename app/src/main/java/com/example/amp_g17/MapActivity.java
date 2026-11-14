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
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

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
    private static final LatLng HANOI_CENTER = new LatLng(21.028511, 105.854167); // Hồ Hoàn Kiếm, Hà Nội

    private TextView tvGreeting;
    private TextView tvScore;
    private Button  btnMapTab;
    private Button btnRank;
    private android.widget.ImageView btnSettings;
    private ListenerRegistration userListener;

    // Google Maps & Location
    private GoogleMap mMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location currentLocation;
    private boolean isFirstLocationUpdate = true;

    // Class quản lý thông tin Rương kho báu
    private static class TreasureItem {
        String id;
        String title;
        String rarity;
        String difficulty;
        String description;
        String rewardRange;
        int rewardPoints;
        LatLng position;

        TreasureItem(String id, String title, String rarity, String difficulty, String description, String rewardRange, int rewardPoints, LatLng position) {
            this.id = id;
            this.title = title;
            this.rarity = rarity;
            this.difficulty = difficulty;
            this.description = description;
            this.rewardRange = rewardRange;
            this.rewardPoints = rewardPoints;
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
        loadUserDataFromFirebase();
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
        btnSettings = findViewById(R.id.btnSettings);
    }

    private void setupListeners() {
        // Navigation chuyển sang màn hình Bảng xếp hạng
        btnRank.setOnClickListener(v -> {
            Intent intent = new Intent(MapActivity.this, RankActivity.class);
            startActivity(intent);
        });

        tvGreeting.setOnClickListener(v -> showSettingsDialog());
        btnSettings.setOnClickListener(v -> showSettingsDialog());
    }

    private void showSettingsDialog() {
        Intent intent = new Intent(MapActivity.this, ProfileActivity.class);
        startActivity(intent);
    }

    private void loadUserDataFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        tvScore.setText("Điểm: Đang tải...");

        userListener = db.collection("users").document(uid).addSnapshotListener(this, (documentSnapshot, error) -> {
            if (error != null) {
                android.util.Log.e("MapActivity", "Lỗi lắng nghe dữ liệu user: ", error);
                tvScore.setText("Điểm: Lỗi Rules");
                Toast.makeText(MapActivity.this, "Lỗi Firebase Rules: " + error.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                return;
            }
            if (documentSnapshot != null && documentSnapshot.exists()) {
                String displayName = documentSnapshot.getString("displayName");
                Long totalScore = documentSnapshot.getLong("totalScore");

                if (displayName != null && !displayName.trim().isEmpty()) {
                    tvGreeting.setText("Xin chào, " + displayName);
                } else if (currentUser.getEmail() != null) {
                    tvGreeting.setText("Xin chào, " + currentUser.getEmail());
                } else {
                    tvGreeting.setText("Xin chào, người chơi");
                }

                if (totalScore != null) {
                    tvScore.setText(String.format(java.util.Locale.getDefault(), "Điểm: %,d", totalScore));
                } else {
                    tvScore.setText("Điểm: 0");
                }
            } else if (documentSnapshot != null && !documentSnapshot.exists()) {
                // Khởi tạo thông tin user mặc định nếu chưa tồn tại trong Firestore
                java.util.Map<String, Object> initialUser = new java.util.HashMap<>();
                initialUser.put("displayName", currentUser.getDisplayName() != null ? currentUser.getDisplayName() : "Người chơi");
                initialUser.put("totalScore", 0);
                initialUser.put("completedTreasureCount", 0);
                initialUser.put("createdAt", Timestamp.now());
                initialUser.put("lastActiveAt", Timestamp.now());
                db.collection("users").document(uid).set(initialUser, com.google.firebase.firestore.SetOptions.merge());
            }
        });

        java.util.Map<String, Object> activeData = new java.util.HashMap<>();
        activeData.put("lastActiveAt", Timestamp.now());
        db.collection("users").document(uid).set(activeData, com.google.firebase.firestore.SetOptions.merge());
    }

    private boolean isLocationInVietnam(Location location) {
        if (location == null) return false;
        double lat = location.getLatitude();
        double lng = location.getLongitude();
        return (lat >= 8.0 && lat <= 24.0 && lng >= 102.0 && lng <= 110.0);
    }

    private LatLng getValidLocationLatLng(Location location) {
        if (location != null && isLocationInVietnam(location)) {
            return new LatLng(location.getLatitude(), location.getLongitude());
        }
        if (location != null) {
            location.setLatitude(HANOI_CENTER.latitude);
            location.setLongitude(HANOI_CENTER.longitude);
        }
        return HANOI_CENTER;
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
                        LatLng currentLatLng = getValidLocationLatLng(location);

                        // Lần đầu lấy được vị trí -> Zoom camera & khởi tạo danh sách kho báu ngẫu nhiên
                        if (isFirstLocationUpdate) {
                            isFirstLocationUpdate = false;
                            if (mMap != null) {
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f));
                            }
                            generateRandomTreasuresAround(currentLatLng);
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

        // Đặt camera mặc định về trung tâm Hà Nội (Hồ Hoàn Kiếm) & tạo các kho báu ngẫu nhiên xung quanh
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HANOI_CENTER, 15.5f));
        generateRandomTreasuresAround(HANOI_CENTER);

        checkLocationPermission();

        // Xử lý sự kiện khi nhấn vào điểm (Marker) trên bản đồ
        mMap.setOnMarkerClickListener(marker -> {
            TreasureItem selectedTreasure = (TreasureItem) marker.getTag();
            if (selectedTreasure != null) {
                Intent intent = new Intent(MapActivity.this, ViewTreasureActivity.class);

                // Đóng gói và truyền các Intent Extras khớp với bên ViewTreasureActivity nhận
                intent.putExtra("EXTRA_TREASURE_ID", selectedTreasure.id);
                intent.putExtra("EXTRA_TREASURE_NAME", selectedTreasure.title);
                intent.putExtra("EXTRA_RARITY", selectedTreasure.rarity);
                intent.putExtra("EXTRA_DIFFICULTY", selectedTreasure.difficulty);
                intent.putExtra("EXTRA_DESCRIPTION", selectedTreasure.description);
                intent.putExtra("EXTRA_REWARD_RANGE", selectedTreasure.rewardRange);
                intent.putExtra("EXTRA_REWARD_POINTS", selectedTreasure.rewardPoints);
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
                    LatLng currentLatLng = getValidLocationLatLng(location);
                    if (isFirstLocationUpdate) {
                        isFirstLocationUpdate = false;
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f));
                        generateRandomTreasuresAround(currentLatLng);
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

    private void generateRandomTreasuresAround(LatLng centerPos) {
        if (mMap == null) return;

        mMap.clear();
        treasureList.clear();
        java.util.Random random = new java.util.Random();

        String[] rarities = {"Common", "Uncommon", "Rare", "Epic", "Legendary"};
        String[] difficulties = {"Dễ", "Dễ - Trung bình", "Trung bình", "Khó", "Rất khó"};
        int[] baseRewards = {100, 200, 400, 700, 2000};
        String[] descriptions = {
                "Kho báu cổ cất giấu từ lâu, hãy nhanh tay khám phá.",
                "Rương quý hiếm nằm sâu trong khu vực bí mật.",
                "Kho báu ẩn rải rác đang chờ người đến mở khóa.",
                "Chiếc rương kho báu ngập tràn điểm thưởng giá trị.",
                "Thử thách đặc biệt dành cho thợ săn kho báu thực thụ."
        };

        int numberOfTreasures = 12 + random.nextInt(3); // Tạo ngẫu nhiên từ 12 đến 14 rương

        for (int i = 1; i <= numberOfTreasures; i++) {
            // Ngẫu nhiên tọa độ trong bán kính khoảng ~150m - 500m quanh vị trí trung tâm
            double latOffset = (random.nextDouble() - 0.5) * 0.007;
            double lngOffset = (random.nextDouble() - 0.5) * 0.007;

            LatLng treasurePos = new LatLng(centerPos.latitude + latOffset, centerPos.longitude + lngOffset);

            int rarityIndex = random.nextInt(rarities.length);
            String rarity = rarities[rarityIndex];
            String difficulty = difficulties[rarityIndex];
            
            // Xây dựng ID kho báu ngẫu nhiên cố định cho phiên này để làm ID check trùng thưởng
            String treasureId = "TREASURE_" + System.currentTimeMillis() + "_" + i;
            
            // Random dao động điểm thưởng từ điểm cơ bản
            int reward = baseRewards[rarityIndex];
            String rewardRange = reward + " điểm";
            String description = descriptions[random.nextInt(descriptions.length)];
            String title = String.format(java.util.Locale.getDefault(), "Kho báu %02d", i);

            TreasureItem item = new TreasureItem(treasureId, title, rarity, difficulty, description, rewardRange, reward, treasurePos);
            treasureList.add(item);

            float markerColor;
            switch (rarity) {
                case "Common":
                    markerColor = BitmapDescriptorFactory.HUE_AZURE;
                    break;
                case "Uncommon":
                    markerColor = BitmapDescriptorFactory.HUE_GREEN;
                    break;
                case "Rare":
                    markerColor = BitmapDescriptorFactory.HUE_YELLOW;
                    break;
                case "Epic":
                    markerColor = BitmapDescriptorFactory.HUE_MAGENTA;
                    break;
                case "Legendary":
                default:
                    markerColor = BitmapDescriptorFactory.HUE_RED;
                    break;
            }

            com.google.android.gms.maps.model.Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(treasurePos)
                    .title(title + " (" + rarity + ")")
                    .snippet("Nhấn để xem thông tin chi tiết")
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

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
    protected void onDestroy() {
        super.onDestroy();
        if (userListener != null) {
            userListener.remove();
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
