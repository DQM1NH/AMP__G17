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
import com.google.firebase.firestore.DocumentSnapshot;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final float OPEN_CHEST_RADIUS_METERS = 50.0f;
    private static final LatLng HANOI_CENTER = new LatLng(21.028511, 105.854167); // Hồ Hoàn Kiếm, Hà Nội

    private TextView tvGreeting;
    private TextView tvScore;
    private Button btnMapTab;
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
        String id;          // ID cố định, dùng để nhớ kho báu đã mở
        String title;
        String rarity;
        String difficulty;
        String description;
        String rewardRange;
        int rewardPoints;
        LatLng position;

        TreasureItem(String id, String title, String rarity, String difficulty, String description,
                     String rewardRange, int rewardPoints, LatLng position) {
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

    // Kho báu được sinh cố định theo ô lưới 0.005 độ (~550m) để mỗi lần mở app vẫn ra cùng một bộ kho báu
    private static final double GRID_STEP = 0.005;
    private String currentCellKey = null;
    private final Map<String, Marker> markerMap = new HashMap<>();          // treasureId -> marker
    private final Set<String> completedTreasureIds = new HashSet<>();        // kho báu đã mở của user
    private ListenerRegistration completionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Kiểm tra xác thực
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
        listenCompletedTreasures();
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
        btnRank.setOnClickListener(v -> {
            Intent intent = new Intent(MapActivity.this, RankActivity.class);
            startActivity(intent);
        });

        tvGreeting.setOnClickListener(v -> showSettingsDialog());
        btnSettings.setOnClickListener(v -> showSettingsDialog());
    }

    private void showSettingsDialog() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings, null);

        TextView tvSettingName = dialogView.findViewById(R.id.tvSettingName);
        TextView tvSettingEmail = dialogView.findViewById(R.id.tvSettingEmail);
        TextView tvSettingScore = dialogView.findViewById(R.id.tvSettingScore);
        TextView tvSettingChests = dialogView.findViewById(R.id.tvSettingChests);
        Button btnDialogLogout = dialogView.findViewById(R.id.btnDialogLogout);

        tvSettingEmail.setText("Email: " + (currentUser.getEmail() != null ? currentUser.getEmail() : "N/A"));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .create();

        FirebaseFirestore.getInstance().collection("users").document(currentUser.getUid()).get()
                .addOnSuccessListener(doc -> {
                    if (doc != null && doc.exists()) {
                        String name = doc.getString("displayName");
                        Long scoreObj = doc.getLong("totalScore");
                        Long chestObj = doc.getLong("completedTreasureCount");

                        long score = scoreObj != null ? scoreObj : 0;
                        long chests = chestObj != null ? chestObj : 0;

                        tvSettingName.setText("Tên: " + (name != null ? name : "Người chơi"));
                        tvSettingScore.setText(String.format(java.util.Locale.getDefault(), "Điểm hiện tại: %,d điểm", score));
                        tvSettingChests.setText("Kho báu đã mở: " + chests);
                    }
                });

        btnDialogLogout.setOnClickListener(v -> {
            dialog.dismiss();
            FirebaseAuth.getInstance().signOut();
            Intent intent = new Intent(MapActivity.this, StartActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        dialog.show();
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

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        currentLocation = location;
                        LatLng currentLatLng = getValidLocationLatLng(location);

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

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(HANOI_CENTER, 15.5f));
        generateRandomTreasuresAround(HANOI_CENTER);

        checkLocationPermission();

        mMap.setOnMarkerClickListener(marker -> {
            TreasureItem selectedTreasure = (TreasureItem) marker.getTag();
            if (selectedTreasure != null) {
                if (completedTreasureIds.contains(selectedTreasure.id)) {
                    Toast.makeText(this, "Kho báu này đã được mở rồi!", Toast.LENGTH_SHORT).show();
                    removeCompletedMarkers();
                } else {
                    fetchRandomChallengeAndNavigate(selectedTreasure);
                }
            }
            return true;
        });
    }

    // lay thu thach ngau nhien tu firestore theo do kho
    private void fetchRandomChallengeAndNavigate(TreasureItem selectedTreasure) {
        Toast.makeText(this, "Đang tải thử thách...", Toast.LENGTH_SHORT).show();

        // Ep kieu de khop voi Firestore
        String searchDifficulty = selectedTreasure.difficulty.toLowerCase().trim();

        android.util.Log.d("MapActivity", "Đang tìm thử thách với difficulty = '" + searchDifficulty + "'");

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Đã bỏ temporary filter isActive để tránh lỗi Index, chỉ lọc theo difficulty
        db.collection("challenges")
                .whereEqualTo("difficulty", searchDifficulty)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    android.util.Log.d("MapActivity", "Số câu hỏi tìm thấy: " + queryDocumentSnapshots.size());

                    // Xu ly khi khong co thu thach phu hop voi do kho
                    if (queryDocumentSnapshots.isEmpty()) {
                        Toast.makeText(MapActivity.this,
                                "Chưa có câu hỏi cho độ khó: " + searchDifficulty + ". Kiểm tra collection 'challenges' trên Firestore.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    Intent intent = new Intent(MapActivity.this, ViewTreasureActivity.class);

                    // Truyen thong tin Kho bau
                    intent.putExtra("EXTRA_TREASURE_NAME", selectedTreasure.title);
                    intent.putExtra("EXTRA_RARITY", selectedTreasure.rarity);
                    intent.putExtra("EXTRA_DIFFICULTY", selectedTreasure.difficulty);
                    intent.putExtra("EXTRA_DESCRIPTION", selectedTreasure.description);
                    intent.putExtra("EXTRA_REWARD_RANGE", selectedTreasure.rewardRange);
                    intent.putExtra("EXTRA_LAT", selectedTreasure.position.latitude);
                    intent.putExtra("EXTRA_LNG", selectedTreasure.position.longitude);
                    intent.putExtra("EXTRA_TREASURE_ID", selectedTreasure.id);
                    intent.putExtra("EXTRA_REWARD_POINTS", selectedTreasure.rewardPoints);

                    List<DocumentSnapshot> validDocs = new ArrayList<>();
                    for (DocumentSnapshot d : queryDocumentSnapshots.getDocuments()) {
                        String reason = getInvalidChallengeReason(d);
                        if (reason == null) {
                            validDocs.add(d);
                        } else {
                            android.util.Log.w("MapActivity", "Bỏ qua challenge '" + d.getId() + "': " + reason);
                        }
                    }

                    if (validDocs.isEmpty()) {
                        Toast.makeText(MapActivity.this,
                                "Các câu hỏi độ khó '" + searchDifficulty + "' đều sai định dạng. Xem Logcat (tag MapActivity).",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    DocumentSnapshot challengeDoc = validDocs.get(new Random().nextInt(validDocs.size()));
                    android.util.Log.d("MapActivity", "Đã chọn challenge ID: " + challengeDoc.getId());

                    intent.putExtra("EXTRA_CHALLENGE_ID", challengeDoc.getId());
                    intent.putExtra("EXTRA_CHALLENGE_QUESTION", challengeDoc.getString("question"));
                    intent.putExtra("EXTRA_CHALLENGE_TYPE", challengeDoc.getString("questionType"));
                    intent.putStringArrayListExtra("EXTRA_CHALLENGE_OPTIONS", getChallengeOptions(challengeDoc));
                    intent.putExtra("EXTRA_CORRECT_OPTION_INDEX", getCorrectIndex(challengeDoc));

                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("MapActivity", "Lỗi Firestore: ", e);
                    Toast.makeText(MapActivity.this, "Lỗi tải thử thách: " + e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // Kiem tra / doc du lieu challenge tu Firestore
    private ArrayList<String> getChallengeOptions(DocumentSnapshot doc) {
        ArrayList<String> result = new ArrayList<>();
        Object raw = doc.get("options");
        if (raw instanceof List) {
            for (Object o : (List<?>) raw) {
                if (o != null) result.add(String.valueOf(o));
            }
        }
        return result;
    }

    private int getCorrectIndex(DocumentSnapshot doc) {
        Object raw = doc.get("correctOptionIndex");
        if (raw instanceof Number) return ((Number) raw).intValue();
        if (raw instanceof String) {
            try { return Integer.parseInt(((String) raw).trim()); } catch (NumberFormatException ignored) { }
        }
        return -1;
    }

    // Tra ve ly do neu khong hop le
    private String getInvalidChallengeReason(DocumentSnapshot doc) {
        String q = doc.getString("question");
        if (q == null || q.trim().isEmpty()) return "thiếu field 'question' (hoặc không phải string)";
        ArrayList<String> opts = getChallengeOptions(doc);
        if (opts.size() < 4) return "'options' chỉ có " + opts.size() + " phần tử (cần 4) hoặc không phải array";
        int idx = getCorrectIndex(doc);
        if (idx < 0 || idx >= opts.size()) return "'correctOptionIndex' thiếu/sai (giá trị: " + doc.get("correctOptionIndex") + ")";
        return null;
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
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager != null && locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGpsEnabled && !isNetworkEnabled) {
            showEnableGpsDialog();
            return;
        }

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

    private void listenCompletedTreasures() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        completionListener = FirebaseFirestore.getInstance().collection("completions")
                .whereEqualTo("userId", user.getUid())
                .addSnapshotListener(this, (snapshot, error) -> {
                    if (error != null) {
                        android.util.Log.e("MapActivity", "Lỗi đọc completions: ", error);
                        return;
                    }
                    if (snapshot == null) return;

                    completedTreasureIds.clear();
                    for (DocumentSnapshot d : snapshot.getDocuments()) {
                        String tid = d.getString("treasureId");
                        if (tid != null) completedTreasureIds.add(tid);
                    }
                    removeCompletedMarkers();
                });
    }

    // Xoa marker, du lieu cac kho bau da mo.
    private void removeCompletedMarkers() {
        for (String id : completedTreasureIds) {
            Marker m = markerMap.remove(id);
            if (m != null) m.remove();
        }
        treasureList.removeIf(t -> completedTreasureIds.contains(t.id));
    }

    private void generateRandomTreasuresAround(LatLng centerPos) {
        if (mMap == null) return;

        // Snap tâm vào ô lưới -> cùng khu vực luôn ra cùng một bộ kho báu (ID + vị trí cố định)
        long latIdx = Math.round(centerPos.latitude / GRID_STEP);
        long lngIdx = Math.round(centerPos.longitude / GRID_STEP);
        String cellKey = latIdx + "_" + lngIdx;

        // Cùng ô lưới và đã vẽ rồi thì không vẽ lại
        if (cellKey.equals(currentCellKey) && !markerMap.isEmpty()) return;
        currentCellKey = cellKey;

        double centerLat = latIdx * GRID_STEP;
        double centerLng = lngIdx * GRID_STEP;

        mMap.clear();
        treasureList.clear();
        markerMap.clear();

        Random random = new Random(latIdx * 1_000_003L + lngIdx); // seed cố định theo ô lưới

        String[] rarities = {"Common", "Rare", "Epic", "Legendary"};
        String[] difficulties = {"easy", "medium", "hard", "hard"};
        String[] rewardRanges = {"100~300 điểm", "200~600 điểm", "500~1000 điểm", "1000~2000 điểm"};
        int[][] rewardMinMax = {{100, 300}, {200, 600}, {500, 1000}, {1000, 2000}};
        String[] descriptions = {
                "Kho báu cổ cất giấu gần khu vực Hà Nội.",
                "Rương quý hiếm nằm sâu trong lòng thành phố.",
                "Kho báu ẩn rải rác xung quanh khu vực của bạn.",
                "Chiếc rương kho báu ngập tràn điểm thưởng.",
                "Thử thách đặc biệt dành cho thợ săn kho báu."
        };

        int numberOfTreasures = 12 + random.nextInt(3);

        for (int i = 1; i <= numberOfTreasures; i++) {
            // Luôn rút đủ số giá trị ngẫu nhiên cho mỗi kho báu (kể cả kho báu bị ẩn) để các kho báu còn lại không bị xê dịch
            double latOffset = (random.nextDouble() - 0.5) * 0.008;
            double lngOffset = (random.nextDouble() - 0.5) * 0.008;
            int rarityIndex = random.nextInt(rarities.length);
            String description = descriptions[random.nextInt(descriptions.length)];
            int minReward = rewardMinMax[rarityIndex][0];
            int maxReward = rewardMinMax[rarityIndex][1];
            int rewardPoints = minReward + random.nextInt((maxReward - minReward) / 50 + 1) * 50;

            String treasureId = "treasure_" + cellKey + "_" + String.format(Locale.US, "%02d", i);

            if (completedTreasureIds.contains(treasureId)) continue;

            LatLng treasurePos = new LatLng(centerLat + latOffset, centerLng + lngOffset);
            String rarity = rarities[rarityIndex];
            String title = String.format(Locale.getDefault(), "Kho báu %02d", i);

            TreasureItem item = new TreasureItem(treasureId, title, rarity, difficulties[rarityIndex],
                    description, rewardRanges[rarityIndex], rewardPoints, treasurePos);
            treasureList.add(item);

            float markerColor;
            switch (rarity) {
                case "Common":
                    markerColor = BitmapDescriptorFactory.HUE_AZURE;
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

            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(treasurePos)
                    .title(title + " (" + rarity + ")")
                    .snippet("Nhấn để xem thông tin chi tiết")
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor)));

            if (marker != null) {
                marker.setTag(item);
                markerMap.put(treasureId, marker);
            }
        }
    }

    private void handleChestClick(TreasureItem treasure) {
        if (currentLocation == null) {
            Toast.makeText(this, "Đang xác định vị trí của bạn, vui lòng đợi...", Toast.LENGTH_SHORT).show();
            return;
        }

        float[] results = new float[1];
        Location.distanceBetween(
                currentLocation.getLatitude(), currentLocation.getLongitude(),
                treasure.position.latitude, treasure.position.longitude,
                results
        );

        float distanceInMeters = results[0];

        if (distanceInMeters <= OPEN_CHEST_RADIUS_METERS) {
            Toast.makeText(this, "Bạn đã đủ gần (" + (int) distanceInMeters + "m). Mở rương!", Toast.LENGTH_SHORT).show();

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
        if (completionListener != null) {
            completionListener.remove();
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