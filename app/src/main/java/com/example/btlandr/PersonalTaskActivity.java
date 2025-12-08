package com.example.btlandr;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class PersonalTaskActivity extends AppCompatActivity {

    public enum FilterType {
        
        DAY, WEEK, MONTH, YEAR, ALL, RANGE
    }
    private String tmp;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String uid;

    private RecyclerView recyclerOngoing, recyclerUpcoming, recyclerPast;
    private EventAdapter adapterOngoing, adapterUpcoming, adapterPast;

    // UI Components cho sections và counters
    private LinearLayout ongoingSection, upcomingSection, pastSection, emptyState, layoutRange;
    private TextView tvEventCount, tvOngoingCount, tvUpcomingCount, tvPastCount, filterType;

    private Button btnStartDate, btnEndDate;
    private FilterType currentFilter = FilterType.ALL;

    private long rangeStart = 0;
    private long rangeEnd = 0;

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
        layoutRange = findViewById(R.id.layoutRange);

        // 🔹 Ánh xạ sections và counters
        ongoingSection = findViewById(R.id.ongoingSection);
        upcomingSection = findViewById(R.id.upcomingSection);
        pastSection = findViewById(R.id.pastSection);
        emptyState = findViewById(R.id.emptyState);

        tvEventCount = findViewById(R.id.tvEventCount);
        tvOngoingCount = findViewById(R.id.tvOngoingCount);
        tvUpcomingCount = findViewById(R.id.tvUpcomingCount);
        tvPastCount = findViewById(R.id.tvPastCount);
        filterType = findViewById(R.id.filterType);

        btnEndDate = findViewById(R.id.btnEndDate);
        btnStartDate = findViewById(R.id.btnStartDate);

        recyclerOngoing.setLayoutManager(new LinearLayoutManager(this));
        recyclerUpcoming.setLayoutManager(new LinearLayoutManager(this));
        recyclerPast.setLayoutManager(new LinearLayoutManager(this));

        layoutRange.setVisibility(View.GONE);
        updateFilterDisplay(currentFilter);

        // 🔹 Ánh xạ nút filter
        ImageView btnFilter = findViewById(R.id.btnFilter);
        btnFilter.setOnClickListener(v -> showFilterBottomSheet());
        btnStartDate.setOnClickListener(v -> showDatePicker(true));
        btnEndDate.setOnClickListener(v -> showDatePicker(false));

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

    // 🧩 Hàm lọc sự kiện
    private boolean matchFilter(Event e, FilterType filterType) {
        long now = System.currentTimeMillis();

        // Lấy start - end của event
        long start = e.getStartTime();
        long end = e.getEndTime();

        // Kiểm tra sự kiện đang diễn ra
        boolean isOngoing = (now >= start && now <= end);

        // Lấy thời điểm start
        Calendar eventCal = Calendar.getInstance();
        eventCal.setTimeInMillis(start);

        // Lấy thời gian hiện tại
        Calendar cal = Calendar.getInstance();

        switch (filterType) {

            case RANGE:
                if (rangeStart == 0 || rangeEnd == 0) return false;
                return start <= rangeEnd && end >= rangeStart;

            case DAY:
                if (isOngoing) return true;
                return cal.get(Calendar.YEAR) == eventCal.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == eventCal.get(Calendar.DAY_OF_YEAR);

            case WEEK:
                if (isOngoing) return true;
                return cal.get(Calendar.YEAR) == eventCal.get(Calendar.YEAR) &&
                        cal.get(Calendar.WEEK_OF_YEAR) == eventCal.get(Calendar.WEEK_OF_YEAR);

            case MONTH:
                if (isOngoing) return true;
                return cal.get(Calendar.YEAR) == eventCal.get(Calendar.YEAR) &&
                        cal.get(Calendar.MONTH) == eventCal.get(Calendar.MONTH);

            case YEAR:
                if (isOngoing) return true;
                return cal.get(Calendar.YEAR) == eventCal.get(Calendar.YEAR);

            case ALL:
            default:
                return true;
        }
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

                            if (!matchFilter(e, currentFilter)) continue; // ⬅️ lọc ở đây

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

    private void updateFilterDisplay(FilterType type) {
        switch (type) {

            case DAY:
                filterType.setText("Hôm nay");
                break;

            case WEEK:
                filterType.setText("Tuần này");
                break;

            case MONTH:
                filterType.setText("Tháng này");
                break;

            case YEAR:
                filterType.setText("Năm nay");
                break;

            case ALL:
                filterType.setText("Tất cả");
                break;

            case RANGE:
                filterType.setText("Khoảng thời gian");
                break;

        }
    }
    private void applyFilter(FilterType type, BottomSheetDialog dialog) {
        currentFilter = type;
        loadEvents();
        dialog.dismiss();

        if (type == FilterType.RANGE) {
            layoutRange.setVisibility(View.VISIBLE);
        } else {
            layoutRange.setVisibility(View.GONE);
        }

        updateFilterDisplay(currentFilter);
    }
    private void showFilterBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_filter_event, null);

        view.findViewById(R.id.filterToday).setOnClickListener(v -> applyFilter(FilterType.DAY, dialog));
        view.findViewById(R.id.filterWeek).setOnClickListener(v -> applyFilter(FilterType.WEEK, dialog));
        view.findViewById(R.id.filterMonth).setOnClickListener(v -> applyFilter(FilterType.MONTH, dialog));
        view.findViewById(R.id.filterYear).setOnClickListener(v -> applyFilter(FilterType.YEAR, dialog));
        view.findViewById(R.id.filterAll).setOnClickListener(v -> applyFilter(FilterType.ALL, dialog));
        view.findViewById(R.id.filterRange).setOnClickListener(v -> applyFilter(FilterType.RANGE, dialog));

        dialog.setContentView(view);
        dialog.show();
    }

    private void showDatePicker(boolean isStart) {
        Calendar cal = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                this,
                (dp, y, m, d) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(y, m, d, 0, 0, 0);

                    if (isStart) {
                        rangeStart = chosen.getTimeInMillis();
                        btnStartDate.setText("Bắt đầu: " + d + "/" + (m+1) + "/" + y);
                    } else {
                        chosen.set(y, m, d, 23, 59, 59);
                        rangeEnd = chosen.getTimeInMillis();
                        btnEndDate.setText("Kết thúc: " + d + "/" + (m+1) + "/" + y);
                    }

                    loadEvents();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        );

        dialog.show();
    }
}