package com.example.btlandr.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

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
        messageText.setText(message.getMessage());

        // Format time
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String timeString = timeFormat.format(new Date(message.getTimestamp()));
        messageTime.setText(timeString);

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
