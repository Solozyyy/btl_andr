package com.example.btlandr.fragment;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btlandr.R;
import com.example.btlandr.activity.GroupDetailActivity;
import com.example.btlandr.adapter.GroupAdapter;
import com.example.btlandr.model.Group;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class MyManagedGroupsFragment extends Fragment {

    private RecyclerView recyclerView;
    private GroupAdapter adapter;
    private final List<Group> managedGroups = new ArrayList<>();

    private FirebaseFirestore db;
    private String uid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_my_managed_groups, container, false);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerView = view.findViewById(R.id.recyclerManagedGroups);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new GroupAdapter(managedGroups, group -> {
            Intent i = new Intent(getContext(), GroupDetailActivity.class);
            i.putExtra("groupId", group.getId());
            i.putExtra("groupName", group.getName());
            i.putExtra("adminId", group.getAdminId());
            i.putExtra("adminEmail", group.getAdminEmail());
            startActivity(i);
        });

        recyclerView.setAdapter(adapter);

        loadManagedGroups();

        FloatingActionButton fabAddGroup = view.findViewById(R.id.fabAddGroup);
        fabAddGroup.setOnClickListener(v -> showCreateGroupDialog());

        return view;
    }

    private void loadManagedGroups() {
        db.collection("Groups")
                .whereEqualTo("adminId", uid)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("Firestore", "Lỗi tải nhóm quản lý: ", e);
                        return;
                    }

                    managedGroups.clear();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Group g = doc.toObject(Group.class);
                            g.setId(doc.getId());
                            managedGroups.add(g);
                        }
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showCreateGroupDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Tạo nhóm mới");

        final EditText input = new EditText(getContext());
        input.setHint("Nhập tên nhóm");
        input.setPadding(32, 32, 32, 32);
        builder.setView(input);

        builder.setPositiveButton("Tạo", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(getContext(), "Vui lòng nhập tên nhóm", Toast.LENGTH_SHORT).show();
                return;
            }

            String email = FirebaseAuth.getInstance().getCurrentUser().getEmail();

            Group group = new Group();
            group.setName(name);
            group.setAdminId(uid);
            group.setAdminEmail(email);
            group.setMembers(new ArrayList<>());
            group.getMembers().add(uid);
            group.setCreatedAt(System.currentTimeMillis());

            db.collection("Groups")
                    .add(group)
                    .addOnSuccessListener(
                            doc -> Toast.makeText(getContext(), "Tạo nhóm thành công!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(
                            e -> Toast.makeText(getContext(), "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
        });

        builder.setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}
