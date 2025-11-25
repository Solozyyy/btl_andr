package com.example.btlandr;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class InvitedGroupsFragment extends Fragment {

    private RecyclerView recyclerView;
    private GroupAdapter adapter;
    private final List<Group> invitedGroups = new ArrayList<>();

    private FirebaseFirestore db;
    private String uid;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // 🔔 Theo dõi realtime tất cả nhóm, để phát hiện khi mình được mời
        db.collection("Groups").addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;

            for (DocumentChange change : snapshots.getDocumentChanges()) {
                if (change.getType() == DocumentChange.Type.MODIFIED) {
                    Group g = change.getDocument().toObject(Group.class);
                    g.setId(change.getDocument().getId());

                    if (g.getMembers() != null && g.getMembers().contains(uid)) {
                        showInviteNotification(g.getName(), g.getAdminEmail());
                    }
                }
            }
        });
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_invited_groups, container, false);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        recyclerView = view.findViewById(R.id.recyclerInvitedGroups);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new GroupAdapter(invitedGroups, group -> {
            Intent i = new Intent(getContext(), GroupDetailActivity.class);
            i.putExtra("groupId", group.getId());
            i.putExtra("groupName", group.getName());
            i.putExtra("adminId", group.getAdminId());
            i.putExtra("adminEmail", group.getAdminEmail());
            startActivity(i);
        });

        recyclerView.setAdapter(adapter);

        loadInvitedGroups();

        return view;
    }

    private void loadInvitedGroups() {
        // Lấy các nhóm mà user là thành viên nhưng KHÔNG phải admin
        db.collection("Groups")
                .whereArrayContains("members", uid)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null) {
                        Log.e("Firestore", "Lỗi tải nhóm được mời: ", e);
                        return;
                    }

                    invitedGroups.clear();
                    if (snapshots != null) {
                        for (QueryDocumentSnapshot doc : snapshots) {
                            Group g = doc.toObject(Group.class);
                            g.setId(doc.getId());

                            if (!uid.equals(g.getAdminId())) {
                                invitedGroups.add(g);
                            }
                        }
                    }

                    adapter.notifyDataSetChanged();
                });
    }

    private void showInviteNotification(String groupName, String adminEmail) {
        Context context = getContext();
        if (context == null) return;

        String channelId = "invite_channel";
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Tạo kênh thông báo (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Lời mời nhóm",
                    NotificationManager.IMPORTANCE_HIGH
            );
            nm.createNotificationChannel(channel);
        }

        // Intent mở app khi nhấn thông báo
        Intent intent = new Intent(context, GroupTaskActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notify)
                .setContentTitle("Lời mời tham gia nhóm")
                .setContentText("Bạn đã được mời vào nhóm \"" + groupName + "\" bởi " + adminEmail)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build();

        nm.notify((int) System.currentTimeMillis(), notification);
    }

}
