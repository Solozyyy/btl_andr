package com.example.btlandr.model;

public class ChatMessage {
    private String id;
    private String senderUid;
    private String senderName;
    private String message;
    private long timestamp;

    public ChatMessage() {
    }

    public ChatMessage(String id, String senderUid, String senderName, String message, long timestamp) {
        this.id = id;
        this.senderUid = senderUid;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSenderUid() {
        return senderUid;
    }

    public void setSenderUid(String senderUid) {
        this.senderUid = senderUid;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
