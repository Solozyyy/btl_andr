package com.example.btlandr;

public class Event {
    private String id;
    private String title;
    private long startTime;
    private long endTime;
    private String category;
    private String note;

    public Event() {} // Bắt buộc có cho Firestore

    public Event(String title, long startTime, long endTime, String category, String note) {
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.category = category;
        this.note = note;
    }

    // Getter Setter
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }

    public long getEndTime() { return endTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
