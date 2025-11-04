package com.example.btlandr;

public class Event {
    private String id;
    private String title;
    private String note;
    private long startTime;
    private long endTime;
    private String category;

    public Event() {}

    public Event(String title, String note, long startTime, long endTime, String category) {
        this.title = title;
        this.note = note;
        this.startTime = startTime;
        this.endTime = endTime;
        this.category = category;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getTitle() { return title; }
    public String getNote() { return note; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public String getCategory() { return category; }

    public void setTitle(String title) { this.title = title; }
    public void setNote(String note) { this.note = note; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
    public void setEndTime(long endTime) { this.endTime = endTime; }
    public void setCategory(String category) { this.category = category; }
}
