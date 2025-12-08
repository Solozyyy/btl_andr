package com.example.btlandr.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.btlandr.R;
import com.example.btlandr.model.ChatMessage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends ArrayAdapter<ChatMessage> {
    private Context context;
    private List<ChatMessage> messages;
    private String currentUserId;

    public ChatAdapter(Context context, int resource, List<ChatMessage> messages, String currentUserId) {
        super(context, resource, messages);
        this.context = context;
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_chat_message, parent, false);
        }

        ChatMessage message = messages.get(position);

        TextView senderName = convertView.findViewById(R.id.senderName);
        TextView messageText = convertView.findViewById(R.id.messageText);
        TextView messageTime = convertView.findViewById(R.id.messageTime);
        LinearLayout messageBubble = convertView.findViewById(R.id.messageBubble);

        senderName.setText(message.getSenderName());

        // Format time
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeString = timeFormat.format(new Date(message.getTimestamp()));
        messageTime.setText(timeString);

        // Check if this is a file or image message
        if (message.getFileUrl() != null && !message.getFileUrl().isEmpty()) {
            messageText.setVisibility(View.GONE);
            
            // Remove existing ImageView if any
            for (int i = messageBubble.getChildCount() - 1; i >= 0; i--) {
                View child = messageBubble.getChildAt(i);
                if (child instanceof ImageView) {
                    messageBubble.removeViewAt(i);
                }
            }

            if ("image".equals(message.getFileType())) {
                // Display image
                ImageView imageView = new ImageView(context);
                imageView.setLayoutParams(new LinearLayout.LayoutParams(200, 200));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                messageBubble.addView(imageView, 0);

                Glide.with(context)
                        .load(message.getFileUrl())
                        .centerCrop()
                        .into(imageView);

                // Click to open full image
                imageView.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(message.getFileUrl()));
                    context.startActivity(intent);
                });
            } else if ("file".equals(message.getFileType())) {
                // Display file link
                messageText.setVisibility(View.VISIBLE);
                messageText.setText("📎 " + message.getMessage());
                messageText.setTextColor(0xFF2196F3);
                messageText.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(message.getFileUrl()));
                    context.startActivity(intent);
                });
            } else {
                messageText.setVisibility(View.VISIBLE);
                messageText.setText(message.getMessage());
            }
        } else {
            // Regular text message
            messageText.setVisibility(View.VISIBLE);
            messageText.setText(message.getMessage());
        }

        // Align based on sender
        boolean isCurrentUser = message.getSenderUid().equals(currentUserId);
        LinearLayout containerLayout = (LinearLayout) convertView;
        
        if (isCurrentUser) {
            containerLayout.setGravity(android.view.Gravity.END);
            messageBubble.setBackgroundColor(0xFF6C63FF);
            senderName.setTextColor(0xFFFFFFFF);
            messageText.setTextColor(0xFFFFFFFF);
            messageTime.setTextColor(0xFFCCCCCC);
        } else {
            containerLayout.setGravity(android.view.Gravity.START);
            messageBubble.setBackgroundColor(0xFFE8E8E8);
            senderName.setTextColor(0xFF666666);
            messageText.setTextColor(0xFF000000);
            messageTime.setTextColor(0xFF999999);
        }

        return convertView;
    }
}
