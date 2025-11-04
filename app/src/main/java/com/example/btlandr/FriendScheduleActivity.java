package com.example.btlandr;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.widget.*;
import com.google.firebase.firestore.*;
import java.util.*;

public class FriendScheduleActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private List<Event> eventList = new ArrayList<>();
    private EditText uidInput;
    private Button loadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friend_schedule);

        db = FirebaseFirestore.getInstance();

        uidInput = findViewById(R.id.uidInput);
        loadButton = findViewById(R.id.loadButton);
        recyclerView = findViewById(R.id.friendRecycler);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EventAdapter(eventList, null);
        recyclerView.setAdapter(adapter);

        loadButton.setOnClickListener(v -> loadFriendSchedule(uidInput.getText().toString().trim()));
    }

    private void loadFriendSchedule(String uid) {
        db.collection("UserAccount").document(uid).collection("events")
                .orderBy("startTime", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) return;
                    eventList.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Event event = doc.toObject(Event.class);
                        eventList.add(event);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
