package com.example.amp_g17;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.List;
import java.util.Locale;

public class RankActivity extends AppCompatActivity {
    private ImageView btnBack;

    private TextView tvTop1Name, tvTop1Score;
    private TextView tvTop2Name, tvTop2Score;
    private TextView tvTop3Name, tvTop3Score;

    // Rank 4 - 7 Views
    private TextView tvTop4Name, tvTop4Score;
    private TextView tvTop5Name, tvTop5Score;
    private TextView tvTop6Name, tvTop6Score;
    private TextView tvTop7Name, tvTop7Score;

    // My Rank Views
    private TextView tvMyRank, tvMyPoints;

    private ListenerRegistration rankListener;
    private ListenerRegistration myRankListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rank);

        btnBack = findViewById(R.id.btnBack);

        tvTop1Name = findViewById(R.id.tvTop1Name);
        tvTop1Score = findViewById(R.id.tvTop1Score);
        tvTop2Name = findViewById(R.id.tvTop2Name);
        tvTop2Score = findViewById(R.id.tvTop2Score);
        tvTop3Name = findViewById(R.id.tvTop3Name);
        tvTop3Score = findViewById(R.id.tvTop3Score);
        tvTop4Name = findViewById(R.id.tvTop4Name);
        tvTop4Score = findViewById(R.id.tvTop4Score);
        tvTop5Name = findViewById(R.id.tvTop5Name);
        tvTop5Score = findViewById(R.id.tvTop5Score);
        tvTop6Name = findViewById(R.id.tvTop6Name);
        tvTop6Score = findViewById(R.id.tvTop6Score);
        tvTop7Name = findViewById(R.id.tvTop7Name);
        tvTop7Score = findViewById(R.id.tvTop7Score);

        // Thông tin người chơi hiện tại
        tvMyRank = findViewById(R.id.tvMyRank);
        tvMyPoints = findViewById(R.id.tvMyPoints);

        btnBack.setOnClickListener(v -> finish());

        // Đặt trạng thái chờ tải ban đầu
        setLoadingState();

        // Tải dữ liệu bảng xếp hạng từ Firebase
        loadRankDataFromFirebase();
    }

    private void setLoadingState() {
        TextView[] nameViews = {tvTop1Name, tvTop2Name, tvTop3Name, tvTop4Name, tvTop5Name, tvTop6Name, tvTop7Name};
        TextView[] scoreViews = {tvTop1Score, tvTop2Score, tvTop3Score, tvTop4Score, tvTop5Score, tvTop6Score, tvTop7Score};

        for (int i = 0; i < 7; i++) {
            nameViews[i].setText("Đang tải...");
            scoreViews[i].setText("--");
        }
        tvMyRank.setText("--");
        tvMyPoints.setText("--");
    }

    private void loadRankDataFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Sử dụng addSnapshotListener để đọc từ bộ nhớ tạm (Cache) tức thì, sau đó tự cập nhật từ Server
        rankListener = db.collection("users")
                .orderBy("totalScore", Query.Direction.DESCENDING)
                .limit(7)
                .addSnapshotListener(this, (queryDocumentSnapshots, error) -> {
                    if (error != null || queryDocumentSnapshots == null) return;
                    List<DocumentSnapshot> docs = queryDocumentSnapshots.getDocuments();

                    TextView[] nameViews = {tvTop1Name, tvTop2Name, tvTop3Name, tvTop4Name, tvTop5Name, tvTop6Name, tvTop7Name};
                    TextView[] scoreViews = {tvTop1Score, tvTop2Score, tvTop3Score, tvTop4Score, tvTop5Score, tvTop6Score, tvTop7Score};

                    boolean currentInTop7 = false;

                    for (int i = 0; i < 7; i++) {
                        if (i < docs.size()) {
                            DocumentSnapshot doc = docs.get(i);
                            String name = doc.getString("displayName");
                            Long scoreObj = doc.getLong("totalScore");
                            long score = scoreObj != null ? scoreObj : 0;

                            if (name == null || name.trim().isEmpty()) {
                                name = "Người chơi " + (i + 1);
                            }

                            nameViews[i].setText(name);
                            scoreViews[i].setText(String.format(Locale.getDefault(), "%,d", score));

                            if (currentUser != null && doc.getId().equals(currentUser.getUid())) {
                                currentInTop7 = true;
                                tvMyRank.setText("#" + (i + 1));
                                tvMyPoints.setText(String.format(Locale.getDefault(), "%,d điểm", score));
                            }
                        } else {
                            nameViews[i].setText("---");
                            scoreViews[i].setText("0");
                        }
                    }

                    if (currentUser != null && !currentInTop7) {
                        fetchMyRank(currentUser.getUid());
                    }
                });
    }

    private void fetchMyRank(String uid) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (myRankListener != null) {
            myRankListener.remove();
        }
        myRankListener = db.collection("users").document(uid).addSnapshotListener(this, (doc, error) -> {
            if (error != null || doc == null || !doc.exists()) return;

            Long scoreObj = doc.getLong("totalScore");
            long myScore = scoreObj != null ? scoreObj : 0;
            tvMyPoints.setText(String.format(Locale.getDefault(), "%,d điểm", myScore));

            db.collection("users")
                    .whereGreaterThan("totalScore", myScore)
                    .get()
                    .addOnSuccessListener(querySnapshots -> {
                        int rank = querySnapshots != null ? querySnapshots.size() + 1 : 1;
                        tvMyRank.setText("#" + rank);
                    });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (rankListener != null) {
            rankListener.remove();
        }
        if (myRankListener != null) {
            myRankListener.remove();
        }
    }
}

