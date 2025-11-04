package com.example.btlandr;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.*;
import android.widget.*;
import android.content.Intent;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class PersonalTaskActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private String uid;

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private final List<Event> eventList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_task);

        // Khởi tạo Firestore và Auth
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        uid = auth.getCurrentUser().getUid();

        // Ánh xạ RecyclerView
        recyclerView = findViewById(R.id.eventRecycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Gắn adapter hiển thị danh sách sự kiện
        adapter = new EventAdapter(eventList, new EventAdapter.OnEventActionListener() {
            @Override
            public void onDelete(String eventId) {
                deleteEvent(eventId);
            }

            @Override
            public void onDetail(Event event) {
                Intent i = new Intent(PersonalTaskActivity.this, EventDetailActivity.class);
                i.putExtra("title", event.getTitle());
                i.putExtra("note", event.getNote());
                i.putExtra("start", event.getStartTime());
                i.putExtra("end", event.getEndTime());
                i.putExtra("category", event.getCategory());
                startActivity(i);
            }
        });

        recyclerView.setAdapter(adapter);

        // Load dữ liệu từ Firestore
        loadEvents();

        // Nút thêm sự kiện (FloatingActionButton)
        FloatingActionButton addEventButton = findViewById(R.id.addEventButton);
        addEventButton.setOnClickListener(v -> {
            Intent i = new Intent(PersonalTaskActivity.this, AddPersonalTaskActivity.class);
            startActivity(i);
        });
    }

    // Load danh sách task từ Firestore
    private void loadEvents() {
        db.collection("UserAccount").document(uid).collection("events")
                .orderBy("startTime", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    eventList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Event event = doc.toObject(Event.class);
                        event.setId(doc.getId());
                        eventList.add(event);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    // Xóa task
    private void deleteEvent(String eventId) {
        db.collection("UserAccount").document(uid).collection("events").document(eventId)
                .delete()
                .addOnSuccessListener(a -> Toast.makeText(this, "Đã xóa!", Toast.LENGTH_SHORT).show());
    }
}
