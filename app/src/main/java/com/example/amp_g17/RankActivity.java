package com.example.amp_g17;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

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
    @Override
    protected void onChildTitleChanged(Activity childActivity, CharSequence title) {
        super.onChildTitleChanged(childActivity, title);
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

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // load rank
        tvTop1Name.setText("Thợ Săn Rồng");
        tvTop1Score.setText("8.200");

        tvTop2Name.setText("Vua Kho Báu");
        tvTop2Score.setText("7.600");

        tvTop3Name.setText("Hải Tặc Bất Bại");
        tvTop3Score.setText("6.900");

        tvTop4Name.setText("Nguyễn Văn A");
        tvTop4Score.setText("5.400");

        tvTop5Name.setText("Trần Thị B");
        tvTop5Score.setText("4.800");

        tvTop6Name.setText("Lê Văn C");
        tvTop6Score.setText("4.100");

        tvTop7Name.setText("Phạm Hoàng D");
        tvTop7Score.setText("3.200");

        tvMyRank.setText("#4");
        tvMyPoints.setText("5.400 điểm");
    }
}
