package com.example.btlandr;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.btlandr.adapter.ChatAdapter;
import com.example.btlandr.model.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupChatActivity extends AppCompatActivity {
    private String groupId;
    private String groupName;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private String currentUserName = "";

    private ListView messagesListView;
    private EditText messageInput;
    private ImageButton sendButton;
    private Button backButton;
    private TextView chatHeaderTitle;

    private ChatAdapter chatAdapter;
    private List<ChatMessage> messagesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_chat);

        // Get data from intent
        groupId = getIntent().getStringExtra("groupId");
        groupName = getIntent().getStringExtra("groupName");

        // Initialize Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        // Initialize views
        messagesListView = findViewById(R.id.messagesListView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        backButton = findViewById(R.id.backButton);
        chatHeaderTitle = findViewById(R.id.chatHeaderTitle);

        // Set header title
        chatHeaderTitle.setText(groupName);

        // Initialize message list and adapter
        messagesList = new ArrayList<>();
        chatAdapter = new ChatAdapter(this, R.layout.item_chat_message, messagesList, 
                currentUser != null ? currentUser.getUid() : "");
        messagesListView.setAdapter(chatAdapter);

        // Set back button
        backButton.setOnClickListener(v -> finish());

        // Set send button
        sendButton.setOnClickListener(v -> sendMessage());

        // Load user name
        if (currentUser != null) {
            loadUserName();
        }

        // Listen for group name changes
        listenForGroupNameChanges();
        
        // Listen for messages
        listenForMessages();
    }

    private void listenForGroupNameChanges() {
        db.collection("Groups").document(groupId)
                .addSnapshotListener((snapshot, e) -> {
                    if (e != null || snapshot == null || !snapshot.exists()) return;
                    
                    String newGroupName = snapshot.getString("groupName");
                    if (newGroupName != null && !newGroupName.equals(groupName)) {
                        groupName = newGroupName;
                        chatHeaderTitle.setText(groupName);
                    }
                });
    }

    private void loadUserName() {
        db.collection("UserAccount").document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        currentUserName = documentSnapshot.getString("username");
                        if (currentUserName == null) {
                            currentUserName = "Ẩn danh";
                        }
                    } else {
                        currentUserName = "Ẩn danh";
                    }
                })
                .addOnFailureListener(e -> {
                    currentUserName = "Ẩn danh";
                });
    }

    private void sendMessage() {
        String message = messageInput.getText().toString().trim();
        
        if (message.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tin nhắn", Toast.LENGTH_SHORT).show();
            return;
        }

        if (currentUser == null) {
            Toast.makeText(this, "Chưa đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderUid", currentUser.getUid());
        messageData.put("senderName", currentUserName);
        messageData.put("message", message);
        messageData.put("timestamp", System.currentTimeMillis());

        db.collection("Groups").document(groupId)
                .collection("messages")
                .add(messageData)
                .addOnSuccessListener(documentReference -> {
                    messageInput.setText("");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(GroupChatActivity.this, "Lỗi gửi tin nhắn", Toast.LENGTH_SHORT).show();
                });
    }

    private void listenForMessages() {
        db.collection("Groups").document(groupId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(GroupChatActivity.this, "Lỗi tải tin nhắn", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value != null) {
                        messagesList.clear();
                        for (QueryDocumentSnapshot doc : value) {
                            ChatMessage chatMessage = new ChatMessage();
                            chatMessage.setId(doc.getId());
                            chatMessage.setSenderUid(doc.getString("senderUid") != null ? doc.getString("senderUid") : "");
                            chatMessage.setSenderName(doc.getString("senderName") != null ? doc.getString("senderName") : "Ẩn danh");
                            chatMessage.setMessage(doc.getString("message") != null ? doc.getString("message") : "");
                            chatMessage.setTimestamp(doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0);
                            messagesList.add(chatMessage);
                        }
                        chatAdapter.notifyDataSetChanged();
                        // Scroll to bottom
                        if (messagesList.size() > 0) {
                            messagesListView.setSelection(messagesList.size() - 1);
                        }
                    }
                });
    }
}
