package com.example.btlandr.fragment;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btlandr.R;
import com.example.btlandr.activity.EventDetailActivity;
import com.example.btlandr.activity.PersonalTaskActivity;
import com.example.btlandr.adapter.EventAdapter;
import com.example.btlandr.model.Event;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllGroupEventsFragment extends Fragment {

    private RecyclerView recyclerOngoing, recyclerUpcoming, recyclerPast;
    private EventAdapter adapterOngoing, adapterUpcoming, adapterPast;

    // UI Components cho sections và counters
    private LinearLayout ongoingSection, upcomingSection, pastSection, emptyState, layoutRange;
    private TextView tvEventCount, tvOngoingCount, tvUpcomingCount, tvPastCount, filterType;

    private Button btnStartDate, btnEndDate;
    private final List<Event> ongoingEvents = new ArrayList<>();
    private final List<Event> upcomingEvents = new ArrayList<>();
    private final List<Event> pastEvents = new ArrayList<>();

    private FirebaseFirestore db;
    private String uid;

    // Lưu listener registration để hủy khi fragment bị destroy
    private final List<ListenerRegistration> taskListeners = new ArrayList<>();

    // Lưu tất cả tasks từ các group để tránh bị xóa mất
    private final Map<String, List<Event>> groupTasksMap = new HashMap<>();

    private PersonalTaskActivity.FilterType currentFilter = PersonalTaskActivity.FilterType.ALL;

    private long rangeStart = 0;
    private long rangeEnd = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_all_group_events, container, false);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerOngoing = view.findViewById(R.id.recyclerOngoing);
        recyclerUpcoming = view.findViewById(R.id.recyclerUpcoming);
        recyclerPast = view.findViewById(R.id.recyclerPast);
        layoutRange = view.findViewById(R.id.layoutRange);

        // 🔹 Ánh xạ sections và counters
        ongoingSection = view.findViewById(R.id.ongoingSection);
        upcomingSection = view.findViewById(R.id.upcomingSection);
        pastSection = view.findViewById(R.id.pastSection);
        emptyState = view.findViewById(R.id.emptyState);

        tvEventCount = view.findViewById(R.id.tvEventCount);
        tvOngoingCount = view.findViewById(R.id.tvOngoingCount);
        tvUpcomingCount = view.findViewById(R.id.tvUpcomingCount);
        tvPastCount = view.findViewById(R.id.tvPastCount);
        filterType = view.findViewById(R.id.filterType);

        btnEndDate = view.findViewById(R.id.btnEndDate);
        btnStartDate = view.findViewById(R.id.btnStartDate);

        recyclerOngoing.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerUpcoming.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerPast.setLayoutManager(new LinearLayoutManager(getContext()));

        layoutRange.setVisibility(View.GONE);
        updateFilterDisplay(currentFilter);

        // 🔹 Ánh xạ nút filter
        ImageView btnFilter = view.findViewById(R.id.btnFilter);
        btnFilter.setOnClickListener(v -> showFilterBottomSheet());
        btnStartDate.setOnClickListener(v -> showDatePicker(true));
        btnEndDate.setOnClickListener(v -> showDatePicker(false));

        adapterOngoing = new EventAdapter(ongoingEvents, listener);
        adapterUpcoming = new EventAdapter(upcomingEvents, listener);
        adapterPast = new EventAdapter(pastEvents, listener);

        recyclerOngoing.setAdapter(adapterOngoing);
        recyclerUpcoming.setAdapter(adapterUpcoming);
        recyclerPast.setAdapter(adapterPast);

        loadAllGroupEventsRealtime();

        return view;
    }

    private final EventAdapter.OnEventActionListener listener = new EventAdapter.OnEventActionListener() {
        @Override
        public void onDelete(String eventId) {
            Toast.makeText(getContext(), "Không thể xóa từ trang tổng hợp", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onDetail(Event event) {
            Intent i = new Intent(getContext(), EventDetailActivity.class);
            i.putExtra("title", event.getTitle());
            i.putExtra("note", event.getNote());
            i.putExtra("start", event.getStartTime());
            i.putExtra("end", event.getEndTime());
            i.putExtra("category", event.getCategory());
            i.putExtra("important", event.isImportant());
            startActivity(i);
        }
    };

    private boolean matchFilter(Event e, PersonalTaskActivity.FilterType filterType) {
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
                if (rangeStart == 0 || rangeEnd == 0)
                    return false;
                return start <= rangeEnd && end >= rangeStart;

            case DAY:
                if (isOngoing)
                    return true;
                return cal.get(Calendar.YEAR) == eventCal.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == eventCal.get(Calendar.DAY_OF_YEAR);

            case WEEK:
                if (isOngoing)
                    return true;
                return cal.get(Calendar.YEAR) == eventCal.get(Calendar.YEAR) &&
                        cal.get(Calendar.WEEK_OF_YEAR) == eventCal.get(Calendar.WEEK_OF_YEAR);

            case MONTH:
                if (isOngoing)
                    return true;
                return cal.get(Calendar.YEAR) == eventCal.get(Calendar.YEAR) &&
                        cal.get(Calendar.MONTH) == eventCal.get(Calendar.MONTH);

            case YEAR:
                if (isOngoing)
                    return true;
                return cal.get(Calendar.YEAR) == eventCal.get(Calendar.YEAR);

            case ALL:
            default:
                return true;
        }
    }

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

    private void loadAllGroupEventsRealtime() {
        // Lắng nghe realtime các nhóm mà user tham gia
        db.collection("Groups")
                .whereArrayContains("members", uid)
                .addSnapshotListener((groupSnapshots, e) -> {
                    if (e != null) {
                        Log.e("Firestore", "Lỗi tải nhóm: ", e);
                        return;
                    }

                    // Xóa listener cũ để tránh trùng
                    for (ListenerRegistration reg : taskListeners) {
                        reg.remove();
                    }
                    taskListeners.clear();
                    groupTasksMap.clear();

                    if (groupSnapshots != null && !groupSnapshots.isEmpty()) {
                        for (QueryDocumentSnapshot groupDoc : groupSnapshots) {
                            String groupId = groupDoc.getId();
                            String groupName = groupDoc.getString("name"); // ✅ Lấy tên nhóm

                            // Lắng nghe realtime các task của từng nhóm
                            ListenerRegistration reg = db.collection("Groups")
                                    .document(groupId)
                                    .collection("tasks")
                                    .orderBy("startTime", Query.Direction.ASCENDING)
                                    .addSnapshotListener((taskSnapshots, taskError) -> {
                                        if (taskError != null) {
                                            Log.e("Firestore", "Lỗi tải task nhóm: ", taskError);
                                            return;
                                        }

                                        // ✅ Lưu tasks của group này vào map
                                        List<Event> tasksOfThisGroup = new ArrayList<>();
                                        if (taskSnapshots != null) {
                                            for (QueryDocumentSnapshot taskDoc : taskSnapshots) {
                                                Event eTask = taskDoc.toObject(Event.class);
                                                eTask.setId(taskDoc.getId());
                                                eTask.setCategory(groupName); // ✅ Set tên nhóm vào category
                                                tasksOfThisGroup.add(eTask);
                                            }
                                        }
                                        groupTasksMap.put(groupId, tasksOfThisGroup);

                                        // ✅ Gộp tất cả tasks từ các groups và phân loại
                                        updateAllLists();
                                    });

                            taskListeners.add(reg);
                        }
                    } else {
                        // Không có group nào
                        updateAllLists();
                    }
                });
    }

    // ✅ Phương thức gộp và phân loại tất cả tasks
    private void updateAllLists() {
        ongoingEvents.clear();
        upcomingEvents.clear();
        pastEvents.clear();

        long now = System.currentTimeMillis();

        // Gộp tất cả tasks từ các groups
        for (List<Event> tasks : groupTasksMap.values()) {
            for (Event event : tasks) {
                if (!matchFilter(event, currentFilter))
                    continue;

                if (event.getEndTime() < now) {
                    pastEvents.add(event);
                } else if (event.getStartTime() <= now && event.getEndTime() >= now) {
                    ongoingEvents.add(event);
                } else {
                    upcomingEvents.add(event);
                }
            }
        }

        // Cập nhật adapter
        if (adapterOngoing != null)
            adapterOngoing.notifyDataSetChanged();
        if (adapterUpcoming != null)
            adapterUpcoming.notifyDataSetChanged();
        if (adapterPast != null)
            adapterPast.notifyDataSetChanged();

        updateUI(ongoingEvents.size(), upcomingEvents.size(), pastEvents.size());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (ListenerRegistration reg : taskListeners) {
            reg.remove();
        }
        taskListeners.clear();
        groupTasksMap.clear();
    }

    private void updateFilterDisplay(PersonalTaskActivity.FilterType type) {
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

    private void applyFilter(PersonalTaskActivity.FilterType type, BottomSheetDialog dialog) {
        currentFilter = type;
        updateAllLists();
        dialog.dismiss();

        if (type == PersonalTaskActivity.FilterType.RANGE) {
            layoutRange.setVisibility(View.VISIBLE);
        } else {
            layoutRange.setVisibility(View.GONE);
        }

        updateFilterDisplay(currentFilter);
    }

    private void showFilterBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        View view = getLayoutInflater().inflate(R.layout.bottom_filter_event, null);

        view.findViewById(R.id.filterToday)
                .setOnClickListener(v -> applyFilter(PersonalTaskActivity.FilterType.DAY, dialog));
        view.findViewById(R.id.filterWeek)
                .setOnClickListener(v -> applyFilter(PersonalTaskActivity.FilterType.WEEK, dialog));
        view.findViewById(R.id.filterMonth)
                .setOnClickListener(v -> applyFilter(PersonalTaskActivity.FilterType.MONTH, dialog));
        view.findViewById(R.id.filterYear)
                .setOnClickListener(v -> applyFilter(PersonalTaskActivity.FilterType.YEAR, dialog));
        view.findViewById(R.id.filterAll)
                .setOnClickListener(v -> applyFilter(PersonalTaskActivity.FilterType.ALL, dialog));
        view.findViewById(R.id.filterRange)
                .setOnClickListener(v -> applyFilter(PersonalTaskActivity.FilterType.RANGE, dialog));

        dialog.setContentView(view);
        dialog.show();
    }

    private void showDatePicker(boolean isStart) {
        Calendar cal = Calendar.getInstance();

        DatePickerDialog dialog = new DatePickerDialog(
                getContext(),
                (dp, y, m, d) -> {
                    Calendar chosen = Calendar.getInstance();
                    chosen.set(y, m, d, 0, 0, 0);

                    if (isStart) {
                        rangeStart = chosen.getTimeInMillis();
                        btnStartDate.setText("Bắt đầu: " + d + "/" + (m + 1) + "/" + y);
                    } else {
                        chosen.set(y, m, d, 23, 59, 59);
                        rangeEnd = chosen.getTimeInMillis();
                        btnEndDate.setText("Kết thúc: " + d + "/" + (m + 1) + "/" + y);
                    }

                    updateAllLists();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));

        dialog.show();
    }
}