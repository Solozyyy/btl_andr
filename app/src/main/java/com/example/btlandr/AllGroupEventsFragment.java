package com.example.btlandr;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AllGroupEventsFragment extends Fragment {

    private RecyclerView recyclerOngoing, recyclerUpcoming, recyclerPast;
    private EventAdapter adapterOngoing, adapterUpcoming, adapterPast;
    private final List<Event> ongoingList = new ArrayList<>();
    private final List<Event> upcomingList = new ArrayList<>();
    private final List<Event> pastList = new ArrayList<>();

    private FirebaseFirestore db;
    private String uid;

    // Lưu listener registration để hủy khi fragment bị destroy
    private final List<ListenerRegistration> taskListeners = new ArrayList<>();

    // Lưu tất cả tasks từ các group để tránh bị xóa mất
    private final Map<String, List<Event>> groupTasksMap = new HashMap<>();

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

        recyclerOngoing.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerUpcoming.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerPast.setLayoutManager(new LinearLayoutManager(getContext()));

        adapterOngoing = new EventAdapter(ongoingList, listener);
        adapterUpcoming = new EventAdapter(upcomingList, listener);
        adapterPast = new EventAdapter(pastList, listener);

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
        ongoingList.clear();
        upcomingList.clear();
        pastList.clear();

        long now = System.currentTimeMillis();

        // Gộp tất cả tasks từ các groups
        for (List<Event> tasks : groupTasksMap.values()) {
            for (Event event : tasks) {
                if (event.getEndTime() < now) {
                    pastList.add(event);
                } else if (event.getStartTime() <= now && event.getEndTime() >= now) {
                    ongoingList.add(event);
                } else {
                    upcomingList.add(event);
                }
            }
        }

        // Cập nhật adapter
        if (adapterOngoing != null) adapterOngoing.notifyDataSetChanged();
        if (adapterUpcoming != null) adapterUpcoming.notifyDataSetChanged();
        if (adapterPast != null) adapterPast.notifyDataSetChanged();
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
}