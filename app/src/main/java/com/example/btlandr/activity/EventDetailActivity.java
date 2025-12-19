package com.example.btlandr.activity;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.btlandr.R;
import com.example.btlandr.model.Event;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class EventDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        // Setup Toolbar

        // Ánh xạ views
        TextView titleText = findViewById(R.id.detailTitle);
        TextView noteText = findViewById(R.id.detailNote);
        TextView categoryText = findViewById(R.id.detailCategory);
        TextView startTimeText = findViewById(R.id.detailStartTime);
        TextView endTimeText = findViewById(R.id.detailEndTime);
        TextView durationText = findViewById(R.id.detailDuration);
        MaterialCardView noteCard = findViewById(R.id.noteCard);
        ImageView importantIcon = findViewById(R.id.importantIcon); // ⭐ Thêm icon
        TextView importantText = findViewById(R.id.importantText); // ⭐ Thêm text
        MaterialCardView importantCard = findViewById(R.id.importantCard); // ⭐ Thêm card
        MaterialCardView cardCategory = findViewById(R.id.cardCategory);

        // Lấy dữ liệu từ Intent
        String title = getIntent().getStringExtra("title");
        String note = getIntent().getStringExtra("note");
        long start = getIntent().getLongExtra("start", 0);
        long end = getIntent().getLongExtra("end", 0);
        String category = getIntent().getStringExtra("category");
        String groupId = getIntent().getStringExtra("groupId"); // Lấy groupId nếu có
        boolean important = getIntent().getBooleanExtra("important", false); // ⭐ Lấy important

        // Format thời gian
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault());
        String startFormatted = fmt.format(new Date(start));
        String endFormatted = fmt.format(new Date(end));

        // Tính thời lượng
        long durationMillis = end - start;
        String durationFormatted = formatDuration(durationMillis);

        // Hiển thị dữ liệu
        titleText.setText(title);
        categoryText.setText(category);
        startTimeText.setText(startFormatted);
        endTimeText.setText(endFormatted);
        durationText.setText("Thời lượng: " + durationFormatted);

        // ⭐ Xử lý hiển thị important badge
        if (important) {
            importantCard.setVisibility(View.VISIBLE);
        } else {
            importantCard.setVisibility(View.GONE);
        }

        // Xử lý note (ẩn card nếu không có ghi chú)
        if (note == null || note.trim().isEmpty()) {
            noteCard.setVisibility(View.GONE);
        } else {
            noteText.setText(note);
            noteCard.setVisibility(View.VISIBLE);
        }

        // Ẩn/hiện navigation nhóm bằng card danh mục
        if (groupId != null && !groupId.isEmpty()) {
            cardCategory.setClickable(true);
            cardCategory.setFocusable(true);
            cardCategory.setOnClickListener(v -> {
                android.content.Intent i = new android.content.Intent(this, GroupDetailActivity.class);
                i.putExtra("groupId", groupId);
                startActivity(i);
            });
        } else {
            cardCategory.setClickable(false);
        }
    }

    /**
     * Format thời lượng từ milliseconds sang dạng dễ đọc
     * VD: "2 giờ 30 phút", "45 phút", "1 ngày 3 giờ"
     */
    private String formatDuration(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        long hours = TimeUnit.MILLISECONDS.toHours(millis) % 24;
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60;

        StringBuilder result = new StringBuilder();

        if (days > 0) {
            result.append(days).append(" ngày");
        }

        if (hours > 0) {
            if (result.length() > 0)
                result.append(" ");
            result.append(hours).append(" giờ");
        }

        if (minutes > 0) {
            if (result.length() > 0)
                result.append(" ");
            result.append(minutes).append(" phút");
        }

        // Nếu dưới 1 phút
        if (result.length() == 0) {
            result.append("Dưới 1 phút");
        }

        return result.toString();
    }
}