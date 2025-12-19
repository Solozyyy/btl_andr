package com.example.btlandr.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.btlandr.R;

import com.example.btlandr.adapter.ChatAdapter;
import com.example.btlandr.model.ChatMessage;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GroupChatActivity extends AppCompatActivity {
    private static final int PICK_FILE_REQUEST = 1;
    private static final int PICK_IMAGE_REQUEST = 2;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    private String groupId;
    private String groupName;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseStorage storage;
    private String currentUserName = "";

    private ListView messagesListView;
    private EditText messageInput;
    private ImageButton sendButton, attachFileButton, attachImageButton;
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
        storage = FirebaseStorage.getInstance();

        // Initialize views
        messagesListView = findViewById(R.id.messagesListView);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        attachFileButton = findViewById(R.id.attachFileButton);
        attachImageButton = findViewById(R.id.attachImageButton);
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

        // Set file/image buttons
        attachFileButton.setOnClickListener(v -> openFileChooser());
        attachImageButton.setOnClickListener(v -> openImageChooser());

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
                    if (e != null || snapshot == null || !snapshot.exists())
                        return;

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
                            chatMessage
                                    .setSenderUid(doc.getString("senderUid") != null ? doc.getString("senderUid") : "");
                            chatMessage.setSenderName(
                                    doc.getString("senderName") != null ? doc.getString("senderName") : "Ẩn danh");
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

    private void openFileChooser() {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Chọn file"), PICK_FILE_REQUEST);
    }

    private void openImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Chọn ảnh"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri fileUri = data.getData();
            uploadFile(fileUri);
        } else if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null
                && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadImage(imageUri);
        }
    }

    private void uploadFile(Uri fileUri) {
        try {
            // Get file size
            android.database.Cursor cursor = getContentResolver().query(fileUri, null, null, null, null);
            int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
            cursor.moveToFirst();
            long fileSize = cursor.getLong(sizeIndex);
            cursor.close();

            if (fileSize > MAX_FILE_SIZE) {
                Toast.makeText(this, "File quá lớn (max 10MB)", Toast.LENGTH_SHORT).show();
                return;
            }

            // Get file name
            String fileName = new java.io.File(fileUri.getPath()).getName();
            if (fileName == null || fileName.isEmpty()) {
                fileName = "file_" + System.currentTimeMillis();
            }

            Toast.makeText(this, "Đang upload file...", Toast.LENGTH_SHORT).show();

            StorageReference fileRef = storage.getReference()
                    .child("chat_files")
                    .child(groupId)
                    .child(UUID.randomUUID().toString() + "_" + fileName);

            fileRef.putFile(fileUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            sendFileMessage(fileUri, uri.toString(), "file");
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(GroupChatActivity.this, "Lỗi upload: " + e.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadImage(Uri imageUri) {
        try {
            // Get file size
            android.database.Cursor cursor = getContentResolver().query(imageUri, null, null, null, null);
            int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
            cursor.moveToFirst();
            long fileSize = cursor.getLong(sizeIndex);
            cursor.close();

            if (fileSize > MAX_FILE_SIZE) {
                Toast.makeText(this, "Ảnh quá lớn (max 10MB)", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Đang upload ảnh...", Toast.LENGTH_SHORT).show();

            StorageReference imageRef = storage.getReference()
                    .child("chat_images")
                    .child(groupId)
                    .child(UUID.randomUUID().toString() + ".jpg");

            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            sendFileMessage(imageUri, uri.toString(), "image");
                        });
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(GroupChatActivity.this, "Lỗi upload: " + e.getMessage(), Toast.LENGTH_SHORT)
                                .show();
                    });
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void sendFileMessage(Uri fileUri, String downloadUrl, String fileType) {
        String fileName = "File";
        if (fileType.equals("file")) {
            fileName = new java.io.File(fileUri.getPath()).getName();
            if (fileName == null || fileName.isEmpty()) {
                fileName = "file_" + System.currentTimeMillis();
            }
        } else {
            fileName = "Image";
        }

        Map<String, Object> messageData = new HashMap<>();
        messageData.put("senderUid", currentUser.getUid());
        messageData.put("senderName", currentUserName);
        messageData.put("message", fileName);
        messageData.put("fileUrl", downloadUrl);
        messageData.put("fileType", fileType);
        messageData.put("timestamp", System.currentTimeMillis());

        db.collection("Groups").document(groupId)
                .collection("messages")
                .add(messageData)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(GroupChatActivity.this, "Đã gửi " + fileType, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(GroupChatActivity.this, "Lỗi gửi tin nhắn", Toast.LENGTH_SHORT).show();
                });
    }
}
