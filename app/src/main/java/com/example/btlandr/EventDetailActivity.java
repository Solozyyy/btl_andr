package com.example.btlandr;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_detail);

        TextView titleText = findViewById(R.id.detailTitle);
        TextView noteText = findViewById(R.id.detailNote);
        TextView timeText = findViewById(R.id.detailTime);
        TextView categoryText = findViewById(R.id.detailCategory);

        String title = getIntent().getStringExtra("title");
        String note = getIntent().getStringExtra("note");
        long start = getIntent().getLongExtra("start", 0);
        long end = getIntent().getLongExtra("end", 0);
        String category = getIntent().getStringExtra("category");

        SimpleDateFormat fmt = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());
        String timeRange = fmt.format(new Date(start)) + " - " + fmt.format(new Date(end));

        titleText.setText(title);
        noteText.setText(note);
        timeText.setText(timeRange);
        categoryText.setText(category);
    }
}
