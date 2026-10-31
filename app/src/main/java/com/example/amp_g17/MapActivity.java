
package com.example.amp_g17;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PatternItem;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MapActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private static final int LOCATION_REQUEST = 100;
    private static final float UNLOCK_DISTANCE = 50f;

    // Google Maps
    private GoogleMap googleMap;

    // GPS
    private FusedLocationProviderClient locationClient;
    private LocationCallback locationCallback;
    private Location playerLocation;

    // Giao diện
    private LinearLayout chestPanel;
    private TextView tvChestName;
    private TextView tvDistance;

    private Button btnHunt;
    private Button btnChallenge;
    private Button btnClose;
    private Button btnMapTab;
    private Button btnRank;
    private Button btnLogout;

    // Rương được chọn
    private Marker selectedChest;
    private boolean isHunting = false;
    private boolean chestsCreated = false;
    private boolean locationPermissionRequested = false;

    // Các đối tượng chỉ đường
    private Polyline directionLine;
    private Marker directionArrow;
    private Circle activationCircle;

    private BitmapDescriptor arrowIcon;

    // Dữ liệu rương mẫu
    private static class ChestInfo {
        String code;
        String name;
        String rarity;

        ChestInfo(String code, String name, String rarity) {
            this.code = code;
            this.name = name;
            this.rarity = rarity;
        }
    }

    //  KHỞI TẠO ACTIVITY

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
        initLocation();
        setupButtons();
        initGoogleMap();
    }

    private void initViews() {

        chestPanel = findViewById(R.id.chestPanel);
        tvChestName = findViewById(R.id.tvChestName);
        tvDistance = findViewById(R.id.tvDistance);

        btnHunt = findViewById(R.id.btnHunt);
        btnChallenge = findViewById(R.id.btnChallenge);
        btnClose = findViewById(R.id.btnClose);

        btnMapTab = findViewById(R.id.btnMapTab);
        btnRank = findViewById(R.id.btnRank);
        btnLogout = findViewById(R.id.btnLogout);

        chestPanel.setVisibility(View.GONE);
    }

    private void initGoogleMap() {

        SupportMapFragment mapFragment =
                (SupportMapFragment)
                        getSupportFragmentManager()
                                .findFragmentById(R.id.map);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    //XỬ LÝ CÁC NÚT


    private void setupButtons() {

        // Đăng xuất khỏi hệ thống (FR-19 & Step 8)
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            Toast.makeText(MapActivity.this, "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(MapActivity.this, StartActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Bắt đầu săn rương
        btnHunt.setOnClickListener(v -> {

            if (selectedChest == null) {
                return;
            }

            if (playerLocation == null) {
                Toast.makeText(
                        this,
                        "Chưa xác định được GPS",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            isHunting = true;

            btnHunt.setVisibility(View.GONE);
            btnChallenge.setVisibility(View.VISIBLE);
            btnChallenge.setEnabled(false);

            updateDistanceAndDirection();
        });

        // Mở thử thách
        btnChallenge.setOnClickListener(v -> {

            if (!isHunting
                    || selectedChest == null
                    || playerLocation == null) {
                return;
            }

            LatLng chest = selectedChest.getPosition();

            float[] results = new float[1];

            Location.distanceBetween(
                    playerLocation.getLatitude(),
                    playerLocation.getLongitude(),
                    chest.latitude,
                    chest.longitude,
                    results
            );

            if (results[0] > UNLOCK_DISTANCE) {
                Toast.makeText(
                        this,
                        "Bạn chưa đủ gần kho báu!",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            ChestInfo info =
                    (ChestInfo) selectedChest.getTag();

            if (info == null) {
                return;
            }

            Intent intent = new Intent(
                    MapActivity.this,
                    ChallengeActivity.class
            );

            intent.putExtra(
                    "EXTRA_TREASURE_TITLE",
                    info.name
            );

            intent.putExtra(
                    "EXTRA_TREASURE_RARITY",
                    info.rarity
            );

            startActivity(intent);
        });

        // Đóng bảng thông tin rương
        btnClose.setOnClickListener(v -> {
            closeChestPanel();
        });

        // Quay lại chế độ xem bản đồ
        btnMapTab.setOnClickListener(v -> {

            closeChestPanel();

            if (playerLocation != null && googleMap != null) {

                LatLng player = new LatLng(
                        playerLocation.getLatitude(),
                        playerLocation.getLongitude()
                );

                googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                                player, 16.5f
                        )
                );
            }
        });

        // Mở bảng xếp hạng có sẵn
        btnRank.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MapActivity.this,
                    RankActivity.class
            );

            startActivity(intent);
        });
    }

    //HIỂN THỊ GOOGLE MAPS

    @Override
    public void onMapReady(@NonNull GoogleMap map) {

        googleMap = map;

        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        googleMap.getUiSettings()
                .setZoomControlsEnabled(true);

        googleMap.getUiSettings()
                .setMyLocationButtonEnabled(true);

        googleMap.getUiSettings()
                .setCompassEnabled(true);

        // Vị trí tạm trước khi nhận GPS
        googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                        new LatLng(16.0, 106.0),
                        5f
                )
        );

        // Nhấn vào biểu tượng rương
        googleMap.setOnMarkerClickListener(marker -> {

            Object tag = marker.getTag();

            if (!(tag instanceof ChestInfo)) {
                return false;
            }

            selectChest(marker);

            return true;
        });

        startLocationUpdates();
    }

    // GPS - VỊ TRÍ NGƯỜI CHƠI

    private void initLocation() {

        locationClient =
                LocationServices
                        .getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(
                    @NonNull LocationResult result) {

                Location location =
                        result.getLastLocation();

                if (location != null) {
                    updatePlayerLocation(location);
                }
            }
        };
    }

    private boolean hasLocationPermission() {

        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {

        if (googleMap == null) {
            return;
        }

        // Kiểm tra quyền GPS
        if (!hasLocationPermission()) {

            if (!locationPermissionRequested) {

                locationPermissionRequested = true;

                ActivityCompat.requestPermissions(
                        this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        },
                        LOCATION_REQUEST
                );
            }

            return;
        }

        // Hiển thị chấm xanh GPS
        googleMap.setMyLocationEnabled(true);

        LocationRequest request =
                new LocationRequest.Builder(
                        Priority.PRIORITY_HIGH_ACCURACY,
                        3000L
                )
                        .setMinUpdateIntervalMillis(1500L)
                        .build();

        locationClient.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
        ).addOnFailureListener(e -> {

            Toast.makeText(
                    this,
                    "Không thể cập nhật vị trí GPS",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_REQUEST) {

            if (hasLocationPermission()) {
                startLocationUpdates();
            } else {

                Toast.makeText(
                        this,
                        "Cần quyền vị trí chính xác để săn rương",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    // Cập nhật GPS khi người chơi di chuyển
    private void updatePlayerLocation(Location location) {

        playerLocation = location;

        LatLng player = new LatLng(
                location.getLatitude(),
                location.getLongitude()
        );

        // Tạo rương một lần khi nhận GPS
        if (!chestsCreated && googleMap != null) {

            createChests(player);

            chestsCreated = true;

            googleMap.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                            player, 16.5f
                    )
            );
        }

        // Nếu đã chọn rương thì tính lại khoảng cách
        if (selectedChest != null) {
            updateDistanceAndDirection();
        }
    }


    //TẠO 4 RƯƠNG KHO BÁU ( random tạo sau)

    private void createChests(LatLng player) {

        addChest(
                offsetLocation(player, 140, 90),
                new ChestInfo("K1", "Kho báu 01", "Rare")
        );

        addChest(
                offsetLocation(player, 230, -170),
                new ChestInfo("K2", "Kho báu 02", "Common")
        );

        addChest(
                offsetLocation(player, -170, 130),
                new ChestInfo("K3", "Kho báu 03", "Epic")
        );

        addChest(
                offsetLocation(player, -260, -180),
                new ChestInfo("K4", "Kho báu 04", "Legendary")
        );
    }

    // Tạo tọa độ cách vị trí hiện tại một số mét
    // north: số mét theo hướng Bắc
    // east: số mét theo hướng Đông
    private LatLng offsetLocation(
            LatLng origin,
            double north,
            double east) {

        double latitude =
                origin.latitude + north / 111320.0;

        double longitude =
                origin.longitude
                        + east / (
                        111320.0
                                * Math.cos(
                                Math.toRadians(origin.latitude)
                        )
                );

        return new LatLng(latitude, longitude);
    }

    private void addChest(
            LatLng location,
            ChestInfo info) {

        Marker marker = googleMap.addMarker(
                new MarkerOptions()
                        .position(location)
                        .title(info.name)
                        .icon(createChestIcon(info.code))
        );

        if (marker != null) {
            marker.setTag(info);
        }
    }

    // Vẽ biểu tượng tròn có chữ K1, K2
    private BitmapDescriptor createChestIcon(String code) {

        Bitmap bitmap = Bitmap.createBitmap(
                96,
                96,
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(
                Paint.ANTI_ALIAS_FLAG
        );

        // Nền trắng
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);

        canvas.drawCircle(48, 48, 38, paint);

        // Viền đen
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);

        canvas.drawCircle(48, 48, 38, paint);

        // Chữ K1...
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(30);
        paint.setFakeBoldText(true);

        Paint.FontMetrics fm = paint.getFontMetrics();
        float baseline =
                48 - (fm.ascent + fm.descent) / 2;

        canvas.drawText(
                code,
                48,
                baseline,
                paint
        );

        return BitmapDescriptorFactory
                .fromBitmap(bitmap);
    }

    //CHỌN RƯƠNG

    private void selectChest(Marker marker) {

        clearNavigation();

        selectedChest = marker;
        isHunting = false;

        ChestInfo info =
                (ChestInfo) marker.getTag();

        if (info == null) {
            return;
        }

        tvChestName.setText(info.name);

        chestPanel.setVisibility(View.VISIBLE);

        btnHunt.setVisibility(View.VISIBLE);

        btnChallenge.setVisibility(View.GONE);
        btnChallenge.setEnabled(false);

        // Chừa vùng bản đồ cho bảng thông tin
        chestPanel.post(() -> {

            if (googleMap != null) {

                googleMap.setPadding(
                        0,
                        0,
                        0,
                        chestPanel.getHeight()
                );

                googleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                                marker.getPosition(),
                                16.5f
                        )
                );
            }
        });

        updateDistanceAndDirection();
    }

    private void closeChestPanel() {

        chestPanel.setVisibility(View.GONE);

        selectedChest = null;
        isHunting = false;

        clearNavigation();

        if (googleMap != null) {
            googleMap.setPadding(0, 0, 0, 0);
        }
    }

    // 7. TÍNH KHOẢNG CÁCH VÀ CHỈ ĐƯỜNG

    private void updateDistanceAndDirection() {

        if (selectedChest == null) {
            return;
        }

        if (playerLocation == null) {

            tvDistance.setText(
                    "Đang xác định GPS..."
            );

            btnChallenge.setEnabled(false);

            return;
        }

        LatLng player = new LatLng(
                playerLocation.getLatitude(),
                playerLocation.getLongitude()
        );

        LatLng chest =
                selectedChest.getPosition();

        float[] results = new float[3];

        Location.distanceBetween(
                player.latitude,
                player.longitude,
                chest.latitude,
                chest.longitude,
                results
        );

        float distance = results[0];

        float bearing =
                (results[1] + 360f) % 360f;

        // Hiển thị khoảng cách
        tvDistance.setText(
                String.format(
                        Locale.getDefault(),
                        "%.0f m",
                        distance
                )
        );

        // Chưa bắt đầu săn thì không vẽ đường
        if (!isHunting) {
            return;
        }

        // Hiển thị đường chỉ hướng
        drawDirection(player, chest, bearing);

        // Vòng tròn kích hoạt bán kính 50m
        drawActivationCircle(chest, distance);

        // Kiểm tra điều kiện mở thử thách
        if (distance <= UNLOCK_DISTANCE) {

            btnChallenge.setEnabled(true);

            btnChallenge.setText(
                    "MỞ THỬ THÁCH"
            );

        } else {

            btnChallenge.setEnabled(false);

            btnChallenge.setText(
                    "THỬ THÁCH CHƯA MỞ"
            );
        }
    }
    // ĐƯỜNG NÉT ĐỨT VÀ MŨI TÊN

    private void drawDirection(
            LatLng player,
            LatLng chest,
            float bearing) {

        List<PatternItem> pattern =
                Arrays.asList(
                        new Dash(20f),
                        new Gap(12f)
                );

        // Đường nét đứt nối người chơi và rương
        if (directionLine == null) {

            directionLine = googleMap.addPolyline(
                    new PolylineOptions()
                            .add(player, chest)
                            .width(7f)
                            .color(Color.DKGRAY)
                            .pattern(pattern)
                            .geodesic(true)
            );

        } else {

            directionLine.setPoints(
                    Arrays.asList(player, chest)
            );
        }

        // Mũi tên ở giữa đường nối
        LatLng midpoint = new LatLng(
                (player.latitude + chest.latitude) / 2,
                (player.longitude + chest.longitude) / 2
        );

        if (arrowIcon == null) {
            arrowIcon = createArrowIcon();
        }

        if (directionArrow == null) {

            directionArrow = googleMap.addMarker(
                    new MarkerOptions()
                            .position(midpoint)
                            .icon(arrowIcon)
                            .anchor(0.5f, 0.5f)
                            .flat(true)
                            .rotation(bearing)
            );

        } else {

            directionArrow.setPosition(midpoint);
            directionArrow.setRotation(bearing);
        }
    }

    // Tạo hình mũi tên hướng Bắc
    private BitmapDescriptor createArrowIcon() {

        Bitmap bitmap = Bitmap.createBitmap(
                64,
                64,
                Bitmap.Config.ARGB_8888
        );

        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint(
                Paint.ANTI_ALIAS_FLAG
        );

        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.FILL);

        Path path = new Path();

        path.moveTo(32, 4);
        path.lineTo(52, 56);
        path.lineTo(32, 42);
        path.lineTo(12, 56);
        path.close();

        canvas.drawPath(path, paint);

        return BitmapDescriptorFactory
                .fromBitmap(bitmap);
    }

    // VÒNG TRÒN KÍCH HOẠT 50 MÉT

    private void drawActivationCircle(
            LatLng chest,
            float distance) {

        if (activationCircle == null) {

            activationCircle = googleMap.addCircle(
                    new CircleOptions()
                            .center(chest)
                            .radius(UNLOCK_DISTANCE)
                            .strokeColor(Color.GRAY)
                            .strokeWidth(3f)
                            .fillColor(
                                    Color.argb(
                                            20, 80, 80, 80
                                    )
                            )
            );

        } else {

            activationCircle.setCenter(chest);
        }

        // Hiện vòng tròn khi tiến gần rương
        activationCircle.setVisible(
                distance <= 120f
        );
    }


    // XÓA ĐƯỜNG CHỈ HƯỚNG


    private void clearNavigation() {

        if (directionLine != null) {
            directionLine.remove();
            directionLine = null;
        }

        if (directionArrow != null) {
            directionArrow.remove();
            directionArrow = null;
        }

        if (activationCircle != null) {
            activationCircle.remove();
            activationCircle = null;
        }
    }


    // ACTIVITY


    @Override
    protected void onResume() {
        super.onResume();

        if (googleMap != null) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (locationClient != null
                && locationCallback != null) {

            locationClient.removeLocationUpdates(
                    locationCallback
            );
        }
    }

    @Override
    protected void onDestroy() {
        clearNavigation();
        super.onDestroy();
    }
}
