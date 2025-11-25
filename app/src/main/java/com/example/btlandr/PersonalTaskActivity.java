package com.example.btlandr;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class PersonalTaskActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String uid;

    private RecyclerView recyclerOngoing, recyclerUpcoming, recyclerPast;
    private EventAdapter adapterOngoing, adapterUpcoming, adapterPast;

    // UI Components cho sections và counters
    private LinearLayout ongoingSection, upcomingSection, pastSection, emptyState;
    private TextView tvEventCount, tvOngoingCount, tvUpcomingCount, tvPastCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_task);

        // 🔹 Khởi tạo Firestore và Auth
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        uid = auth.getCurrentUser().getUid();

        // 🔹 Ánh xạ RecyclerView từ layout
        recyclerOngoing = findViewById(R.id.recyclerOngoing);
        recyclerUpcoming = findViewById(R.id.recyclerUpcoming);
        recyclerPast = findViewById(R.id.recyclerPast);

        // 🔹 Ánh xạ sections và counters
        ongoingSection = findViewById(R.id.ongoingSection);
        upcomingSection = findViewById(R.id.upcomingSection);
        pastSection = findViewById(R.id.pastSection);
        emptyState = findViewById(R.id.emptyState);

        tvEventCount = findViewById(R.id.tvEventCount);
        tvOngoingCount = findViewById(R.id.tvOngoingCount);
        tvUpcomingCount = findViewById(R.id.tvUpcomingCount);
        tvPastCount = findViewById(R.id.tvPastCount);

        recyclerOngoing.setLayoutManager(new LinearLayoutManager(this));
        recyclerUpcoming.setLayoutManager(new LinearLayoutManager(this));
        recyclerPast.setLayoutManager(new LinearLayoutManager(this));

        // 🔹 Gắn adapter cho mỗi danh sách
        adapterOngoing = new EventAdapter(new ArrayList<>(), new EventAdapter.OnEventActionListener() {
            @Override
            public void onDelete(String eventId) {
                deleteEvent(eventId);
            }

            @Override
            public void onDetail(Event event) {
                openDetail(event);
            }
        });

        adapterUpcoming = new EventAdapter(new ArrayList<>(), new EventAdapter.OnEventActionListener() {
            @Override
            public void onDelete(String eventId) {
                deleteEvent(eventId);
            }

            @Override
            public void onDetail(Event event) {
                openDetail(event);
            }
        });

        adapterPast = new EventAdapter(new ArrayList<>(), new EventAdapter.OnEventActionListener() {
            @Override
            public void onDelete(String eventId) {
                deleteEvent(eventId);
            }

            @Override
            public void onDetail(Event event) {
                openDetail(event);
            }
        });

        recyclerOngoing.setAdapter(adapterOngoing);
        recyclerUpcoming.setAdapter(adapterUpcoming);
        recyclerPast.setAdapter(adapterPast);

        // 🔹 Nút thêm sự kiện
        FloatingActionButton addEventButton = findViewById(R.id.addEventButton);
        addEventButton.setOnClickListener(v -> {
            Intent i = new Intent(PersonalTaskActivity.this, AddPersonalTaskActivity.class);
            startActivity(i);
        });

        // 🔹 Tải dữ liệu từ Firestore
        loadEvents();
    }

    // 🧩 Hàm mở chi tiết sự kiện
    private void openDetail(Event event) {
        Intent i = new Intent(PersonalTaskActivity.this, EventDetailActivity.class);
        i.putExtra("title", event.getTitle());
        i.putExtra("note", event.getNote());
        i.putExtra("start", event.getStartTime());
        i.putExtra("end", event.getEndTime());
        i.putExtra("category", event.getCategory());
        startActivity(i);
    }

    // 🧭 Load danh sách sự kiện từ Firestore và chia thành 3 nhóm
    private void loadEvents() {
        db.collection("UserAccount").document(uid).collection("events")
                .orderBy("startTime", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("Firestore", "Lỗi tải dữ liệu: ", error);
                        return;
                    }

                    List<Event> pastEvents = new ArrayList<>();
                    List<Event> ongoingEvents = new ArrayList<>();
                    List<Event> upcomingEvents = new ArrayList<>();

                    long now = System.currentTimeMillis();

                    if (value != null) {
                        for (QueryDocumentSnapshot doc : value) {
                            Event e = doc.toObject(Event.class);
                            e.setId(doc.getId());

                            long start = e.getStartTime();
                            long end = e.getEndTime();

                            if (end < now) {
                                pastEvents.add(e);
                            } else if (start <= now && end >= now) {
                                ongoingEvents.add(e);
                            } else {
                                upcomingEvents.add(e);
                            }
                        }

                        // 🔽 Sắp xếp từng nhóm theo thời gian bắt đầu
                        Comparator<Event> byStart = Comparator.comparingLong(Event::getStartTime);
                        Collections.sort(pastEvents, byStart);
                        Collections.sort(ongoingEvents, byStart);
                        Collections.sort(upcomingEvents, byStart);
                    }

                    // 🔹 Cập nhật adapter
                    adapterPast.setEventList(pastEvents);
                    adapterOngoing.setEventList(ongoingEvents);
                    adapterUpcoming.setEventList(upcomingEvents);

                    adapterPast.notifyDataSetChanged();
                    adapterOngoing.notifyDataSetChanged();
                    adapterUpcoming.notifyDataSetChanged();

                    // 🎨 Cập nhật UI (sections, counters, empty state)
                    updateUI(ongoingEvents.size(), upcomingEvents.size(), pastEvents.size());
                });
    }

    // 🎨 Cập nhật giao diện theo số lượng sự kiện
    private void updateUI(int ongoingCount, int upcomingCount, int pastCount) {
        int totalCount = ongoingCount + upcomingCount + pastCount;

        // Cập nhật tổng số sự kiện ở header
        tvEventCount.setText(totalCount + " sự kiện");

        // Hiển thị/ẩn empty state
        if (totalCount == 0) {
            emptyState.setVisibility(View.VISIBLE);
            ongoingSection.setVisibility(View.GONE);
            upcomingSection.setVisibility(View.GONE);
            pastSection.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);

            // Hiển thị/ẩn từng section
            if (ongoingCount > 0) {
                ongoingSection.setVisibility(View.VISIBLE);
                tvOngoingCount.setText(String.valueOf(ongoingCount));
            } else {
                ongoingSection.setVisibility(View.GONE);
            }

            if (upcomingCount > 0) {
                upcomingSection.setVisibility(View.VISIBLE);
                tvUpcomingCount.setText(String.valueOf(upcomingCount));
            } else {
                upcomingSection.setVisibility(View.GONE);
            }

            if (pastCount > 0) {
                pastSection.setVisibility(View.VISIBLE);
                tvPastCount.setText(String.valueOf(pastCount));
            } else {
                pastSection.setVisibility(View.GONE);
            }
        }
    }

    // 🗑️ Xóa task
    private void deleteEvent(String eventId) {
        db.collection("UserAccount").document(uid).collection("events").document(eventId)
                .delete()
                .addOnSuccessListener(a -> Toast.makeText(this, "Đã xóa!", Toast.LENGTH_SHORT).show());
    }
}