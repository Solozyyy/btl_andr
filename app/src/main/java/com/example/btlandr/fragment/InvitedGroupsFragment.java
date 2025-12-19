package com.example.btlandr.fragment;

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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.btlandr.R;
import com.example.btlandr.activity.GroupDetailActivity;
import com.example.btlandr.activity.GroupTaskActivity;
import com.example.btlandr.adapter.GroupAdapter;
import com.example.btlandr.model.Group;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.*;

public class InvitedGroupsFragment extends Fragment {

    private RecyclerView recyclerView;
    private GroupAdapter adapter;
    private final List<Group> invitedGroups = new ArrayList<>();

    private FirebaseFirestore db;
    private String uid;

    // Lưu danh sách members cũ theo group
    private final Map<String, List<String>> lastMembersMap = new HashMap<>();

    // Chặn thông báo ở lần load đầu
    private boolean initialLoaded = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Lắng nghe mọi thay đổi trong Groups
        db.collection("Groups").addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null)
                return;

            for (QueryDocumentSnapshot doc : snapshots) {

                String groupId = doc.getId();
                Group newGroup = doc.toObject(Group.class);
                newGroup.setId(groupId);

                List<String> newMembers = newGroup.getMembers();
                if (newMembers == null)
                    newMembers = new ArrayList<>();

                List<String> oldMembers = lastMembersMap.get(groupId);
                if (oldMembers == null)
                    oldMembers = new ArrayList<>();

                boolean wasNotMember = !oldMembers.contains(uid);
                boolean isNowMember = newMembers.contains(uid);

                // 🔥 Chỉ thông báo từ lần thứ 2 trở đi
                if (initialLoaded) {
                    if (wasNotMember && isNowMember) {
                        if (!uid.equals(newGroup.getAdminId())) {
                            showInviteNotification(newGroup.getName(), newGroup.getAdminEmail());
                        }
                    }
                }

                // Lưu lại members mới
                lastMembersMap.put(groupId, new ArrayList<>(newMembers));
            }

            // Kích hoạt sau lần load đầu tiên
            initialLoaded = true;
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_invited_groups, container, false);

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
        if (context == null)
            return;

        String channelId = "invite_channel";
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Lời mời nhóm",
                    NotificationManager.IMPORTANCE_HIGH);
            nm.createNotificationChannel(channel);
        }

        Intent intent = new Intent(context, GroupTaskActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Android 8.0+ dùng channelId
            notification = new Notification.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_notify)
                    .setContentTitle("Lời mời tham gia nhóm")
                    .setContentText("Bạn đã được mời vào nhóm \"" + groupName + "\" bởi " + adminEmail)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build();
        } else {
            // Android 7.1 trở xuống dùng Builder không có channelId
            notification = new Notification.Builder(context)
                    .setSmallIcon(R.drawable.ic_notify)
                    .setContentTitle("Lời mời tham gia nhóm")
                    .setContentText("Bạn đã được mời vào nhóm \"" + groupName + "\" bởi " + adminEmail)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .build();
        }

        nm.notify((int) System.currentTimeMillis(), notification);
    }
}
