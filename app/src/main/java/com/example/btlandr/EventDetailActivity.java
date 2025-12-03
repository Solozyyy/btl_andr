package com.example.btlandr;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
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
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Ánh xạ views
        TextView titleText = findViewById(R.id.detailTitle);
        TextView noteText = findViewById(R.id.detailNote);
        TextView categoryText = findViewById(R.id.detailCategory);
        TextView startTimeText = findViewById(R.id.detailStartTime);
        TextView endTimeText = findViewById(R.id.detailEndTime);
        TextView durationText = findViewById(R.id.detailDuration);
        MaterialCardView noteCard = findViewById(R.id.noteCard);
        ImageView importantIcon = findViewById(R.id.importantIcon); // ⭐ Thêm icon
        TextView importantText = findViewById(R.id.importantText);   // ⭐ Thêm text
        MaterialCardView importantCard = findViewById(R.id.importantCard); // ⭐ Thêm card

        // Lấy dữ liệu từ Intent
        String title = getIntent().getStringExtra("title");
        String note = getIntent().getStringExtra("note");
        long start = getIntent().getLongExtra("start", 0);
        long end = getIntent().getLongExtra("end", 0);
        String category = getIntent().getStringExtra("category");
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
            if (result.length() > 0) result.append(" ");
            result.append(hours).append(" giờ");
        }

        if (minutes > 0) {
            if (result.length() > 0) result.append(" ");
            result.append(minutes).append(" phút");
        }

        // Nếu dưới 1 phút
        if (result.length() == 0) {
            result.append("Dưới 1 phút");
        }

        return result.toString();
    }
}